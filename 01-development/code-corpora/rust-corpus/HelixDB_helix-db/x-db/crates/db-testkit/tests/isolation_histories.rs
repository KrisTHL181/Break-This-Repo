//! Public planner/interpreter isolation histories with recorded request windows.

#![recursion_limit = "256"]

use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

use db::{HelixDB, HelixDbSource};
use helix_ast::batch;
use helix_ast::graph::NodeRef;
use helix_ast::query::QueryRequest;
use helix_ast::traversal::{self, Order};
use helix_ast::value::PropertyInput;

#[derive(Debug, Clone, Copy)]
struct RequestWindow {
    started: u64,
    finished: u64,
}

#[tokio::test(flavor = "multi_thread", worker_threads = 4)]
async fn overlapping_public_reads_never_mix_pre_and_post_commit_state() {
    for repetition in 0..12_u64 {
        let db = Arc::new(
            HelixDB::open(HelixDbSource::InMemory {
                database: format!("testkit-isolation-history-{repetition}"),
            })
            .await
            .unwrap(),
        );
        let source = insert_node(&db, 0, 0).await;
        let target = insert_node(&db, 1, 0).await;
        insert_edge(&db, source, target).await;
        let before = db.query(read_snapshot(source)).await.unwrap();

        let clock = Arc::new(AtomicU64::new(1));
        let barrier = Arc::new(tokio::sync::Barrier::new(3));
        let read_db = Arc::clone(&db);
        let read_clock = Arc::clone(&clock);
        let read_barrier = Arc::clone(&barrier);
        let read_task = tokio::spawn(async move {
            let started = read_clock.fetch_add(1, Ordering::SeqCst);
            read_barrier.wait().await;
            let result = read_db.query(read_snapshot(source)).await;
            let finished = read_clock.fetch_add(1, Ordering::SeqCst);
            (RequestWindow { started, finished }, result)
        });
        let write_db = Arc::clone(&db);
        let write_clock = Arc::clone(&clock);
        let write_barrier = Arc::clone(&barrier);
        let write_task = tokio::spawn(async move {
            let started = write_clock.fetch_add(1, Ordering::SeqCst);
            write_barrier.wait().await;
            let result = write_db.query(concurrent_write(source)).await;
            let finished = write_clock.fetch_add(1, Ordering::SeqCst);
            (RequestWindow { started, finished }, result)
        });
        barrier.wait().await;
        let (read_window, observed) = read_task.await.unwrap();
        let (write_window, write_result) = write_task.await.unwrap();
        write_result.unwrap();
        let observed = observed.unwrap();
        let after = db.query(read_snapshot(source)).await.unwrap();

        assert!(read_window.started < write_window.finished);
        assert!(write_window.started < read_window.finished);
        assert_ne!(before, after, "fixture mutation must change the read image");
        assert!(
            observed == before || observed == after,
            "one request returned a mixed snapshot: {observed}"
        );
        assert_eq!(before["point"][0]["version"], 0);
        assert_eq!(after["point"][0]["version"], 1);
        assert_eq!(before["aggregate"], 2);
        assert_eq!(after["aggregate"], 3);
        assert_eq!(before["traversal"], 1);
        assert_eq!(after["traversal"], 2);
        db.close().await.unwrap();
    }
}

#[tokio::test(flavor = "multi_thread", worker_threads = 4)]
async fn overlapping_public_writers_serialize_or_return_a_typed_conflict() {
    for repetition in 0..12_u64 {
        let db = Arc::new(
            HelixDB::open(HelixDbSource::InMemory {
                database: format!("testkit-writer-history-{repetition}"),
            })
            .await
            .unwrap(),
        );
        let source = insert_node(&db, 0, 0).await;
        let barrier = Arc::new(tokio::sync::Barrier::new(3));
        let mut tasks = Vec::new();
        for version in [1_i64, 2_i64] {
            let task_db = Arc::clone(&db);
            let task_barrier = Arc::clone(&barrier);
            tasks.push(tokio::spawn(async move {
                task_barrier.wait().await;
                task_db.query(set_version(source, version)).await
            }));
        }
        barrier.wait().await;
        let mut successes = 0;
        for task in tasks {
            match task.await.unwrap() {
                Ok(_) => successes += 1,
                Err(error) => assert!(
                    error.is_transaction_conflict(),
                    "overlapping writer returned a non-conflict error: {error}"
                ),
            }
        }
        assert!(successes >= 1);
        let visible = db.query(point_version(source)).await.unwrap();
        assert!(visible["version"][0]["version"] == 1 || visible["version"][0]["version"] == 2);
        db.close().await.unwrap();
    }
}

