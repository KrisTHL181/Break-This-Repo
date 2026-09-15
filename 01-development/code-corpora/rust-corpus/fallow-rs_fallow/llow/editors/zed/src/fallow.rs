use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use std::fs;
use std::path::{Path, PathBuf};

use zed_extension_api::{
    self as zed, DownloadedFileType, LanguageServerId,
    LanguageServerInstallationStatus as InstallStatus, Result, make_file_executable,
    set_language_server_installation_status, settings::LspSettings,
};

const LANGUAGE_SERVER_ID: &str = "fallow";
const RELEASE_REPOSITORY: &str = "fallow-rs/fallow";
const BINARY_BASENAME: &str = "fallow-lsp";
const MANAGED_DIR_PREFIX: &str = "fallow-";
const SIGNATURE_SUFFIX: &str = ".sig";
const BINARY_SIGNING_VERIFY_KEY: [u8; 32] = [
    131, 78, 111, 215, 115, 51, 230, 238, 223, 119, 147, 71, 199, 16, 172, 180, 3, 210, 216, 35,
    77, 85, 159, 94, 215, 200, 126, 85, 42, 222, 11, 209,
];

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum Platform {
    DarwinAarch64,
    DarwinX8664,
    LinuxAarch64Gnu,
    LinuxX8664Gnu,
    WindowsX8664,
}

impl Platform {
    fn current() -> Result<Self> {
        Self::from_parts(zed::current_platform().0, zed::current_platform().1)
    }

    fn from_parts(os: zed::Os, arch: zed::Architecture) -> Result<Self> {
        match (os, arch) {
            (zed::Os::Mac, zed::Architecture::Aarch64) => Ok(Self::DarwinAarch64),
            (zed::Os::Mac, zed::Architecture::X8664) => Ok(Self::DarwinX8664),
            (zed::Os::Linux, zed::Architecture::Aarch64) => Ok(Self::LinuxAarch64Gnu),
            (zed::Os::Linux, zed::Architecture::X8664) => Ok(Self::LinuxX8664Gnu),
            (zed::Os::Windows, zed::Architecture::X8664) => Ok(Self::WindowsX8664),
            (_, zed::Architecture::X86) => {
                Err("32-bit x86 is not supported by Fallow release binaries".to_string())
            }
            _ => Err("This platform is not supported by the Fallow Zed extension".to_string()),
        }
    }

    fn release_asset_name(self) -> &'static str {
        match self {
            Self::DarwinAarch64 => "fallow-lsp-darwin-arm64",
            Self::DarwinX8664 => "fallow-lsp-darwin-x64",
            Self::LinuxAarch64Gnu => "fallow-lsp-linux-arm64-gnu",
            Self::LinuxX8664Gnu => "fallow-lsp-linux-x64-gnu",
            Self::WindowsX8664 => "fallow-lsp-win32-x64-msvc.exe",
        }
    }

    fn executable_name(self) -> &'static str {
        match self {
            Self::WindowsX8664 => "fallow-lsp.exe",
            _ => BINARY_BASENAME,
        }
    }

    fn local_binary_candidates(self) -> &'static [&'static str] {
        match self {
            Self::WindowsX8664 => &["fallow-lsp.cmd", "fallow-lsp.exe"],
            _ => &[BINARY_BASENAME],
        }
    }

    fn needs_executable_bit(self) -> bool {
        !matches!(self, Self::WindowsX8664)
    }
}

#[derive(Debug, Eq, PartialEq)]
struct ResolvedBinary {
    path: String,
    args: Vec<String>,
    env: Vec<(String, String)>,
}

#[derive(Default)]
struct FallowExtension {
    cached_binary_path: Option<String>,
}

impl FallowExtension {
    fn resolve_binary(
        &mut self,
        language_server_id: &LanguageServerId,
        worktree: &zed::Worktree,
    ) -> Result<ResolvedBinary> {
        let settings = LspSettings::for_worktree(language_server_id.as_ref(), worktree).ok();
        let args = settings
            .as_ref()
            .and_then(|value| value.binary.as_ref())
            .and_then(|value| value.arguments.clone())
            .unwrap_or_default();
        let env = settings
            .as_ref()
            .and_then(|value| value.binary.as_ref())
            .and_then(|value| value.env.clone())
            .map(|items| items.into_iter().collect())
            .unwrap_or_default();

        let path = if let Some(path) = settings
            .as_ref()
            .and_then(|value| value.binary.as_ref())
            .and_then(|value| value.path.clone())
        {
            ensure_binary_exists(&path)?;
            path
        } else if let Some(path) = find_local_workspace_binary(worktree, Platform::current()?) {
            path
        } else if let Some(path) = worktree.which(BINARY_BASENAME) {
            path
        } else {
            self.managed_binary_path(language_server_id)?
        };

        Ok(ResolvedBinary { path, args, env })
    }

