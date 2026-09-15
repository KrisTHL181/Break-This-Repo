//! Native pure-Rust local embeddings via the `LatticeEmbedding` provider.
//!
//! Demonstrates asymmetric retrieval on a BGE model: a query is embedded
//! differently from a passage, which is what makes retrieval scores correct.
//! The provider exposes two sides:
//!   - `embed(text)`: the passage/document side (no query instruction)
//!   - `embed_query(text)`: the query side (applies the model's query instruction)
//!
//! Run with (downloads `bge-small-en-v1.5` from HuggingFace into
//! `~/.lattice/models` on first use):
//! ```bash
//! cargo run --example lattice_embedding_example --features lattice-embeddings
//! ```

use ruvector_core::embeddings::{EmbeddingProvider, LatticeEmbedding};

/// Cosine similarity of two equal-length vectors.
fn cosine(a: &[f32], b: &[f32]) -> f32 {
    let dot: f32 = a.iter().zip(b).map(|(x, y)| x * y).sum();
    let na = a.iter().map(|x| x * x).sum::<f32>().sqrt();
    let nb = b.iter().map(|x| x * x).sum::<f32>().sqrt();
    if na == 0.0 || nb == 0.0 {
        0.0
    } else {
        dot / (na * nb)
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("=== LatticeEmbedding (native pure-Rust) Example ===\n");

    // BGE is an asymmetric retriever: query and passage are embedded differently.
    let provider = LatticeEmbedding::from_pretrained("bge-small-en-v1.5")?;
    println!(
        "✓ Loaded provider: {} ({} dimensions)\n",
        provider.name(),
        provider.dimensions()
    );

    let passage = "The Eiffel Tower is located in Paris, France.";
    let query = "Where is the Eiffel Tower?";
    println!("Passage: {passage:?}");
    println!("Query:   {query:?}\n");

    // Passage side: no query instruction.
    let passage_vec = provider.embed(passage)?;

    // The same query text, embedded two ways:
    //   (a) as a passage: WRONG for a query on an asymmetric model,
    //   (b) as a query: CORRECT, applies BGE's retrieval instruction.
    let query_as_passage = provider.embed(query)?;
    let query_as_query = provider.embed_query(query)?;

    println!("--- Asymmetry: query embedded as passage vs. as query ---");
    println!(
        "cosine(passage, query-as-passage) = {:.4}",
        cosine(&passage_vec, &query_as_passage)
    );
    println!(
        "cosine(passage, query-as-query)   = {:.4}   <- embed_query()",
        cosine(&passage_vec, &query_as_query)
    );

    let self_sim = cosine(&query_as_passage, &query_as_query);
    println!("\ncosine(query-as-passage, query-as-query) = {self_sim:.4}");
    println!(
        "A value below 1.0 confirms embed_query() prepended BGE's retrieval\n\
         instruction and produced a different vector. That is the whole point:\n\
         queries and passages live in the same space but are encoded by\n\
         different protocols. (A single pair's absolute cosine is not the\n\
         retrieval signal; what matters is ranking across a corpus, where\n\
         embedding queries with embed_query() is what BGE was trained for.)"
    );

    Ok(())
}
