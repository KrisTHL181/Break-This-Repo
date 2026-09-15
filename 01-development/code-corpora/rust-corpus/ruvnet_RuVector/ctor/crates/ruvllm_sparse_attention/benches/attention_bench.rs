use criterion::{black_box, criterion_group, criterion_main, Criterion};
use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use ruvllm_sparse_attention::{
    dense_attention, AttentionBackend, SparseAttentionConfig, SubquadraticSparseAttention, Tensor3,
};

fn random_tensor(seq: usize, heads: usize, dim: usize, seed: u64) -> Tensor3 {
    let mut rng = StdRng::seed_from_u64(seed);
    let len = seq * heads * dim;
    let data = (0..len)
        .map(|_| rng.gen_range(-0.02f32..0.02f32))
        .collect::<Vec<f32>>();
    Tensor3::from_vec(data, seq, heads, dim).unwrap()
}

fn bench_sparse(c: &mut Criterion) {
    let mut group = c.benchmark_group("sparse_attention");
    let heads = 8;
    let dim = 64;

    for seq in [512usize, 1024, 2048, 4096, 8192] {
        let q = random_tensor(seq, heads, dim, 1);
        let k = random_tensor(seq, heads, dim, 2);
        let v = random_tensor(seq, heads, dim, 3);
        let attention = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();

        group.bench_function(format!("seq_{}", seq), |b| {
            b.iter(|| {
                attention
                    .forward(black_box(&q), black_box(&k), black_box(&v))
                    .unwrap()
            })
        });
    }

    group.finish();
}

fn bench_dense_reference(c: &mut Criterion) {
    let mut group = c.benchmark_group("dense_attention_reference");
    let heads = 8;
    let dim = 64;

    for seq in [128usize, 256, 512, 1024] {
        let q = random_tensor(seq, heads, dim, 4);
        let k = random_tensor(seq, heads, dim, 5);
        let v = random_tensor(seq, heads, dim, 6);

        group.bench_function(format!("seq_{}", seq), |b| {
            b.iter(|| dense_attention(black_box(&q), black_box(&k), black_box(&v), true).unwrap())
        });
    }

    group.finish();
}

fn bench_flash_sparse(c: &mut Criterion) {
    let mut group = c.benchmark_group("flash_sparse_attention");
    let heads = 8;
    let dim = 64;

    for seq in [512usize, 1024, 2048, 4096] {
        let q = random_tensor(seq, heads, dim, 7);
        let k = random_tensor(seq, heads, dim, 8);
        let v = random_tensor(seq, heads, dim, 9);
        let attention = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();

        group.bench_function(format!("seq_{}", seq), |b| {
            b.iter(|| {
                attention
                    .forward_flash(black_box(&q), black_box(&k), black_box(&v), 128)
                    .unwrap()
            })
        });
    }

    group.finish();
}

criterion_group!(
    benches,
    bench_sparse,
    bench_flash_sparse,
    bench_dense_reference
);
criterion_main!(benches);