    fn managed_binary_path(&mut self, language_server_id: &LanguageServerId) -> Result<String> {
        if let Some(path) = &self.cached_binary_path {
            let binary_path = Path::new(path);
            if fs::metadata(binary_path).is_ok_and(|metadata| metadata.is_file())
                && verify_binary_signature(binary_path).is_ok()
            {
                return Ok(path.clone());
            }
        }

        let platform = Platform::current()?;

        set_language_server_installation_status(
            language_server_id,
            &InstallStatus::CheckingForUpdate,
        );

        let release = zed::latest_github_release(
            RELEASE_REPOSITORY,
            zed::GithubReleaseOptions {
                require_assets: true,
                pre_release: false,
            },
        )?;

        let asset = release
            .assets
            .iter()
            .find(|asset| asset.name == platform.release_asset_name())
            .ok_or_else(|| {
                format!(
                    "No fallow-lsp asset found for {} in release {}",
                    platform.release_asset_name(),
                    release.version
                )
            })?;
        let signature_asset_name = format!("{}{}", platform.release_asset_name(), SIGNATURE_SUFFIX);
        let signature_asset = release
            .assets
            .iter()
            .find(|asset| asset.name == signature_asset_name)
            .ok_or_else(|| {
                format!(
                    "No fallow-lsp signature asset found for {} in release {}",
                    signature_asset_name, release.version
                )
            })?;

        let base_dir = extension_dir()?;
        let version_dir_name = managed_dir_name(&release.version);
        let version_dir = base_dir.join(&version_dir_name);
        let binary_path = managed_binary_path_for(&base_dir, &release.version, platform);
        let signature_path = binary_signature_path(&binary_path);
        let binary_path_string = path_to_string(binary_path.clone());
        let signature_path_string = path_to_string(signature_path.clone());

        if fs::metadata(&binary_path).is_ok_and(|metadata| metadata.is_file())
            && verify_binary_signature(&binary_path).is_ok()
        {
            self.cached_binary_path = Some(binary_path_string.clone());
            return Ok(binary_path_string);
        }

        remove_file_if_exists(&binary_path);
        remove_file_if_exists(&signature_path);

        fs::create_dir_all(&version_dir)
            .map_err(|error| format!("Failed to create managed binary directory: {error}"))?;

        set_language_server_installation_status(language_server_id, &InstallStatus::Downloading);
        zed::download_file(
            &asset.download_url,
            &binary_path_string,
            DownloadedFileType::Uncompressed,
        )
        .map_err(|error| format!("Failed to download fallow-lsp: {error}"))?;
        zed::download_file(
            &signature_asset.download_url,
            &signature_path_string,
            DownloadedFileType::Uncompressed,
        )
        .map_err(|error| format!("Failed to download fallow-lsp signature: {error}"))?;

        verify_binary_signature(&binary_path)
            .map_err(|error| format!("Failed to verify downloaded fallow-lsp: {error}"))?;

        if platform.needs_executable_bit() {
            make_file_executable(&binary_path_string)
                .map_err(|error| format!("Failed to make fallow-lsp executable: {error}"))?;
        }

        cleanup_stale_managed_dirs(&base_dir, &version_dir_name);

        self.cached_binary_path = Some(binary_path_string.clone());
        Ok(binary_path_string)
    }
}

impl zed::Extension for FallowExtension {
    fn new() -> Self {
        Self::default()
    }

    fn language_server_command(
        &mut self,
        language_server_id: &LanguageServerId,
        worktree: &zed::Worktree,
    ) -> Result<zed::Command> {
        if language_server_id.as_ref() != LANGUAGE_SERVER_ID {
            return Err(format!(
                "Unrecognized language server for Fallow: {language_server_id}"
            ));
        }

        let binary = self.resolve_binary(language_server_id, worktree)?;
        Ok(zed::Command {
            command: binary.path,
            args: binary.args,
            env: binary.env,
        })
    }

    fn language_server_initialization_options(
        &mut self,
        language_server_id: &LanguageServerId,
        worktree: &zed::Worktree,
    ) -> Result<Option<zed::serde_json::Value>> {
        let options = LspSettings::for_worktree(language_server_id.as_ref(), worktree)
            .ok()
            .and_then(|settings| settings.initialization_options.clone())
            .unwrap_or_default();
        Ok(Some(options))
    }
}

fn ensure_binary_exists(path: &str) -> Result<()> {
    if fs::metadata(path).is_ok_and(|metadata| metadata.is_file()) {
        Ok(())
    } else {
        Err(format!(
            "Configured fallow-lsp binary does not exist: {path}"
        ))
    }
}

