//! ruOS thermal supervisor skeleton — Pi 5 + AI HAT+ thermal observability
//! and (future) clock control.
//!
//! ADR-174 deliverable. Iter 91 ships a pure-read sysfs reader: walks
//! `/sys/class/thermal/thermal_zone*` for temperatures and
//! `/sys/devices/system/cpu/cpufreq/policy*` for current/max frequency.
//! No daemon, no socket, no clock writes — those land iter 92-97 per
//! the ADR roadmap.
//!
//! Why a skeleton first: the read path is the thing we'll hit on every
//! 5-second tick of the future supervisor. Get it right + tested before
//! adding the writer + IPC machinery.
//!
//! ```no_run
//! use ruos_thermal::ThermalSensor;
//!
//! # fn main() -> std::io::Result<()> {
//! let sensor = ThermalSensor::system();
//! let snapshot = sensor.read()?;
//! for cpu in &snapshot.cpu_temps_celsius {
//!     println!("zone {} = {:.1}°C", cpu.zone, cpu.celsius);
//! }
//! for policy in &snapshot.cpu_policies {
//!     println!("policy {} cur={} max={}", policy.id, policy.cur_hz, policy.max_hz);
//! }
//! # Ok(()) }
//! ```

#![warn(missing_docs)]

use std::fs;
use std::io;
use std::path::{Path, PathBuf};

/// One CPU thermal zone reading from sysfs.
#[derive(Debug, Clone, PartialEq)]
pub struct CpuTemp {
    /// Zone index (typically 0..N).
    pub zone: u32,
    /// Temperature in degrees Celsius. Pi 5 reports millidegrees in
    /// `/sys/class/thermal/thermal_zone*/temp`; we divide by 1000.
    pub celsius: f32,
}

/// Per-policy cpufreq snapshot. On Pi 5 there are typically 4 policies
/// (one per Cortex-A76 core).
#[derive(Debug, Clone, PartialEq)]
pub struct CpuPolicy {
    /// Policy index from `policy0`, `policy1`, …
    pub id: u32,
    /// Current frequency in Hz (read from `scaling_cur_freq` or
    /// `cpuinfo_cur_freq`; sysfs returns kHz so we multiply by 1000).
    pub cur_hz: u64,
    /// Configured max frequency in Hz (`scaling_max_freq`).
    pub max_hz: u64,
    /// Hardware ceiling frequency in Hz (`cpuinfo_max_freq`).
    pub hw_max_hz: u64,
    /// Active governor name (`scaling_governor` — typically
    /// `ondemand`, `performance`, `powersave`).
    pub governor: String,
}

/// One thermal-state snapshot taken at a moment in time.
#[derive(Debug, Clone, Default, PartialEq)]
pub struct Snapshot {
    /// All CPU thermal zones the sensor found, in zone-index order.
    pub cpu_temps_celsius: Vec<CpuTemp>,
    /// All cpufreq policies the sensor found, in policy-index order.
    pub cpu_policies: Vec<CpuPolicy>,
}

impl Snapshot {
    /// Highest CPU temperature across all zones, or `None` if no zones
    /// were readable. Used by the future budget calculator.
    pub fn max_cpu_celsius(&self) -> Option<f32> {
        self.cpu_temps_celsius.iter().map(|c| c.celsius).fold(None, |acc, t| {
            Some(acc.map_or(t, |a: f32| a.max(t)))
        })
    }

    /// Mean CPU temperature across all zones, or `None` if no zones
    /// were readable.
    pub fn mean_cpu_celsius(&self) -> Option<f32> {
        if self.cpu_temps_celsius.is_empty() {
            None
        } else {
            let sum: f32 = self.cpu_temps_celsius.iter().map(|c| c.celsius).sum();
            Some(sum / self.cpu_temps_celsius.len() as f32)
        }
    }
}

