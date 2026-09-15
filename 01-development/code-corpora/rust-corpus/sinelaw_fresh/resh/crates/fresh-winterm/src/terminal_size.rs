//! Terminal size query via Windows console screen buffer.

use std::io;

use windows_sys::Win32::System::Console::{
    GetConsoleScreenBufferInfo, GetStdHandle, CONSOLE_SCREEN_BUFFER_INFO, STD_OUTPUT_HANDLE,
};

/// Terminal dimensions in character cells.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct TerminalSize {
    pub cols: u16,
    pub rows: u16,
}

/// Get the current terminal size by querying the console screen buffer.
pub fn get_terminal_size() -> io::Result<TerminalSize> {
    unsafe {
        let handle = GetStdHandle(STD_OUTPUT_HANDLE);
        let mut info: CONSOLE_SCREEN_BUFFER_INFO = std::mem::zeroed();
        if GetConsoleScreenBufferInfo(handle, &mut info) == 0 {
            return Err(io::Error::last_os_error());
        }
        let cols = (info.srWindow.Right - info.srWindow.Left + 1) as u16;
        let rows = (info.srWindow.Bottom - info.srWindow.Top + 1) as u16;
        Ok(TerminalSize { cols, rows })
    }
}
