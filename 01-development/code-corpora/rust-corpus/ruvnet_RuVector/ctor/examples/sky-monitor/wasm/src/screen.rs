//! Polar "fisheye" all-sky screen mapping (pure math, natively testable).
//!
//! The whole sky dome is flattened onto a disc inscribed in the canvas:
//!
//! * zenith (elevation 90°) → canvas centre,
//! * horizon (elevation 0°) → edge of the inscribed circle
//!   (radius = `min(width, height) / 2`),
//! * azimuth 0° = North = straight **up**, 90° = East = right, 180° = South =
//!   down, 270° = West = left (i.e. the view looking straight up, with the
//!   compass laid out as on a map).
//!
//! Radius grows linearly with zenith angle: `r = (90 − el) / 90 · R`.
//! Below-horizon directions (el < 0) land **outside** the disc and are marked
//! not visible — trails can still be drawn fading off the edge.

/// Map azimuth/elevation (degrees) onto a `width`×`height` canvas.
/// Returns `(x, y, visible)`; `visible` is true iff the target is at or above
/// the horizon. Elevation is clamped to `[-90, 90]` for the radius math.
pub fn polar_screen_xy(
    azimuth_deg: f64,
    elevation_deg: f64,
    width: f64,
    height: f64,
) -> (f64, f64, bool) {
    let cx = width / 2.0;
    let cy = height / 2.0;
    let radius = width.min(height) / 2.0;
    let el = elevation_deg.clamp(-90.0, 90.0);
    let r = (90.0 - el) / 90.0 * radius;
    let az = azimuth_deg.to_radians();
    // Screen y grows downward, so North (az 0) maps to cy − r.
    (cx + r * az.sin(), cy - r * az.cos(), elevation_deg >= 0.0)
}

#[cfg(test)]
mod tests {
    use super::polar_screen_xy;

    const W: f64 = 800.0;
    const H: f64 = 600.0;
    const EPS: f64 = 1e-9;

    /// Inscribed-circle radius for the 800×600 test canvas.
    const R: f64 = 300.0;

    #[test]
    fn zenith_maps_to_canvas_centre() {
        let (x, y, visible) = polar_screen_xy(123.0, 90.0, W, H);
        assert!((x - 400.0).abs() < EPS, "x {x}");
        assert!((y - 300.0).abs() < EPS, "y {y}");
        assert!(visible);
    }

    #[test]
    fn horizon_north_maps_to_top_edge_centre() {
        // az 0 (North), el 0 → straight up from centre by the full radius.
        let (x, y, visible) = polar_screen_xy(0.0, 0.0, W, H);
        assert!((x - 400.0).abs() < EPS, "x {x}");
        assert!((y - (300.0 - R)).abs() < EPS, "y {y}");
        assert!(visible);
    }

    #[test]
    fn horizon_east_south_west_map_to_compass_points() {
        let (x, y, _) = polar_screen_xy(90.0, 0.0, W, H); // East → right
        assert!(
            (x - (400.0 + R)).abs() < 1e-6 && (y - 300.0).abs() < 1e-6,
            "E ({x},{y})"
        );
        let (x, y, _) = polar_screen_xy(180.0, 0.0, W, H); // South → down
        assert!(
            (x - 400.0).abs() < 1e-6 && (y - (300.0 + R)).abs() < 1e-6,
            "S ({x},{y})"
        );
        let (x, y, _) = polar_screen_xy(270.0, 0.0, W, H); // West → left
        assert!(
            (x - (400.0 - R)).abs() < 1e-6 && (y - 300.0).abs() < 1e-6,
            "W ({x},{y})"
        );
    }

    #[test]
    fn elevation_scales_radius_linearly() {
        // el 45 → half the radius; el 30 → two thirds.
        let (_, y, _) = polar_screen_xy(0.0, 45.0, W, H);
        assert!((y - (300.0 - R / 2.0)).abs() < EPS, "y {y}");
        let (_, y, _) = polar_screen_xy(0.0, 30.0, W, H);
        assert!((y - (300.0 - R * 2.0 / 3.0)).abs() < 1e-6, "y {y}");
    }

    #[test]
    fn below_horizon_is_outside_disc_and_invisible() {
        let (x, y, visible) = polar_screen_xy(90.0, -10.0, W, H);
        assert!(!visible);
        let r = ((x - 400.0).powi(2) + (y - 300.0).powi(2)).sqrt();
        assert!(
            r > R,
            "below-horizon point must land outside the disc, r {r}"
        );
        // Clamp: el −90 stays finite (2R).
        let (x, y, visible) = polar_screen_xy(0.0, -90.0, W, H);
        assert!(!visible && x.is_finite() && y.is_finite());
    }
}
