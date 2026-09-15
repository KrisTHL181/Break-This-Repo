//! Observer-relative coordinate model (ADR-199 §10).
//!
//! Pipeline: **WGS-84 geodetic → ECEF → ENU → azimuth / elevation / range**.
//!
//! All math is plain `f64`; formulas are the standard geodesy ones:
//!
//! * Geodetic → ECEF (WGS-84 ellipsoid, semi-major axis `a`, first
//!   eccentricity squared `e²`):
//!   `N(φ) = a / sqrt(1 − e² sin²φ)`,
//!   `x = (N + h)·cosφ·cosλ`, `y = (N + h)·cosφ·sinλ`,
//!   `z = (N·(1 − e²) + h)·sinφ`.
//! * ECEF Δ → local East/North/Up tangent plane at the observer:
//!   `e = −sinλ·Δx + cosλ·Δy`,
//!   `n = −sinφ·cosλ·Δx − sinφ·sinλ·Δy + cosφ·Δz`,
//!   `u =  cosφ·cosλ·Δx + cosφ·sinλ·Δy + sinφ·Δz`.
//! * Azimuth = `atan2(e, n)` (clockwise from true north),
//!   elevation = `atan2(u, hypot(e, n))`, slant range = `‖(e,n,u)‖`.
//! * Bearing = great-circle initial bearing from observer to target:
//!   `θ = atan2(sinΔλ·cosφ₂, cosφ₁·sinφ₂ − sinφ₁·cosφ₂·cosΔλ)`.
//!
//! Note that for distant targets at the *same* ellipsoidal altitude, elevation
//! is slightly **negative** because the Earth curves away under the line of
//! sight (≈ −range / 2R radians).

use serde::{Deserialize, Serialize};

/// WGS-84 semi-major axis (metres).
pub const WGS84_A: f64 = 6_378_137.0;
/// WGS-84 flattening.
pub const WGS84_F: f64 = 1.0 / 298.257_223_563;
/// WGS-84 first eccentricity squared, `e² = f·(2 − f)`.
pub const WGS84_E2: f64 = WGS84_F * (2.0 - WGS84_F);

/// Earth-Centred Earth-Fixed cartesian coordinates (metres).
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Ecef {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

/// Local East/North/Up tangent-plane coordinates (metres) at an observer.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Enu {
    pub east: f64,
    pub north: f64,
    pub up: f64,
}

/// Observer-relative spherical frame (ADR-199 §11 `observer_frame`).
#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct ObserverFrame {
    /// Slant range, metres.
    pub range_m: f64,
    /// Azimuth, degrees clockwise from true north, in `[0, 360)`.
    pub azimuth_deg: f64,
    /// Elevation above the local horizon, degrees, in `[-90, 90]`.
    pub elevation_deg: f64,
    /// Great-circle initial bearing observer → target, degrees, in `[0, 360)`.
    pub bearing_deg: f64,
}

/// Convert WGS-84 geodetic coordinates to ECEF.
pub fn geodetic_to_ecef(lat_deg: f64, lon_deg: f64, alt_m: f64) -> Ecef {
    let lat = lat_deg.to_radians();
    let lon = lon_deg.to_radians();
    let (sin_lat, cos_lat) = lat.sin_cos();
    let (sin_lon, cos_lon) = lon.sin_cos();
    // Prime-vertical radius of curvature.
    let n = WGS84_A / (1.0 - WGS84_E2 * sin_lat * sin_lat).sqrt();
    Ecef {
        x: (n + alt_m) * cos_lat * cos_lon,
        y: (n + alt_m) * cos_lat * sin_lon,
        z: (n * (1.0 - WGS84_E2) + alt_m) * sin_lat,
    }
}

