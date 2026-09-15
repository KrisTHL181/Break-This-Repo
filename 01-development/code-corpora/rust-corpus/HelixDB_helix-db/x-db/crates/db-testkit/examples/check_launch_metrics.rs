//! Validate a same-host ten-run launch comparison JSON document.

use std::error::Error;
use std::path::PathBuf;

use helix_db_testkit::launch_gate::LaunchComparison;
use helix_db_testkit::sustained::ReplicaLagPolicy;

fn main() -> Result<(), Box<dyn Error>> {
    let mut arguments = std::env::args_os().skip(1);
    let Some(path) = arguments.next().map(PathBuf::from) else {
        return Err("usage: check_launch_metrics <comparison.json>".into());
    };
    if arguments.next().is_some() {
        return Err("usage: check_launch_metrics <comparison.json>".into());
    }

    let comparison = serde_json::from_slice::<LaunchComparison>(&std::fs::read(path)?)?;
    match comparison.evaluate(ReplicaLagPolicy::launch_default()) {
        Ok(()) => {
            println!("cloud-launch metrics passed");
            Ok(())
        }
        Err(failures) => {
            eprintln!(
                "{}",
                serde_json::to_string_pretty(&failures.iter().collect::<Vec<_>>())?
            );
            Err("cloud-launch metrics failed".into())
        }
    }
}
