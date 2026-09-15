use crate::cloud::CloudClient;
use crate::config::{DatabaseReference, InstanceInfo};
use crate::local_runtime::LocalRuntime;
use crate::project::ProjectContext;
use crate::prompts;
use chrono::{DateTime, Duration, Utc};
use eyre::{eyre, Result};
use serde_json::Value;

pub async fn run(
    instance: Option<String>,
    follow: bool,
    range: bool,
    start: Option<String>,
    end: Option<String>,
) -> Result<()> {
    let project = ProjectContext::find_and_load(None)?;
    let instance = resolve_instance(&project, instance)?;
    match project.config.get_instance(&instance)? {
        InstanceInfo::Local(_) => {
            if range || start.is_some() || end.is_some() {
                return Err(eyre!(
                    "--range, --start, and --end are only supported for Cloud query errors; local logs use docker/podman logs"
                ));
            }
            LocalRuntime::new(&project).logs(&instance, follow)?;
        }
        InstanceInfo::Enterprise(config) => {
            if follow {
                return Err(eyre!(
                    "live Cloud log streaming is not supported; omit --follow to list recent query errors"
                ));
            }
            let (start, end) = parse_range(range, start, end)?;
            let path = match &config.database {
                DatabaseReference::Cluster(id) => format!(
                    "/v1/clusters/{id}/query-errors?startTime={}&endTime={}",
                    start.timestamp(),
                    end.timestamp()
                ),
                DatabaseReference::Tenant(id) => format!(
                    "/v1/tenants/{id}/query-errors?startTime={}&endTime={}",
                    start.timestamp(),
                    end.timestamp()
                ),
            };
            let response = CloudClient::new()?
                .get(&path, "list Cloud query errors")
                .await?;
            let errors = response
                .get("errors")
                .and_then(Value::as_array)
                .ok_or_else(|| eyre!("Cloud query-error response has no errors list"))?;
            for error in errors {
                let timestamp = error
                    .get("timestamp")
                    .and_then(Value::as_str)
                    .unwrap_or("unknown time");
                let query = error
                    .get("queryName")
                    .and_then(Value::as_str)
                    .unwrap_or("unknown query");
                let output = error
                    .get("output")
                    .and_then(Value::as_str)
                    .unwrap_or("unknown error");
                println!("{timestamp} {query}: {output}");
            }
        }
    }
    Ok(())
}

fn resolve_instance(project: &ProjectContext, instance: Option<String>) -> Result<String> {
    if let Some(instance) = instance {
        return Ok(instance);
    }
    let instances = all_instances(project);
    if prompts::is_interactive() && instances.len() > 1 {
        return prompts::select_instance(&instances, "Show logs for which instance?");
    }
    if project.config.local.contains_key("dev") || project.config.enterprise.contains_key("dev") {
        return Ok("dev".to_string());
    }
    if instances.len() == 1 {
        return Ok(instances[0].0.clone());
    }
    Err(eyre!("No instance specified"))
}

fn all_instances(project: &ProjectContext) -> Vec<(String, String)> {
    project
        .config
        .list_instances_with_types()
        .into_iter()
        .map(|(name, kind)| (name.clone(), kind.to_string()))
        .collect()
}

fn parse_range(
    _range: bool,
    start: Option<String>,
    end: Option<String>,
) -> Result<(DateTime<Utc>, DateTime<Utc>)> {
    let end = match end {
        Some(end) => DateTime::parse_from_rfc3339(&end)?.with_timezone(&Utc),
        None => Utc::now(),
    };
    let start = match start {
        Some(start) => DateTime::parse_from_rfc3339(&start)?.with_timezone(&Utc),
        None => end - Duration::hours(1),
    };
    Ok((start, end))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn explicit_log_range_is_parsed_in_utc() {
        let (start, end) = parse_range(
            true,
            Some("2026-01-01T00:00:00+01:00".to_string()),
            Some("2026-01-01T02:00:00+01:00".to_string()),
        )
        .unwrap();
        assert_eq!(start.timestamp(), 1_767_222_000);
        assert_eq!(end.timestamp(), 1_767_229_200);
    }

    #[test]
    fn missing_start_defaults_to_one_hour_before_end() {
        let (start, end) =
            parse_range(true, None, Some("2026-01-01T02:00:00Z".to_string())).unwrap();
        assert_eq!(end - start, Duration::hours(1));
    }

    #[test]
    fn malformed_log_timestamp_is_rejected() {
        assert!(parse_range(true, Some("yesterday".to_string()), None).is_err());
        assert!(parse_range(true, None, Some("later".to_string())).is_err());
    }
}
