//! PostgreSQL operator functions for Sona self-learning.

use pgrx::prelude::*;
use pgrx::JsonB;

use super::get_engine;

fn parse_vector(value: &serde_json::Value, field: &str) -> Result<Vec<f32>, String> {
    let values = value
        .as_array()
        .ok_or_else(|| format!("{field} must be an array of finite numbers"))?;
    if values.is_empty() {
        return Err(format!("{field} must not be empty"));
    }

    values
        .iter()
        .enumerate()
        .map(|(index, value)| {
            let number = value
                .as_f64()
                .ok_or_else(|| format!("{field}[{index}] must be a finite number"))?;
            let converted = number as f32;
            if !converted.is_finite() {
                return Err(format!("{field}[{index}] is outside the f32 range"));
            }
            Ok(converted)
        })
        .collect()
}

/// Record a learning trajectory for a table (Micro-LoRA).
#[pg_extern]
pub fn ruvector_sona_learn(table_name: &str, trajectory_json: JsonB) -> JsonB {
    let initial_value = trajectory_json
        .0
        .get("initial")
        .unwrap_or_else(|| pgrx::error!("SONA learn: initial is required"));
    let initial = parse_vector(initial_value, "initial")
        .unwrap_or_else(|message| pgrx::error!("SONA learn: {}", message));
    let dim = initial.len() as u32;

    let steps = trajectory_json
        .0
        .get("steps")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default();

    // Validate the complete trajectory before mutating engine state. The old
    // filter_map path silently shortened malformed vectors and still returned
    // status=learned.
    let parsed_steps: Vec<(Vec<f32>, Vec<f32>, f32)> = steps
        .iter()
        .enumerate()
        .map(|(step_index, step)| {
            let embedding_value = step
                .get("embedding")
                .ok_or_else(|| format!("steps[{step_index}].embedding is required"))?;
            let embedding =
                parse_vector(embedding_value, &format!("steps[{step_index}].embedding"))?;
            if embedding.len() != dim as usize {
                return Err(format!(
                    "steps[{step_index}].embedding has {} dimensions; expected {dim}",
                    embedding.len()
                ));
            }

            let attention_weights = match step.get("attention_weights") {
                Some(value) => {
                    parse_vector(value, &format!("steps[{step_index}].attention_weights"))?
                }
                None => Vec::new(),
            };
            let reward = step
                .get("reward")
                .and_then(|value| value.as_f64())
                .unwrap_or(0.0) as f32;
            if !reward.is_finite() {
                return Err(format!("steps[{step_index}].reward must be finite"));
            }
            Ok((embedding, attention_weights, reward))
        })
        .collect::<Result<_, String>>()
        .unwrap_or_else(|message| pgrx::error!("SONA learn: {}", message));

    let final_reward = trajectory_json
        .0
        .get("final_reward")
        .and_then(|v| v.as_f64())
        .unwrap_or(0.5) as f32;
    if !final_reward.is_finite() {
        pgrx::error!("SONA learn: final_reward must be finite");
    }

    let engine = super::get_or_create_engine_with_dim(table_name, dim).unwrap_or_else(|mismatch| {
        pgrx::error!(
            "SONA learn: table '{}' uses {} dimensions; received {}",
            table_name,
            mismatch.expected,
            mismatch.actual
        )
    });

    let mut builder = engine.begin_trajectory(initial);
    for (embedding, attention_weights, reward) in parsed_steps {
        builder.add_step(embedding, attention_weights, reward);
    }

    engine.end_trajectory(builder, final_reward);

    JsonB(serde_json::json!({
        "status": "learned",
        "table": table_name,
        "steps": steps.len(),
        "final_reward": final_reward,
    }))
}

