//! **Setup-time** Fleet composition: a suggestion schema, and nothing else.
//!
//! The same Reasoning Router service an operator attaches to a Fleet may later
//! *assist* Fleet setup, by proposing which configured provider/model should
//! take which role. That is a fundamentally different act from what the runtime
//! router does, and this module exists to keep the two from ever being
//! confused:
//!
//! | | Runtime router | Setup-time composition |
//! |---|---|---|
//! | Input | one frozen worker route | an explicit, redacted model pool + role list |
//! | Output | a reasoning tier | a *suggested* provider/model per role |
//! | Authority | applies immediately | **none** — a human must review and save |
//!
//! Three properties make this safe to have in the tree at all:
//!
//! 1. **It is pure.** No clock, no filesystem, no network, no config. It maps
//!    an input value to an output value.
//! 2. **It cannot save or launch.** There is no path from a
//!    [`crate::fleet_composition::FleetCompositionProposal`] to an
//!    [`crate::ExactFleet`], a snapshot, or a
//!    spawn. Turning a proposal into a saved Fleet is a separate, explicit act
//!    that belongs to the saved-Fleet UI lane.
//! 3. **It cannot invent a model.** Every suggestion must name a model that is
//!    already in the operator's explicitly configured pool; anything else is
//!    rejected, not silently substituted.
//!
//! Every proposal is born
//! [`crate::fleet_composition::RatificationState::Unratified`] and there is no
//! method here that ratifies one. Ratification is a human act recorded
//! elsewhere; this module can only ever describe a suggestion.
//!
//! ## The seam
//!
//! Runtime **must not** call anything in this module, and nothing here reads a
//! [`crate::FleetSnapshot`] or mutates a saved Fleet. The TUI setup wizard wires
//! this on the UI side of the boundary: parse → propose → *show the human* →
//! human saves a Fleet profile through the ordinary setup save path. This
//! module remains unable to save, launch, or snapshot anything itself.

use std::collections::BTreeSet;

use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::redaction::redact_for_disclosure;

/// One entry in the operator's explicitly configured model pool.
///
/// "Explicitly configured" is load-bearing: this is not a catalogue of models
/// that exist in the world, it is the set the operator has already set up and
/// is willing to spend money on.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ConfiguredModel {
    /// Exact configured provider id.
    pub provider: String,
    /// Exact model id.
    pub model: String,
    /// Optional non-secret operator note (e.g. "cheap", "long context").
    /// Redacted on construction — a note is free text and may hold a path.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub note: Option<String>,
}

impl ConfiguredModel {
    /// Build a pool entry, redacting the note.
    #[must_use]
    pub fn new(provider: impl Into<String>, model: impl Into<String>, note: Option<&str>) -> Self {
        Self {
            provider: provider.into(),
            model: model.into(),
            note: note
                .map(|note| redact_for_disclosure(note).into_text())
                .filter(|note| !note.trim().is_empty()),
        }
    }

    /// `provider/model` — the key a suggestion is checked against.
    #[must_use]
    pub fn key(&self) -> String {
        format!("{}/{}", self.provider, self.model)
    }
}

/// A role the operator wants filled.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CompositionRole {
    /// Semantic role name, e.g. `builder`.
    pub role: String,
    /// What the operator wants this role to do. Redacted on construction.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub intent: Option<String>,
}

impl CompositionRole {
    #[must_use]
    pub fn new(role: impl Into<String>, intent: Option<&str>) -> Self {
        Self {
            role: role.into(),
            intent: intent
                .map(|intent| redact_for_disclosure(intent).into_text())
                .filter(|intent| !intent.trim().is_empty()),
        }
    }
}

/// The complete, explicit input to a composition request.
///
/// Nothing is discovered: the caller states the pool and the roles. A composer
/// that could go looking for models would be choosing on the operator's behalf,
/// which is exactly the authority this schema withholds.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct FleetCompositionRequest {
    /// The explicitly configured, already-redacted model pool.
    pub pool: Vec<ConfiguredModel>,
    /// The roles to fill.
    pub roles: Vec<CompositionRole>,
}