fn find_local_workspace_binary(worktree: &zed::Worktree, platform: Platform) -> Option<String> {
    let root_path = worktree.root_path();
    let root = Path::new(&root_path);
    find_local_workspace_binary_path(root, platform).map(path_to_string)
}

fn find_local_workspace_binary_path(root: &Path, platform: Platform) -> Option<PathBuf> {
    let bin_dir = root.join("node_modules").join(".bin");
    for candidate in platform.local_binary_candidates() {
        let path = bin_dir.join(candidate);
        if path.is_file() {
            return Some(path);
        }
    }
    None
}

fn extension_dir() -> Result<PathBuf> {
    std::env::current_dir()
        .map_err(|error| format!("Failed to resolve the Zed extension directory: {error}"))
}

fn managed_dir_name(version: &str) -> String {
    format!("{MANAGED_DIR_PREFIX}{version}")
}

fn managed_binary_path_for(base_dir: &Path, version: &str, platform: Platform) -> PathBuf {
    base_dir
        .join(managed_dir_name(version))
        .join(platform.executable_name())
}

fn binary_signature_path(binary_path: &Path) -> PathBuf {
    let mut path = binary_path.as_os_str().to_os_string();
    path.push(SIGNATURE_SUFFIX);
    PathBuf::from(path)
}

fn verify_binary_signature(binary_path: &Path) -> Result<()> {
    verify_binary_signature_with_key(binary_path, &BINARY_SIGNING_VERIFY_KEY)
}

fn verify_binary_signature_with_key(binary_path: &Path, key_bytes: &[u8; 32]) -> Result<()> {
    let signature_path = binary_signature_path(binary_path);
    let signature_bytes = fs::read(&signature_path).map_err(|error| {
        format!(
            "Managed binary at {} is missing its signature file {}: {error}",
            binary_path.display(),
            signature_path.display()
        )
    })?;
    let signature_array: [u8; 64] = signature_bytes.as_slice().try_into().map_err(|_| {
        format!(
            "Managed signature file at {} is {} bytes; expected 64",
            signature_path.display(),
            signature_bytes.len()
        )
    })?;
    let signature = Signature::from_bytes(&signature_array);

    let key = VerifyingKey::from_bytes(key_bytes).map_err(|error| {
        format!("compiled-in binary-signing key is invalid: {error} (build-time bug)")
    })?;
    let binary_bytes = fs::read(binary_path).map_err(|error| {
        format!(
            "Failed to read managed binary at {} for signature verification: {error}",
            binary_path.display()
        )
    })?;

    key.verify(&binary_bytes, &signature).map_err(|error| {
        format!(
            "Managed binary at {} failed Ed25519 signature verification: {error}",
            binary_path.display()
        )
    })?;

    Ok(())
}

fn remove_file_if_exists(path: &Path) {
    if fs::metadata(path).is_ok() {
        let _ = fs::remove_file(path);
    }
}

fn cleanup_stale_managed_dirs(base_dir: &Path, keep_dir: &str) {
    let Ok(entries) = fs::read_dir(base_dir) else {
        return;
    };

    for entry in entries.flatten() {
        let path = entry.path();
        let Some(name) = entry.file_name().to_str().map(str::to_owned) else {
            continue;
        };

        if !should_remove_stale_managed_entry(&name, keep_dir) {
            continue;
        }

        if path.is_dir() {
            let _ = fs::remove_dir_all(path);
        } else {
            let _ = fs::remove_file(path);
        }
    }
}

fn should_remove_stale_managed_entry(name: &str, keep_dir: &str) -> bool {
    name.starts_with(MANAGED_DIR_PREFIX) && name != keep_dir
}

fn path_to_string(path: PathBuf) -> String {
    path.to_string_lossy().into_owned()
}

zed::register_extension!(FallowExtension);

#[cfg(test)]
mod tests {
    use super::{
        Platform, binary_signature_path, find_local_workspace_binary_path, managed_binary_path_for,
        should_remove_stale_managed_entry, verify_binary_signature_with_key,
    };
    use ed25519_dalek::{Signer, SigningKey};
    use std::fs;
    use std::path::PathBuf;
    use zed_extension_api::{Architecture, Os};

    #[test]
    fn maps_release_asset_names() {
        assert_eq!(
            Platform::from_parts(Os::Mac, Architecture::Aarch64)
                .expect("mac arm64 platform should resolve")
                .release_asset_name(),
            "fallow-lsp-darwin-arm64"
        );
        assert_eq!(
            Platform::from_parts(Os::Mac, Architecture::X8664)
                .expect("mac x64 platform should resolve")
                .release_asset_name(),
            "fallow-lsp-darwin-x64"
        );
        assert_eq!(
            Platform::from_parts(Os::Linux, Architecture::X8664)
                .expect("linux x64 platform should resolve")
                .release_asset_name(),
            "fallow-lsp-linux-x64-gnu"
        );
        assert_eq!(
            Platform::from_parts(Os::Windows, Architecture::X8664)
                .expect("windows x64 platform should resolve")
                .release_asset_name(),
            "fallow-lsp-win32-x64-msvc.exe"
        );
    }

