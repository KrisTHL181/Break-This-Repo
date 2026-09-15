//! Resolve the user-level Codex model-provider connection for Kaku Assistant.
//!
//! This is intentionally a concrete Codex module, not a provider abstraction.
//! It reads only `$CODEX_HOME` (falling back to `~/.codex`) and never applies
//! project config, profiles, or CLI overrides.

use anyhow::{Context, Result};
use std::collections::BTreeMap;
use std::convert::TryFrom;

use crate::ai_auth::{self, CodexAuth};

pub(crate) const FOLLOW_CODEX_MODEL: &str = "Follow Codex";
const OPENAI_BASE_URL: &str = "https://api.openai.com/v1";
const CHATGPT_BASE_URL: &str = "https://chatgpt.com/backend-api";
const DEFAULT_REQUEST_RETRIES: u32 = 2;
const MAX_CONFIGURED_RETRIES: u32 = 10;

#[derive(Clone)]
pub(crate) enum CodexCredential {
    ChatGpt(CodexAuth),
    Bearer(String),
    None,
}

#[derive(Clone)]
pub(crate) struct CodexConnection {
    pub(crate) model: Option<String>,
    pub(crate) reasoning_effort: Option<String>,
    pub(crate) reasoning_summary: Option<String>,
    pub(crate) base_url: String,
    pub(crate) headers: Vec<(String, String)>,
    pub(crate) query_params: Vec<(String, String)>,
    pub(crate) credential: CodexCredential,
    pub(crate) request_max_attempts: u32,
    pub(crate) stream_max_retries: u32,
    pub(crate) stream_idle_timeout_ms: Option<u64>,
}

impl CodexConnection {
    pub(crate) fn endpoint(&self) -> String {
        format!("{}/responses", self.base_url.trim_end_matches('/'))
    }

    pub(crate) fn models_endpoint(&self) -> String {
        format!("{}/models", self.base_url.trim_end_matches('/'))
    }
}

pub(crate) fn load_codex_connection() -> Result<CodexConnection> {
    let home = ai_auth::codex_home_dir();
    let config_path = home.join("config.toml");
    let config_raw = match std::fs::read_to_string(&config_path) {
        Ok(raw) => raw,
        Err(err) if err.kind() == std::io::ErrorKind::NotFound => String::new(),
        Err(err) => return Err(err).with_context(|| format!("read {}", config_path.display())),
    };
    let auth_path = home.join("auth.json");
    let auth = match std::fs::read_to_string(&auth_path) {
        Ok(raw) => Some(
            serde_json::from_str::<serde_json::Value>(&raw)
                .with_context(|| format!("parse {}", auth_path.display()))?,
        ),
        Err(err) if err.kind() == std::io::ErrorKind::NotFound => None,
        Err(err) => return Err(err).with_context(|| format!("read {}", auth_path.display())),
    };

    resolve_codex_connection(&config_raw, auth.as_ref(), |name| std::env::var(name).ok())
}

pub(crate) fn load_configured_codex_model() -> Result<Option<String>> {
    Ok(load_codex_connection()?
        .model
        .filter(|model| !model.trim().is_empty()))
}

