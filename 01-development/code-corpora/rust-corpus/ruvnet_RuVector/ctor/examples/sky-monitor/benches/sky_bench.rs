//! Criterion benches for the ADR-199 hot paths (§29): coordinate projection,
//! track embedding, RuVector insert+search, anomaly scoring, and the full
//! synthetic pipeline.

use criterion::{black_box, criterion_group, criterion_main, Criterion, Throughput};
use sky_monitor::{
    anomaly::{score_track, BaselineStats},
    embedding::{track_embedding, TRACK_EMBEDDING_DIM},
    indexer::TrackIndexer,
    observer_frame, AnomalyConfig, ObserverConfig, Pipeline,
};

fn bench_projection(c: &mut Criterion) {
    let cfg = ObserverConfig::default();
    c.bench_function("coords/observer_frame_single", |b| {
        b.iter(|| {
            observer_frame(
                black_box(cfg.lat),
                black_box(cfg.lon),
                black_box(cfg.alt_m),
                black_box(43.62),
                black_box(-79.40),
                black_box(10_500.0),
            )
        })
    });

    // 10k-target batch (a busy wide-area sweep).
    let targets: Vec<(f64, f64, f64)> = (0..10_000)
        .map(|i| {
            let t = i as f64;
            (
                43.0 + (t * 0.731).fract(),
                -80.5 + (t * 0.377).fract() * 2.0,
                500.0 + (t * 13.7) % 11_000.0,
            )
        })
        .collect();
    let mut g = c.benchmark_group("coords/observer_frame_batch");
    g.throughput(Throughput::Elements(targets.len() as u64));
    g.bench_function("10k_targets", |b| {
        b.iter(|| {
            targets
                .iter()
                .map(|(la, lo, al)| {
                    observer_frame(cfg.lat, cfg.lon, cfg.alt_m, *la, *lo, *al).range_m
                })
                .sum::<f64>()
        })
    });
    g.finish();
}

fn bench_embedding(c: &mut Criterion) {
    let (tracks, _) = Pipeline::default().tracks_and_embeddings().unwrap();
    let track = tracks.iter().max_by_key(|t| t.points.len()).unwrap();
    c.bench_function("embedding/track_embedding", |b| {
        b.iter(|| track_embedding(black_box(track)))
    });
}

fn bench_vector_db(c: &mut Criterion) {
    let (tracks, embeddings) = Pipeline::default().tracks_and_embeddings().unwrap();
    // 1000 jittered variants of the real embeddings.
    let corpus: Vec<Vec<f32>> = (0..1_000)
        .map(|i| {
            let base = &embeddings[i % embeddings.len()];
            base.iter()
                .enumerate()
                .map(|(d, v)| v + ((i * 31 + d) % 17) as f32 * 1e-3)
                .collect()
        })
        .collect();
    c.bench_function("ruvector/insert_1000_then_search", |b| {
        b.iter(|| {
            let mut idx = TrackIndexer::new(TRACK_EMBEDDING_DIM).unwrap();
            for (i, e) in corpus.iter().enumerate() {
                let mut t = tracks[i % tracks.len()].clone();
                t.track_id = format!("bench-{i}");
                idx.insert_track(&t, e.clone()).unwrap();
            }
            idx.similar_tracks(black_box(&embeddings[0]), None, 10)
                .unwrap()
        })
    });
}

fn bench_anomaly(c: &mut Criterion) {
    let (tracks, embeddings) = Pipeline::default().tracks_and_embeddings().unwrap();
    let cfg = AnomalyConfig::default();
    let baseline = BaselineStats::from_tracks(&tracks[..tracks.len() - 1]);
    let mut idx = TrackIndexer::new(TRACK_EMBEDDING_DIM).unwrap();
    for (t, e) in tracks.iter().zip(&embeddings).take(tracks.len() - 1) {
        idx.insert_track(t, e.clone()).unwrap();
    }
    let target = tracks.last().unwrap();
    let target_emb = embeddings.last().unwrap();
    c.bench_function("anomaly/score_track_full", |b| {
        b.iter(|| {
            let novelty = idx.novelty_score(black_box(target_emb)).unwrap() as f64;
            score_track(&cfg, black_box(target), &baseline, novelty, 0.0)
        })
    });
}

fn bench_pipeline(c: &mut Criterion) {
    let mut g = c.benchmark_group("pipeline");
    g.sample_size(10); // end-to-end run: keep the bench fast
    g.bench_function("end_to_end_standard_scenario", |b| {
        b.iter(|| Pipeline::default().run().unwrap())
    });
    g.finish();
}

criterion_group!(
    benches,
    bench_projection,
    bench_embedding,
    bench_vector_db,
    bench_anomaly,
    bench_pipeline
);
criterion_main!(benches);
