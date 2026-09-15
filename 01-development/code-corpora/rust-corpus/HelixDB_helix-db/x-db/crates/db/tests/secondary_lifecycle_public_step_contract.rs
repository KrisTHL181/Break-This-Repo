//! Compile-time contract for the production stepping surface promised by
//! `SecondaryIndexLifecycleWorkerMode::Disabled`.

async fn advance_one_bounded_item(db: &db::HelixDB) -> Result<bool, db::error::HelixDbError> {
    db.process_secondary_index_lifecycle_once().await
}

#[test]
fn disabled_secondary_lifecycle_has_a_public_one_step_api() {
    let _ = advance_one_bounded_item;
}
