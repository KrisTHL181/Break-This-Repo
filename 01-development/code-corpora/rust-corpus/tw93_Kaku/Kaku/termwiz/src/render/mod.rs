pub mod terminfo;

pub trait RenderTty: std::io::Write {
    /// Returns the (cols, rows) for the terminal
    fn get_size_in_cells(&mut self) -> crate::Result<(usize, usize)>;
}
