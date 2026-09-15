//! A regular 2-D scalar field sampled on a square grid.

use crate::types::{Point, Result, SonicError};

/// A dense row-major 2-D scalar field with physical spacing.
///
/// Index convention: `data[y * nx + x]`, with cell `(0,0)` centred at
/// `origin + (0.5*dx, 0.5*dy)`. Physical coordinates increase with index.
#[derive(Debug, Clone, PartialEq)]
pub struct Grid {
    /// Number of cells along X.
    pub nx: usize,
    /// Number of cells along Y.
    pub ny: usize,
    /// Cell size along X (m).
    pub dx: f32,
    /// Cell size along Y (m).
    pub dy: f32,
    /// Physical coordinate of the grid's lower-left corner (m).
    pub origin: Point,
    /// Row-major scalar values.
    pub data: Vec<f32>,
}

impl Grid {
    /// Allocate a grid filled with `fill`.
    pub fn filled(nx: usize, ny: usize, dx: f32, dy: f32, origin: Point, fill: f32) -> Self {
        Grid {
            nx,
            ny,
            dx,
            dy,
            origin,
            data: vec![fill; nx * ny],
        }
    }

    /// Build a square grid spanning `extent` metres centred on the origin.
    pub fn square(n: usize, extent: f32, fill: f32) -> Self {
        let d = extent / n as f32;
        let origin = Point::new(-extent / 2.0, -extent / 2.0);
        Grid::filled(n, n, d, d, origin, fill)
    }

    /// Number of cells.
    #[inline]
    pub fn len(&self) -> usize {
        self.data.len()
    }