/// Rotate an ECEF delta (target − observer) into the observer's ENU frame.
pub fn ecef_to_enu(obs_lat_deg: f64, obs_lon_deg: f64, obs: Ecef, target: Ecef) -> Enu {
    let lat = obs_lat_deg.to_radians();
    let lon = obs_lon_deg.to_radians();
    let (sin_lat, cos_lat) = lat.sin_cos();
    let (sin_lon, cos_lon) = lon.sin_cos();
    let dx = target.x - obs.x;
    let dy = target.y - obs.y;
    let dz = target.z - obs.z;
    Enu {
        east: -sin_lon * dx + cos_lon * dy,
        north: -sin_lat * cos_lon * dx - sin_lat * sin_lon * dy + cos_lat * dz,
        up: cos_lat * cos_lon * dx + cos_lat * sin_lon * dy + sin_lat * dz,
    }
}

/// Normalize an angle in degrees into `[0, 360)`.
pub fn normalize_deg(deg: f64) -> f64 {
    let d = deg % 360.0;
    if d < 0.0 {
        d + 360.0
    } else {
        d
    }
}

/// Great-circle initial bearing from `(lat1, lon1)` to `(lat2, lon2)`, degrees.
pub fn initial_bearing_deg(lat1_deg: f64, lon1_deg: f64, lat2_deg: f64, lon2_deg: f64) -> f64 {
    let phi1 = lat1_deg.to_radians();
    let phi2 = lat2_deg.to_radians();
    let dl = (lon2_deg - lon1_deg).to_radians();
    let y = dl.sin() * phi2.cos();
    let x = phi1.cos() * phi2.sin() - phi1.sin() * phi2.cos() * dl.cos();
    normalize_deg(y.atan2(x).to_degrees())
}

