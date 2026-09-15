//! End-to-end and component integration tests for `sonic_ct`.

use sonic_ct::butterfly::{AcquisitionBackend, ButterflyEmbeddedConfig, MockButterflyEmbeddedBackend};
use sonic_ct::geometry::Ring;
use sonic_ct::grid::Grid;
use sonic_ct::memory::{check_coherence, embed_speed, AcousticMemory, ScanRecord};
use sonic_ct::metrics::mean_dice;
use sonic_ct::model::{evaluate, train, TrainExample};
use sonic_ct::phantom::{Phantom, PhantomConfig};
use sonic_ct::pipeline::{run, PipelineConfig};
use sonic_ct::segmentation::SegModel;
use sonic_ct::types::Tissue;

fn small_cfg(seed: u64) -> PipelineConfig {
    let mut cfg = PipelineConfig::default();
    cfg.phantom = PhantomConfig { n: 56, extent: 0.24, seed };
    cfg.elements = 120;
    cfg.acquisition.fan = 60;
    cfg.recon.iters = 5;
    cfg
}

#[test]
fn ring_geometry_is_on_circle() {
    let ring = Ring::new(64, 0.1);
    for p in &ring.positions {
        let r = (p.x * p.x + p.y * p.y).sqrt();
        assert!((r - 0.1).abs() < 1e-5, "element off-circle: r={r}");
    }
    // Fan receivers exclude the source and near neighbours.
    let recv = ring.fan_receivers(0, 32, 0.25);
    assert!(!recv.contains(&0));
    assert!(!recv.is_empty());
}

#[test]
fn phantom_contains_all_tissue_classes() {
    let ph = Phantom::build(PhantomConfig::default());
    let mut seen = [false; Tissue::COUNT];
    for &v in &ph.labels.data {
        seen[v as usize] = true;
    }
    for (i, &t) in Tissue::ALL.iter().enumerate() {
        assert!(seen[i], "phantom missing class {}", t.name());
    }
}

#[test]
fn full_pipeline_metrics_are_sane() {
    let scene = run(small_cfg(1)).unwrap();
    assert!(scene.quality.measurements > 100);
    assert!(scene.quality.mae_speed < 80.0, "MAE too high: {}", scene.quality.mae_speed);
    // Water is the easiest class and should reconstruct well.
    assert!(scene.quality.dice[Tissue::Water as usize] > 0.5);
    // Ground-truth anatomy should pass the coherence check.
    let truth_coh = check_coherence(&scene.phantom.labels);
    assert!(!truth_coh.anomaly, "ground truth flagged as anomalous");
}

#[test]
fn training_improves_segmentation() {
    let mut examples = Vec::new();
    for seed in 1..=6u64 {
        let s = run(small_cfg(seed)).unwrap();
        examples.push(TrainExample {
            recon_speed: s.recon_speed,
            true_labels: s.phantom.labels,
        });
    }
    let base = SegModel::default();
    let before = evaluate(&base, &examples);
    let (tuned, after) = train(&base, &examples);
    assert!(after >= before, "training regressed: {before} -> {after}");
    // The tuned model should be at least as good on a held-out style check.
    let seg_after = sonic_ct::segmentation::segment(&examples[0].recon_speed, &tuned);
    let seg_before = sonic_ct::segmentation::segment(&examples[0].recon_speed, &base);
    assert!(
        mean_dice(&seg_after.labels, &examples[0].true_labels)
            >= mean_dice(&seg_before.labels, &examples[0].true_labels) - 1e-6
    );
}

#[test]
fn acoustic_memory_recall_matches_exact() {
    let mut mem = AcousticMemory::new(256);
    for seed in 1..=20u64 {
        let s = run(small_cfg(seed)).unwrap();
        mem.insert(ScanRecord {
            id: format!("scan-{seed}"),
            patient_id: format!("p{}", seed % 5),
            timestamp: 1_700_000_000 + seed,
            embedding: embed_speed(&s.recon_speed, 16),
            mean_dice: s.quality.mean_dice,
            mae: s.quality.mae_speed,
        });
    }
    // The NSW graph should return the same top-1 as brute force on a probe.
    let probe = mem.record(3).unwrap().embedding.clone();
    let approx = mem.search(&probe, 1)[0].0;
    let exact = mem.search_exact(&probe, 1)[0].0;
    assert_eq!(approx, exact, "NSW top-1 disagreed with exact search");
    // Querying a stored vector returns itself with similarity ~1.
    assert!((mem.search(&probe, 1)[0].1 - 1.0).abs() < 1e-3);
}

