//! Straight-ray sampling through a grid.
//!
//! Reconstruction here uses a straight-ray (Born/first-arrival) approximation:
//! refraction and diffraction are ignored. This is the standard time-of-flight
//! USCT baseline that full-waveform inversion later improves upon.

use crate::grid::Grid;
use crate::types::Point;

/// A discretised straight ray expressed as a list of `(cell_index, length)`
/// contributions, where `length` is the path length (m) the ray spends in that
/// cell. Built once per source/receiver pair and reused across SART iterations.
#[derive(Debug, Clone)]
pub struct Ray {
    /// `(flat cell index, path length in metres)` pairs.
    pub cells: Vec<(usize, f32)>,
    /// Total straight-line length of the ray (m).
    pub length: f32,
}

impl Ray {
    /// Build a ray between `a` and `b`, accumulating per-cell path lengths on
    /// `grid` via uniform supersampling.
    ///
    /// `samples_per_cell` controls accuracy: the segment is split into roughly
    /// `samples_per_cell` points per grid cell traversed. Contributions to the
    /// same cell are merged so each cell appears once.
    pub fn between(grid: &Grid, a: Point, b: Point, samples_per_cell: f32) -> Ray {
        let length = a.dist(b);
        if length <= 0.0 || grid.dx <= 0.0 {
            return Ray { cells: Vec::new(), length };
        }
        // Number of integration steps along the ray.
        let cells_crossed = length / grid.dx;
        let steps = ((cells_crossed * samples_per_cell).ceil() as usize).max(1);
        let dl = length / steps as f32;

        // Accumulate into a small map keyed by cell index. Rays are short so a
        // linear-probe Vec is faster than a HashMap here.
        let mut acc: Vec<(usize, f32)> = Vec::with_capacity(steps.min(grid.nx * 2));
        let inv = 1.0 / steps as f32;
        for s in 0..steps {
            let t = (s as f32 + 0.5) * inv;
            let p = Point::new(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
            if let Some((cx, cy)) = grid.point_to_cell(p) {
                let ci = grid.idx(cx, cy);
                match acc.iter_mut().find(|(c, _)| *c == ci) {
                    Some(e) => e.1 += dl,
                    None => acc.push((ci, dl)),
                }
            }
        }
        Ray { cells: acc, length }
    }

    /// Integrate a per-cell field along the ray: `Σ field[cell] * length`.
    ///
    /// With `field = slowness (1/c)` this yields travel time; with
    /// `field = attenuation` it yields total attenuation in nepers.
    #[inline]
    pub fn integrate(&self, field: &[f32]) -> f32 {
        let mut acc = 0.0f32;
        for &(c, l) in &self.cells {
            acc += field[c] * l;
        }
        acc
    }

    /// Sum of path lengths actually deposited inside the grid (m).
    #[inline]
    pub fn interior_length(&self) -> f32 {
        self.cells.iter().map(|&(_, l)| l).sum()
    }
}
