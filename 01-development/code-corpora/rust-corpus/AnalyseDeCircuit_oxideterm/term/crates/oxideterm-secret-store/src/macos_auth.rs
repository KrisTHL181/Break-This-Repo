use anyhow::{Context, Result};
use objc2::{class, msg_send};
use objc2_foundation::{NSError, NSString};
use std::sync::mpsc;

const DEVICE_OWNER_AUTHENTICATION_POLICY: i64 = 2;

#[link(name = "LocalAuthentication", kind = "framework")]
unsafe extern "C" {}

/// Requests Touch ID with the system password fallback used by Preview 15.
pub fn authenticate_device_owner(reason: &str) -> Result<()> {
    let (sender, receiver) = mpsc::channel::<Result<()>>();

    unsafe {
        let class = class!(LAContext);
        let context: *mut objc2::runtime::AnyObject = msg_send![class, alloc];
        let context: *mut objc2::runtime::AnyObject = msg_send![context, init];
        if context.is_null() {
            anyhow::bail!("failed to create the macOS authentication context");
        }

        let mut availability_error: *mut NSError = std::ptr::null_mut();
        let available: objc2::runtime::Bool = msg_send![
            context,
            canEvaluatePolicy: DEVICE_OWNER_AUTHENTICATION_POLICY,
            error: &mut availability_error
        ];
        if !available.as_bool() {
            // Preview 15 allowed access when device-owner authentication was unavailable.
            return Ok(());
        }

        let reason = NSString::from_str(reason);
        let reply =
            block2::RcBlock::new(move |success: objc2::runtime::Bool, error: *mut NSError| {
                let result = if success.as_bool() {
                    Ok(())
                } else {
                    let message = if error.is_null() {
                        "macOS authentication failed".to_string()
                    } else {
                        let error = &*error;
                        let description: objc2::rc::Retained<NSString> =
                            msg_send![error, localizedDescription];
                        description.to_string()
                    };
                    Err(anyhow::anyhow!(message))
                };
                let _ = sender.send(result);
            });

        let _: () = msg_send![
            context,
            evaluatePolicy: DEVICE_OWNER_AUTHENTICATION_POLICY,
            localizedReason: &*reason,
            reply: &*reply
        ];
    }

    receiver
        .recv()
        .context("macOS authentication response channel closed")?
}
