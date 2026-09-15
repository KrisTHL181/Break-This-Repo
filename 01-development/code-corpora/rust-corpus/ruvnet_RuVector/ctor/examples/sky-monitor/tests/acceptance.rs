//! Acceptance tests mapped to ADR-199 §31 (system acceptance) and the §22
//! Phase 1–4 build-plan acceptance columns.

use chrono::Duration;
use sky_monitor::{
    observer_frame, parse_dump1090, AnomalyConfig, Interpretation, ObserverConfig, Pipeline,
    ANOMALOUS_ICAO24, GA_OVERHEAD_ICAO24,
};

const EASTBOUND_CORRIDOR: [&str; 4] = ["c01a01", "a02b02", "a03c03", "c04d04"];
const WESTBOUND_CORRIDOR: [&str; 2] = ["400a05", "39a006"];

fn run() -> sky_monitor::PipelineReport {
    Pipeline::default().run().expect("pipeline runs")
}

/// (1) §31.2 — state vectors convert to azimuth/elevation/range.
#[test]
fn acceptance_1_positions_convert_to_az_el_range() {
    let cfg = ObserverConfig::default();
    // Synthetic target ~10 km north-east, 5 km up.
    let f = observer_frame(
        cfg.lat,
        cfg.lon,
        cfg.alt_m,
        cfg.lat + 0.0636,
        cfg.lon + 0.0875,
        5_000.0,
    );
    assert!(
        f.range_m > 9_000.0 && f.range_m < 13_500.0,
        "range {}",
        f.range_m
    );
    assert!(
        f.azimuth_deg > 30.0 && f.azimuth_deg < 60.0,
        "az {}",
        f.azimuth_deg
    );
    assert!(
        f.elevation_deg > 20.0 && f.elevation_deg < 35.0,
        "el {}",
        f.elevation_deg
    );

    // And every pipeline observation carries a finite observer frame.
    let report = run();
    assert!(report.observations.iter().all(|o| {
        o.observer_frame.range_m.is_finite()
            && (0.0..360.0).contains(&o.observer_frame.azimuth_deg)
            && o.observer_frame.elevation_deg.abs() <= 90.0
    }));
}

/// (2) §14 rule 1 / §22 Phase 3 — the overhead query returns the low GA pass
/// (and the low anomalous pass) but not the high en-route corridor flights.
#[test]
fn acceptance_2_overhead_query_excludes_en_route() {
    let report = run();
    let overhead = report.skygraph.overhead_candidates();
    assert!(
        overhead.iter().any(|id| id.contains(GA_OVERHEAD_ICAO24)),
        "GA overhead pass missing from {overhead:?}"
    );
    for icao in EASTBOUND_CORRIDOR.iter().chain(&WESTBOUND_CORRIDOR) {
        assert!(
            !overhead.iter().any(|id| id.contains(icao)),
            "en-route corridor flight {icao} must not be an overhead candidate"
        );
    }
}

/// (3) §31.6 — "what flew overhead in this period" via the SkyGraph
/// time-window query.
#[test]
fn acceptance_3_aircraft_by_time_window() {
    let report = run();
    let pipeline = Pipeline::default();
    // 11:00–12:00 UTC contains exactly the first eastbound corridor flight.
    let start = pipeline.day_start + Duration::hours(11);
    let in_window = report
        .skygraph
        .aircraft_in_window(start, start + Duration::hours(1));
    assert_eq!(in_window.len(), 1, "got {in_window:?}");
    assert_eq!(in_window[0].0, "c01a01");
    // The anomaly night window (+27 h) contains exactly the anomalous track.
    let night = pipeline.day_start + Duration::hours(27);
    let in_night = report
        .skygraph
        .aircraft_in_window(night, night + Duration::hours(1));
    assert_eq!(in_night.len(), 1, "got {in_night:?}");
    assert_eq!(in_night[0].0, ANOMALOUS_ICAO24);
    // An empty pre-dawn window has no aircraft.
    let empty = pipeline.day_start + Duration::hours(2);
    assert!(report
        .skygraph
        .aircraft_in_window(empty, empty + Duration::hours(1))
        .is_empty());
}

/// (4) §31.8 / §15 — after the baseline period the anomalous track raises a
/// local alert (> 0.76) while normal corridor flights stay ≤ 0.55.
#[test]
fn acceptance_4_anomaly_scoring_separates_corridor_traffic() {
    let report = run();
    let cfg = AnomalyConfig::default();
    let anomaly = report
        .reports
        .iter()
        .find(|r| r.icao24 == ANOMALOUS_ICAO24)
        .expect("anomalous track is scored (it is last in the day)");
    assert!(
        anomaly.score > cfg.alert_threshold,
        "anomalous track must exceed the {} alert threshold, got {:.3} ({:?})",
        cfg.alert_threshold,
        anomaly.score,
        anomaly.components
    );
    assert!(anomaly.band >= Interpretation::StrongAnomaly);
    assert!(
        !anomaly.reasons.is_empty(),
        "governance rule 2: reasons required"
    );

    let corridor: Vec<_> = report
        .reports
        .iter()
        .filter(|r| {
            EASTBOUND_CORRIDOR.contains(&r.icao24.as_str())
                || WESTBOUND_CORRIDOR.contains(&r.icao24.as_str())
        })
        .collect();
    assert!(
        !corridor.is_empty(),
        "some corridor flights must be scored post-baseline"
    );
    for r in corridor {
        assert!(
            r.score <= 0.55,
            "corridor flight {} should stay ≤ 0.55, got {:.3} ({:?})",
            r.icao24,
            r.score,
            r.components
        );
    }
    // Governance: every report carries at least one reason.
    assert!(report.reports.iter().all(|r| !r.reasons.is_empty()));
}

