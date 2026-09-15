#![recursion_limit = "256"]

//! Process-isolated public contracts for the row-mode environment cap.
//!
//! Environment variables are process-global, so the parent test launches this
//! binary as a child with the requested value present from process startup.
//! The child then exercises the compiled production library without mutating a
//! multithreaded test process's environment.

use std::process::Command;

use db::{HelixDB, HelixDbSource};
use helix_ast::batch;
use helix_ast::graph::NodeRef;
use helix_ast::projection::BindingProjection;
use helix_ast::query::QueryRequest;
use helix_ast::traversal;
use helix_ast::value::PropertyInput;

const PROBE_CASE_ENV: &str = "HELIX_ROW_MODE_PROBE_CASE";
const ROW_MODE_MAX_ROWS_ENV: &str = "HELIX_ROW_MODE_MAX_ROWS";

#[test]
fn public_row_mode_environment_contracts_are_process_isolated() {
    for (case, cap) in [
        ("enabled", "1"),
        ("cached", "3"),
        ("zero", "0"),
        ("invalid", "not-a-number"),
    ] {
        let status = Command::new(std::env::current_exe().expect("test executable is available"))
            .arg("--exact")
            .arg("row_mode_environment_probe")
            .arg("--nocapture")
            .env(PROBE_CASE_ENV, case)
            .env(ROW_MODE_MAX_ROWS_ENV, cap)
            .status()
            .expect("row-mode probe process starts");
        assert!(status.success(), "row-mode {case} probe succeeds");
    }

    #[cfg(unix)]
    {
        use std::os::unix::ffi::OsStringExt;

        let status = Command::new(std::env::current_exe().expect("test executable is available"))
            .arg("--exact")
            .arg("row_mode_environment_probe")
            .arg("--nocapture")
            .env(PROBE_CASE_ENV, "not-unicode")
            .env(
                ROW_MODE_MAX_ROWS_ENV,
                std::ffi::OsString::from_vec(vec![0xFF]),
            )
            .status()
            .expect("non-Unicode row-mode probe process starts");
        assert!(status.success(), "non-Unicode row-mode probe succeeds");
    }
}

#[tokio::test]
async fn row_mode_environment_probe() {
    let Ok(case) = std::env::var(PROBE_CASE_ENV) else {
        return;
    };
    let db = HelixDB::open(HelixDbSource::InMemory {
        database: format!("production-row-mode-{case}-{}", std::process::id()),
    })
    .await
    .expect("row-mode fixture opens");
    db.query(QueryRequest::write(
        batch::write_batch()
            .var_as(
                "first",
                traversal::g().add_n("Item", vec![("rank", PropertyInput::from(1_i64))]),
            )
            .var_as(
                "second",
                traversal::g().add_n("Item", vec![("rank", PropertyInput::from(2_i64))]),
            )
            .returning(Vec::<String>::new()),
    ))
    .await
    .expect("row-mode fixture is written");

    if case == "cached" {
        let response = db
            .query(QueryRequest::read(
                batch::read_batch()
                    .var_as(
                        "rows",
                        traversal::g()
                            .n(NodeRef::all())
                            .bind("row")
                            .limit(1_usize)
                            .project_bindings(vec![BindingProjection::current("$id", "id")]),
                    )
                    .returning(["rows"]),
            ))
            .await
            .expect("cached row-mode cap accepts each bounded operation");
        assert_eq!(response, serde_json::json!({ "rows": [{ "id": 0 }] }));
        db.close().await.expect("row-mode fixture closes");
        return;
    }

    let error = db
        .query(QueryRequest::read(
            batch::read_batch()
                .var_as(
                    "rows",
                    traversal::g()
                        .n(NodeRef::all())
                        .bind("row")
                        .project_bindings(vec![BindingProjection::current("$id", "id")]),
                )
                .returning(["rows"]),
        ))
        .await
        .expect_err("configured row-mode cap rejects the bound stream");
    match case.as_str() {
        "enabled" => assert_eq!(
            error.to_string(),
            "Query error: bind() produced 2 row-mode rows, exceeding HELIX_ROW_MODE_MAX_ROWS=1"
        ),
        "invalid" => assert_eq!(
            error.to_string(),
            "Query error: HELIX_ROW_MODE_MAX_ROWS must be a positive integer; got `not-a-number`"
        ),
        "zero" => assert_eq!(
            error.to_string(),
            "Query error: HELIX_ROW_MODE_MAX_ROWS must be a positive integer; got `0`"
        ),
        "not-unicode" => assert_eq!(
            error.to_string(),
            "Query error: HELIX_ROW_MODE_MAX_ROWS must be a positive integer; value is not valid unicode"
        ),
        other => panic!("unknown row-mode probe case `{other}`"),
    }
    db.close().await.expect("row-mode fixture closes");
}
