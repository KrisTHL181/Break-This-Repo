use crate::macos::{nsstring, nsstring_to_str};
use crate::ClipboardData;
use cocoa::appkit::{NSFilenamesPboardType, NSPasteboard, NSStringPboardType};
use cocoa::base::*;
use cocoa::foundation::NSArray;
use objc::*;
#[cfg(unix)]
use std::os::unix::fs::OpenOptionsExt;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

const PNG_PASTEBOARD_TYPE: &str = "public.png";
const TIFF_PASTEBOARD_TYPE: &str = "public.tiff";
const MAX_CLIPBOARD_IMAGE_BYTES: usize = 32 * 1024 * 1024;
const CLIPBOARD_IMAGE_DIR: &str = "clipboard-images";
const CLIPBOARD_IMAGE_FILE_PREFIX: &str = "clipboard-image-";
const MAX_CLIPBOARD_IMAGE_FILES: usize = 128;
const CLIPBOARD_IMAGE_RETENTION_SECS: u64 = 24 * 60 * 60;
static CLIPBOARD_IMAGE_CLEANUP_RUNNING: AtomicBool = AtomicBool::new(false);

pub struct Clipboard {
    pasteboard: id,
}

impl Clipboard {
    pub fn new() -> Self {
        let pasteboard = unsafe { NSPasteboard::generalPasteboard(nil) };
        if pasteboard.is_null() {
            panic!("NSPasteboard::generalPasteboard returned null");
        }
        Clipboard { pasteboard }
    }

    fn read_pasteboard_data(&self, uti: &str) -> anyhow::Result<Option<Vec<u8>>> {
        unsafe {
            let data: id = msg_send![self.pasteboard, dataForType:*nsstring(uti)];
            if data.is_null() {
                return Ok(None);
            }

            let len: usize = msg_send![data, length];
            if len == 0 {
                return Ok(None);
            }
            anyhow::ensure!(
                len <= MAX_CLIPBOARD_IMAGE_BYTES,
                "clipboard image exceeds {} bytes",
                MAX_CLIPBOARD_IMAGE_BYTES
            );

            let bytes: *const u8 = msg_send![data, bytes];
            anyhow::ensure!(!bytes.is_null(), "clipboard image bytes returned null");

            Ok(Some(std::slice::from_raw_parts(bytes, len).to_vec()))
        }
    }

    fn convert_tiff_to_png(&self, tiff_data: &[u8]) -> anyhow::Result<Vec<u8>> {
        const NS_BITMAP_IMAGE_FILE_TYPE_PNG: usize = 4;

        unsafe {
            let tiff_nsdata: id = msg_send![
                class!(NSData),
                dataWithBytes:tiff_data.as_ptr()
                length:tiff_data.len()
            ];
            anyhow::ensure!(
                !tiff_nsdata.is_null(),
                "failed to create NSData for clipboard TIFF image"
            );

            let image_rep: id = msg_send![class!(NSBitmapImageRep), imageRepWithData:tiff_nsdata];
            anyhow::ensure!(
                !image_rep.is_null(),
                "failed to decode clipboard TIFF image"
            );

            let png_data: id = msg_send![
                image_rep,
                representationUsingType:NS_BITMAP_IMAGE_FILE_TYPE_PNG
                properties:nil
            ];
            anyhow::ensure!(
                !png_data.is_null(),
                "failed to encode clipboard TIFF image as PNG"
            );

            let len: usize = msg_send![png_data, length];
            anyhow::ensure!(
                len <= MAX_CLIPBOARD_IMAGE_BYTES,
                "clipboard PNG image exceeds {} bytes",
                MAX_CLIPBOARD_IMAGE_BYTES
            );

            let bytes: *const u8 = msg_send![png_data, bytes];
            anyhow::ensure!(!bytes.is_null(), "clipboard PNG bytes returned null");

            Ok(std::slice::from_raw_parts(bytes, len).to_vec())
        }
    }

