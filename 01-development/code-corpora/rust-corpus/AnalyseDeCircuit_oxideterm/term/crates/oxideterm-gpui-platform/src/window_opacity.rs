use gpui::Window;

pub const MIN_WINDOW_OPACITY: f32 = 0.5;
pub const MAX_WINDOW_OPACITY: f32 = 1.0;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum WindowOpacitySupport {
    Supported,
    Unsupported { reason: &'static str },
}

/// Applies whole-window opacity after bounding it to the product's readable range.
pub fn apply_window_opacity(window: &mut Window, opacity: f64) -> WindowOpacitySupport {
    imp::apply_window_opacity(window, normalized_window_opacity(opacity))
}

pub fn normalized_window_opacity(opacity: f64) -> f32 {
    if opacity.is_finite() {
        (opacity as f32).clamp(MIN_WINDOW_OPACITY, MAX_WINDOW_OPACITY)
    } else {
        MAX_WINDOW_OPACITY
    }
}

#[cfg(target_os = "macos")]
mod imp {
    use objc2_app_kit::NSView;
    use raw_window_handle::RawWindowHandle;

    use super::*;

    pub(super) fn apply_window_opacity(window: &mut Window, opacity: f32) -> WindowOpacitySupport {
        let Ok(handle) = raw_window_handle::HasWindowHandle::window_handle(window) else {
            return WindowOpacitySupport::Unsupported {
                reason: "window handle is unavailable",
            };
        };
        let RawWindowHandle::AppKit(handle) = handle.as_raw() else {
            return WindowOpacitySupport::Unsupported {
                reason: "window is not an AppKit window",
            };
        };
        // GPUI exposes its root NSView. Native window alpha fades chrome and
        // rendered content together without repainting every element.
        let view = unsafe { handle.ns_view.cast::<NSView>().as_ref() };
        let Some(native_window) = view.window() else {
            return WindowOpacitySupport::Unsupported {
                reason: "AppKit view is not attached to a window",
            };
        };
        native_window.setAlphaValue(opacity as f64);
        WindowOpacitySupport::Supported
    }
}

#[cfg(target_os = "windows")]
mod imp {
    use raw_window_handle::RawWindowHandle;
    use windows::Win32::{
        Foundation::{COLORREF, HWND},
        UI::WindowsAndMessaging::{
            GWL_EXSTYLE, GetWindowLongW, LWA_ALPHA, SetLayeredWindowAttributes, SetWindowLongW,
            WS_EX_LAYERED,
        },
    };

    use super::*;

    pub(super) fn apply_window_opacity(window: &mut Window, opacity: f32) -> WindowOpacitySupport {
        let Ok(handle) = raw_window_handle::HasWindowHandle::window_handle(window) else {
            return WindowOpacitySupport::Unsupported {
                reason: "window handle is unavailable",
            };
        };
        let RawWindowHandle::Win32(handle) = handle.as_raw() else {
            return WindowOpacitySupport::Unsupported {
                reason: "window is not a Win32 window",
            };
        };
        let hwnd = HWND(handle.hwnd.get() as *mut _);
        let alpha = (opacity * u8::MAX as f32).round() as u8;
        unsafe {
            // Layered window alpha is the supported Win32 path for fading the
            // entire native window, including custom GPUI chrome.
            let extended_style = GetWindowLongW(hwnd, GWL_EXSTYLE);
            if extended_style & WS_EX_LAYERED.0 as i32 == 0 {
                SetWindowLongW(hwnd, GWL_EXSTYLE, extended_style | WS_EX_LAYERED.0 as i32);
            }
            if SetLayeredWindowAttributes(hwnd, COLORREF(0), alpha, LWA_ALPHA).is_err() {
                return WindowOpacitySupport::Unsupported {
                    reason: "Win32 layered window opacity is unavailable",
                };
            }
        }
        WindowOpacitySupport::Supported
    }
}

#[cfg(any(target_os = "linux", target_os = "freebsd"))]
mod imp {
    use std::ffi::{CString, c_ulong};

    use raw_window_handle::{RawDisplayHandle, RawWindowHandle};
    use x11_dl::xlib;

    use super::*;

    pub(super) fn apply_window_opacity(window: &mut Window, opacity: f32) -> WindowOpacitySupport {
        let Ok(window_handle) = raw_window_handle::HasWindowHandle::window_handle(window) else {
            return WindowOpacitySupport::Unsupported {
                reason: "window handle is unavailable",
            };
        };
        let Ok(display_handle) = raw_window_handle::HasDisplayHandle::display_handle(window) else {
            return WindowOpacitySupport::Unsupported {
                reason: "display handle is unavailable",
            };
        };
        let RawWindowHandle::Xlib(window_handle) = window_handle.as_raw() else {
            return WindowOpacitySupport::Unsupported {
                reason: "whole-window opacity is unavailable on this Wayland compositor",
            };
        };
        let RawDisplayHandle::Xlib(display_handle) = display_handle.as_raw() else {
            return WindowOpacitySupport::Unsupported {
                reason: "window is not using an X11 display",
            };
        };
        let Some(display) = display_handle.display else {
            return WindowOpacitySupport::Unsupported {
                reason: "X11 display pointer is unavailable",
            };
        };
        let Ok(xlib) = xlib::Xlib::open() else {
            return WindowOpacitySupport::Unsupported {
                reason: "X11 client library is unavailable",
            };
        };
        let property_name = CString::new("_NET_WM_WINDOW_OPACITY")
            .expect("static X11 atom name must not contain NUL");
        // Xlib requires format-32 properties to be passed as native C longs,
        // even though the protocol serializes each value to 32 bits.
        let opacity_value = (opacity as f64 * u32::MAX as f64).round() as c_ulong;
        unsafe {
            let display = display.as_ptr() as *mut xlib::Display;
            let opacity_atom = (xlib.XInternAtom)(display, property_name.as_ptr(), 0);
            (xlib.XChangeProperty)(
                display,
                window_handle.window,
                opacity_atom,
                xlib::XA_CARDINAL,
                32,
                xlib::PropModeReplace,
                (&opacity_value as *const c_ulong).cast::<u8>(),
                1,
            );
            (xlib.XFlush)(display);
        }
        WindowOpacitySupport::Supported
    }
}

#[cfg(not(any(
    target_os = "macos",
    target_os = "windows",
    target_os = "linux",
    target_os = "freebsd"
)))]
mod imp {
    use super::*;

    pub(super) fn apply_window_opacity(
        _window: &mut Window,
        _opacity: f32,
    ) -> WindowOpacitySupport {
        WindowOpacitySupport::Unsupported {
            reason: "whole-window opacity is unsupported on this platform",
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalization_preserves_supported_values_and_safe_bounds() {
        assert_eq!(normalized_window_opacity(0.85), 0.85);
        assert_eq!(normalized_window_opacity(0.1), MIN_WINDOW_OPACITY);
        assert_eq!(normalized_window_opacity(1.5), MAX_WINDOW_OPACITY);
        assert_eq!(normalized_window_opacity(f64::NAN), MAX_WINDOW_OPACITY);
    }
}
