//! Synthetic, anatomically-structured body-model generation.
//!
//! The phantom is a procedural *digital human torso* (in the spirit of
//! computational phantoms such as XCAT) — not a scanned real patient. It is
//! fully deterministic given a seed, which makes it a reproducible "public"
//! dataset for benchmarking reconstruction quality.
//!
//! Anatomy varies along the cranio-caudal axis `z ∈ [0, 1]` (0 = pelvis,
//! 1 = upper abdomen / lower thorax), so a vertical sweep through the model
//! produces a coherent 3-D body (see [`crate::volume3d`]).

use crate::grid::Grid;
use crate::types::{Point, Tissue, WATER_SPEED};

/// A ground-truth phantom slice: co-registered speed, attenuation, and labels.
#[derive(Debug, Clone)]
pub struct Phantom {
    /// Speed-of-sound map (m/s).
    pub speed: Grid,
    /// Acoustic attenuation map (Np/m).
    pub attenuation: Grid,
    /// Per-cell tissue labels (stored as `f32` of the `u8` value).
    pub labels: Grid,
}

/// Parameters controlling phantom synthesis.
#[derive(Debug, Clone, Copy)]
pub struct PhantomConfig {
    /// Grid resolution (cells per side).
    pub n: usize,
    /// Physical field of view (m).
    pub extent: f32,
    /// Deterministic seed; varies organ placement and sizes.
    pub seed: u64,
}

impl Default for PhantomConfig {
    fn default() -> Self {
        PhantomConfig {
            n: 96,
            extent: 0.24,
            seed: 1,
        }
    }
}

/// A tiny deterministic PRNG (SplitMix64) so phantoms are reproducible without
/// pulling in the `rand` crate (keeps the core dependency-free).
struct SplitMix64(u64);

impl SplitMix64 {
    fn next_u64(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }
    fn uniform(&mut self, lo: f32, hi: f32) -> f32 {
        let u = (self.next_u64() >> 11) as f32 / (1u64 << 53) as f32;
        lo + (hi - lo) * u
    }
}

#[inline]
fn in_ellipse(p: Point, c: Point, rx: f32, ry: f32) -> bool {
    let dx = (p.x - c.x) / rx;
    let dy = (p.y - c.y) / ry;
    dx * dx + dy * dy <= 1.0
}

/// A filled anatomical structure (ellipse) drawn at a fixed tissue class.
#[derive(Clone, Copy)]
struct Blob {
    c: Point,
    rx: f32,
    ry: f32,
    tissue: Tissue,
}

#[inline]
fn smoothstep(edge0: f32, edge1: f32, x: f32) -> f32 {
    let t = ((x - edge0) / (edge1 - edge0)).clamp(0.0, 1.0);
    t * t * (3.0 - 2.0 * t)
}

impl Phantom {
    /// Build the canonical mid-abdomen slice (`z = 0.5`).
    pub fn build(cfg: PhantomConfig) -> Phantom {
        Phantom::build_slice(cfg, 0.5)
    }