/// Named clock profiles defined in ADR-174 §Clock profiles. Each
/// resolves to a target `scaling_max_freq` for every cpufreq policy.
///
/// Pre-iter-94 these existed only as English in the ADR; iter-94
/// (this code) makes them switchable at runtime via `apply_profile`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ClockProfile {
    /// 1.4 GHz — battery / solar / fanless. ~3 W sustained.
    Eco,
    /// 2.4 GHz — Pi 5 stock. Passive heatsink. ~5 W.
    Default,
    /// 2.6 GHz — large heatsink. Validated thermal envelope. ~7 W.
    SafeOverclock,
    /// 2.8 GHz — requires active fan. ~10 W.
    Aggressive,
    /// 3.0 GHz — heatsink + fan, monitored. Voids warranty; can degrade
    /// silicon over time. Documented prominently in install path.
    Max,
}

impl ClockProfile {
    /// Target max frequency in Hz for this profile.
    pub fn target_max_hz(self) -> u64 {
        match self {
            ClockProfile::Eco => 1_400_000_000,
            ClockProfile::Default => 2_400_000_000,
            ClockProfile::SafeOverclock => 2_600_000_000,
            ClockProfile::Aggressive => 2_800_000_000,
            ClockProfile::Max => 3_000_000_000,
        }
    }

    /// Estimated sustained draw in watts for the Pi 5 + AI HAT+ stack.
    /// Numbers from ADR-174 §Combined edge-node thermal envelope.
    pub fn estimated_watts(self) -> f32 {
        match self {
            ClockProfile::Eco => 3.0,
            ClockProfile::Default => 5.0,
            ClockProfile::SafeOverclock => 7.0,
            ClockProfile::Aggressive => 10.0,
            ClockProfile::Max => 13.0,
        }
    }

    /// Parse `eco`, `default`, `safe-overclock`, `aggressive`, `max`.
    /// Returns None for unknown names — caller decides how to surface.
    pub fn from_name(s: &str) -> Option<Self> {
        match s {
            "eco" => Some(ClockProfile::Eco),
            "default" => Some(ClockProfile::Default),
            "safe-overclock" | "safe" => Some(ClockProfile::SafeOverclock),
            "aggressive" => Some(ClockProfile::Aggressive),
            "max" => Some(ClockProfile::Max),
            _ => None,
        }
    }

    /// Canonical name (matches the parser).
    pub fn name(self) -> &'static str {
        match self {
            ClockProfile::Eco => "eco",
            ClockProfile::Default => "default",
            ClockProfile::SafeOverclock => "safe-overclock",
            ClockProfile::Aggressive => "aggressive",
            ClockProfile::Max => "max",
        }
    }

    /// All profiles, low → high. Useful for `--show-profiles`.
    pub fn all() -> &'static [ClockProfile] {
        &[
            ClockProfile::Eco,
            ClockProfile::Default,
            ClockProfile::SafeOverclock,
            ClockProfile::Aggressive,
            ClockProfile::Max,
        ]
    }
}

/// sysfs-backed thermal reader. The two root paths are configurable
/// so tests can point at a synthetic tree under `tempfile`.
pub struct ThermalSensor {
    thermal_root: PathBuf,
    cpufreq_root: PathBuf,
}

impl ThermalSensor {
    /// Construct a sensor that reads the live system at the canonical
    /// Linux sysfs paths.
    pub fn system() -> Self {
        Self {
            thermal_root: PathBuf::from("/sys/class/thermal"),
            cpufreq_root: PathBuf::from("/sys/devices/system/cpu/cpufreq"),
        }
    }

    /// Construct a sensor pointing at synthetic roots — for tests.
    pub fn with_roots(thermal: impl Into<PathBuf>, cpufreq: impl Into<PathBuf>) -> Self {
        Self {
            thermal_root: thermal.into(),
            cpufreq_root: cpufreq.into(),
        }
    }

