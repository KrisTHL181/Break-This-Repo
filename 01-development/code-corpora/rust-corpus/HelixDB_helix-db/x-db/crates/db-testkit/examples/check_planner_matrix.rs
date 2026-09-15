//! Sharded complete normalized planner-property matrix.

use std::error::Error;
use std::num::NonZeroUsize;

use helix_db_testkit::planner_domain::NormalizedPlannerCase;

fn main() -> Result<(), Box<dyn Error>> {
    let shard_count = parse_positive_env("HELIX_PLANNER_SHARD_COUNT", 1)?;
    let shard_index = std::env::var("HELIX_PLANNER_SHARD_INDEX")
        .unwrap_or_else(|_| "0".to_string())
        .parse::<usize>()?;
    if shard_index >= shard_count.get() {
        return Err(format!(
            "HELIX_PLANNER_SHARD_INDEX {shard_index} must be below HELIX_PLANNER_SHARD_COUNT {}",
            shard_count.get()
        )
        .into());
    }

    let mut checked = 0_usize;
    for (index, case) in NormalizedPlannerCase::complete().into_iter().enumerate() {
        if index % shard_count.get() != shard_index {
            continue;
        }
        case.check()
            .unwrap_or_else(|error| panic!("normalized planner case {case:?} failed: {error}"));
        checked += 1;
    }
    println!(
        "checked {checked} normalized planner cases in shard {shard_index}/{}",
        shard_count.get()
    );
    Ok(())
}

fn parse_positive_env(name: &'static str, default: usize) -> Result<NonZeroUsize, Box<dyn Error>> {
    let value = std::env::var(name)
        .unwrap_or_else(|_| default.to_string())
        .parse::<usize>()?;
    NonZeroUsize::new(value).ok_or_else(|| format!("{name} must be positive").into())
}