fn resolve_codex_connection<F>(
    config_raw: &str,
    auth: Option<&serde_json::Value>,
    env: F,
) -> Result<CodexConnection>
where
    F: Fn(&str) -> Option<String>,
{
    let config = if config_raw.trim().is_empty() {
        toml::Value::Table(Default::default())
    } else {
        config_raw
            .parse::<toml::Value>()
            .context("parse Codex config.toml")?
    };
    let root = config
        .as_table()
        .ok_or_else(|| anyhow::anyhow!("Codex config.toml root must be a table"))?;
    let model = optional_string(root.get("model"), "model")?;
    let reasoning_effort =
        optional_string(root.get("model_reasoning_effort"), "model_reasoning_effort")?;
    let reasoning_summary = optional_string(
        root.get("model_reasoning_summary"),
        "model_reasoning_summary",
    )?;
    let openai_base_url = optional_string(root.get("openai_base_url"), "openai_base_url")?;
    let chatgpt_base_url = optional_string(root.get("chatgpt_base_url"), "chatgpt_base_url")?;
    let provider_id = optional_string(root.get("model_provider"), "model_provider")?
        .unwrap_or_else(|| "openai".to_string());

    if provider_id == "openai" {
        return resolve_builtin_openai(
            model,
            reasoning_effort,
            reasoning_summary,
            openai_base_url,
            chatgpt_base_url,
            auth,
        );
    }

    let providers = root
        .get("model_providers")
        .and_then(toml::Value::as_table)
        .ok_or_else(|| {
            anyhow::anyhow!(
                "Codex model_provider `{provider_id}` has no [model_providers.{provider_id}] table"
            )
        })?;
    let provider = providers
        .get(&provider_id)
        .and_then(toml::Value::as_table)
        .ok_or_else(|| {
            anyhow::anyhow!(
                "Codex model_provider `{provider_id}` has no [model_providers.{provider_id}] table"
            )
        })?;

    reject_unsupported_auth(provider, &provider_id)?;
    let wire_api = optional_string(provider.get("wire_api"), "wire_api")?
        .unwrap_or_else(|| "responses".to_string());
    if wire_api != "responses" {
        anyhow::bail!(
            "Codex provider `{provider_id}` uses unsupported wire_api `{wire_api}`; Kaku sent no request"
        );
    }
    let base_url = required_string(provider.get("base_url"), "base_url", &provider_id)?;
    validate_base_url(&base_url, &provider_id)?;

    let mut headers = string_map(provider.get("http_headers"), "http_headers")?;
    for (header, env_name) in string_map(provider.get("env_http_headers"), "env_http_headers")? {
        let value = required_env(&env, &env_name, &provider_id)?;
        headers.insert(header, value);
    }
    let query_params = string_map(provider.get("query_params"), "query_params")?;

    let requires_openai_auth = provider
        .get("requires_openai_auth")
        .map(|value| {
            value.as_bool().ok_or_else(|| {
                anyhow::anyhow!(
                    "Codex provider `{provider_id}` field `requires_openai_auth` must be boolean"
                )
            })
        })
        .transpose()?
        .unwrap_or(false);
    let env_key = optional_string(provider.get("env_key"), "env_key")?;
    let credential = if let Some(env_name) = env_key {
        CodexCredential::Bearer(required_env(&env, &env_name, &provider_id)?)
    } else if requires_openai_auth {
        credential_from_auth(auth)?
    } else {
        CodexCredential::None
    };

    let request_retries = optional_u32(provider.get("request_max_retries"), "request_max_retries")?
        .unwrap_or(DEFAULT_REQUEST_RETRIES);
    let stream_max_retries =
        optional_u32(provider.get("stream_max_retries"), "stream_max_retries")?.unwrap_or(0);
    let stream_idle_timeout_ms = optional_u64(
        provider.get("stream_idle_timeout_ms"),
        "stream_idle_timeout_ms",
    )?;

    Ok(CodexConnection {
        model,
        reasoning_effort,
        reasoning_summary,
        base_url,
        headers: headers.into_iter().collect(),
        query_params: query_params.into_iter().collect(),
        credential,
        request_max_attempts: checked_retry_attempts(request_retries, &provider_id)?,
        stream_max_retries: checked_retries(stream_max_retries, &provider_id)?,
        stream_idle_timeout_ms,
    })
}

