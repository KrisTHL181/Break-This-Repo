#[cfg(windows)]
pub(in crate::workspace) const SETTINGS_EXTERNAL_BRIDGE_CREATE_NO_WINDOW: u32 = 0x08000000;

pub(in crate::workspace) fn open_path_external(path: &std::path::Path) -> std::io::Result<()> {
    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(path)
            .spawn()?
            .wait()?;
        return Ok(());
    }
    #[cfg(target_os = "windows")]
    {
        let mut command = std::process::Command::new("cmd");
        configure_settings_external_bridge(&mut command);
        command
            .args(["/C", "start", "", &path.to_string_lossy()])
            .spawn()?
            .wait()?;
        return Ok(());
    }
    #[cfg(all(unix, not(target_os = "macos")))]
    {
        std::process::Command::new("xdg-open")
            .arg(path)
            .spawn()?
            .wait()?;
        Ok(())
    }
}

pub(in crate::workspace) fn open_external_url(url: &str) -> std::io::Result<()> {
    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(url)
            .spawn()?
            .wait()?;
        return Ok(());
    }
    #[cfg(target_os = "windows")]
    {
        let mut command = std::process::Command::new("cmd");
        configure_settings_external_bridge(&mut command);
        command.args(["/C", "start", "", url]).spawn()?.wait()?;
        return Ok(());
    }
    #[cfg(all(unix, not(target_os = "macos")))]
    {
        std::process::Command::new("xdg-open")
            .arg(url)
            .spawn()?
            .wait()?;
        Ok(())
    }
}

#[cfg(target_os = "windows")]
pub(in crate::workspace) fn configure_settings_external_bridge(
    command: &mut std::process::Command,
) {
    use std::os::windows::process::CommandExt;

    // The external target may show UI, but the cmd.exe bridge should stay
    // hidden because the app only waits for it to hand off the open request.
    command.creation_flags(SETTINGS_EXTERNAL_BRIDGE_CREATE_NO_WINDOW);
}
