//! Probe: render a swarm-plan-style flowchart (the exact shape
//! `jcode-tui`'s `swarm_plan_graph` emits) through the real mermaid pipeline
//! and report success/error. Useful when iterating on the renderer:
//!   cargo run -p jcode-tui-mermaid --features renderer --example plan_graph_probe
fn probe(name: &str, src: &str) -> bool {
    match jcode_tui_mermaid::render_mermaid_untracked(src, Some(100)) {
        jcode_tui_mermaid::RenderResult::Image { width, height, .. } => {
            println!("OK   {name}: {width}x{height}");
            true
        }
        jcode_tui_mermaid::RenderResult::Error(err) => {
            println!("FAIL {name}: {err}");
            false
        }
    }
}

fn main() {
    let mut ok = true;
    ok &= probe("minimal", "flowchart TD\nA-->B\n");
    ok &= probe(
        "plan-graph",
        concat!(
            "flowchart TD\n",
            "    t_a_1[\"✓ wire the bus tap\"]:::done\n",
            "    t_b_2[\"▶ carve the gallery band · @worker-fox\"]:::active\n",
            "    t_c_3[\"· run the ui tests\"]:::pending\n",
            "    t_a_1 --> t_b_2\n",
            "    t_b_2 --> t_c_3\n",
            "    classDef done fill:#1d3a1d,stroke:#64c864,color:#a8e0a8\n",
            "    classDef active fill:#3a321d,stroke:#ffc864,color:#ffe0a8\n",
            "    classDef pending fill:#26262e,stroke:#8c8c96,color:#b4b4be\n",
        ),
    );
    std::process::exit(if ok { 0 } else { 1 });
}