fn resolve_builtin_openai(
    model: Option<String>,
    reasoning_effort: Option<String>,
    reasoning_summary: Option<String>,
    openai_base_url: Option<String>,
    chatgpt_base_url: Option<String>,
    auth: Option<&serde_json::Value>,
) -> Result<CodexConnection> {
    let auth_mode = auth
        .and_then(|value| value.get("auth_mode"))
        .and_then(serde_json::Value::as_str);
    let (base_url, credential) = match auth_mode {
        Some("chatgpt") => (
            format!(
                "{}/codex",
                chatgpt_base_url
                    .as_deref()
                    .unwrap_or(CHATGPT_BASE_URL)
                    .trim_end_matches('/')
            ),
            CodexCredential::ChatGpt(auth.and_then(ai_auth::codex_auth_from_value).ok_or_else(
                || anyhow::anyhow!("Codex ChatGPT login is incomplete; run `codex login` again"),
            )?),
        ),
        Some("apikey") | Some("api-key") | Some("api_key") => {
            let key = auth
                .and_then(|value| value.get("OPENAI_API_KEY"))
                .and_then(serde_json::Value::as_str)
                .filter(|key| !key.trim().is_empty())
                .ok_or_else(|| {
                    anyhow::anyhow!(
                        "Codex API-key login has no OPENAI_API_KEY; run `codex login --with-api-key`"
                    )
                })?;
            (
                openai_base_url.unwrap_or_else(|| OPENAI_BASE_URL.to_string()),
                CodexCredential::Bearer(key.to_string()),
            )
        }
        Some(other) => {
            anyhow::bail!("Codex auth_mode `{other}` is unsupported; Kaku sent no request")
        }
        None => anyhow::bail!("Codex is not logged in; run `codex login`"),
    };
    validate_base_url(&base_url, "openai")?;

    Ok(CodexConnection {
        model,
        reasoning_effort,
        reasoning_summary,
        base_url,
        headers: Vec::new(),
        query_params: Vec::new(),
        credential,
        request_max_attempts: DEFAULT_REQUEST_RETRIES + 1,
        stream_max_retries: 0,
        stream_idle_timeout_ms: None,
    })
}

fn credential_from_auth(auth: Option<&serde_json::Value>) -> Result<CodexCredential> {
    match auth
        .and_then(|value| value.get("auth_mode"))
        .and_then(serde_json::Value::as_str)
    {
        Some("chatgpt") => Ok(CodexCredential::ChatGpt(
            auth.and_then(ai_auth::codex_auth_from_value)
                .ok_or_else(|| {
                    anyhow::anyhow!("Codex ChatGPT login is incomplete; run `codex login` again")
                })?,
        )),
        Some("apikey") | Some("api-key") | Some("api_key") => {
            let key = auth
                .and_then(|value| value.get("OPENAI_API_KEY"))
                .and_then(serde_json::Value::as_str)
                .filter(|key| !key.trim().is_empty())
                .ok_or_else(|| anyhow::anyhow!("Codex API-key login has no OPENAI_API_KEY"))?;
            Ok(CodexCredential::Bearer(key.to_string()))
        }
        Some(other) => anyhow::bail!("Codex auth_mode `{other}` is unsupported"),
        None => anyhow::bail!("Codex provider requires OpenAI auth, but Codex is not logged in"),
    }
}

fn reject_unsupported_auth(provider: &toml::value::Table, provider_id: &str) -> Result<()> {
    for field in ["experimental_bearer_token", "aws", "auth"] {
        if provider.contains_key(field) {
            anyhow::bail!(
                "Codex provider `{provider_id}` uses unsupported authentication field `{field}`; Kaku sent no request"
            );
        }
    }
    Ok(())
}

fn validate_base_url(base_url: &str, provider_id: &str) -> Result<()> {
    let parsed = url::Url::parse(base_url)
        .with_context(|| format!("Codex provider `{provider_id}` has invalid base_url"))?;
    if !matches!(parsed.scheme(), "http" | "https") || parsed.host_str().is_none() {
        anyhow::bail!("Codex provider `{provider_id}` base_url must be an absolute HTTP(S) URL");
    }
    Ok(())
}

