// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

//! Native user-presence verification for unlocking the current application process.

use anyhow::Result;

/// Whether the current platform can present its native biometric/user-presence prompt.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum BiometricAvailability {
    Available,
    Unavailable,
}

/// Result of an interactive native authentication request.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum BiometricOutcome {
    Verified,
    Canceled,
    Failed,
    Unavailable,
}

/// Checks native authentication capability without presenting a prompt.
pub fn biometric_availability() -> BiometricAvailability {
    platform::availability()
}

/// Requests native authentication. No password or biometric material crosses this boundary.
pub fn authenticate_biometric(
    reason: &str,
    native_window_handle: Option<isize>,
) -> Result<BiometricOutcome> {
    platform::authenticate(reason, native_window_handle)
}

#[cfg(target_os = "macos")]
mod platform {
    use super::{BiometricAvailability, BiometricOutcome};
    use anyhow::{Context, Result};
    use objc2::{class, msg_send};
    use objc2_foundation::{NSError, NSString};
    use std::sync::mpsc;

    // LAPolicy.deviceOwnerAuthenticationWithBiometrics. The application password remains
    // OxideTerm's fallback instead of asking macOS to substitute the account password.
    const BIOMETRIC_POLICY: i64 = 1;
    const LA_ERROR_USER_CANCEL: isize = -2;
    const LA_ERROR_USER_FALLBACK: isize = -3;
    const LA_ERROR_SYSTEM_CANCEL: isize = -4;
    const LA_ERROR_APP_CANCEL: isize = -9;

    #[link(name = "LocalAuthentication", kind = "framework")]
    unsafe extern "C" {}

    pub(super) fn availability() -> BiometricAvailability {
        unsafe {
            let context: *mut objc2::runtime::AnyObject = msg_send![class!(LAContext), new];
            if context.is_null() {
                return BiometricAvailability::Unavailable;
            }
            let mut error: *mut NSError = std::ptr::null_mut();
            let available: objc2::runtime::Bool = msg_send![
                context,
                canEvaluatePolicy: BIOMETRIC_POLICY,
                error: &mut error
            ];
            let _: () = msg_send![context, release];
            if available.as_bool() {
                BiometricAvailability::Available
            } else {
                BiometricAvailability::Unavailable
            }
        }
    }

    pub(super) fn authenticate(
        reason: &str,
        _native_window_handle: Option<isize>,
    ) -> Result<BiometricOutcome> {
        let (sender, receiver) = mpsc::channel();
        let context = unsafe {
            let context: *mut objc2::runtime::AnyObject = msg_send![class!(LAContext), new];
            if context.is_null() {
                anyhow::bail!("failed to create the macOS biometric authentication context");
            }
            let mut error: *mut NSError = std::ptr::null_mut();
            let available: objc2::runtime::Bool = msg_send![
                context,
                canEvaluatePolicy: BIOMETRIC_POLICY,
                error: &mut error
            ];
            if !available.as_bool() {
                let _: () = msg_send![context, release];
                return Ok(BiometricOutcome::Unavailable);
            }

            let reason = NSString::from_str(reason);
            let reply =
                block2::RcBlock::new(move |success: objc2::runtime::Bool, error: *mut NSError| {
                    let outcome = if success.as_bool() {
                        BiometricOutcome::Verified
                    } else if error.is_null() {
                        BiometricOutcome::Failed
                    } else {
                        let code: isize = msg_send![error, code];
                        if matches!(
                            code,
                            LA_ERROR_USER_CANCEL
                                | LA_ERROR_USER_FALLBACK
                                | LA_ERROR_SYSTEM_CANCEL
                                | LA_ERROR_APP_CANCEL
                        ) {
                            BiometricOutcome::Canceled
                        } else {
                            BiometricOutcome::Failed
                        }
                    };
                    let _ = sender.send(outcome);
                });
            let _: () = msg_send![
                context,
                evaluatePolicy: BIOMETRIC_POLICY,
                localizedReason: &*reason,
                reply: &*reply
            ];
            context
        };

        let outcome = receiver
            .recv()
            .context("macOS biometric authentication response channel closed")?;
        // The evaluation callback has completed, so the context can no longer outlive Rust state.
        unsafe {
            let _: () = msg_send![context, release];
        }
        Ok(outcome)
    }
}

