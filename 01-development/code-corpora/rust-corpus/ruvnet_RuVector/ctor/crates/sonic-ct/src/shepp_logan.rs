//! The Shepp–Logan head phantom — the standard analytic benchmark used across
//! computed-tomography reconstruction literature. We rasterise the classic
//! ten-ellipse definition and map its intensity to a speed-of-sound field so
//! our transmission solver can be compared against recognised methods on a
//! recognised target.

use crate::grid::Grid;
use crate::phantom::Phantom;
use crate::types::{Point, Tissue, WATER_SPEED};

/// One ellipse: centre `(x, y)`, semi-axes `(a, b)`, rotation `phi` (radians),
/// additive intensity `gray`.
struct Ellipse {
    x: f32,
    y: f32,
    a: f32,
    b: f32,
    phi: f32,
    gray: f32,
}

/// The canonical Shepp–Logan ellipses (Toft variant intensities).
const ELLIPSES: [Ellipse; 10] = [
    Ellipse { x: 0.0, y: 0.0, a: 0.69, b: 0.92, phi: 0.0, gray: 2.0 },
    Ellipse { x: 0.0, y: -0.0184, a: 0.6624, b: 0.874, phi: 0.0, gray: -0.98 },
    Ellipse { x: 0.22, y: 0.0, a: 0.11, b: 0.31, phi: -0.31416, gray: -0.02 },
    Ellipse { x: -0.22, y: 0.0, a: 0.16, b: 0.41, phi: 0.31416, gray: -0.02 },
    Ellipse { x: 0.0, y: 0.35, a: 0.21, b: 0.25, phi: 0.0, gray: 0.01 },
    Ellipse { x: 0.0, y: 0.1, a: 0.046, b: 0.046, phi: 0.0, gray: 0.01 },
    Ellipse { x: 0.0, y: -0.1, a: 0.046, b: 0.046, phi: 0.0, gray: 0.01 },
    Ellipse { x: -0.08, y: -0.605, a: 0.046, b: 0.023, phi: 0.0, gray: 0.01 },
    Ellipse { x: 0.0, y: -0.606, a: 0.023, b: 0.023, phi: 0.0, gray: 0.01 },
    Ellipse { x: 0.06, y: -0.605, a: 0.023, b: 0.046, phi: 0.0, gray: 0.01 },
];

/// Build a Shepp–Logan speed-of-sound phantom on an `n × n` grid spanning
/// `extent` metres. Intensity is mapped to speed: the high-contrast skull becomes
/// the fast/bone-like ring, brain parenchyma sits near soft tissue, and the
/// coupling background is water.
pub fn shepp_logan(n: usize, extent: f32) -> Phantom {
    let mut speed = Grid::square(n, extent, WATER_SPEED);
    let mut atten = Grid::square(n, extent, Tissue::Water.nominal_attenuation());
    let mut labels = Grid::square(n, extent, Tissue::Water as u8 as f32);

    // The phantom is defined on the unit disc; map grid coords into [-1, 1].
    let half = extent / 2.0;
    for yy in 0..n {
        for xx in 0..n {
            let p = speed.cell_center(xx, yy);
            let u = p.x / half; // [-1, 1]
            let v = p.y / half;
            let mut intensity = 0.0f32;
            for e in &ELLIPSES {
                let (s, c) = e.phi.sin_cos();
                let dx = u - e.x;
                let dy = v - e.y;
                let xr = dx * c + dy * s;
                let yr = -dx * s + dy * c;
                if (xr * xr) / (e.a * e.a) + (yr * yr) / (e.b * e.b) <= 1.0 {
                    intensity += e.gray;
                }
            }
            let i = speed.idx(xx, yy);
            if intensity <= 0.001 {
                continue; // background water
            }
            // Map intensity (~0.01..2.0) to speed (~1450..2400 m/s).
            let c = WATER_SPEED + intensity * 480.0;
            speed.data[i] = c;
            // Derive a plausible attenuation + class label from the speed.
            let t = classify_speed(c);
            atten.data[i] = t.nominal_attenuation();
            labels.data[i] = t as u8 as f32;
        }
    }
    Phantom { speed, attenuation: atten, labels }
}

fn classify_speed(c: f32) -> Tissue {
    if c >= 2200.0 {
        Tissue::Bone
    } else if c >= 1600.0 {
        Tissue::Muscle
    } else if c >= 1500.0 {
        Tissue::Organ
    } else {
        Tissue::Fat
    }
}

/// The phantom's field-of-view centre (origin), for callers building rings.
pub fn center() -> Point {
    Point::new(0.0, 0.0)
}