fn required_env<F>(env: &F, name: &str, provider_id: &str) -> Result<String>
where
    F: Fn(&str) -> Option<String>,
{
    env(name)
        .filter(|value| !value.is_empty())
        .ok_or_else(|| {
            anyhow::anyhow!(
                "Codex provider `{provider_id}` requires environment variable `{name}` in the Kaku process"
            )
        })
}

fn string_map(value: Option<&toml::Value>, field: &str) -> Result<BTreeMap<String, String>> {
    let Some(value) = value else {
        return Ok(BTreeMap::new());
    };
    let table = value
        .as_table()
        .ok_or_else(|| anyhow::anyhow!("Codex provider field `{field}` must be a table"))?;
    table
        .iter()
        .map(|(key, value)| {
            let value = value.as_str().ok_or_else(|| {
                anyhow::anyhow!("Codex provider field `{field}.{key}` must be a string")
            })?;
            Ok((key.clone(), value.to_string()))
        })
        .collect()
}

fn optional_string(value: Option<&toml::Value>, field: &str) -> Result<Option<String>> {
    value
        .map(|value| {
            value
                .as_str()
                .map(str::to_string)
                .ok_or_else(|| anyhow::anyhow!("Codex field `{field}` must be a string"))
        })
        .transpose()
}

fn required_string(value: Option<&toml::Value>, field: &str, provider_id: &str) -> Result<String> {
    optional_string(value, field)?.ok_or_else(|| {
        anyhow::anyhow!("Codex provider `{provider_id}` is missing required field `{field}`")
    })
}

fn optional_u32(value: Option<&toml::Value>, field: &str) -> Result<Option<u32>> {
    optional_u64(value, field)?
        .map(|value| {
            u32::try_from(value).map_err(|_| anyhow::anyhow!("Codex field `{field}` is too large"))
        })
        .transpose()
}

fn optional_u64(value: Option<&toml::Value>, field: &str) -> Result<Option<u64>> {
    value
        .map(|value| {
            value
                .as_integer()
                .filter(|value| *value >= 0)
                .map(|value| value as u64)
                .ok_or_else(|| {
                    anyhow::anyhow!("Codex field `{field}` must be a non-negative integer")
                })
        })
        .transpose()
}

fn checked_retries(retries: u32, provider_id: &str) -> Result<u32> {
    if retries > MAX_CONFIGURED_RETRIES {
        anyhow::bail!(
            "Codex provider `{provider_id}` configures {retries} retries; maximum supported is {MAX_CONFIGURED_RETRIES}"
        );
    }
    Ok(retries)
}

