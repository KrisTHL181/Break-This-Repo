//! Acknowledged-write latency for the default embedded storage profiles.

use std::sync::Arc;

use db::{HelixDB, HelixDbSource};
use helix_ast::prelude::*;
use helix_ast::query::QueryRequest;

#[global_allocator]
static ALLOC: divan::AllocProfiler = divan::AllocProfiler::system();

fn main() {
    divan::main();
}

struct WriteFixture {
    runtime: tokio::runtime::Runtime,
    db: Arc<HelixDB>,
    request: QueryRequest,
    _disk_root: Option<tempfile::TempDir>,
}

impl WriteFixture {
    fn in_memory() -> Self {
        Self::new(None)
    }

    fn on_disk() -> Self {
        Self::new(Some(
            tempfile::tempdir().expect("embedded benchmark disk root"),
        ))
    }

    fn new(disk_root: Option<tempfile::TempDir>) -> Self {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("embedded benchmark runtime starts");
        let source = match &disk_root {
            Some(root) => HelixDbSource::Disk {
                root: root.path().to_path_buf(),
                database: "embedded-write-latency".to_string(),
            },
            None => HelixDbSource::InMemory {
                database: "embedded-write-latency".to_string(),
            },
        };
        let db = Arc::new(
            runtime
                .block_on(HelixDB::open(source))
                .expect("embedded benchmark database opens"),
        );
        let request = QueryRequest::write(
            write_batch()
                .var_as(
                    "node",
                    g().add_n(
                        "EmbeddedWrite",
                        vec![("value", PropertyInput::from("benchmark"))],
                    ),
                )
                .returning(Vec::<String>::new()),
        );
        runtime
            .block_on(db.query(request.clone()))
            .expect("embedded benchmark warm-up write succeeds");
        Self {
            runtime,
            db,
            request,
            _disk_root: disk_root,
        }
    }
}

#[divan::bench]
fn in_memory(bencher: divan::Bencher<'_, '_>) {
    let fixture = WriteFixture::in_memory();
    bencher.bench_local(|| {
        divan::black_box(
            fixture
                .runtime
                .block_on(fixture.db.query(fixture.request.clone()))
                .expect("in-memory embedded write succeeds"),
        )
    });
}

#[divan::bench]
fn on_disk(bencher: divan::Bencher<'_, '_>) {
    let fixture = WriteFixture::on_disk();
    bencher.bench_local(|| {
        divan::black_box(
            fixture
                .runtime
                .block_on(fixture.db.query(fixture.request.clone()))
                .expect("disk embedded write succeeds"),
        )
    });
}