impl FleetCompositionRequest {
    /// Build and validate a request.
    pub fn new(
        pool: Vec<ConfiguredModel>,
        roles: Vec<CompositionRole>,
    ) -> Result<Self, CompositionError> {
        if pool.is_empty() {
            return Err(CompositionError::EmptyPool);
        }
        if roles.is_empty() {
            return Err(CompositionError::NoRoles);
        }
        let mut seen = BTreeSet::new();
        for role in &roles {
            let key = role.role.trim().to_ascii_lowercase();
            if key.is_empty() {
                return Err(CompositionError::InvalidRole {
                    role: role.role.clone(),
                });
            }
            if !seen.insert(key.clone()) {
                return Err(CompositionError::DuplicateRole { role: key });
            }
        }
        Ok(Self { pool, roles })
    }

    /// Whether a provider/model pair is in the pool.
    #[must_use]
    pub fn pool_contains(&self, provider: &str, model: &str) -> bool {
        self.pool
            .iter()
            .any(|entry| entry.provider == provider && entry.model == model)
    }

    /// The pool keys, for an error message that tells the operator what *is*
    /// available without them having to go look.
    #[must_use]
    pub fn pool_keys(&self) -> Vec<String> {
        self.pool.iter().map(ConfiguredModel::key).collect()
    }
}

/// One suggested role → provider/model assignment.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RoleSuggestion {
    pub role: String,
    pub provider: String,
    pub model: String,
    /// Short, redacted reason. Advisory text for a human reader only.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Whether a proposal has been reviewed and accepted by a human.
///
/// There is deliberately no constructor for [`Self::Ratified`] in this module:
/// ratification happens where a human actually clicks, and that is not here.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum RatificationState {
    /// A suggestion. Not a Fleet, not saved, not runnable.
    Unratified,
    /// Reviewed and accepted by a human, elsewhere.
    Ratified,
}

impl RatificationState {
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Unratified => "unratified",
            Self::Ratified => "ratified",
        }
    }
}

/// A composition proposal: suggestions plus the fact that nobody has agreed.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct FleetCompositionProposal {
    /// Always [`RatificationState::Unratified`] as produced here.
    pub ratification: RatificationState,
    /// One suggestion per requested role.
    pub suggestions: Vec<RoleSuggestion>,
    /// The banner a UI must show. Present in the value itself so a surface
    /// cannot render the suggestions without it being obvious what they are.
    pub advisory: String,
}

/// The advisory every proposal carries.
pub const COMPOSITION_ADVISORY: &str = "Suggestion only — not saved, not running. Review each \
     role's provider and model, then save a Fleet yourself. Nothing here changes an existing \
     Fleet or starts a Workflow.";

impl FleetCompositionProposal {
    /// Validate a set of suggestions against the request that produced them.
    ///
    /// Every suggestion must name a role that was asked for and a provider/model
    /// that is in the pool. A model outside the pool is an error, never a
    /// substitution: silently swapping in something the operator did not
    /// configure is the exact failure this whole schema exists to prevent.
    pub fn validate(
        request: &FleetCompositionRequest,
        suggestions: Vec<RoleSuggestion>,
    ) -> Result<Self, CompositionError> {
        let mut seen = BTreeSet::new();
        for suggestion in &suggestions {
            let role = suggestion.role.trim().to_ascii_lowercase();
            if !request
                .roles
                .iter()
                .any(|wanted| wanted.role.trim().to_ascii_lowercase() == role)
            {
                return Err(CompositionError::UnknownRole {
                    role: suggestion.role.clone(),
                });
            }
            if !seen.insert(role.clone()) {
                return Err(CompositionError::DuplicateRole { role });
            }
            if !request.pool_contains(&suggestion.provider, &suggestion.model) {
                return Err(CompositionError::ModelOutsidePool {
                    role: suggestion.role.clone(),
                    provider: suggestion.provider.clone(),
                    model: suggestion.model.clone(),
                    pool: request.pool_keys(),
                });
            }
        }

        Ok(Self {
            ratification: RatificationState::Unratified,
            suggestions: suggestions
                .into_iter()
                .map(|suggestion| RoleSuggestion {
                    reason: suggestion
                        .reason
                        .as_deref()
                        .map(|reason| redact_for_disclosure(reason).into_text())
                        .filter(|reason| !reason.trim().is_empty()),
                    ..suggestion
                })
                .collect(),
            advisory: COMPOSITION_ADVISORY.to_string(),
        })
    }

