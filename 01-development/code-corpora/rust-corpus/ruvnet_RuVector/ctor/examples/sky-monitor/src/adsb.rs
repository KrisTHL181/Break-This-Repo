//! ADS-B sources (ADR-199 §9.1, Phase 1).
//!
//! Two front-ends produce the same [`AircraftState`] samples:
//!
//! 1. [`generate_scenario`] — a **deterministic seeded synthetic generator**
//!    (no network, no SDR) producing a realistic mixed day of traffic over the
//!    observer: en-route corridor flights, arrivals/departures, a low GA
//!    overhead pass, and one anomalous low/slow off-corridor night track.
//! 2. [`parse_dump1090`] — a parser for dump1090-style `aircraft.json`
//!    payloads, so a real RTL-SDR + dump1090 feed can be plugged into the same
//!    pipeline unchanged.

use chrono::{DateTime, Duration, TimeZone, Utc};
use serde::{Deserialize, Serialize};

/// icao24 of the single anomalous synthetic track (low, slow, off-corridor,
/// 03:10 UTC). Exposed so tests and demos can identify it.
pub const ANOMALOUS_ICAO24: &str = "deadbf";
/// icao24 of the low-altitude general-aviation overhead pass.
pub const GA_OVERHEAD_ICAO24: &str = "c0a9a9";

/// One decoded ADS-B state vector sample.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AircraftState {
    /// 24-bit ICAO transponder address, lowercase hex.
    pub icao24: String,
    /// Callsign (may be empty — itself a mildly unusual attribute).
    pub callsign: String,
    pub lat: f64,
    pub lon: f64,
    /// Barometric altitude, metres.
    pub alt_m: f64,
    /// Ground speed, m/s.
    pub speed_mps: f64,
    /// Ground track, degrees from true north.
    pub track_deg: f64,
    /// Vertical rate, m/s (positive = climb).
    pub vertical_rate_mps: f64,
    /// Receiver signal strength, dBFS (0 = full scale, more negative = weaker).
    pub signal_dbfs: f64,
    /// Sample timestamp (UTC).
    pub ts: DateTime<Utc>,
}

/// Tiny deterministic linear congruential generator (Numerical Recipes
/// constants). Avoids a `rand` dependency while keeping the scenario fully
/// reproducible from a single `u64` seed.
pub struct Lcg(u64);

impl Lcg {
    pub fn new(seed: u64) -> Self {
        Self(
            seed.wrapping_mul(6364136223846793005)
                .wrapping_add(1442695040888963407),
        )
    }
    pub fn next_u64(&mut self) -> u64 {
        self.0 = self
            .0
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        self.0 ^ (self.0 >> 33)
    }
    /// Uniform in `[0, 1)`.
    pub fn next_f64(&mut self) -> f64 {
        (self.next_u64() >> 11) as f64 / (1u64 << 53) as f64
    }
    /// Uniform in `[-1, 1)` — handy for jitter.
    pub fn jitter(&mut self) -> f64 {
        self.next_f64() * 2.0 - 1.0
    }
}

/// Internal flight description used by the synthetic generator.
struct FlightPlan {
    icao24: &'static str,
    callsign: &'static str,
    /// Start time as seconds offset from `day_start`.
    start_offset_s: i64,
    duration_s: i64,
    heading_deg: f64,
    speed_mps: f64,
    /// Altitude at the start of the segment, metres.
    alt0_m: f64,
    vertical_rate_mps: f64,
    /// Cross-track offset of the closest point of approach from the observer,
    /// km, positive = right of the heading direction.
    cross_offset_km: f64,
    /// Mean receiver signal strength for this flight, dBFS.
    signal_dbfs: f64,
}

