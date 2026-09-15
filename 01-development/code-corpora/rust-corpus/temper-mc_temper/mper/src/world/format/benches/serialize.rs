use criterion::Criterion;
use std::hint::black_box;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::{ChunkBlockPos, ChunkHeight};
use temper_world_format::Chunk;

fn serialize_chunk(chunk: &mut Chunk) -> Vec<u8> {
    black_box(bitcode::serialize(black_box(chunk)).unwrap())
}

pub fn bench_serialize_world(c: &mut Criterion) {
    let mut chunk = Chunk::new_empty_with_height(ChunkHeight::new(-64, 384));
    for x in 0..16 {
        for y in -64..320 {
            for z in 0..16 {
                if (x + z) % 3 == 0 {
                    chunk.set_block(
                        ChunkBlockPos::new(x, y, z),
                        BlockStateId::new(rand::random_range(0..20_000)),
                    )
                }
            }
        }
    }
    c.bench_function("format/Serialize", |b| {
        b.iter(|| {
            black_box(serialize_chunk(&mut chunk));
        })
    });

    let serialized = serialize_chunk(&mut chunk);

    c.bench_function("format/Deserialize", |b| {
        b.iter(|| {
            black_box(bitcode::deserialize::<Chunk>(black_box(&serialized))).unwrap();
        })
    });
}