/// (5) §31.9 / §27 rule 1 — explain() cites observation and track ids.
#[test]
fn acceptance_5_explain_cites_observation_ids() {
    let report = run();
    let anomalous = report
        .tracks
        .iter()
        .find(|t| t.icao24 == ANOMALOUS_ICAO24)
        .unwrap();
    let explanation = report
        .skygraph
        .explain(&anomalous.track_id)
        .expect("track explainable");
    assert_eq!(explanation.aircraft_id, ANOMALOUS_ICAO24);
    let joined = explanation.evidence.join("\n");
    assert!(
        joined.contains(&anomalous.track_id),
        "must cite the track id"
    );
    let (first, closest, last) = anomalous.evidence_observation_ids();
    for oid in [first, closest, last] {
        assert!(
            joined.contains(&oid.to_string()),
            "must cite observation {oid}"
        );
    }
    assert!(
        joined.contains("anomaly score"),
        "must surface the anomaly evidence"
    );
    assert!(
        joined.contains("anomalous_relative_to"),
        "must cite the deviated-from baseline"
    );
}

/// (6) §22 Phase 4 — similarity search returns same-corridor flights before
/// the anomalous track.
#[test]
fn acceptance_6_similarity_prefers_same_corridor() {
    let report = run();
    let east: Vec<_> = report
        .tracks
        .iter()
        .filter(|t| EASTBOUND_CORRIDOR.contains(&t.icao24.as_str()))
        .collect();
    // Every eastbound corridor flight's best partner in the top similarity
    // pairs must be corridor traffic, never the anomalous track.
    for (a, b, _) in report.similar_pairs.iter().take(5) {
        assert!(
            !a.contains(ANOMALOUS_ICAO24) && !b.contains(ANOMALOUS_ICAO24),
            "anomalous track must not appear in the closest pairs: {a} <-> {b}"
        );
    }
    // And explicitly: nearest neighbour of an eastbound flight is eastbound.
    let pair = report
        .similar_pairs
        .iter()
        .find(|(a, b, _)| {
            east.iter().any(|t| t.track_id == *a) || east.iter().any(|t| t.track_id == *b)
        })
        .expect("an eastbound pair exists");
    let both_eastbound = EASTBOUND_CORRIDOR
        .iter()
        .filter(|i| pair.0.contains(*i) || pair.1.contains(*i))
        .count();
    assert_eq!(
        both_eastbound, 2,
        "closest eastbound pair must be two eastbound flights: {pair:?}"
    );
}

/// (7) §31.7 — the daily brief renders with non-zero counts.
#[test]
fn acceptance_7_brief_renders_with_counts() {
    let report = run();
    let brief = &report.brief;
    assert_eq!(brief.aircraft_observed, 10);
    assert!(brief.overhead_candidates > 0);
    assert!(brief.unusual_tracks > 0);
    assert!(
        !brief.weather_events.is_empty(),
        "the rain band must be reported"
    );
    let text = brief.to_string();
    assert!(text.contains("Sky brief — oakville_node"));
    assert!(text.contains("10 aircraft observed"));
    assert!(text.contains("Most unusual event"), "brief: {text}");
    let mu = brief.most_unusual.as_ref().unwrap();
    assert!(mu.confidence > 0.55);
}

/// (8) §9.1 — dump1090-style JSON parses into the same pipeline input type.
#[test]
fn acceptance_8_dump1090_parser() {
    let json = r#"{
      "now": 1765219200.0,
      "aircraft": [
        { "hex": "c01a01", "flight": "ACA101 ", "alt_baro": 35000, "gs": 460,
          "track": 71.8, "baro_rate": 0, "lat": 43.49, "lon": -79.62, "rssi": -17.9 }
      ]
    }"#;
    let states = parse_dump1090(json).unwrap();
    assert_eq!(states.len(), 1);
    assert_eq!(states[0].icao24, "c01a01");
    assert_eq!(states[0].callsign, "ACA101");
    assert!((states[0].alt_m - 10_668.0).abs() < 1.0);
    // The parsed state projects into the observer frame like any other.
    let cfg = ObserverConfig::default();
    let f = observer_frame(
        cfg.lat,
        cfg.lon,
        cfg.alt_m,
        states[0].lat,
        states[0].lon,
        states[0].alt_m,
    );
    assert!(f.range_m > 5_000.0 && f.elevation_deg > 30.0);
}