/// The standard synthetic day of traffic over the observer (12 a.m. day-start
/// based timeline; see each row's offset). All headings/altitudes/speeds are
/// realistic for the Toronto-area corridor the Oakville node sits under.
fn scenario_plans() -> Vec<FlightPlan> {
    const H: i64 = 3600;
    vec![
        // (a) En-route commercial corridor: eastbound ~072 deg at FL350-ish.
        FlightPlan {
            icao24: "c01a01",
            callsign: "ACA101",
            start_offset_s: 11 * H + 300,
            duration_s: 240,
            heading_deg: 72.0,
            speed_mps: 236.0,
            alt0_m: 10_600.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 8.0,
            signal_dbfs: -18.0,
        },
        FlightPlan {
            icao24: "a02b02",
            callsign: "DAL202",
            start_offset_s: 13 * H + 2400,
            duration_s: 240,
            heading_deg: 74.0,
            speed_mps: 232.0,
            alt0_m: 10_800.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: -6.0,
            signal_dbfs: -19.0,
        },
        FlightPlan {
            icao24: "a03c03",
            callsign: "UAL303",
            start_offset_s: 15 * H + 1200,
            duration_s: 240,
            heading_deg: 71.0,
            speed_mps: 238.0,
            alt0_m: 10_700.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 12.0,
            signal_dbfs: -20.0,
        },
        FlightPlan {
            icao24: "c04d04",
            callsign: "WJA404",
            start_offset_s: 18 * H + 1800,
            duration_s: 240,
            heading_deg: 73.0,
            speed_mps: 234.0,
            alt0_m: 10_500.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 6.0,
            signal_dbfs: -18.0,
        },
        // Westbound return corridor ~252 deg.
        FlightPlan {
            icao24: "400a05",
            callsign: "BAW505",
            start_offset_s: 12 * H + 600,
            duration_s: 240,
            heading_deg: 252.0,
            speed_mps: 228.0,
            alt0_m: 11_200.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: -10.0,
            signal_dbfs: -19.0,
        },
        FlightPlan {
            icao24: "39a006",
            callsign: "AFR606",
            start_offset_s: 17 * H + 300,
            duration_s: 240,
            heading_deg: 251.0,
            speed_mps: 230.0,
            alt0_m: 11_000.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 7.0,
            signal_dbfs: -18.0,
        },
        // (b) Arrivals descending through the area toward Pearson (~032 deg).
        FlightPlan {
            icao24: "c07e07",
            callsign: "JZA707",
            start_offset_s: 14 * H + 900,
            duration_s: 300,
            heading_deg: 32.0,
            speed_mps: 145.0,
            alt0_m: 4_800.0,
            vertical_rate_mps: -7.5,
            cross_offset_km: 6.0,
            signal_dbfs: -12.0,
        },
        FlightPlan {
            icao24: "c08f08",
            callsign: "SKV808",
            start_offset_s: 19 * H + 2700,
            duration_s: 300,
            heading_deg: 34.0,
            speed_mps: 150.0,
            alt0_m: 4_600.0,
            vertical_rate_mps: -7.0,
            cross_offset_km: -4.0,
            signal_dbfs: -12.0,
        },
        // (c) Low-altitude general-aviation overhead pass (lake-shore VFR).
        FlightPlan {
            icao24: GA_OVERHEAD_ICAO24,
            callsign: "CGSKY",
            start_offset_s: 16 * H + 1800,
            duration_s: 360,
            heading_deg: 88.0,
            speed_mps: 62.0,
            alt0_m: 1_100.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 0.4,
            signal_dbfs: -8.0,
        },
        // (d) Anomalous track: low altitude, slow, off-corridor heading 165,
        //     at 03:10 the following night, very strong signal, no callsign.
        FlightPlan {
            icao24: ANOMALOUS_ICAO24,
            callsign: "",
            start_offset_s: 27 * H + 600,
            duration_s: 420,
            heading_deg: 165.0,
            speed_mps: 48.0,
            alt0_m: 450.0,
            vertical_rate_mps: 0.0,
            cross_offset_km: 2.0,
            signal_dbfs: -3.0,
        },
    ]
}

/// Default scenario day start: 2026-06-08T00:00:00Z.
pub fn default_day_start() -> DateTime<Utc> {
    Utc.with_ymd_and_hms(2026, 6, 8, 0, 0, 0).unwrap()
}

/// Generate the deterministic synthetic scenario at ~1 Hz, sorted by time.
///
/// The geometry places each flight so its closest point of approach to the
/// observer happens mid-segment at `cross_offset_km` perpendicular distance,
/// using a flat-earth metre→degree step (fine at < 100 km scales; the precise
/// observer-relative frame is recomputed later via the §10 ECEF/ENU pipeline).
pub fn generate_scenario(
    observer_lat: f64,
    observer_lon: f64,
    seed: u64,
    day_start: DateTime<Utc>,
) -> Vec<AircraftState> {
    let mut rng = Lcg::new(seed);
    let m_per_deg_lat = 111_132.0;
    let m_per_deg_lon = 111_320.0 * observer_lat.to_radians().cos();
    let mut out = Vec::new();

    for plan in scenario_plans() {
        let h = plan.heading_deg.to_radians();
        // Unit vectors in local (east, north) metres.
        let dir = (h.sin(), h.cos());
        let perp = (h.cos(), -h.sin()); // 90 deg right of heading
                                        // Closest point of approach, then back up half the segment.
        let cpa = (
            perp.0 * plan.cross_offset_km * 1_000.0,
            perp.1 * plan.cross_offset_km * 1_000.0,
        );
        let half = plan.speed_mps * plan.duration_s as f64 / 2.0;
        let mut east = cpa.0 - dir.0 * half;
        let mut north = cpa.1 - dir.1 * half;
        let mut alt = plan.alt0_m;

        for t in 0..plan.duration_s {
            let ts = day_start + Duration::seconds(plan.start_offset_s + t);
            let track = plan.heading_deg + rng.jitter() * 1.5;
            let speed = plan.speed_mps + rng.jitter() * 3.0;
            out.push(AircraftState {
                icao24: plan.icao24.to_string(),
                callsign: plan.callsign.to_string(),
                lat: observer_lat + north / m_per_deg_lat,
                lon: observer_lon + east / m_per_deg_lon,
                alt_m: alt + rng.jitter() * 10.0,
                speed_mps: speed,
                track_deg: crate::coords::normalize_deg(track),
                vertical_rate_mps: plan.vertical_rate_mps + rng.jitter() * 0.4,
                signal_dbfs: plan.signal_dbfs + rng.jitter() * 1.5,
                ts,
            });
            east += dir.0 * plan.speed_mps;
            north += dir.1 * plan.speed_mps;
            alt += plan.vertical_rate_mps;
        }
    }
    out.sort_by_key(|s| s.ts);
    out
}