    fn ensure_png_pasteboard_data(&self, png_data: &[u8]) -> anyhow::Result<()> {
        unsafe {
            let _: isize = msg_send![
                self.pasteboard,
                addTypes:NSArray::arrayWithObject(nil, *nsstring(PNG_PASTEBOARD_TYPE))
                owner:nil
            ];

            let png_nsdata: id = msg_send![
                class!(NSData),
                dataWithBytes:png_data.as_ptr()
                length:png_data.len()
            ];
            anyhow::ensure!(
                !png_nsdata.is_null(),
                "failed to create NSData for clipboard PNG image"
            );

            let success: BOOL = msg_send![
                self.pasteboard,
                setData:png_nsdata
                forType:*nsstring(PNG_PASTEBOARD_TYPE)
            ];
            anyhow::ensure!(success == YES, "failed to publish PNG clipboard image");
        }

        Ok(())
    }

    fn read_image_data(&self) -> anyhow::Result<Option<(Vec<u8>, &'static str)>> {
        if let Some(png_data) = self.read_pasteboard_data(PNG_PASTEBOARD_TYPE)? {
            return Ok(Some((png_data, "png")));
        }

        if let Some(tiff_data) = self.read_pasteboard_data(TIFF_PASTEBOARD_TYPE)? {
            match self.convert_tiff_to_png(&tiff_data) {
                Ok(png_data) => {
                    if let Err(err) = self.ensure_png_pasteboard_data(&png_data) {
                        log::warn!(
                            "failed to add PNG clipboard flavor alongside TIFF image: {err:#}"
                        );
                    }
                    return Ok(Some((png_data, "png")));
                }
                Err(err) => {
                    log::warn!(
                        "failed to normalize clipboard TIFF image to PNG, using TIFF as-is: {err:#}"
                    );
                    return Ok(Some((tiff_data, "tiff")));
                }
            }
        }

        Ok(None)
    }

    fn write_image_to_runtime_dir(
        &self,
        image_data: &[u8],
        extension: &str,
    ) -> anyhow::Result<PathBuf> {
        let dir = config::RUNTIME_DIR.join(CLIPBOARD_IMAGE_DIR);
        config::create_user_owned_dirs(&dir)?;
        // Spawn cleanup in background to avoid blocking paste operation
        if CLIPBOARD_IMAGE_CLEANUP_RUNNING
            .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
            .is_ok()
        {
            let dir_clone = dir.clone();
            promise::spawn::spawn(async move {
                if let Err(err) = Self::cleanup_runtime_image_dir_static(&dir_clone) {
                    log::warn!(
                        "failed to prune clipboard image cache at {}: {err:#}",
                        dir_clone.display()
                    );
                }
                CLIPBOARD_IMAGE_CLEANUP_RUNNING.store(false, Ordering::Release);
            })
            .detach();
        }

        let pid = std::process::id();
        for attempt in 0..64u32 {
            let now = SystemTime::now().duration_since(UNIX_EPOCH)?.as_nanos();
            let file_name =
                format!("{CLIPBOARD_IMAGE_FILE_PREFIX}{pid}-{now}-{attempt}.{extension}");
            let path = dir.join(file_name);

            let mut options = std::fs::OpenOptions::new();
            options.write(true).create_new(true);
            #[cfg(unix)]
            options.mode(0o600);

            match options.open(&path) {
                Ok(mut file) => {
                    use std::io::Write;
                    file.write_all(image_data)?;
                    return Ok(path);
                }
                Err(err) if err.kind() == std::io::ErrorKind::AlreadyExists => continue,
                Err(err) => return Err(err.into()),
            }
        }

        anyhow::bail!("failed to allocate unique clipboard image path")
    }

