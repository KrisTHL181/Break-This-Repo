use criterion::{Criterion, criterion_group, criterion_main};
mod serialize;

fn world_format_bench(c: &mut Criterion) {
    serialize::bench_serialize_world(c);
}

criterion_group!(benches, world_format_bench);
criterion_main!(benches);
