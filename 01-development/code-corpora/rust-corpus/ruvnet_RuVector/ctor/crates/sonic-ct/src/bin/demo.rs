//! `sonic_ct_demo` — run the full pipeline once and emit PGM images + metrics.
//!
//! Usage: `cargo run --release --bin sonic_ct_demo [out_dir]`

use std::fs;
use std::io::Write;
use std::path::PathBuf;

use sonic_ct::memory::check_coherence;
use sonic_ct::pipeline::{run, PipelineConfig};
use sonic_ct::types::Tissue;

fn main() {
    let out = std::env::args().nth(1).unwrap_or_else(|| "sonic_ct_out".to_string());
    let out = PathBuf::from(out);
    fs::create_dir_all(&out).expect("create output dir");

    let cfg = PipelineConfig::default();
    let scene = run(cfg).expect("pipeline runs");

    // Export inspection images.
    let (s_lo, s_hi) = scene.phantom.speed.min_max();
    write_pgm(&out.join("truth_speed.pgm"), &scene.phantom.speed.to_pgm(s_lo, s_hi));
    write_pgm(&out.join("recon_speed.pgm"), &scene.recon_speed.to_pgm(s_lo, s_hi));
    let (a_lo, a_hi) = scene.phantom.attenuation.min_max();
    write_pgm(&out.join("recon_attenuation.pgm"), &scene.recon_attenuation.to_pgm(a_lo, a_hi));
    write_pgm(&out.join("truth_labels.pgm"), &scene.phantom.labels.to_pgm(0.0, 4.0));
    write_pgm(&out.join("recon_labels.pgm"), &scene.segmentation.labels.to_pgm(0.0, 4.0));

    let coherence = check_coherence(&scene.segmentation.labels);

    println!("== sonic_ct demo ==");
    println!("grid:          {}x{}", scene.phantom.n(), scene.phantom.n());
    println!("elements:      {}", scene.ring.count());
    println!("measurements:  {}", scene.quality.measurements);
    println!("MAE (speed):   {:.2} m/s", scene.quality.mae_speed);
    println!("mean Dice:     {:.4}", scene.quality.mean_dice);
    for (i, &t) in Tissue::ALL.iter().enumerate() {
        println!("  Dice[{:>6}] = {:.4}", t.name(), scene.quality.dice[i]);
    }
    println!("coherence:     bone↔water={} organ↔water={} anomaly={}",
        coherence.bone_touching_water, coherence.organ_touching_water, coherence.anomaly);
    println!("images written to: {}", out.display());
}

fn write_pgm(path: &std::path::Path, bytes: &[u8]) {
    let mut f = fs::File::create(path).expect("create pgm");
    f.write_all(bytes).expect("write pgm");
}