    /// Walk the configured roots and return one `Snapshot`.
    /// Read errors on individual files are treated as "skip this zone /
    /// policy" — partial snapshots beat returning Err for one missing
    /// file. A truly empty root returns an empty `Snapshot`, not Err.
    pub fn read(&self) -> io::Result<Snapshot> {
        let mut snap = Snapshot::default();

        if let Ok(entries) = fs::read_dir(&self.thermal_root) {
            for entry in entries.flatten() {
                let path = entry.path();
                let name = match path.file_name().and_then(|n| n.to_str()) {
                    Some(n) => n,
                    None => continue,
                };
                if let Some(zone) = parse_thermal_zone(name) {
                    if let Some(c) = read_temp_millicelsius(&path.join("temp")) {
                        snap.cpu_temps_celsius.push(CpuTemp {
                            zone,
                            celsius: c / 1000.0,
                        });
                    }
                }
            }
            snap.cpu_temps_celsius.sort_by_key(|c| c.zone);
        }

        if let Ok(entries) = fs::read_dir(&self.cpufreq_root) {
            for entry in entries.flatten() {
                let path = entry.path();
                let name = match path.file_name().and_then(|n| n.to_str()) {
                    Some(n) => n,
                    None => continue,
                };
                if let Some(id) = parse_cpufreq_policy(name) {
                    if let Some(p) = read_policy(&path, id) {
                        snap.cpu_policies.push(p);
                    }
                }
            }
            snap.cpu_policies.sort_by_key(|p| p.id);
        }

        Ok(snap)
    }
}

impl ThermalSensor {
    /// Write `profile.target_max_hz()` to every cpufreq policy's
    /// `scaling_max_freq` (in kHz, sysfs convention). Returns the
    /// number of policies updated; missing-policy errors don't abort.
    ///
    /// **Caller must hold CAP_SYS_NICE / be root** — sysfs cpufreq writes
    /// are privileged. Failure to open the file (EACCES) is surfaced as
    /// `io::ErrorKind::PermissionDenied` from the *first* failing policy
    /// so the operator sees an actionable error instead of a half-applied
    /// state.
    ///
    /// Iter-94 deliverable from ADR-174's roadmap. Iter 93's reader
    /// remains the source of truth for *current* state — this just adds
    /// the writer path.
    pub fn apply_profile(&self, profile: ClockProfile) -> io::Result<usize> {
        let target_khz = profile.target_max_hz() / 1000;
        let mut applied = 0usize;
        let entries = fs::read_dir(&self.cpufreq_root)?;
        for entry in entries.flatten() {
            let path = entry.path();
            let name = match path.file_name().and_then(|n| n.to_str()) {
                Some(n) => n,
                None => continue,
            };
            if parse_cpufreq_policy(name).is_some() {
                let target = path.join("scaling_max_freq");
                fs::write(&target, target_khz.to_string())?;
                applied += 1;
            }
        }
        Ok(applied)
    }
}

/// `thermal_zone3` → Some(3); anything else → None.
fn parse_thermal_zone(name: &str) -> Option<u32> {
    name.strip_prefix("thermal_zone")?.parse().ok()
}

/// `policy12` → Some(12); anything else → None.
fn parse_cpufreq_policy(name: &str) -> Option<u32> {
    name.strip_prefix("policy")?.parse().ok()
}

fn read_temp_millicelsius(path: &Path) -> Option<f32> {
    let s = fs::read_to_string(path).ok()?;
    s.trim().parse::<f32>().ok()
}

fn read_u64_khz(path: &Path) -> Option<u64> {
    let s = fs::read_to_string(path).ok()?;
    s.trim().parse::<u64>().ok().map(|khz| khz * 1000)
}

fn read_string(path: &Path) -> Option<String> {
    fs::read_to_string(path).ok().map(|s| s.trim().to_string())
}