/// Apply learned LoRA transformation to an embedding.
/// Dynamically matches engine dimension to input size.
#[pg_extern(immutable, parallel_safe)]
pub fn ruvector_sona_apply(table_name: &str, embedding: Vec<f32>) -> Vec<f32> {
    if embedding.is_empty() {
        return embedding;
    }

    let dim = embedding.len() as u32;
    let engine = super::get_or_create_engine_with_dim(table_name, dim).unwrap_or_else(|mismatch| {
        pgrx::error!(
            "SONA apply: table '{}' uses {} dimensions; received {}",
            table_name,
            mismatch.expected,
            mismatch.actual
        )
    });

    let mut output = vec![0.0f32; embedding.len()];

    // Guard against panics from the native engine
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        engine.apply_micro_lora(&embedding, &mut output);
    }));

    match result {
        Ok(()) => {
            // If output is all zeros (no learned weights yet), return the input
            if output.iter().all(|&x| x == 0.0) {
                embedding
            } else {
                output
            }
        }
        Err(_) => {
            // On panic, return input unchanged rather than crashing PostgreSQL
            pgrx::warning!(
                "SONA apply: internal error for dim={}, returning input unchanged",
                dim
            );
            embedding
        }
    }
}

/// Get EWC++ forgetting metrics for a table.
#[pg_extern]
pub fn ruvector_sona_ewc_status(table_name: &str) -> JsonB {
    let Some(engine) = get_engine(table_name) else {
        return JsonB(serde_json::json!({
            "table": table_name,
            "initialized": false,
            "ewc_tasks": 0,
            "trajectories_buffered": 0,
            "trajectories_dropped": 0,
            "patterns_stored": 0,
            "buffer_success_rate": 0.0,
        }));
    };
    let stats = engine.stats();

    JsonB(serde_json::json!({
        "table": table_name,
        "initialized": true,
        "ewc_tasks": stats.ewc_tasks,
        "trajectories_buffered": stats.trajectories_buffered,
        "trajectories_dropped": stats.trajectories_dropped,
        "patterns_stored": stats.patterns_stored,
        "buffer_success_rate": stats.buffer_success_rate,
    }))
}

/// Get Sona engine statistics for a table.
#[pg_extern]
pub fn ruvector_sona_stats(table_name: &str) -> JsonB {
    let Some(engine) = get_engine(table_name) else {
        return JsonB(serde_json::json!({
            "table": table_name,
            "initialized": false,
            "trajectories_buffered": 0,
            "trajectories_dropped": 0,
            "buffer_success_rate": 0.0,
            "patterns_stored": 0,
            "ewc_tasks": 0,
            "instant_enabled": false,
            "background_enabled": false,
            "hidden_dim": null,
            "embedding_dim": null,
            "micro_lora_rank": null,
            "base_lora_rank": null,
        }));
    };
    let stats = engine.stats();
    let config = engine.config();

    JsonB(serde_json::json!({
        "table": table_name,
        "initialized": true,
        "trajectories_buffered": stats.trajectories_buffered,
        "trajectories_dropped": stats.trajectories_dropped,
        "buffer_success_rate": stats.buffer_success_rate,
        "patterns_stored": stats.patterns_stored,
        "ewc_tasks": stats.ewc_tasks,
        "instant_enabled": stats.instant_enabled,
        "background_enabled": stats.background_enabled,
        "hidden_dim": config.hidden_dim,
        "embedding_dim": config.embedding_dim,
        "micro_lora_rank": config.micro_lora_rank,
        "base_lora_rank": config.base_lora_rank,
    }))
}

#[cfg(test)]
mod tests {
    use super::parse_vector;

    #[test]
    fn vector_parser_rejects_non_numeric_and_empty_inputs() {
        assert!(parse_vector(&serde_json::json!([]), "initial").is_err());
        assert!(parse_vector(&serde_json::json!([0.1, "bad"]), "initial").is_err());
        assert_eq!(
            parse_vector(&serde_json::json!([0.1, 0.2]), "initial")
                .unwrap()
                .len(),
            2
        );
    }
}
