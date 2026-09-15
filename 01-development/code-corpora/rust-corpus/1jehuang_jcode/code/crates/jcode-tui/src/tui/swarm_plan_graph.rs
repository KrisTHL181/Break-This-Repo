//! Mermaid source generation for the swarm plan graph.
//!
//! The generator itself lives in `jcode-plan` (`crate::plan::mermaid`) so the
//! renderer stress probe (`jcode-tui-mermaid/examples/swarm_plan_stress.rs`)
//! exercises the exact production logic instead of a drifting copy. This
//! module re-exports it for the TUI's `SwarmPlan` event handler, which pushes
//! the graph as an inline chat message through the normal mermaid pipeline.

pub(crate) use crate::plan::mermaid::swarm_plan_mermaid;
