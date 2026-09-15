//! Versioned plugin activation policy.
//!
//! This is the single source of truth for which reviewed component adapters
//! this Codewhale build will execute. Compatibility, `active()` decisions,
//! consumption-boundary checks, and the capability hash all read this policy.
//! Enabling a new adapter later must change the policy (version and/or mask)
//! so existing trust receipts fail closed as `CapabilitiesChanged`.

use sha2::Digest;

/// Capability-hash domain for the current activation-policy binding.
pub const CAPABILITY_HASH_DOMAIN_V3: &[u8] = b"codewhale-plugin-capabilities-v3\0";

/// Historical policy domain kept so persisted v2 receipts are intentionally
/// invalidated when the declarative Commands, Agents, and Hooks adapters ship.
pub const CAPABILITY_HASH_DOMAIN_V2: &[u8] = b"codewhale-plugin-capabilities-v2\0";

/// Historical domain used before the activation policy was bound into the
/// receipt. Kept so discovery can prove a v1 receipt no longer matches.
pub const CAPABILITY_HASH_DOMAIN_V1: &[u8] = b"codewhale-plugin-capabilities-v1\0";

pub const ACTIVATION_POLICY_VERSION: u32 = 3;

/// A runtime adapter or inventoried capability that a bundle may declare.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum PluginActivationCapability {
    Skills,
    McpStdio,
    McpRemote,
    Commands,
    Agents,
    Hooks,
    Lsp,
    Native,
    FilesystemRoots,
    LifecycleMutation,
}

impl PluginActivationCapability {
    pub const ALL: &'static [Self] = &[
        Self::Skills,
        Self::McpStdio,
        Self::McpRemote,
        Self::Commands,
        Self::Agents,
        Self::Hooks,
        Self::Lsp,
        Self::Native,
        Self::FilesystemRoots,
        Self::LifecycleMutation,
    ];

    #[must_use]
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Skills => "skills",
            Self::McpStdio => "mcp-stdio",
            Self::McpRemote => "mcp-remote",
            Self::Commands => "commands",
            Self::Agents => "agents",
            Self::Hooks => "hooks",
            Self::Lsp => "lsp",
            Self::Native => "native",
            Self::FilesystemRoots => "filesystem-roots",
            Self::LifecycleMutation => "lifecycle-mutation",
        }
    }
}

/// The adapters this exact Codewhale build will activate, plus the inventoried
/// surfaces that stay inactive. Fields are public so tests can construct a
/// mutated policy and prove the capability hash moves.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct PluginActivationPolicy {
    pub version: u32,
    pub supported: &'static [PluginActivationCapability],
    pub inactive: &'static [PluginActivationCapability],
}

impl PluginActivationPolicy {
    #[must_use]
    pub const fn current() -> Self {
        Self {
            version: ACTIVATION_POLICY_VERSION,
            supported: &[
                PluginActivationCapability::Skills,
                PluginActivationCapability::McpStdio,
                PluginActivationCapability::McpRemote,
                PluginActivationCapability::Commands,
                PluginActivationCapability::Agents,
                PluginActivationCapability::Hooks,
            ],
            inactive: &[
                PluginActivationCapability::Lsp,
                PluginActivationCapability::Native,
                PluginActivationCapability::FilesystemRoots,
                PluginActivationCapability::LifecycleMutation,
            ],
        }
    }

    #[must_use]
    pub fn is_supported(self, capability: PluginActivationCapability) -> bool {
        self.supported.contains(&capability)
    }

    pub fn write_hash_material(self, hasher: &mut impl Digest) {
        hasher.update(CAPABILITY_HASH_DOMAIN_V3);
        hasher.update(b"policy-version\0");
        hasher.update(self.version.to_string().as_bytes());
        hasher.update(b"\0");
        for capability in self.supported {
            hasher.update(b"supported\0");
            hasher.update(capability.as_str().as_bytes());
            hasher.update(b"\0");
        }
        for capability in self.inactive {
            hasher.update(b"inactive\0");
            hasher.update(capability.as_str().as_bytes());
            hasher.update(b"\0");
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn current_activation_policy_partitions_known_capabilities() {
        let policy = PluginActivationPolicy::current();
        for capability in PluginActivationCapability::ALL {
            let supported = policy.supported.contains(capability);
            let inactive = policy.inactive.contains(capability);
            assert_ne!(
                supported, inactive,
                "{capability:?} must be supported or inactive, not both or neither"
            );
        }
    }
}