#[cfg(target_os = "windows")]
mod platform {
    use super::{BiometricAvailability, BiometricOutcome};
    use anyhow::{Context, Result};
    use windows::{
        Security::Credentials::UI::{
            UserConsentVerificationResult, UserConsentVerifier, UserConsentVerifierAvailability,
        },
        Win32::{
            Foundation::HWND,
            System::WinRT::{
                IUserConsentVerifierInterop, RO_INIT_MULTITHREADED, RoInitialize, RoUninitialize,
            },
        },
    };
    use windows_core::HSTRING;
    use windows_future::IAsyncOperation;

    struct WinRtApartment {
        initialized_here: bool,
    }

    impl Drop for WinRtApartment {
        fn drop(&mut self) {
            if self.initialized_here {
                // Every successful RoInitialize call, including S_FALSE, owns one matching release.
                unsafe { RoUninitialize() };
            }
        }
    }

    fn initialize_winrt() -> Result<WinRtApartment> {
        // Each blocking authentication task owns its WinRT apartment initialization.
        match unsafe { RoInitialize(RO_INIT_MULTITHREADED) } {
            Ok(()) => Ok(WinRtApartment {
                initialized_here: true,
            }),
            Err(error) => {
                // RPC_E_CHANGED_MODE means the thread already owns a different valid apartment.
                if error.code().0 as u32 == 0x8001_0106 {
                    Ok(WinRtApartment {
                        initialized_here: false,
                    })
                } else {
                    Err(error).context("failed to initialize Windows Runtime")
                }
            }
        }
    }

    pub(super) fn availability() -> BiometricAvailability {
        let Ok(_apartment) = initialize_winrt() else {
            return BiometricAvailability::Unavailable;
        };
        // Check the desktop HWND interop as well as Hello enrollment. Windows 10 may expose
        // UserConsentVerifier while lacking the desktop interop introduced in Windows 11.
        if windows_core::factory::<UserConsentVerifier, IUserConsentVerifierInterop>().is_err() {
            return BiometricAvailability::Unavailable;
        }
        match UserConsentVerifier::CheckAvailabilityAsync().and_then(|operation| operation.get()) {
            Ok(UserConsentVerifierAvailability::Available) => BiometricAvailability::Available,
            _ => BiometricAvailability::Unavailable,
        }
    }

    pub(super) fn authenticate(
        reason: &str,
        native_window_handle: Option<isize>,
    ) -> Result<BiometricOutcome> {
        let _apartment = initialize_winrt()?;
        let Some(native_window_handle) = native_window_handle.filter(|handle| *handle != 0) else {
            return Ok(BiometricOutcome::Unavailable);
        };
        let interop = windows_core::factory::<UserConsentVerifier, IUserConsentVerifierInterop>()
            .context("Windows Hello desktop interop is unavailable")?;
        let operation: IAsyncOperation<UserConsentVerificationResult> = unsafe {
            interop.RequestVerificationForWindowAsync(
                HWND(native_window_handle as *mut std::ffi::c_void),
                &HSTRING::from(reason),
            )
        }
        .context("failed to start Windows Hello verification")?;
        let result = operation
            .get()
            .context("Windows Hello verification did not complete")?;
        Ok(match result {
            UserConsentVerificationResult::Verified => BiometricOutcome::Verified,
            UserConsentVerificationResult::Canceled => BiometricOutcome::Canceled,
            UserConsentVerificationResult::DeviceNotPresent
            | UserConsentVerificationResult::NotConfiguredForUser
            | UserConsentVerificationResult::DisabledByPolicy => BiometricOutcome::Unavailable,
            _ => BiometricOutcome::Failed,
        })
    }
}

#[cfg(not(any(target_os = "macos", target_os = "windows")))]
mod platform {
    use super::{BiometricAvailability, BiometricOutcome};
    use anyhow::Result;

    pub(super) fn availability() -> BiometricAvailability {
        BiometricAvailability::Unavailable
    }

    pub(super) fn authenticate(
        _reason: &str,
        _native_window_handle: Option<isize>,
    ) -> Result<BiometricOutcome> {
        Ok(BiometricOutcome::Unavailable)
    }
}
