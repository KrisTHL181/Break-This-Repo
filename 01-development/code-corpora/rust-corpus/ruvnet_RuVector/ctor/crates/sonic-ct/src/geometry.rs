//! Circular transducer ring geometry.

use crate::types::Point;

/// A circular ring of transducer elements, each able to transmit and receive.
///
/// Elements are placed counter-clockwise starting at angle 0 (the +X axis).
#[derive(Debug, Clone)]
pub struct Ring {
    /// Ring radius (m).
    pub radius: f32,
    /// Element centre positions.
    pub positions: Vec<Point>,
    /// Inward-pointing unit normals (towards the ring centre at the origin).
    pub normals: Vec<Point>,
}

impl Ring {
    /// Build a ring of `count` elements with the given `radius`, centred at the
    /// origin.
    pub fn new(count: usize, radius: f32) -> Self {
        let mut positions = Vec::with_capacity(count);
        let mut normals = Vec::with_capacity(count);
        for i in 0..count {
            let theta = (i as f32) * std::f32::consts::TAU / count as f32;
            let (s, c) = theta.sin_cos();
            positions.push(Point::new(radius * c, radius * s));
            // Inward normal points from the element towards the centre.
            normals.push(Point::new(-c, -s));
        }
        Ring {
            radius,
            positions,
            normals,
        }
    }

    /// Number of elements.
    #[inline]
    pub fn count(&self) -> usize {
        self.positions.len()
    }

    /// Receiver indices forming a transmission fan opposite `source`.
    ///
    /// For element `source`, returns the `fan` receivers centred on the
    /// diametrically opposite element, skipping any receiver whose angular
    /// separation from the source is below `min_sep_frac` of a half-turn (these
    /// near-neighbour paths graze the ring and carry little tissue information).
    pub fn fan_receivers(&self, source: usize, fan: usize, min_sep_frac: f32) -> Vec<usize> {
        let n = self.count();
        if n == 0 {
            return Vec::new();
        }
        let opposite = (source + n / 2) % n;
        let half = fan / 2;
        let min_sep = ((min_sep_frac.clamp(0.0, 1.0)) * (n as f32 / 2.0)) as usize;
        let mut out = Vec::with_capacity(fan);
        for k in 0..fan {
            // Centre the fan window on the opposite element.
            let offset = k as isize - half as isize;
            let r = (opposite as isize + offset).rem_euclid(n as isize) as usize;
            if r == source {
                continue;
            }
            // Angular separation in element steps, taken the short way round.
            let raw = (r as isize - source as isize).rem_euclid(n as isize) as usize;
            let sep = raw.min(n - raw);
            if sep < min_sep {
                continue;
            }
            out.push(r);
        }
        out
    }
}
