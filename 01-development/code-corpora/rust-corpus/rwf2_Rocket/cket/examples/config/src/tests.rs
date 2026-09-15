use rocket::config::Config;
use rocket::trace::{Level, TraceFormat};

async fn test_config(profile: &str) {
    let provider = Config::figment().select(profile);
    let rocket = rocket::custom(provider).ignite().await.unwrap();
    let config = rocket.config();
    match profile {
        "debug" => {
            assert_eq!(config.workers, 1);
            assert_eq!(config.keep_alive, 0);
            assert_eq!(config.log_level, Some(Level::INFO));
            assert_eq!(config.log_format, TraceFormat::Pretty);
        }
        "release" => {
            assert_eq!(config.workers, 12);
            assert_eq!(config.keep_alive, 5);
            assert_eq!(config.log_level, Some(Level::ERROR));
            assert_eq!(config.log_format, TraceFormat::Compact);
            assert!(!config.secret_key.is_zero());
        }
        _ => {
            panic!("Unknown profile: {}", profile);
        }
    }
}

#[async_test]
async fn test_debug_config() {
    test_config("debug").await;
}

#[async_test]
async fn test_release_config() {
    test_config("release").await;
}
