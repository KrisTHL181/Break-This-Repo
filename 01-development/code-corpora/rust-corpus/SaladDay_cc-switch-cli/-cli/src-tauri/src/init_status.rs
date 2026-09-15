use serde::Serialize;
use std::sync::{OnceLock, RwLock};

#[derive(Debug, Clone, Serialize)]
pub struct InitErrorPayload {
    pub path: String,
    pub error: String,
}

static INIT_ERROR: OnceLock<RwLock<Option<InitErrorPayload>>> = OnceLock::new();

fn cell() -> &'static RwLock<Option<InitErrorPayload>> {
    INIT_ERROR.get_or_init(|| RwLock::new(None))
}

pub fn set_init_error(payload: InitErrorPayload) {
    if let Ok(mut guard) = cell().write() {
        *guard = Some(payload);
    }
}

pub fn get_init_error() -> Option<InitErrorPayload> {
    cell().read().ok()?.clone()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn init_error_roundtrip() {
        let payload = InitErrorPayload {
            path: "/tmp/config.json".into(),
            error: "broken json".into(),
        };
        set_init_error(payload.clone());
        let got = get_init_error().expect("should get payload back");
        assert_eq!(got.path, payload.path);
        assert_eq!(got.error, payload.error);
    }
}
