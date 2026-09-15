use crate::cloud::CloudClient;
use crate::config::{DatabaseReference, InstanceInfo};
use crate::local_runtime::LocalRuntime;
use crate::project::ProjectContext;
use crate::prompts::{self, StatusSelection};
use crate::utils::{print_field, print_header, print_newline};
use eyre::Result;
use serde_json::Value;

pub async fn run(instance: Option<String>) -> Result<()> {
    let project = ProjectContext::find_and_load(None)?;

    print_header("Helix Project Status");
    print_field("Project", &project.config.project.name);
    print_field("Root", &project.root.display().to_string());
    print_newline();

    let runtime = LocalRuntime::new(&project);
    print_header("Instances");
    match resolve_status_selection(&project, instance)? {
        StatusSelection::All => {
            for name in project.config.list_instances() {
                print_instance(&project, &runtime, name).await?;
            }
        }
        StatusSelection::Instance(instance) => {
            print_instance(&project, &runtime, &instance).await?;
        }
    }

    Ok(())
}

fn resolve_status_selection(
    project: &ProjectContext,
    instance: Option<String>,
) -> Result<StatusSelection> {
    if let Some(instance) = instance {
        return Ok(StatusSelection::Instance(instance));
    }
    let instances = all_instances(project);
    if prompts::is_interactive() && instances.len() > 1 {
        return prompts::select_status(&instances);
    }
    Ok(StatusSelection::All)
}

async fn print_instance(
    project: &ProjectContext,
    runtime: &LocalRuntime,
    name: &str,
) -> Result<()> {
    match project.config.get_instance(name)? {
        InstanceInfo::Local(config) => {
            let status = runtime.status(name)?;
            let state = status
                .as_ref()
                .map(|status| status.status.as_str())
                .unwrap_or("not created");
            print_field(
                &format!("{name} (local)"),
                &format!(
                    "http://localhost:{} - {state} - storage: {}",
                    config.port,
                    config.storage.as_str()
                ),
            );
        }
        InstanceInfo::Enterprise(config) => {
            let client = CloudClient::new()?;
            let (resource, state) = match &config.database {
                DatabaseReference::Cluster(id) => {
                    let cluster = client
                        .get(&format!("/v1/clusters/{id}"), "get Cloud cluster status")
                        .await?;
                    let topology = client
                        .get(
                            &format!("/v1/clusters/{id}/topology"),
                            "get Cloud cluster topology",
                        )
                        .await?;
                    (cluster, topology_state(&topology))
                }
                DatabaseReference::Tenant(id) => {
                    let tenant = client
                        .get(&format!("/v1/tenants/{id}"), "get Cloud tenant status")
                        .await?;
                    let state = field(&tenant, &["status", "state"]);
                    (tenant, state)
                }
            };
            let display_name =
                field(&resource, &["name", "slug"]).unwrap_or_else(|| config.database.to_string());
            print_field(
                &format!("{name} (Cloud)"),
                &format!(
                    "{display_name} - {}",
                    state.unwrap_or_else(|| "available".into())
                ),
            );
        }
    }
    Ok(())
}

fn topology_state(topology: &Value) -> Option<String> {
    field(topology, &["phase", "status", "state"])
}

fn field(value: &Value, names: &[&str]) -> Option<String> {
    names.iter().find_map(|name| {
        value.get(*name).and_then(|value| match value {
            Value::String(value) => Some(value.clone()),
            Value::Number(value) => Some(value.to_string()),
            _ => None,
        })
    })
}

fn all_instances(project: &ProjectContext) -> Vec<(String, String)> {
    project
        .config
        .list_instances_with_types()
        .into_iter()
        .map(|(name, kind)| (name.clone(), kind.to_string()))
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn status_field_uses_first_available_name() {
        let value = serde_json::json!({"state":"ready", "status":"active"});
        assert_eq!(
            field(&value, &["phase", "status", "state"]).as_deref(),
            Some("active")
        );
    }
}
