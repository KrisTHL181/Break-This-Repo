use crate::error::AppError;
use crate::Database;
use serde_json::{json, Value};
use std::path::PathBuf;

/// Export the SQLite database to a CC Switch compatible SQL file.
///
/// This mirrors the upstream command signature style (`Result<Value, String>`).
pub async fn export_config_to_file(file_path: String) -> Result<Value, String> {
    let target_path = PathBuf::from(&file_path);

    let Some(parent) = target_path.parent() else {
        return Err(
            AppError::InvalidInput(format!("Invalid export path: {file_path}")).to_string(),
        );
    };
    if !parent.as_os_str().is_empty() {
        std::fs::create_dir_all(parent).map_err(|e| AppError::io(parent, e).to_string())?;
    }

    let db = Database::init().map_err(|e| e.to_string())?;
    db.export_sql(&target_path).map_err(|e| e.to_string())?;

    Ok(json!({
        "success": true,
        "message": "SQL exported successfully",
        "filePath": file_path
    }))
}
