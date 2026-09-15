use pyo3::prelude::*;

#[pymodule]
fn break_this_repo(_module: &Bound<'_, PyModule>) -> PyResult<()> {
    Ok(())
}