    #[test]
    fn rejects_unsupported_x86() {
        let error = Platform::from_parts(Os::Linux, Architecture::X86)
            .expect_err("32-bit x86 should not be supported");
        assert!(error.contains("32-bit x86"), "unexpected error: {error}");
    }

    #[test]
    fn finds_local_workspace_binary_for_unix_and_windows() {
        let root = unique_temp_dir("fallow-zed-local-binary");
        let bin_dir = root.join("node_modules").join(".bin");
        fs::create_dir_all(&bin_dir).expect("failed to create node_modules/.bin");

        let unix_binary = bin_dir.join("fallow-lsp");
        fs::write(&unix_binary, "#!/bin/sh\n").expect("failed to write unix binary");
        assert_eq!(
            find_local_workspace_binary_path(&root, Platform::DarwinAarch64),
            Some(unix_binary)
        );

        let windows_binary = bin_dir.join("fallow-lsp.cmd");
        fs::write(&windows_binary, "@echo off\r\n").expect("failed to write windows binary");
        assert_eq!(
            find_local_workspace_binary_path(&root, Platform::WindowsX8664),
            Some(windows_binary)
        );

        fs::remove_dir_all(&root).expect("failed to clean temp dir");
    }

    #[test]
    fn stale_cleanup_only_targets_managed_dirs() {
        assert!(should_remove_stale_managed_entry(
            "fallow-v2.44.0",
            "fallow-v2.45.0"
        ));
        assert!(!should_remove_stale_managed_entry(
            "fallow-v2.45.0",
            "fallow-v2.45.0"
        ));
        assert!(!should_remove_stale_managed_entry(
            "other-extension",
            "fallow-v2.45.0"
        ));
    }

    #[test]
    fn managed_binary_path_is_absolute() {
        let base_dir = unique_temp_dir("fallow-zed-managed");
        let binary_path = managed_binary_path_for(&base_dir, "v2.45.0", Platform::WindowsX8664);

        assert!(
            binary_path.is_absolute(),
            "managed binary path should stay absolute so Zed can launch it from any worktree"
        );
        assert_eq!(
            binary_path.file_name().and_then(|name| name.to_str()),
            Some("fallow-lsp.exe")
        );
        assert_eq!(
            binary_path
                .parent()
                .and_then(|parent| parent.file_name())
                .and_then(|name| name.to_str()),
            Some("fallow-v2.45.0")
        );

        fs::remove_dir_all(&base_dir).expect("failed to clean temp dir");
    }

    #[test]
    fn verifies_valid_managed_binary_signature() {
        let root = unique_temp_dir("fallow-zed-signature-ok");
        let binary_path = root.join("fallow-lsp");
        fs::write(&binary_path, b"hello from fallow").expect("failed to write binary");

        let signing_key = SigningKey::from_bytes(&[7; 32]);
        let signature = signing_key.sign(b"hello from fallow");
        fs::write(binary_signature_path(&binary_path), signature.to_bytes())
            .expect("failed to write signature");

        let verifying_key = signing_key.verifying_key().to_bytes();
        verify_binary_signature_with_key(&binary_path, &verifying_key)
            .expect("signature should verify");

        fs::remove_dir_all(&root).expect("failed to clean temp dir");
    }

    #[test]
    fn rejects_invalid_managed_binary_signature() {
        let root = unique_temp_dir("fallow-zed-signature-bad");
        let binary_path = root.join("fallow-lsp");
        fs::write(&binary_path, b"hello from fallow").expect("failed to write binary");
        fs::write(binary_signature_path(&binary_path), [0_u8; 64])
            .expect("failed to write signature");

        let error = verify_binary_signature_with_key(&binary_path, &[9; 32])
            .expect_err("invalid signature should be rejected");
        assert!(
            error.contains("failed Ed25519 signature verification"),
            "unexpected error: {error}"
        );

        fs::remove_dir_all(&root).expect("failed to clean temp dir");
    }

    fn unique_temp_dir(prefix: &str) -> PathBuf {
        let path = std::env::temp_dir().join(format!(
            "{prefix}-{}-{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .expect("system time before unix epoch")
                .as_nanos()
        ));
        fs::create_dir_all(&path).expect("failed to create temp dir");
        path
    }
}