    /// Whether this proposal may be acted on automatically. Always `false`.
    ///
    /// A constant rather than a field lookup, so no future edit can make a
    /// proposal self-executing by flipping a boolean somewhere.
    #[must_use]
    pub const fn is_actionable(&self) -> bool {
        false
    }

    /// Roles the request asked for that no suggestion covers.
    #[must_use]
    pub fn unfilled_roles(&self, request: &FleetCompositionRequest) -> Vec<String> {
        request
            .roles
            .iter()
            .filter(|wanted| {
                !self.suggestions.iter().any(|suggestion| {
                    suggestion
                        .role
                        .trim()
                        .eq_ignore_ascii_case(wanted.role.trim())
                })
            })
            .map(|wanted| wanted.role.clone())
            .collect()
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum CompositionError {
    #[error(
        "fleet composition needs an explicit configured model pool; there is nothing to choose \
         from and this schema will not go looking"
    )]
    EmptyPool,
    #[error("fleet composition needs at least one role to fill")]
    NoRoles,
    #[error("`{role}` is not a valid role name")]
    InvalidRole { role: String },
    #[error("role `{role}` appears more than once")]
    DuplicateRole { role: String },
    #[error("suggestion names role `{role}`, which was not one of the requested roles")]
    UnknownRole { role: String },
    #[error(
        "suggestion for role `{role}` names `{provider}/{model}`, which is not in the configured \
         model pool ({}). A composition may only choose from what the operator already \
         configured — it is rejected rather than substituted.",
        pool.join(", ")
    )]
    ModelOutsidePool {
        role: String,
        provider: String,
        model: String,
        pool: Vec<String>,
    },
}

#[cfg(test)]
mod tests {
    use super::*;

    fn request() -> FleetCompositionRequest {
        FleetCompositionRequest::new(
            vec![
                ConfiguredModel::new("zai", "glm-5", Some("strong")),
                ConfiguredModel::new("openai", "gpt-5.6-luna", Some("cheap")),
            ],
            vec![
                CompositionRole::new("builder", Some("lands focused code changes")),
                CompositionRole::new("scout", Some("fast read-only exploration")),
            ],
        )
        .expect("valid request")
    }

    fn suggestion(role: &str, provider: &str, model: &str) -> RoleSuggestion {
        RoleSuggestion {
            role: role.to_string(),
            provider: provider.to_string(),
            model: model.to_string(),
            reason: None,
        }
    }

    #[test]
    fn a_valid_proposal_is_born_unratified_and_not_actionable() {
        let request = request();
        let proposal = FleetCompositionProposal::validate(
            &request,
            vec![
                suggestion("builder", "zai", "glm-5"),
                suggestion("scout", "openai", "gpt-5.6-luna"),
            ],
        )
        .expect("valid proposal");

        assert_eq!(proposal.ratification, RatificationState::Unratified);
        assert_eq!(proposal.ratification.as_str(), "unratified");
        assert!(!proposal.is_actionable());
        assert_eq!(proposal.advisory, COMPOSITION_ADVISORY);
        assert!(proposal.advisory.contains("not saved"));
        assert!(proposal.unfilled_roles(&request).is_empty());
    }

    /// The central guardrail: a model the operator never configured is an
    /// error, and the error names what *is* available.
    #[test]
    fn a_model_outside_the_pool_is_rejected_not_substituted() {
        let request = request();
        let err = FleetCompositionProposal::validate(
            &request,
            vec![suggestion("builder", "anthropic", "claude-opus-5")],
        )
        .expect_err("out-of-pool model");

        assert!(
            matches!(err, CompositionError::ModelOutsidePool { .. }),
            "{err:?}"
        );
        let message = err.to_string();
        assert!(message.contains("zai/glm-5"), "{message}");
        assert!(
            message.contains("rejected rather than substituted"),
            "{message}"
        );
    }