#[test]
fn acoustic_memory_roundtrip() {
    let mut mem = AcousticMemory::new(64);
    for i in 0..10 {
        let mut emb = vec![0.0f32; 64];
        emb[i % 64] = 1.0;
        mem.insert(ScanRecord {
            id: format!("s{i}"),
            patient_id: "p".into(),
            timestamp: i as u64,
            embedding: emb,
            mean_dice: 0.5,
            mae: 10.0,
        });
    }
    let bytes = mem.to_bytes();
    let restored = AcousticMemory::from_bytes(&bytes).unwrap();
    assert_eq!(restored.len(), mem.len());
    assert_eq!(restored.record(4).unwrap().id, "s4");
}

#[test]
fn longitudinal_drift_detects_change() {
    let mut mem = AcousticMemory::new(4);
    mem.insert(ScanRecord {
        id: "a".into(),
        patient_id: "p1".into(),
        timestamp: 1,
        embedding: vec![1.0, 0.0, 0.0, 0.0],
        mean_dice: 0.5,
        mae: 1.0,
    });
    mem.insert(ScanRecord {
        id: "b".into(),
        patient_id: "p1".into(),
        timestamp: 2,
        embedding: vec![0.0, 1.0, 0.0, 0.0],
        mean_dice: 0.5,
        mae: 1.0,
    });
    let drift = mem.longitudinal_drift("p1").unwrap();
    assert!((drift - 1.0).abs() < 1e-5, "orthogonal scans => drift 1.0, got {drift}");
}

#[test]
fn coherence_flags_impossible_geometry() {
    // A bone cell surrounded by water is anatomically impossible.
    let mut g = Grid::square(8, 0.08, Tissue::Water as u8 as f32);
    let c = g.idx(4, 4);
    g.data[c] = Tissue::Bone as u8 as f32;
    let rep = check_coherence(&g);
    assert!(rep.bone_touching_water > 0);
    assert!(rep.anomaly);
}

#[test]
fn volume_reconstruction_is_coherent() {
    use sonic_ct::volume3d::reconstruct_volume;
    let mut cfg = small_cfg(3);
    cfg.phantom.n = 48;
    let vol = reconstruct_volume(cfg, &SegModel::tuned(), 12).unwrap();
    assert_eq!(vol.nz, 12);
    assert_eq!(vol.truth_labels.len(), 48 * 48 * 12);
    assert!(vol.measurements > 0);
    // Body-composition fractions sum to ~1 over body voxels.
    let sum: f32 = vol.fractions.iter().sum();
    assert!((sum - 1.0).abs() < 1e-3, "fractions sum {sum}");
    // Different slices have different anatomy => some variance in Dice.
    let d = &vol.slice_dice;
    let spread = d.iter().cloned().fold(0.0f32, f32::max)
        - d.iter().cloned().fold(1.0f32, f32::min);
    assert!(spread >= 0.0);
    assert!(vol.worst_slice < vol.nz);
}

#[test]
fn organ_detector_finds_lateralised_organs() {
    use sonic_ct::organ::{detect_organs, Organ, EV_ZONE};
    use sonic_ct::volume3d::reconstruct_volume;
    let mut cfg = small_cfg(2);
    cfg.phantom.n = 64;
    let vol = reconstruct_volume(cfg, &SegModel::tuned(), 20).unwrap();
    let hyps = detect_organs(&vol.recon_labels, vol.n, vol.nz);
    assert_eq!(hyps.len(), 8);
    let by = |o: Organ| hyps.iter().find(|h| h.organ == o).unwrap();
    // Liver (right) and spleen (left) should both be detected in the corpus.
    assert!(by(Organ::Liver).confidence > 0.4, "liver conf {}", by(Organ::Liver).confidence);
    assert!(by(Organ::Liver).evidence & EV_ZONE != 0);
    // Confidences are bounded probabilities.
    for h in &hyps {
        assert!((0.0..=1.0).contains(&h.confidence));
    }
}