/// Full projection: target geodetic position → observer-relative frame.
pub fn observer_frame(
    obs_lat: f64,
    obs_lon: f64,
    obs_alt_m: f64,
    target_lat: f64,
    target_lon: f64,
    target_alt_m: f64,
) -> ObserverFrame {
    // Same math as `geodetic_to_ecef` → `ecef_to_enu` → `initial_bearing_deg`,
    // inlined so each sin/cos is computed exactly once (the helper composition
    // recomputes the observer trig three times and the target trig twice).
    let lat1 = obs_lat.to_radians();
    let lon1 = obs_lon.to_radians();
    let lat2 = target_lat.to_radians();
    let lon2 = target_lon.to_radians();
    let (sin_lat1, cos_lat1) = lat1.sin_cos();
    let (sin_lon1, cos_lon1) = lon1.sin_cos();
    let (sin_lat2, cos_lat2) = lat2.sin_cos();
    let (sin_lon2, cos_lon2) = lon2.sin_cos();

    // Geodetic → ECEF for observer and target (WGS-84).
    let n1 = WGS84_A / (1.0 - WGS84_E2 * sin_lat1 * sin_lat1).sqrt();
    let ox = (n1 + obs_alt_m) * cos_lat1 * cos_lon1;
    let oy = (n1 + obs_alt_m) * cos_lat1 * sin_lon1;
    let oz = (n1 * (1.0 - WGS84_E2) + obs_alt_m) * sin_lat1;
    let n2 = WGS84_A / (1.0 - WGS84_E2 * sin_lat2 * sin_lat2).sqrt();
    let tx = (n2 + target_alt_m) * cos_lat2 * cos_lon2;
    let ty = (n2 + target_alt_m) * cos_lat2 * sin_lon2;
    let tz = (n2 * (1.0 - WGS84_E2) + target_alt_m) * sin_lat2;

    // ECEF Δ → ENU at the observer.
    let dx = tx - ox;
    let dy = ty - oy;
    let dz = tz - oz;
    let east = -sin_lon1 * dx + cos_lon1 * dy;
    let north = -sin_lat1 * cos_lon1 * dx - sin_lat1 * sin_lon1 * dy + cos_lat1 * dz;
    let up = cos_lat1 * cos_lon1 * dx + cos_lat1 * sin_lon1 * dy + sin_lat1 * dz;

    let horizontal = east.hypot(north);
    let range_m = (horizontal * horizontal + up * up).sqrt();
    let azimuth_deg = if horizontal < 1e-9 {
        0.0 // directly overhead/underfoot: azimuth undefined, report 0
    } else {
        normalize_deg(east.atan2(north).to_degrees())
    };
    let elevation_deg = up.atan2(horizontal).to_degrees();

    // Great-circle initial bearing, reusing the trig above via the angle
    // subtraction identities (sin/cos of Δλ from the per-longitude values).
    let sin_dl = sin_lon2 * cos_lon1 - cos_lon2 * sin_lon1;
    let cos_dl = cos_lon2 * cos_lon1 + sin_lon2 * sin_lon1;
    let by = sin_dl * cos_lat2;
    let bx = cos_lat1 * sin_lat2 - sin_lat1 * cos_lat2 * cos_dl;
    let bearing_deg = normalize_deg(by.atan2(bx).to_degrees());

    ObserverFrame {
        range_m,
        azimuth_deg,
        elevation_deg,
        bearing_deg,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const OBS: (f64, f64, f64) = (43.4675, -79.6877, 100.0);

    #[test]
    fn aircraft_due_north_same_altitude() {
        // ~50 km due north at the same ellipsoidal altitude.
        let dlat = 50_000.0 / 111_132.0; // metres per degree latitude (approx.)
        let f = observer_frame(OBS.0, OBS.1, OBS.2, OBS.0 + dlat, OBS.1, OBS.2);
        let az = if f.azimuth_deg > 180.0 {
            f.azimuth_deg - 360.0
        } else {
            f.azimuth_deg
        };
        assert!(
            az.abs() < 0.5,
            "azimuth should be ~0 deg, got {}",
            f.azimuth_deg
        );
        // Earth curvature drops the target below the horizon: ~ -r/2R rad ≈ -0.22 deg.
        assert!(
            f.elevation_deg < 0.0 && f.elevation_deg > -0.5,
            "expected slightly negative elevation, got {}",
            f.elevation_deg
        );
        assert!((f.range_m - 50_000.0).abs() < 500.0);
        assert!(f.bearing_deg < 0.5 || f.bearing_deg > 359.5);
    }

    #[test]
    fn aircraft_directly_overhead() {
        let f = observer_frame(OBS.0, OBS.1, OBS.2, OBS.0, OBS.1, OBS.2 + 5_000.0);
        assert!((f.elevation_deg - 90.0).abs() < 1e-6);
        assert!((f.range_m - 5_000.0).abs() < 1.0);
    }

    #[test]
    fn aircraft_due_east_above_horizon() {
        // ~20 km east, 10 km up: azimuth ~90, elevation ~atan(9900/20000) ≈ 26.3 deg.
        let dlon = 20_000.0 / (111_320.0 * OBS.0.to_radians().cos());
        let f = observer_frame(OBS.0, OBS.1, OBS.2, OBS.0, OBS.1 + dlon, 10_000.0);
        assert!((f.azimuth_deg - 90.0).abs() < 1.0, "az {}", f.azimuth_deg);
        assert!(
            (f.elevation_deg - 26.3).abs() < 1.5,
            "el {}",
            f.elevation_deg
        );
        assert!(
            (f.bearing_deg - 90.0).abs() < 1.0,
            "bearing {}",
            f.bearing_deg
        );
    }

    #[test]
    fn ecef_of_equator_prime_meridian() {
        let e = geodetic_to_ecef(0.0, 0.0, 0.0);
        assert!((e.x - WGS84_A).abs() < 1e-6);
        assert!(e.y.abs() < 1e-6 && e.z.abs() < 1e-6);
    }

    #[test]
    fn normalize_wraps_negative() {
        assert!((normalize_deg(-90.0) - 270.0).abs() < 1e-9);
        assert!((normalize_deg(725.0) - 5.0).abs() < 1e-9);
    }
}