fn checked_retry_attempts(retries: u32, provider_id: &str) -> Result<u32> {
    Ok(checked_retries(retries, provider_id)? + 1)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn connection_matches_shared_readiness_contract() {
        let cases: serde_json::Value = serde_json::from_str(include_str!(concat!(
            env!("CARGO_MANIFEST_DIR"),
            "/../tests/fixtures/codex-connection.json"
        )))
        .expect("shared connection fixtures");
        for case in cases.as_array().unwrap() {
            let result = resolve_codex_connection(
                case["config"].as_str().unwrap(),
                case.get("auth"),
                |key| case["env"][key].as_str().map(str::to_owned),
            );
            assert_eq!(
                result.is_ok(),
                case["ready"].as_bool().unwrap(),
                "{}: {:?}",
                case["name"],
                result.err()
            );
        }
    }

    fn api_key_auth() -> serde_json::Value {
        serde_json::json!({
            "auth_mode": "apikey",
            "OPENAI_API_KEY": "sk-test"
        })
    }

    fn chatgpt_auth() -> serde_json::Value {
        serde_json::json!({
            "auth_mode": "chatgpt",
            "tokens": {
                "access_token": "oauth-test",
                "account_id": "account-test"
            }
        })
    }

    #[test]
    fn builtin_openai_uses_api_key_login_and_responses() {
        let auth = api_key_auth();
        let connection = resolve_codex_connection("model = \"gpt-test\"", Some(&auth), |_| None)
            .expect("resolve built-in OpenAI");
        assert_eq!(connection.model.as_deref(), Some("gpt-test"));
        assert_eq!(connection.endpoint(), "https://api.openai.com/v1/responses");
        assert!(matches!(connection.credential, CodexCredential::Bearer(_)));
    }

    #[test]
    fn builtin_chatgpt_uses_codex_backend_and_honors_override() {
        let auth = chatgpt_auth();
        let connection = resolve_codex_connection(
            "chatgpt_base_url = \"https://proxy.example.com/backend-api/\"",
            Some(&auth),
            |_| None,
        )
        .expect("resolve ChatGPT Codex");
        assert_eq!(
            connection.endpoint(),
            "https://proxy.example.com/backend-api/codex/responses"
        );
        match connection.credential {
            CodexCredential::ChatGpt(auth) => {
                assert_eq!(auth.access_token, "oauth-test");
                assert_eq!(auth.account_id.as_deref(), Some("account-test"));
            }
            _ => panic!("expected ChatGPT credential"),
        }
    }

    #[test]
    fn custom_provider_preserves_connection_fields_without_openai_auth() {
        let auth = api_key_auth();
        let raw = r#"
model = "gpt-custom"
model_provider = "local"

[model_providers.local]
base_url = "http://localhost:58424/v1"
wire_api = "responses"
requires_openai_auth = false
query_params = { tenant = "kaku" }
http_headers = { x-static = "yes" }
env_http_headers = { x-secret = "SECRET_HEADER" }
request_max_retries = 4
stream_max_retries = 2
stream_idle_timeout_ms = 30000
"#;
        let connection = resolve_codex_connection(raw, Some(&auth), |name| {
            (name == "SECRET_HEADER").then(|| "secret-value".to_string())
        })
        .expect("resolve custom provider");
        assert_eq!(connection.endpoint(), "http://localhost:58424/v1/responses");
        assert!(matches!(connection.credential, CodexCredential::None));
        assert_eq!(connection.request_max_attempts, 5);
        assert_eq!(connection.stream_max_retries, 2);
        assert_eq!(connection.stream_idle_timeout_ms, Some(30_000));
        assert!(connection
            .headers
            .contains(&("x-secret".to_string(), "secret-value".to_string())));
        assert_eq!(
            connection.query_params,
            vec![("tenant".into(), "kaku".into())]
        );
    }

    #[test]
    fn missing_provider_environment_fails_closed_without_secret_value() {
        let raw = r#"
model_provider = "custom"
[model_providers.custom]
base_url = "https://example.com/v1"
wire_api = "responses"
env_key = "MISSING_KEY"
"#;
        let error = resolve_codex_connection(raw, None, |_| None)
            .err()
            .expect("missing environment must fail")
            .to_string();
        assert!(error.contains("MISSING_KEY"));
        assert!(!error.contains("Bearer"));
    }

    #[test]
    fn unsupported_auth_field_fails_before_request() {
        let raw = r#"
model_provider = "aws"
[model_providers.aws]
base_url = "https://bedrock.example.com/v1"
wire_api = "responses"
aws = { region = "us-east-1" }
"#;
        let error = resolve_codex_connection(raw, None, |_| None)
            .err()
            .expect("AWS must fail closed")
            .to_string();
        assert!(error.contains("unsupported authentication field `aws`"));
    }

    #[test]
    fn non_responses_provider_fails_closed() {
        let raw = r#"
model_provider = "legacy"
[model_providers.legacy]
base_url = "https://example.com/v1"
wire_api = "chat"
"#;
        let error = resolve_codex_connection(raw, None, |_| None)
            .err()
            .expect("legacy wire API must fail")
            .to_string();
        assert!(error.contains("unsupported wire_api `chat`"));
    }
}
