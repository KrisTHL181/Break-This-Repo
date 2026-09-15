use std::path::Path;

/// Document and media file extensions that should open with the macOS
/// default app instead of being forced into VS Code. Text and source files
/// are intentionally excluded so they still open in the editor.
pub const NON_TEXT_FILE_EXTENSIONS: &[&str] = &[
    "xls", "xlsx", "doc", "docx", "ppt", "pptx", "pages", "numbers", "key", "odt", "ods", "odp",
    "pdf", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "tif", "heic", "heif", "webp", "icns",
    "psd", "mp3", "m4a", "wav", "aac", "flac", "mp4", "mov", "avi", "mkv", "m4v", "webm", "zip",
    "dmg", "pkg", "rar", "7z", "gz", "tar", "bz2", "xz", "iso",
];

/// Returns true when the file extension is in the non-text whitelist.
pub fn has_non_text_extension(path: &Path) -> bool {
    path.extension()
        .and_then(|ext| ext.to_str())
        .map(|ext| NON_TEXT_FILE_EXTENSIONS.contains(&ext.to_ascii_lowercase().as_str()))
        .unwrap_or(false)
}

/// Returns true when Cmd+Click should route the file to the system default app.
pub fn should_open_with_default_app(path: &Path) -> bool {
    has_non_text_extension(path)
}

#[cfg(target_os = "macos")]
pub fn open_with_default_app(path: &Path) -> anyhow::Result<bool> {
    let status = std::process::Command::new("/usr/bin/open")
        .arg(path)
        .status()?;
    Ok(status.success())
}

#[cfg(test)]
mod tests {
    use super::{has_non_text_extension, NON_TEXT_FILE_EXTENSIONS};
    use std::path::Path;

    #[test]
    fn non_text_extension_whitelist_covers_issue_regression() {
        assert!(has_non_text_extension(Path::new("/tmp/test.xlsx")));
        assert!(has_non_text_extension(Path::new("/tmp/test.PDF")));
        assert!(!has_non_text_extension(Path::new("/tmp/demo.rs")));
        assert!(!has_non_text_extension(Path::new("/tmp/README")));
    }

    #[test]
    fn non_text_extension_whitelist_entries_are_lowercase() {
        for ext in NON_TEXT_FILE_EXTENSIONS {
            assert!(
                ext.chars().all(|c| !c.is_ascii_uppercase()),
                "extension `{ext}` should be lowercase",
                ext = ext
            );
        }
    }
}
