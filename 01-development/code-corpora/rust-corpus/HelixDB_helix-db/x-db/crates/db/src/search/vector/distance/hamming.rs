use std::fmt;

use bytemuck::{Pod, Zeroable};

use crate::search::vector::{
    distance::Distance,
    item::Item,
    unaligned_vector::{Binary, UnalignedVector},
};

/// The Hamming distance between two vectors is the number of positions at
/// which the corresponding symbols are different.
///
/// `d(u,v) = ||u ^ v||₁`
///
/// /!\ This distance function is binary, which means it loses all its precision
///     and their scalar values are converted to `0` or `1` under the rule
///     `x > 0.0 => 1`, otherwise `0`
#[derive(Debug, Clone)]
pub enum Hamming {}

/// The header of BinaryEuclidean Item nodes.
#[repr(C)]
#[derive(Pod, Zeroable, Clone, Copy)]
pub struct NodeHeaderHamming {
    idx: usize,
}
impl fmt::Debug for NodeHeaderHamming {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("NodeHeaderHamming ")
            .field("idx", &format!("{}", self.idx))
            .finish()
    }
}

impl Distance for Hamming {
    type Header = NodeHeaderHamming;
    type VectorCodec = Binary;

    fn name() -> &'static str {
        "hamming"
    }

    fn new_header(_vector: &UnalignedVector<Self::VectorCodec>) -> Self::Header {
        NodeHeaderHamming { idx: 0 }
    }

    fn distance(p: &Item<Self>, q: &Item<Self>) -> f32 {
        let dist = hamming_bitwise_fast(p.vector.as_bytes(), q.vector.as_bytes());
        dist / (p.vector.len() as f32)
    }

    fn norm_no_header(v: &UnalignedVector<Self::VectorCodec>) -> f32 {
        v.as_bytes()
            .iter()
            .map(|b| b.count_ones() as i32)
            .sum::<i32>() as f32
    }
}

#[inline]
pub fn hamming_bitwise_fast(u: &[u8], v: &[u8]) -> f32 {
    // based on : https://github.com/emschwartz/hamming-bitwise-fast
    // Explicitly structuring the code as below lends itself to SIMD optimizations by
    // the compiler -> https://matklad.github.io/2023/04/09/can-you-trust-a-compiler-to-optimize-your-code.html
    assert_eq!(u.len(), v.len());

    type BitPackedWord = u64;
    const CHUNK_SIZE: usize = std::mem::size_of::<BitPackedWord>();
    let (u_chunks, u_remainder) = u.as_chunks::<CHUNK_SIZE>();
    let (v_chunks, v_remainder) = v.as_chunks::<CHUNK_SIZE>();

    let mut distance = u_chunks
        .iter()
        .zip(v_chunks)
        .map(|(u_chunk, v_chunk)| {
            let u_val = BitPackedWord::from_ne_bytes(*u_chunk);
            let v_val = BitPackedWord::from_ne_bytes(*v_chunk);
            (u_val ^ v_val).count_ones()
        })
        .sum::<u32>();

    distance += u_remainder
        .iter()
        .zip(v_remainder)
        .map(|(u_byte, v_byte)| (u_byte ^ v_byte).count_ones())
        .sum::<u32>();

    distance as f32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bitwise_distance_covers_empty_full_word_and_remainder_inputs() {
        assert_eq!(hamming_bitwise_fast(&[], &[]), 0.0);
        assert_eq!(hamming_bitwise_fast(&[0; 8], &[u8::MAX; 8]), 64.0);
        assert_eq!(hamming_bitwise_fast(&[0; 9], &[u8::MAX; 9]), 72.0);
        assert_eq!(hamming_bitwise_fast(&[0b1010_0000], &[0b0011_0000]), 2.0);
    }

    #[test]
    #[should_panic(expected = "assertion `left == right` failed")]
    fn bitwise_distance_rejects_length_mismatch() {
        let _ = hamming_bitwise_fast(&[0], &[]);
    }
}