    /// Build an anatomically-structured cross-section at cranio-caudal height
    /// `z ∈ [0, 1]` (0 = pelvis, 1 = upper abdomen / lower thorax).
    pub fn build_slice(cfg: PhantomConfig, z: f32) -> Phantom {
        let z = z.clamp(0.0, 1.0);
        // Seed mixes the subject seed with the slice height so each plane is
        // distinct yet reproducible.
        let zk = (z * 1000.0) as u64;
        let mut rng = SplitMix64(
            cfg.seed
                .wrapping_mul(0x2545_F491_4F6C_DD1D)
                .wrapping_add(zk.wrapping_mul(0x9E37_79B9))
                .wrapping_add(1),
        );

        let n = cfg.n;
        let ext = cfg.extent;
        let mut speed = Grid::square(n, ext, WATER_SPEED);
        let mut atten = Grid::square(n, ext, Tissue::Water.nominal_attenuation());
        let mut labels = Grid::square(n, ext, Tissue::Water as u8 as f32);

        // --- Body outline: torso widens superiorly (chest) and the waist is the
        //     narrowest point of the abdomen. ---
        let chest = smoothstep(0.55, 1.0, z);
        let body_c = Point::new(rng.uniform(-0.008, 0.008), rng.uniform(-0.008, 0.008));
        let body_rx = (0.078 + 0.018 * chest) * rng.uniform(0.97, 1.03);
        let body_ry = (0.060 + 0.014 * chest) * rng.uniform(0.97, 1.03);
        let fat_t = rng.uniform(0.007, 0.012);
        let muscle_t = rng.uniform(0.009, 0.015);

        // Posterior spine position (negative Y is the back).
        let spine_c = Point::new(body_c.x, body_c.y - body_ry * rng.uniform(0.60, 0.70));

        // --- Region-dependent soft organs (Organ class). ---
        let mut organs: Vec<Blob> = Vec::new();
        // Aorta / great vessel: a small lumen just anterior to the spine,
        // present through abdomen and chest.
        if z > 0.2 {
            organs.push(Blob {
                c: Point::new(spine_c.x + rng.uniform(-0.004, 0.004), spine_c.y + 0.012),
                rx: rng.uniform(0.005, 0.008),
                ry: rng.uniform(0.005, 0.008),
                tissue: Tissue::Organ,
            });
        }
        if z >= 0.82 {
            // Thorax: heart (central, slightly left) + paired lungs (lateral).
            // Lungs are modelled as soft-tissue parenchyma here; true air-lung
            // acoustics (near-total shadowing) is future work.
            organs.push(Blob {
                c: Point::new(body_c.x - rng.uniform(0.004, 0.014), body_c.y + rng.uniform(0.006, 0.018)),
                rx: rng.uniform(0.020, 0.028),
                ry: rng.uniform(0.020, 0.028),
                tissue: Tissue::Organ,
            }); // heart
            for &s in &[-1.0f32, 1.0] {
                organs.push(Blob {
                    c: Point::new(body_c.x + s * rng.uniform(0.030, 0.044), body_c.y + rng.uniform(0.002, 0.014)),
                    rx: rng.uniform(0.018, 0.026),
                    ry: rng.uniform(0.022, 0.032),
                    tissue: Tissue::Organ,
                }); // lung
            }
        } else if z >= 0.6 {
            // Upper abdomen: large liver (right) + spleen (left).
            organs.push(Blob {
                c: Point::new(body_c.x + rng.uniform(0.014, 0.030), body_c.y + rng.uniform(0.000, 0.014)),
                rx: rng.uniform(0.030, 0.040),
                ry: rng.uniform(0.024, 0.032),
                tissue: Tissue::Organ,
            });
            organs.push(Blob {
                c: Point::new(body_c.x - rng.uniform(0.026, 0.040), body_c.y + rng.uniform(0.004, 0.018)),
                rx: rng.uniform(0.013, 0.019),
                ry: rng.uniform(0.014, 0.020),
                tissue: Tissue::Organ,
            });
        } else if z >= 0.35 {
            // Mid abdomen: paired kidneys (posterior) + liver tail.
            organs.push(Blob {
                c: Point::new(body_c.x + rng.uniform(0.018, 0.030), spine_c.y + rng.uniform(0.018, 0.030)),
                rx: rng.uniform(0.010, 0.016),
                ry: rng.uniform(0.014, 0.020),
                tissue: Tissue::Organ,
            });
            organs.push(Blob {
                c: Point::new(body_c.x - rng.uniform(0.018, 0.030), spine_c.y + rng.uniform(0.018, 0.030)),
                rx: rng.uniform(0.010, 0.016),
                ry: rng.uniform(0.014, 0.020),
                tissue: Tissue::Organ,
            });
            organs.push(Blob {
                c: Point::new(body_c.x + rng.uniform(0.010, 0.024), body_c.y + rng.uniform(0.006, 0.018)),
                rx: rng.uniform(0.018, 0.026),
                ry: rng.uniform(0.014, 0.020),
                tissue: Tissue::Organ,
            });
        } else {
            // Pelvis: bowel / bladder soft-tissue blobs, central-anterior.
            for _ in 0..2 {
                organs.push(Blob {
                    c: Point::new(body_c.x + rng.uniform(-0.018, 0.018), body_c.y + rng.uniform(0.000, 0.022)),
                    rx: rng.uniform(0.012, 0.020),
                    ry: rng.uniform(0.012, 0.018),
                    tissue: Tissue::Organ,
                });
            }
        }

        // --- Bone: spine (all slices) + ribs (upper) or pelvis (lower). ---
        let mut bones: Vec<Blob> = Vec::new();
        let spine_r = rng.uniform(0.009, 0.013) * (1.0 + 0.4 * (1.0 - z)); // sacrum larger inferiorly
        bones.push(Blob { c: spine_c, rx: spine_r, ry: spine_r, tissue: Tissue::Bone });

        if z >= 0.6 {
            // Rib arcs: small bone nodes along the posterolateral body wall.
            let ribs = 4;
            for i in 0..ribs {
                let ang = std::f32::consts::PI * (0.55 + 0.9 * (i as f32 / (ribs - 1) as f32));
                let r = 0.92;
                let c = Point::new(
                    body_c.x + (body_rx - fat_t) * r * ang.cos(),
                    body_c.y + (body_ry - fat_t) * r * ang.sin(),
                );
                let rr = rng.uniform(0.004, 0.006);
                bones.push(Blob { c, rx: rr, ry: rr, tissue: Tissue::Bone });
                // Mirror to the other side.
                let cm = Point::new(2.0 * body_c.x - c.x, c.y);
                bones.push(Blob { c: cm, rx: rr, ry: rr, tissue: Tissue::Bone });
            }
        } else if z < 0.35 {
            // Iliac wings of the pelvis: two lateral bone masses.
            for &s in &[-1.0f32, 1.0] {
                bones.push(Blob {
                    c: Point::new(body_c.x + s * rng.uniform(0.030, 0.044), body_c.y - rng.uniform(0.004, 0.014)),
                    rx: rng.uniform(0.009, 0.014),
                    ry: rng.uniform(0.016, 0.024),
                    tissue: Tissue::Bone,
                });
            }
        }

        // --- Rasterise: shells, then organs, then bone (bone wins). ---
        for y in 0..n {
            for x in 0..n {
                let p = speed.cell_center(x, y);
                let i = speed.idx(x, y);
                let mut t = Tissue::Water;

                if in_ellipse(p, body_c, body_rx, body_ry) {
                    if !in_ellipse(p, body_c, body_rx - fat_t, body_ry - fat_t) {
                        t = Tissue::Fat;
                    } else if !in_ellipse(
                        p,
                        body_c,
                        body_rx - fat_t - muscle_t,
                        body_ry - fat_t - muscle_t,
                    ) {
                        t = Tissue::Muscle;
                    } else {
                        t = Tissue::Organ; // interior soft tissue
                    }

                    for o in &organs {
                        if in_ellipse(p, o.c, o.rx, o.ry) {
                            t = o.tissue;
                        }
                    }
                }
                // Bone (ribs/pelvis) may sit within the body wall, so test it
                // last and let it override regardless of the shell.
                for b in &bones {
                    if in_ellipse(p, b.c, b.rx, b.ry) {
                        t = Tissue::Bone;
                    }
                }

                speed.data[i] = t.nominal_speed();
                atten.data[i] = t.nominal_attenuation();
                labels.data[i] = t as u8 as f32;
            }
        }

        Phantom {
            speed,
            attenuation: atten,
            labels,
        }
    }

