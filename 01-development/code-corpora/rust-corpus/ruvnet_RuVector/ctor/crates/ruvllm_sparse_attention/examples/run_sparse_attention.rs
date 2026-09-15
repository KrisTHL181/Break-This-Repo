use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use ruvllm_sparse_attention::{
    AttentionBackend, SparseAttentionConfig, SubquadraticSparseAttention, Tensor3,
};

fn random_tensor(seq: usize, heads: usize, dim: usize, seed: u64) -> Tensor3 {
    let mut rng = StdRng::seed_from_u64(seed);
    let len = seq * heads * dim;
    let data = (0..len)
        .map(|_| rng.gen_range(-0.02f32..0.02f32))
        .collect::<Vec<f32>>();
    Tensor3::from_vec(data, seq, heads, dim).unwrap()
}

fn main() {
    let seq = 2048;
    let heads = 8;
    let dim = 64;

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

    let out = attention.forward(&q, &k, &v).unwrap();
    let sparse_edges = attention.estimate_sparse_edges(seq);
    let dense_edges = seq * (seq + 1) / 2;

    println!("shape = {:?}", out.shape());
    println!("dense causal edges = {}", dense_edges);
    println!("sparse edges = {}", sparse_edges);
    println!(
        "edge reduction = {:.2}x",
        dense_edges as f64 / sparse_edges as f64
    );
}
