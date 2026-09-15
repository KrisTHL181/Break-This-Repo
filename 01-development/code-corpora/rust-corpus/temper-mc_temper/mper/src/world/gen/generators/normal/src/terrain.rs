use noise::{Fbm, MultiFractal, NoiseFn, Perlin, RidgedMulti};

#[derive(Clone)]
pub(crate) struct NoiseGenerator {
    base: Fbm<Perlin>,
    peaks: RidgedMulti<Perlin>,
    mountain_mask: Fbm<Perlin>,
    pub seed: u64,
    caves_layer: RidgedMulti<noise::OpenSimplex>,
}

impl NoiseGenerator {
    pub fn new(seed: u64) -> Self {
        let base = Fbm::<Perlin>::new(seed as u32)
            .set_octaves(4)
            .set_frequency(0.002);

        let peaks = RidgedMulti::<Perlin>::new((seed as u32).wrapping_add(1))
            .set_octaves(4)
            .set_frequency(0.008);

        let mountain_mask = Fbm::<Perlin>::new((seed as u32).wrapping_add(2))
            .set_octaves(2)
            .set_frequency(0.0006);

        let caves_layer = RidgedMulti::new((seed + 100) as u32)
            .set_frequency(0.01)
            .set_lacunarity(2.5)
            .set_octaves(5)
            .set_persistence(0.8)
            .set_attenuation(0.3);

        Self {
            base,
            peaks,
            mountain_mask,
            seed,
            caves_layer,
        }
    }

    pub fn get_noise(&self, x: f64, z: f64) -> f64 {
        let to01 = |n: f64| (n * 0.5 + 0.5).clamp(0.0, 1.0);

        let base = self.base.get([x, z]);
        let base01 = to01(base);

        let peaks = self.peaks.get([x, z]);
        let peaks01 = to01(peaks);

        let mask = self.mountain_mask.get([x, z]);
        let mask01 = to01(mask);

        let height = shape_height(base01, peaks01, mask01);

        (height * 2.0) - 1.0
    }

    pub fn get_cave_noise(&self, x: f64, y: f64, z: f64) -> f64 {
        self.caves_layer.get([x, y, z])
    }
}

fn shape_height(base01: f64, peaks01: f64, mask01: f64) -> f64 {
    let land_lift = smoothstep(((base01 - 0.38) / 0.34).clamp(0.0, 1.0));
    let mountain_mask = smoothstep(((mask01 - 0.5) / 0.3).clamp(0.0, 1.0));
    let peak_gate = smoothstep(((base01 - 0.58) / 0.24).clamp(0.0, 1.0));

    let plains = base01.powf(1.15) * 0.92 + land_lift * 0.06;
    let peaks = peaks01.powf(3.0) * 0.16 * mountain_mask * peak_gate;

    (plains + peaks).clamp(0.0, 1.0)
}

#[inline(always)]
pub fn lerp(a: f64, b: f64, t: f64) -> f64 {
    a + (b - a) * t
}

#[inline(always)]
pub fn smoothstep(t: f64) -> f64 {
    t * t * (3.0 - 2.0 * t)
}

#[inline(always)]
pub fn bilerp(c00: f64, c10: f64, c01: f64, c11: f64, tx: f64, tz: f64) -> f64 {
    let x0 = lerp(c00, c10, tx);
    let x1 = lerp(c01, c11, tx);
    lerp(x0, x1, tz)
}

#[expect(clippy::too_many_arguments)]
#[inline(always)]
pub fn trilerp(
    c000: f64,
    c100: f64,
    c010: f64,
    c110: f64,
    c001: f64,
    c101: f64,
    c011: f64,
    c111: f64,
    tx: f64,
    ty: f64,
    tz: f64,
) -> f64 {
    let x00 = lerp(c000, c100, tx);
    let x10 = lerp(c010, c110, tx);
    let x01 = lerp(c001, c101, tx);
    let x11 = lerp(c011, c111, tx);

    let y0 = lerp(x00, x10, ty);
    let y1 = lerp(x01, x11, ty);

    lerp(y0, y1, tz)
}

#[inline(always)]
fn quick_hash(seed: u64, x: i32, z: i32) -> f64 {
    let mut value = seed
        ^ (x as u64).wrapping_mul(0x9E3779B185EBCA87)
        ^ (z as u64).wrapping_mul(0xC2B2AE3D27D4EB4F);
    value ^= value >> 33;
    value = value.wrapping_mul(0xFF51AFD7ED558CCD);
    value ^= value >> 33;
    value as f64 / u64::MAX as f64
}

#[inline(always)]
pub fn dither_field(seed: u64, x: i32, z: i32, cell_size: i32) -> f64 {
    let cx0 = x.div_euclid(cell_size);
    let cz0 = z.div_euclid(cell_size);
    let cx1 = cx0 + 1;
    let cz1 = cz0 + 1;

    let fx = f64::from(x.rem_euclid(cell_size)) / f64::from(cell_size);
    let fz = f64::from(z.rem_euclid(cell_size)) / f64::from(cell_size);

    let tx = smoothstep(fx);
    let tz = smoothstep(fz);

    let v00 = quick_hash(seed, cx0, cz0);
    let v10 = quick_hash(seed, cx1, cz0);
    let v01 = quick_hash(seed, cx0, cz1);
    let v11 = quick_hash(seed, cx1, cz1);

    let a = lerp(v00, v10, tx);
    let b = lerp(v01, v11, tx);
    lerp(a, b, tz)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn peak_noise_is_gated_out_of_lower_terrain() {
        let low_peak = shape_height(0.45, 0.1, 1.0);
        let high_peak = shape_height(0.45, 1.0, 1.0);

        assert!((high_peak - low_peak).abs() < 0.01);
    }

    #[test]
    fn peak_noise_still_lifts_high_terrain() {
        let low_peak = shape_height(0.75, 0.1, 1.0);
        let high_peak = shape_height(0.75, 1.0, 1.0);

        assert!(high_peak > low_peak + 0.1);
    }
}