/// Parse a dump1090-style `aircraft.json` payload into state vectors.
///
/// Unit conversions: `alt_baro` ft → m, `gs` knots → m/s, `baro_rate` ft/min →
/// m/s. Entries without a position fix (`lat`/`lon`) are skipped. `rssi` maps
/// to `signal_dbfs`. The `now` field (epoch seconds) timestamps all entries.
pub fn parse_dump1090(json: &str) -> Result<Vec<AircraftState>, serde_json::Error> {
    const FT: f64 = 0.3048;
    const KT: f64 = 0.514_444;
    let v: serde_json::Value = serde_json::from_str(json)?;
    let now = v.get("now").and_then(|n| n.as_f64()).unwrap_or(0.0);
    let ts = Utc
        .timestamp_opt(now as i64, ((now.fract()) * 1e9) as u32)
        .single()
        .unwrap_or_else(|| Utc.timestamp_opt(0, 0).single().unwrap());
    let mut out = Vec::new();
    if let Some(list) = v.get("aircraft").and_then(|a| a.as_array()) {
        for ac in list {
            let (Some(lat), Some(lon)) = (
                ac.get("lat").and_then(|x| x.as_f64()),
                ac.get("lon").and_then(|x| x.as_f64()),
            ) else {
                continue; // no position fix yet
            };
            let f = |k: &str| ac.get(k).and_then(|x| x.as_f64());
            out.push(AircraftState {
                icao24: ac
                    .get("hex")
                    .and_then(|x| x.as_str())
                    .unwrap_or("")
                    .to_lowercase(),
                callsign: ac
                    .get("flight")
                    .and_then(|x| x.as_str())
                    .unwrap_or("")
                    .trim()
                    .to_string(),
                lat,
                lon,
                alt_m: f("alt_baro").unwrap_or(0.0) * FT,
                speed_mps: f("gs").unwrap_or(0.0) * KT,
                track_deg: f("track").unwrap_or(0.0),
                vertical_rate_mps: f("baro_rate").unwrap_or(0.0) * FT / 60.0,
                signal_dbfs: f("rssi").unwrap_or(-30.0),
                ts,
            });
        }
    }
    Ok(out)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn scenario_is_deterministic_and_realistic() {
        let a = generate_scenario(43.4675, -79.6877, 42, default_day_start());
        let b = generate_scenario(43.4675, -79.6877, 42, default_day_start());
        assert_eq!(a.len(), b.len());
        assert!(
            a.len() > 2_500,
            "expected ~1 Hz day of samples, got {}",
            a.len()
        );
        assert_eq!(
            a[100].lat.to_bits(),
            b[100].lat.to_bits(),
            "must be bit-deterministic"
        );
        assert!(a.iter().any(|s| s.icao24 == ANOMALOUS_ICAO24));
        // Sorted by time.
        assert!(a.windows(2).all(|w| w[0].ts <= w[1].ts));
    }

    #[test]
    fn parses_dump1090_aircraft_json() {
        let json = r#"{
          "now": 1765219200.5,
          "messages": 142001,
          "aircraft": [
            { "hex": "C01A01", "flight": "ACA123  ", "alt_baro": 36000, "gs": 451.2,
              "track": 72.4, "baro_rate": -64, "lat": 43.5121, "lon": -79.5512,
              "rssi": -18.4, "squawk": "3417", "seen": 0.2 },
            { "hex": "a9b8c7", "alt_baro": 12000, "gs": 220.0, "track": 250.1,
              "lat": 43.4011, "lon": -79.8821, "rssi": -22.1 },
            { "hex": "ffffff", "seen": 12.0 }
          ]
        }"#;
        let states = parse_dump1090(json).unwrap();
        assert_eq!(states.len(), 2, "entry without lat/lon must be skipped");
        let s = &states[0];
        assert_eq!(s.icao24, "c01a01");
        assert_eq!(s.callsign, "ACA123");
        assert!((s.alt_m - 36000.0 * 0.3048).abs() < 0.1);
        assert!((s.speed_mps - 451.2 * 0.514444).abs() < 0.01);
        assert!((s.vertical_rate_mps - (-64.0 * 0.3048 / 60.0)).abs() < 1e-6);
        assert!((s.signal_dbfs + 18.4).abs() < 1e-9);
        assert_eq!(s.ts.timestamp(), 1_765_219_200);
    }
}