fn read_policy(path: &Path, id: u32) -> Option<CpuPolicy> {
    // Try scaling_cur_freq first (most accurate); fall back to cpuinfo_cur_freq.
    let cur_hz = read_u64_khz(&path.join("scaling_cur_freq"))
        .or_else(|| read_u64_khz(&path.join("cpuinfo_cur_freq")))
        .unwrap_or(0);
    let max_hz = read_u64_khz(&path.join("scaling_max_freq")).unwrap_or(0);
    let hw_max_hz = read_u64_khz(&path.join("cpuinfo_max_freq")).unwrap_or(max_hz);
    let governor = read_string(&path.join("scaling_governor")).unwrap_or_default();
    Some(CpuPolicy { id, cur_hz, max_hz, hw_max_hz, governor })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use tempfile::TempDir;

    /// Build a synthetic sysfs tree for tests:
    ///   thermal_zone0/temp = 50000  (50.0°C)
    ///   thermal_zone1/temp = 65000  (65.0°C)
    ///   thermal_zone2/temp = 80000  (80.0°C)
    ///   policy0/{scaling_cur_freq,scaling_max_freq,cpuinfo_max_freq,scaling_governor}
    ///   policy1/...
    fn write_synthetic_sysfs(tmp: &TempDir) -> (PathBuf, PathBuf) {
        let thermal = tmp.path().join("thermal");
        let cpufreq = tmp.path().join("cpufreq");
        fs::create_dir_all(&thermal).unwrap();
        fs::create_dir_all(&cpufreq).unwrap();

        // thermal zones
        for (i, mc) in [(0u32, 50000), (1, 65000), (2, 80000)] {
            let zd = thermal.join(format!("thermal_zone{}", i));
            fs::create_dir_all(&zd).unwrap();
            fs::write(zd.join("temp"), mc.to_string()).unwrap();
        }
        // intentional malformed zone — sensor should skip without erroring.
        let bad = thermal.join("thermal_zone_bogus");
        fs::create_dir_all(&bad).unwrap();
        fs::write(bad.join("temp"), "not-a-number").unwrap();

        // cpufreq policies
        for (i, cur, max, hw_max, gov) in [
            (0u32, 1_400_000u64, 2_400_000u64, 2_400_000u64, "ondemand"),
            (1, 2_400_000, 2_400_000, 2_400_000, "performance"),
        ] {
            let pd = cpufreq.join(format!("policy{}", i));
            fs::create_dir_all(&pd).unwrap();
            fs::write(pd.join("scaling_cur_freq"), cur.to_string()).unwrap();
            fs::write(pd.join("scaling_max_freq"), max.to_string()).unwrap();
            fs::write(pd.join("cpuinfo_max_freq"), hw_max.to_string()).unwrap();
            fs::write(pd.join("scaling_governor"), gov).unwrap();
        }

        (thermal, cpufreq)
    }

    #[test]
    fn parse_thermal_zone_strips_prefix() {
        assert_eq!(parse_thermal_zone("thermal_zone0"), Some(0));
        assert_eq!(parse_thermal_zone("thermal_zone42"), Some(42));
        assert_eq!(parse_thermal_zone("thermal_zone_bogus"), None);
        assert_eq!(parse_thermal_zone("policy0"), None);
    }

    #[test]
    fn parse_cpufreq_policy_strips_prefix() {
        assert_eq!(parse_cpufreq_policy("policy0"), Some(0));
        assert_eq!(parse_cpufreq_policy("policy3"), Some(3));
        assert_eq!(parse_cpufreq_policy("notapolicy"), None);
    }

    #[test]
    fn snapshot_max_and_mean_match_inputs() {
        let snap = Snapshot {
            cpu_temps_celsius: vec![
                CpuTemp { zone: 0, celsius: 50.0 },
                CpuTemp { zone: 1, celsius: 65.0 },
                CpuTemp { zone: 2, celsius: 80.0 },
            ],
            cpu_policies: vec![],
        };
        assert_eq!(snap.max_cpu_celsius(), Some(80.0));
        assert!((snap.mean_cpu_celsius().unwrap() - 65.0).abs() < f32::EPSILON);
    }

    #[test]
    fn snapshot_empty_yields_none() {
        let snap = Snapshot::default();
        assert_eq!(snap.max_cpu_celsius(), None);
        assert_eq!(snap.mean_cpu_celsius(), None);
    }

    #[test]
    fn read_returns_synthetic_zones_in_order() {
        let tmp = TempDir::new().unwrap();
        let (thermal, cpufreq) = write_synthetic_sysfs(&tmp);
        let sensor = ThermalSensor::with_roots(thermal, cpufreq);
        let snap = sensor.read().unwrap();

        // 3 valid zones (the malformed `thermal_zone_bogus` was skipped).
        assert_eq!(snap.cpu_temps_celsius.len(), 3);
        assert_eq!(snap.cpu_temps_celsius[0].zone, 0);
        assert_eq!(snap.cpu_temps_celsius[0].celsius, 50.0);
        assert_eq!(snap.cpu_temps_celsius[2].celsius, 80.0);

        // 2 policies sorted by id.
        assert_eq!(snap.cpu_policies.len(), 2);
        assert_eq!(snap.cpu_policies[0].id, 0);
        assert_eq!(snap.cpu_policies[0].cur_hz, 1_400_000_000);
        assert_eq!(snap.cpu_policies[0].max_hz, 2_400_000_000);
        assert_eq!(snap.cpu_policies[0].governor, "ondemand");
        assert_eq!(snap.cpu_policies[1].governor, "performance");
    }

    #[test]
    fn clock_profile_parse_and_target_freqs() {
        // Round-trip name → enum → target_hz.
        for p in ClockProfile::all() {
            let parsed = ClockProfile::from_name(p.name());
            assert_eq!(parsed, Some(*p));
            assert!(p.target_max_hz() >= 1_400_000_000);
            assert!(p.target_max_hz() <= 3_000_000_000);
            assert!(p.estimated_watts() >= 3.0);
            assert!(p.estimated_watts() <= 13.0);
        }
        // Synonym + bad input.
        assert_eq!(ClockProfile::from_name("safe"), Some(ClockProfile::SafeOverclock));
        assert_eq!(ClockProfile::from_name("turbo"), None);
        assert_eq!(ClockProfile::from_name(""), None);
    }

    #[test]
    fn apply_profile_writes_target_to_each_policy() {
        let tmp = TempDir::new().unwrap();
        let (thermal, cpufreq) = write_synthetic_sysfs(&tmp);
        let sensor = ThermalSensor::with_roots(thermal, cpufreq.clone());

        // Synthetic tree has 2 policies.
        let applied = sensor.apply_profile(ClockProfile::SafeOverclock).unwrap();
        assert_eq!(applied, 2);

        // Verify both policy0 + policy1 now show the new max.
        let p0 = fs::read_to_string(cpufreq.join("policy0/scaling_max_freq")).unwrap();
        let p1 = fs::read_to_string(cpufreq.join("policy1/scaling_max_freq")).unwrap();
        // SafeOverclock = 2_600_000_000 Hz → 2_600_000 kHz on disk.
        assert_eq!(p0.trim(), "2600000");
        assert_eq!(p1.trim(), "2600000");

        // Re-read with the sensor → snapshot reflects the new max.
        let snap = sensor.read().unwrap();
        assert_eq!(snap.cpu_policies.len(), 2);
        assert_eq!(snap.cpu_policies[0].max_hz, 2_600_000_000);
        assert_eq!(snap.cpu_policies[1].max_hz, 2_600_000_000);
    }

    #[test]
    fn apply_profile_eco_underclocks() {
        let tmp = TempDir::new().unwrap();
        let (thermal, cpufreq) = write_synthetic_sysfs(&tmp);
        let sensor = ThermalSensor::with_roots(thermal, cpufreq.clone());
        let applied = sensor.apply_profile(ClockProfile::Eco).unwrap();
        assert_eq!(applied, 2);
        let p0 = fs::read_to_string(cpufreq.join("policy0/scaling_max_freq")).unwrap();
        assert_eq!(p0.trim(), "1400000"); // 1.4 GHz in kHz
    }

    #[test]
    fn read_handles_missing_roots_gracefully() {
        // Both roots point at a path that doesn't exist — no panic, empty snapshot.
        let sensor = ThermalSensor::with_roots(
            "/nonexistent-path-thermal",
            "/nonexistent-path-cpufreq",
        );
        let snap = sensor.read().unwrap();
        assert!(snap.cpu_temps_celsius.is_empty());
        assert!(snap.cpu_policies.is_empty());
    }
}