    fn cleanup_runtime_image_dir_static(dir: &Path) -> anyhow::Result<()> {
        let retention = Duration::from_secs(CLIPBOARD_IMAGE_RETENTION_SECS);
        let now = SystemTime::now();
        let mut retained = Vec::new();

        for entry in std::fs::read_dir(dir)? {
            let entry = match entry {
                Ok(entry) => entry,
                Err(err) => {
                    log::warn!(
                        "failed to list clipboard image cache entry in {}: {err:#}",
                        dir.display()
                    );
                    continue;
                }
            };

            let path = entry.path();
            if !path.is_file() {
                continue;
            }

            let Some(file_name) = path.file_name().and_then(|name| name.to_str()) else {
                continue;
            };
            if !file_name.starts_with(CLIPBOARD_IMAGE_FILE_PREFIX) {
                continue;
            }

            let metadata = match entry.metadata() {
                Ok(metadata) => metadata,
                Err(err) => {
                    log::warn!(
                        "failed to read metadata for clipboard image {}: {err:#}",
                        path.display()
                    );
                    continue;
                }
            };
            let modified = metadata.modified().unwrap_or(UNIX_EPOCH);
            let expired = now
                .duration_since(modified)
                .map(|elapsed| elapsed > retention)
                .unwrap_or(false);
            if expired {
                if let Err(err) = std::fs::remove_file(&path) {
                    if err.kind() != std::io::ErrorKind::NotFound {
                        log::warn!(
                            "failed to remove expired clipboard image {}: {err:#}",
                            path.display()
                        );
                    }
                }
                continue;
            }

            retained.push((modified, path));
        }

        if retained.len() <= MAX_CLIPBOARD_IMAGE_FILES {
            return Ok(());
        }

        retained.sort_by_key(|(modified, _)| *modified);
        let remove_count = retained.len().saturating_sub(MAX_CLIPBOARD_IMAGE_FILES);
        for (_, path) in retained.into_iter().take(remove_count) {
            if let Err(err) = std::fs::remove_file(&path) {
                if err.kind() != std::io::ErrorKind::NotFound {
                    log::warn!(
                        "failed to trim clipboard image cache file {}: {err:#}",
                        path.display()
                    );
                }
            }
        }

        Ok(())
    }

    pub fn read_data(&self) -> anyhow::Result<ClipboardData> {
        unsafe {
            let plist = self.pasteboard.propertyListForType(NSFilenamesPboardType);
            if !plist.is_null() {
                let mut filenames = vec![];
                for i in 0..plist.count() {
                    filenames.push(PathBuf::from(nsstring_to_str(plist.objectAtIndex(i))));
                }
                return Ok(ClipboardData::Files(filenames));
            }
            let s = self.pasteboard.stringForType(NSStringPboardType);
            if !s.is_null() {
                let str = nsstring_to_str(s);
                return Ok(ClipboardData::Text(str.to_string()));
            }
        }

        if let Some((image_data, extension)) = self.read_image_data()? {
            let path = self.write_image_to_runtime_dir(&image_data, extension)?;
            return Ok(ClipboardData::Image(path));
        }

        anyhow::bail!("pasteboard read returned empty");
    }

    pub fn read(&self) -> anyhow::Result<String> {
        match self.read_data()? {
            ClipboardData::Text(text) => Ok(text),
            ClipboardData::Image(_) => Ok(String::new()),
            ClipboardData::Files(paths) => {
                let quoted = paths
                    .iter()
                    .map(|path| {
                        let path_str = path.to_string_lossy().to_string();
                        match shlex::try_quote(&path_str) {
                            Ok(quoted) => quoted.into_owned(),
                            Err(err) => {
                                log::warn!(
                                    "Failed to quote path {:?} for clipboard read: {}. Using as-is.",
                                    path_str, err
                                );
                                path_str
                            }
                        }
                    })
                    .collect::<Vec<_>>()
                    .join(" ");
                Ok(quoted)
            }
        }
    }

    pub fn write(&mut self, data: String) -> anyhow::Result<()> {
        unsafe {
            self.pasteboard.clearContents();
            let success: BOOL = self
                .pasteboard
                .writeObjects(NSArray::arrayWithObject(nil, *nsstring(&data)));
            anyhow::ensure!(success == YES, "pasteboard write returned false");
            Ok(())
        }
    }
}