#[tokio::test]
async fn writer_following_request_observes_commit_and_rejected_write_is_atomic() {
    let db = HelixDB::open(HelixDbSource::InMemory {
        database: "testkit-read-after-write".to_string(),
    })
    .await
    .unwrap();
    let source = insert_node(&db, 0, 0).await;
    db.query(set_version(source, 7)).await.unwrap();
    assert_eq!(
        db.query(point_version(source)).await.unwrap()["version"][0]["version"],
        7
    );

    let rejected = QueryRequest::write(
        batch::write_batch()
            .var_as(
                "updated",
                traversal::g()
                    .n(NodeRef::id(source))
                    .set_property("version", 99),
            )
            .returning(["updated", "updated"]),
    );
    assert!(db.query(rejected).await.is_err());
    assert_eq!(
        db.query(point_version(source)).await.unwrap()["version"][0]["version"],
        7
    );
    db.close().await.unwrap();
}

fn read_snapshot(source: u64) -> QueryRequest {
    QueryRequest::read(
        batch::read_batch()
            .var_as(
                "point",
                traversal::g()
                    .n(NodeRef::id(source))
                    .values(vec!["version"]),
            )
            .var_as(
                "range",
                traversal::g()
                    .n(NodeRef::all())
                    .order_by("rank", Order::Asc)
                    .range(0_usize, 4_usize)
                    .id(),
            )
            .var_as(
                "traversal",
                traversal::g()
                    .n(NodeRef::id(source))
                    .both(Some("LINK"))
                    .count(),
            )
            .var_as(
                "projection",
                traversal::g()
                    .n(NodeRef::id(source))
                    .value_map(Some(vec!["version", "rank"])),
            )
            .var_as("aggregate", traversal::g().n(NodeRef::all()).count())
            .returning(["point", "range", "traversal", "projection", "aggregate"]),
    )
}

fn point_version(source: u64) -> QueryRequest {
    QueryRequest::read(
        batch::read_batch()
            .var_as(
                "version",
                traversal::g()
                    .n(NodeRef::id(source))
                    .values(vec!["version"]),
            )
            .returning(["version"]),
    )
}

fn concurrent_write(source: u64) -> QueryRequest {
    QueryRequest::write(
        batch::write_batch()
            .var_as(
                "updated",
                traversal::g()
                    .n(NodeRef::id(source))
                    .set_property("version", 1_i64),
            )
            .var_as(
                "created",
                traversal::g()
                    .add_n(
                        "Document",
                        vec![
                            ("rank", PropertyInput::from(2_i64)),
                            ("version", PropertyInput::from(1_i64)),
                        ],
                    )
                    .add_e(
                        "LINK",
                        NodeRef::id(source),
                        Vec::<(&str, PropertyInput)>::new(),
                    ),
            )
            .returning(["updated", "created"]),
    )
}

fn set_version(source: u64, version: i64) -> QueryRequest {
    QueryRequest::write(
        batch::write_batch()
            .var_as(
                "updated",
                traversal::g()
                    .n(NodeRef::id(source))
                    .set_property("version", version),
            )
            .returning(["updated"]),
    )
}

async fn insert_node(db: &HelixDB, rank: i64, version: i64) -> u64 {
    let response = db
        .query(QueryRequest::write(
            batch::write_batch()
                .var_as(
                    "created",
                    traversal::g()
                        .add_n(
                            "Document",
                            vec![
                                ("rank", PropertyInput::from(rank)),
                                ("version", PropertyInput::from(version)),
                            ],
                        )
                        .id(),
                )
                .returning(["created"]),
        ))
        .await
        .unwrap();
    response["created"][0].as_u64().unwrap()
}

async fn insert_edge(db: &HelixDB, source: u64, target: u64) {
    db.query(QueryRequest::write(
        batch::write_batch()
            .var_as(
                "created",
                traversal::g().n(NodeRef::id(source)).add_e(
                    "LINK",
                    NodeRef::id(target),
                    Vec::<(&str, PropertyInput)>::new(),
                ),
            )
            .returning(["created"]),
    ))
    .await
    .unwrap();
}