    /// Grid resolution (cells per side).
    pub fn n(&self) -> usize {
        self.speed.nx
    }

    /// Build a ground-truth phantom from a real anatomical intensity image
    /// (0..255 grayscale, e.g. a windowed CT slice). Intensity is banded into
    /// the five acoustic classes; this is a proxy mapping for benchmarking
    /// reconstruction on real anatomy, not a calibrated HU→speed conversion.
    pub fn from_intensity_grid(gray: &Grid) -> Phantom {
        let n = gray.nx;
        let ext = gray.dx * n as f32;
        let mut speed = Grid::square(n, ext, WATER_SPEED);
        let mut atten = Grid::square(n, ext, Tissue::Water.nominal_attenuation());
        let mut labels = Grid::square(n, ext, Tissue::Water as u8 as f32);
        for i in 0..gray.data.len() {
            let g = gray.data[i];
            let t = if g < 22.0 {
                Tissue::Water
            } else if g < 70.0 {
                Tissue::Fat
            } else if g < 120.0 {
                Tissue::Muscle
            } else if g < 190.0 {
                Tissue::Organ
            } else {
                Tissue::Bone
            };
            speed.data[i] = t.nominal_speed();
            atten.data[i] = t.nominal_attenuation();
            labels.data[i] = t as u8 as f32;
        }
        Phantom { speed, attenuation: atten, labels }
    }
}
