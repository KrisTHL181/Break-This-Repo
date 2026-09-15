//! `sonic_ct_train` — fit the segmentation model and build the acoustic memory.
//!
//! This is the "training on public (synthetic, reproducible) data" entry point.
//! It generates a corpus of phantoms, reconstructs each, optimises the
//! segmentation thresholds against ground truth, and populates the RuVector-style
//! [`AcousticMemory`] with longitudinal subjects to demonstrate warm-starting,
//! drift tracking, and anomaly flagging.
//!
//! Usage: `cargo run --release --bin sonic_ct_train [n_train] [out_dir]`

use std::fs;
use std::path::PathBuf;

use sonic_ct::memory::{check_coherence, embed_speed, AcousticMemory, ScanRecord};
use sonic_ct::model::{evaluate, train, TrainExample};
use sonic_ct::phantom::PhantomConfig;
use sonic_ct::pipeline::{run, PipelineConfig};
use sonic_ct::segmentation::SegModel;

const EMBED_K: usize = 16; // 16x16 = 256-d descriptor

fn main() {
    let n_train: usize = std::env::args().nth(1).and_then(|s| s.parse().ok()).unwrap_or(24);
    let out = PathBuf::from(std::env::args().nth(2).unwrap_or_else(|| "sonic_ct_out".into()));
    fs::create_dir_all(&out).expect("create out dir");

    println!("== sonic_ct training ==");
    println!("generating {n_train} synthetic scans (16x16 embeddings)...");

    let mut examples: Vec<TrainExample> = Vec::with_capacity(n_train);
    let mut memory = AcousticMemory::new(EMBED_K * EMBED_K);

    for i in 0..n_train {
        let mut cfg = PipelineConfig::default();
        cfg.phantom = PhantomConfig {
            n: 80,
            extent: 0.24,
            seed: (i as u64) + 1,
        };
        cfg.elements = 160;
        cfg.acquisition.fan = 80;
        cfg.recon.iters = 6;

        let scene = run(cfg).expect("pipeline");
        let embedding = embed_speed(&scene.recon_speed, EMBED_K);

        // Each phantom seed is treated as a distinct subject scanned twice
        // (a baseline and a follow-up) to exercise longitudinal queries.
        let patient = format!("subj-{:03}", i);
        memory.insert(ScanRecord {
            id: format!("{patient}-t0"),
            patient_id: patient.clone(),
            timestamp: 1_700_000_000 + (i as u64) * 86_400,
            embedding: embedding.clone(),
            mean_dice: scene.quality.mean_dice,
            mae: scene.quality.mae_speed,
        });

        examples.push(TrainExample {
            recon_speed: scene.recon_speed,
            true_labels: scene.phantom.labels,
        });
    }

    // Fit the segmentation thresholds.
    let base = SegModel::default();
    let base_score = evaluate(&base, &examples);
    let (tuned, tuned_score) = train(&base, &examples);

    println!("\n-- segmentation model --");
    println!("default mean Dice: {:.4}", base_score);
    println!("trained mean Dice: {:.4}  (Δ {:+.4})", tuned_score, tuned_score - base_score);
    println!("trained bands:");
    for (u, t) in &tuned.bands {
        if u.is_finite() {
            println!("   <= {:>7.1} m/s -> {}", u, t.name());
        } else {
            println!("       > prev    -> {}", t.name());
        }
    }

    // Demonstrate a longitudinal follow-up + warm-start retrieval.
    if let Some(first) = memory.record(0).cloned() {
        let nn = memory.search(&first.embedding, 3);
        println!("\n-- acoustic memory ({} scans) --", memory.len());
        println!("warm-start NN for subj-000-t0: {:?}",
            nn.iter().map(|(i, s)| (memory.record(*i).unwrap().id.clone(), (s * 1000.0).round() / 1000.0)).collect::<Vec<_>>());
    }

    // Verify the index round-trips through the portable container format.
    let bytes = memory.to_bytes();
    let restored = AcousticMemory::from_bytes(&bytes).expect("roundtrip");
    assert_eq!(restored.len(), memory.len());
    fs::write(out.join("acoustic_memory.rvf"), &bytes).expect("write memory");
    println!("memory archived: {} bytes -> {}", bytes.len(), out.join("acoustic_memory.rvf").display());

    // Coherence summary across the corpus.
    let mut anomalies = 0;
    for ex in &examples {
        if check_coherence(&ex.true_labels).anomaly {
            anomalies += 1;
        }
    }
    println!("ground-truth anomalies flagged: {}/{}", anomalies, examples.len());
    println!("\ntraining complete.");
}
