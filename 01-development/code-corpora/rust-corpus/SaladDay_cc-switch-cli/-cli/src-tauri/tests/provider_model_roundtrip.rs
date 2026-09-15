use std::fs;

use serde_json::json;

use cc_switch_lib::MultiAppConfig;

mod support;
use support::{ensure_test_home, lock_test_mutex, reset_test_fs};

#[test]
fn provider_model_roundtrip_preserves_phase2_fields() {
    let _guard = lock_test_mutex();
    reset_test_fs();
    let home = ensure_test_home();

    let config_dir = home.join(".cc-switch");
    fs::create_dir_all(&config_dir).expect("create config dir");
    let config_path = config_dir.join("config.json");

    let raw = json!({
        "version": 2,
        "claude": {
            "providers": {
                "p1": {
                    "id": "p1",
                    "name": "P1",
                    "settingsConfig": {
                        "env": {
                            "ANTHROPIC_AUTH_TOKEN": "token",
                            "ANTHROPIC_BASE_URL": "https://claude.example"
                        }
                    },
                    "inFailoverQueue": true,
                    "meta": {
                        "usage_script": {
                            "enabled": true,
                            "language": "javascript",
                            "code": "({request:{url:'https://example.com',method:'GET'},extractor:(_)=>({isValid:true})})",
                            "templateType": "newapi"
                        },
                        "apiFormat": "openai_chat",
                        "endpointAutoSelect": true,
                        "limitDailyUsd": "10"
                    }
                }
            },
            "current": "p1"
        },
        "codex": { "providers": {}, "current": "" },
        "gemini": { "providers": {}, "current": "" }
    });

    fs::write(
        &config_path,
        serde_json::to_string_pretty(&raw).expect("serialize config"),
    )
    .expect("write config.json");

    let loaded = MultiAppConfig::load().expect("load should succeed");
    loaded.save().expect("save should succeed");

    let saved_text = fs::read_to_string(&config_path).expect("read saved config");
    let saved: serde_json::Value = serde_json::from_str(&saved_text).expect("parse saved config");

    assert_eq!(
        saved
            .pointer("/claude/providers/p1/inFailoverQueue")
            .and_then(|v| v.as_bool()),
        Some(true),
        "inFailoverQueue should be preserved after load+save"
    );
    assert_eq!(
        saved
            .pointer("/claude/providers/p1/meta/usage_script/templateType")
            .and_then(|v| v.as_str()),
        Some("newapi"),
        "usage_script.templateType should be preserved after load+save"
    );
    assert_eq!(
        saved
            .pointer("/claude/providers/p1/meta/endpointAutoSelect")
            .and_then(|v| v.as_bool()),
        Some(true),
        "meta.endpointAutoSelect should be preserved after load+save"
    );
    assert_eq!(
        saved
            .pointer("/claude/providers/p1/meta/apiFormat")
            .and_then(|v| v.as_str()),
        Some("openai_chat"),
        "meta.apiFormat should be preserved after load+save"
    );
    assert_eq!(
        saved
            .pointer("/claude/providers/p1/meta/limitDailyUsd")
            .and_then(|v| v.as_str()),
        Some("10"),
        "meta.limitDailyUsd should be preserved after load+save"
    );
}
