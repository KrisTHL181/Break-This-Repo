//! Overlay bundled per-model API compatibility metadata onto catalog models.
//!
//! The model compatibility catalog (`codex-api/model_compatibility_catalog.json`)
//! carries per-model API capability metadata such as supported reasoning levels,
//! parallel tool call support, and context windows. This module applies that
//! metadata on top of the generated provider catalog / live `/models` merge so
//! that compatibility data wins wherever it has an opinion.

use codex_api::compatible_model_catalog_entry;
use codex_protocol::openai_models::InputModality;
use codex_protocol::openai_models::ModelInfo;
use codex_protocol::openai_models::ReasoningControl;
use codex_protocol::openai_models::ReasoningEffort;

/// Enrich `models` with bundled compatibility metadata, keyed by model slug.
///
/// Picker visibility is intentionally left untouched: the provider catalog and
/// live `/models` decoding own which models are listed, while the compatibility
/// catalog only refines per-model API capability metadata.
pub(crate) fn apply_compatibility_catalog(models: Vec<ModelInfo>) -> Vec<ModelInfo> {
    models
        .into_iter()
        .map(|mut model| {
            let Some(entry) = compatible_model_catalog_entry(model.slug.as_str()) else {
                return model;
            };

            if !entry.supported_reasoning_levels.is_empty() {
                model.supported_reasoning_levels = entry.supported_reasoning_levels.clone();
            }
            if entry.reasoning_control != ReasoningControl::None {
                model.reasoning_control = entry.reasoning_control;
            } else if entry.supports_thinking_toggle
                && model.reasoning_control == ReasoningControl::None
            {
                model.reasoning_control = ReasoningControl::ThinkingToggle;
            }
            if model.default_reasoning_level.is_none()
                && (model.reasoning_control != ReasoningControl::None
                    || !model.supported_reasoning_levels.is_empty())
            {
                model.default_reasoning_level = Some(ReasoningEffort::Medium);
            }
            if entry.supports_search_tool {
                model.supports_search_tool = true;
            }
            if entry.input_modalities.contains(&InputModality::Image)
                && !model.input_modalities.contains(&InputModality::Image)
            {
                model.input_modalities.push(InputModality::Image);
            }
            if let Some(context_window) = entry.context_window {
                model.context_window = Some(context_window);
                model.max_context_window = Some(context_window);
            }
            if model.description.is_none() {
                model.description = entry.description.clone();
            }

            model
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model_info::model_info_from_slug;
    use codex_protocol::openai_models::ModelVisibility;
    use pretty_assertions::assert_eq;

    fn catalog_model(slug: &str) -> ModelInfo {
        let mut model = model_info_from_slug(slug);
        model.visibility = ModelVisibility::List;
        model.context_window = None;
        model.max_context_window = None;
        model
    }

    #[test]
    fn enriches_reasoning_levels_from_compatibility_catalog() {
        let enriched = apply_compatibility_catalog(vec![catalog_model("deepseek-reasoner")]);

        let model = enriched.first().expect("deepseek-reasoner model");
        assert_eq!(
            model
                .supported_reasoning_levels
                .iter()
                .map(|preset| preset.effort.clone())
                .collect::<Vec<_>>(),
            vec![
                ReasoningEffort::Low,
                ReasoningEffort::Medium,
                ReasoningEffort::High,
            ]
        );
        assert_eq!(model.reasoning_control, ReasoningControl::Effort);
        assert_eq!(model.default_reasoning_level, Some(ReasoningEffort::Medium));
        assert_eq!(model.context_window, Some(131072));
    }

    #[test]
    fn keeps_picker_visibility_owned_by_the_provider_catalog() {
        let enriched = apply_compatibility_catalog(vec![catalog_model("deepseek-reasoner")]);

        // The compatibility catalog hides deepseek-reasoner (litellm reports no
        // tool calling), but listed provider catalog models must stay listed.
        assert_eq!(
            enriched.first().expect("model").visibility,
            ModelVisibility::List
        );
    }

    #[test]
    fn leaves_unknown_models_untouched() {
        let model = catalog_model("totally-unknown-model-slug");
        let enriched = apply_compatibility_catalog(vec![model.clone()]);

        assert_eq!(enriched, vec![model]);
    }
}