    /// A right model on the wrong provider is still outside the pool.
    #[test]
    fn a_pool_entry_is_a_provider_and_model_pair_not_just_a_model() {
        let request = request();
        let err = FleetCompositionProposal::validate(
            &request,
            vec![suggestion("builder", "openai", "glm-5")],
        )
        .expect_err("provider must match too");
        assert!(matches!(err, CompositionError::ModelOutsidePool { .. }));
    }

    #[test]
    fn suggestions_must_answer_the_roles_that_were_asked_for() {
        let request = request();
        let err = FleetCompositionProposal::validate(
            &request,
            vec![suggestion("wizard", "zai", "glm-5")],
        )
        .expect_err("unknown role");
        assert!(matches!(err, CompositionError::UnknownRole { .. }));

        let duplicate = FleetCompositionProposal::validate(
            &request,
            vec![
                suggestion("builder", "zai", "glm-5"),
                suggestion("builder", "openai", "gpt-5.6-luna"),
            ],
        )
        .expect_err("duplicate role");
        assert!(matches!(duplicate, CompositionError::DuplicateRole { .. }));
    }

    #[test]
    fn a_partial_proposal_reports_what_it_did_not_fill() {
        let request = request();
        let proposal = FleetCompositionProposal::validate(
            &request,
            vec![suggestion("builder", "zai", "glm-5")],
        )
        .expect("partial is allowed, and visible");

        assert_eq!(proposal.unfilled_roles(&request), vec!["scout".to_string()]);
    }

    #[test]
    fn an_empty_pool_or_role_list_is_refused() {
        assert!(matches!(
            FleetCompositionRequest::new(vec![], vec![CompositionRole::new("builder", None)])
                .expect_err("empty pool"),
            CompositionError::EmptyPool
        ));
        assert!(matches!(
            FleetCompositionRequest::new(vec![ConfiguredModel::new("zai", "glm-5", None)], vec![])
                .expect_err("no roles"),
            CompositionError::NoRoles
        ));
    }

    /// Free-text fields are operator prose and may hold a path or a key. They
    /// are redacted on the way in, because a proposal is displayed and may be
    /// saved alongside logs.
    #[test]
    fn free_text_is_redacted_on_the_way_in() {
        let model = ConfiguredModel::new("zai", "glm-5", Some("configured in /Users/hunter/.env"));
        assert!(!model.note.as_deref().unwrap().contains("/Users/"));

        let role = CompositionRole::new("builder", Some("uses ZAI_API_KEY=zzz"));
        assert!(!role.intent.as_deref().unwrap().contains("zzz"));

        let request = FleetCompositionRequest::new(vec![model], vec![role]).expect("request");
        let proposal = FleetCompositionProposal::validate(
            &request,
            vec![RoleSuggestion {
                role: "builder".to_string(),
                provider: "zai".to_string(),
                model: "glm-5".to_string(),
                reason: Some("matches /home/x/notes".to_string()),
            }],
        )
        .expect("valid");

        let json = serde_json::to_string(&proposal).expect("serialize");
        assert!(!json.contains("/home/"), "{json}");
    }

    /// The schema is inert: a proposal serializes as a suggestion and carries
    /// no route, snapshot, or launch surface a runtime could act on.
    #[test]
    fn a_proposal_carries_no_runtime_surface() {
        let request = request();
        let proposal = FleetCompositionProposal::validate(
            &request,
            vec![suggestion("builder", "zai", "glm-5")],
        )
        .expect("valid");
        let json = serde_json::to_string(&proposal).expect("serialize");

        assert!(json.contains("\"unratified\""), "{json}");
        for forbidden in [
            "snapshot",
            "content_hash",
            "permissions",
            "reasoning_router",
            "schema_revision",
        ] {
            assert!(
                !json.contains(forbidden),
                "a composition proposal must not look like a fleet: {forbidden} in {json}"
            );
        }
    }
}