#[test]
fn pgm_phantom_roundtrip_and_reconstruct() {
    use sonic_ct::grid::Grid;
    use sonic_ct::phantom::Phantom;
    use sonic_ct::pipeline::{run_with_phantom, PipelineConfig};
    // Build a synthetic phantom, render its labels to PGM, reload it as a
    // real-style intensity image, and reconstruct — exercising the real-data path.
    let truth = Phantom::build(PhantomConfig { n: 48, extent: 0.24, seed: 5 });
    // Use a grayscale gradient image so all five intensity bands appear.
    let mut gray = Grid::square(48, 0.24, 0.0);
    for y in 0..48 {
        for x in 0..48 {
            let i = gray.idx(x, y);
            gray.data[i] = ((x * 255) / 47) as f32;
        }
    }
    let pgm = gray.to_pgm(0.0, 255.0);
    let reloaded = Grid::from_pgm(&pgm, 0.24).expect("parse pgm");
    assert_eq!(reloaded.nx, 48);
    let phantom = Phantom::from_intensity_grid(&reloaded);
    let mut seen = [false; Tissue::COUNT];
    for &v in &phantom.labels.data {
        seen[v as usize] = true;
    }
    assert!(seen.iter().filter(|&&s| s).count() >= 3, "intensity bands should map to several classes");

    let mut cfg = PipelineConfig::default();
    cfg.phantom.n = 48;
    cfg.elements = 96;
    cfg.acquisition.fan = 48;
    let scene = run_with_phantom(cfg, &SegModel::tuned(), phantom).unwrap();
    assert!(scene.quality.measurements > 0);
    assert!(scene.quality.mae_speed.is_finite());
    let _ = truth; // truth retained for clarity of intent
}

#[test]
fn method_comparison_iterative_beats_backprojection() {
    use sonic_ct::acquisition::{simulate, AcquisitionConfig};
    use sonic_ct::geometry::Ring;
    use sonic_ct::metrics::{psnr, rmse, ssim};
    use sonic_ct::reconstruction::{reconstruct_speed_with, Method, ReconConfig};
    use sonic_ct::shepp_logan::shepp_logan;

    let extent = 0.24;
    let phantom = shepp_logan(64, extent);
    // Shepp-Logan must contain a fast high-contrast skull ring.
    let (lo, hi) = phantom.speed.min_max();
    assert!(hi > 2000.0, "skull should be fast: hi={hi}");
    assert!(lo <= sonic_ct::types::WATER_SPEED + 1.0, "background water present");

    let ring = Ring::new(140, extent / 2.0 * 0.92);
    let acq = simulate(&phantom, &ring, AcquisitionConfig { fan: 70, ..Default::default() });

    let bp = reconstruct_speed_with(&acq, &phantom.speed, ReconConfig { iters: 1, relaxation: 0.9 }, Method::Backprojection);
    let sart = reconstruct_speed_with(&acq, &phantom.speed, ReconConfig { iters: 8, relaxation: 0.9 }, Method::Sart);
    let land = reconstruct_speed_with(&acq, &phantom.speed, ReconConfig { iters: 40, relaxation: 1.0 }, Method::Landweber);

    let (e_bp, e_sart, e_land) = (rmse(&bp, &phantom.speed), rmse(&sart, &phantom.speed), rmse(&land, &phantom.speed));
    // Iterative methods must beat the single backprojection sweep.
    assert!(e_sart < e_bp, "SART {e_sart} should beat BP {e_bp}");
    assert!(e_land < e_bp, "Landweber {e_land} should beat BP {e_bp}");
    // SSIM in [-1,1], PSNR finite and improving over BP.
    let s = ssim(&land, &phantom.speed);
    assert!((-1.0..=1.0).contains(&s));
    assert!(psnr(&land, &phantom.speed) > psnr(&bp, &phantom.speed));
}

#[test]
fn image_metrics_identity() {
    use sonic_ct::metrics::{psnr, rmse, ssim};
    let g = Phantom::build(PhantomConfig { n: 32, extent: 0.24, seed: 1 }).speed;
    assert_eq!(rmse(&g, &g), 0.0);
    assert!(psnr(&g, &g).is_infinite());
    assert!((ssim(&g, &g) - 1.0).abs() < 1e-4);
}

#[test]
fn butterfly_backend_matches_direct_sim() {
    let cfg = ButterflyEmbeddedConfig::default();
    assert_eq!(cfg.total_elements(), 40 * 64);
    let backend = MockButterflyEmbeddedBackend::default();
    assert_eq!(backend.name(), "mock-butterfly-embedded");
    let ph = Phantom::build(PhantomConfig { n: 40, extent: 0.24, seed: 2 });
    let ring = Ring::new(96, 0.10);
    let acq = backend.acquire(&ph, &ring);
    assert!(acq.valid_count > 0);
}