    /// Whether the grid has zero cells.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.data.is_empty()
    }

    /// Flat index for integer cell coordinates (no bounds check beyond clamp).
    #[inline]
    pub fn idx(&self, x: usize, y: usize) -> usize {
        y * self.nx + x
    }

    /// Physical centre of cell `(x, y)`.
    #[inline]
    pub fn cell_center(&self, x: usize, y: usize) -> Point {
        Point::new(
            self.origin.x + (x as f32 + 0.5) * self.dx,
            self.origin.y + (y as f32 + 0.5) * self.dy,
        )
    }

    /// Map a physical point to integer cell coordinates, or `None` if outside.
    #[inline]
    pub fn point_to_cell(&self, p: Point) -> Option<(usize, usize)> {
        let fx = (p.x - self.origin.x) / self.dx;
        let fy = (p.y - self.origin.y) / self.dy;
        if fx < 0.0 || fy < 0.0 {
            return None;
        }
        let x = fx as usize;
        let y = fy as usize;
        if x >= self.nx || y >= self.ny {
            None
        } else {
            Some((x, y))
        }
    }

    /// Nearest-neighbour sample at a physical point; returns `None` if outside.
    #[inline]
    pub fn sample(&self, p: Point) -> Option<f32> {
        self.point_to_cell(p).map(|(x, y)| self.data[self.idx(x, y)])
    }

    /// Minimum and maximum values; `(0,0)` for an empty grid.
    pub fn min_max(&self) -> (f32, f32) {
        let mut lo = f32::INFINITY;
        let mut hi = f32::NEG_INFINITY;
        for &v in &self.data {
            if v < lo {
                lo = v;
            }
            if v > hi {
                hi = v;
            }
        }
        if self.data.is_empty() {
            (0.0, 0.0)
        } else {
            (lo, hi)
        }
    }

    /// Mean absolute difference against another identically shaped grid.
    pub fn mean_abs_diff(&self, other: &Grid) -> Result<f32> {
        if self.nx != other.nx || self.ny != other.ny {
            return Err(SonicError::DimensionMismatch);
        }
        if self.data.is_empty() {
            return Ok(0.0);
        }
        let mut acc = 0.0f64;
        for (a, b) in self.data.iter().zip(&other.data) {
            acc += (a - b).abs() as f64;
        }
        Ok((acc / self.data.len() as f64) as f32)
    }

    /// Downsample to a `k x k` average-pooled, L2-normalised feature vector.
    ///
    /// This is the embedding used by the acoustic memory index: it captures the
    /// coarse spatial structure of a reconstruction while being robust to small
    /// pixel-level shifts (semantic rather than pixel-exact comparison).
    pub fn embedding(&self, k: usize) -> Vec<f32> {
        let mut out = vec![0.0f32; k * k];
        if self.nx == 0 || self.ny == 0 {
            return out;
        }
        let mut counts = vec![0u32; k * k];
        for y in 0..self.ny {
            let by = (y * k) / self.ny;
            for x in 0..self.nx {
                let bx = (x * k) / self.nx;
                let bi = by * k + bx;
                out[bi] += self.data[self.idx(x, y)];
                counts[bi] += 1;
            }
        }
        for i in 0..out.len() {
            if counts[i] > 0 {
                out[i] /= counts[i] as f32;
            }
        }
        // Mean-centre so the (large, uninformative) water-background DC term does
        // not dominate cosine similarity — what matters is structural deviation.
        let mean = out.iter().sum::<f32>() / out.len().max(1) as f32;
        for v in &mut out {
            *v -= mean;
        }
        // L2 normalise so cosine similarity is well-defined.
        let norm: f32 = out.iter().map(|v| v * v).sum::<f32>().sqrt();
        if norm > 0.0 {
            for v in &mut out {
                *v /= norm;
            }
        }
        out
    }

    /// Parse a binary PGM (P5) into a square grid of raw 0..255 values.
    ///
    /// Returns `None` on malformed input. Used to ingest real anatomical slices
    /// as ground-truth phantoms.
    pub fn from_pgm(bytes: &[u8], extent: f32) -> Option<Grid> {
        // Header: "P5\n<w> <h>\n<max>\n" (whitespace-separated, may include comments).
        let mut pos = 0usize;
        let magic = read_token(bytes, &mut pos)?;
        if magic != b"P5" {
            return None;
        }
        let w: usize = parse_ascii(read_token(bytes, &mut pos)?)?;
        let h: usize = parse_ascii(read_token(bytes, &mut pos)?)?;
        let _max: usize = parse_ascii(read_token(bytes, &mut pos)?)?;
        pos += 1; // single whitespace after maxval
        if w != h || pos + w * h > bytes.len() {
            return None;
        }
        let mut g = Grid::square(w, extent, 0.0);
        // PGM is top-down; flip to the grid's bottom-up convention.
        for y in 0..h {
            for x in 0..w {
                g.data[(h - 1 - y) * w + x] = bytes[pos + y * w + x] as f32;
            }
        }
        Some(g)
    }

    /// Render to an 8-bit PGM (P5) byte buffer, linearly scaled to `[lo, hi]`.
    pub fn to_pgm(&self, lo: f32, hi: f32) -> Vec<u8> {
        let mut out = Vec::with_capacity(64 + self.nx * self.ny);
        let header = format!("P5\n{} {}\n255\n", self.nx, self.ny);
        out.extend_from_slice(header.as_bytes());
        let span = if (hi - lo).abs() < f32::EPSILON { 1.0 } else { hi - lo };
        // PGM origin is top-left; flip Y so images look upright.
        for y in (0..self.ny).rev() {
            for x in 0..self.nx {
                let v = self.data[self.idx(x, y)];
                let t = ((v - lo) / span).clamp(0.0, 1.0);
                out.push((t * 255.0) as u8);
            }
        }
        out
    }
}

/// Read a whitespace-delimited token from `bytes` starting at `*pos`, skipping
/// leading whitespace and `#` comment lines. Advances `*pos` past the token.
fn read_token<'a>(bytes: &'a [u8], pos: &mut usize) -> Option<&'a [u8]> {
    while *pos < bytes.len() {
        let c = bytes[*pos];
        if c == b'#' {
            while *pos < bytes.len() && bytes[*pos] != b'\n' {
                *pos += 1;
            }
        } else if c.is_ascii_whitespace() {
            *pos += 1;
        } else {
            break;
        }
    }
    let start = *pos;
    while *pos < bytes.len() && !bytes[*pos].is_ascii_whitespace() {
        *pos += 1;
    }
    if *pos > start {
        Some(&bytes[start..*pos])
    } else {
        None
    }
}

fn parse_ascii<T: std::str::FromStr>(b: &[u8]) -> Option<T> {
    std::str::from_utf8(b).ok()?.parse().ok()
}
