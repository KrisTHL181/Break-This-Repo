use anyhow::{Result, bail};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AuthSourceKind {
    Command,
    Secret,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct ProviderAuthSourceToml {
    #[serde(alias = "type")]
    pub source: AuthSourceKind,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub command: Vec<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub timeout_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub secret_id: Option<String>,
}

impl ProviderAuthSourceToml {
    pub fn validate(&self) -> Result<()> {
        match self.source {
            AuthSourceKind::Command => {
                if self.command.is_empty() || self.command.iter().all(|part| part.trim().is_empty())
                {
                    bail!(
                        "provider auth source command must include at least one non-empty argv item"
                    );
                }
            }
            AuthSourceKind::Secret => {
                if self
                    .secret_id
                    .as_deref()
                    .is_none_or(|secret_id| secret_id.trim().is_empty())
                {
                    bail!("provider auth source secret must include secret_id");
                }
            }
        }
        Ok(())
    }

    #[must_use]
    pub fn source_class(&self) -> &'static str {
        match self.source {
            AuthSourceKind::Command => "command",
            AuthSourceKind::Secret => "secret",
        }
    }
}
