use crate::cloud::CloudClient;
use crate::commands::auth::require_auth;
use crate::config::DatabaseReference;
use crate::project::ProjectContext;
use crate::{
    prompts, ClusterConfigAction, ConfigAction, ConfigOutputFormat, ProjectConfigAction,
    WorkspaceAction,
};
use color_eyre::owo_colors::OwoColorize as _;
use eyre::{eyre, Result, WrapErr as _};
use serde_json::{json, Value};

pub async fn run(action: Option<ConfigAction>) -> Result<()> {
    match action {
        Some(ConfigAction::Workspace { action }) => run_workspace(Some(action)).await,
        Some(ConfigAction::Project { action }) => run_project(Some(action)).await,
        Some(ConfigAction::Cluster { action }) => run_cluster(Some(action)).await,
        None => Err(eyre!(
            "Specify 'helix workspace', 'helix project', or 'helix cluster'"
        )),
    }
}

pub async fn run_workspace(action: Option<WorkspaceAction>) -> Result<()> {
    match action {
        Some(WorkspaceAction::List { format }) => {
            let client = require_auth().await?;
            let response = client.get("/v1/workspaces", "list workspaces").await?;
            print_collection(&response, "workspaces", "Workspaces", format)
        }
        Some(WorkspaceAction::Get { workspace, format }) => {
            let client = require_auth().await?;
            let response = client
                .get(&format!("/v1/workspaces/{workspace}"), "get workspace")
                .await?;
            print_resource(&response, "Workspace", format)
        }
        None => Err(eyre!(
            "Specify 'helix workspace list' or 'helix workspace get <workspace-id>'"
        )),
    }
}

pub async fn run_project(action: Option<ProjectConfigAction>) -> Result<()> {
    match action {
        Some(ProjectConfigAction::List {
            workspace_id,
            format,
        }) => {
            let workspace_id = workspace_id
                .or_else(|| linked_project().and_then(|(_, workspace)| workspace))
                .ok_or_else(|| eyre!("Pass --workspace-id or link a project in helix.toml"))?;
            let client = require_auth().await?;
            let response = client
                .get(
                    &format!(
                        "/v1/projects?workspace_id={}",
                        urlencoding::encode(&workspace_id)
                    ),
                    "list projects",
                )
                .await?;
            print_collection(&response, "projects", "Projects", format)
        }
        Some(ProjectConfigAction::Get { project, format }) => {
            let project = project
                .or_else(|| linked_project().map(|(project, _)| project))
                .ok_or_else(|| eyre!("Pass a project ID or link one in helix.toml"))?;
            let client = require_auth().await?;
            let response = client
                .get(&format!("/v1/projects/{project}"), "get project")
                .await?;
            print_resource(&response, "Project", format)
        }
        Some(ProjectConfigAction::Create {
            workspace,
            slug,
            name,
            format,
        }) => {
            let client = require_auth().await?;
            let response = client
                .post(
                    "/v1/projects",
                    json!({"workspaceId": workspace, "slug": slug, "displayName": name}),
                    "create project",
                )
                .await?;
            print_resource(&response, "Project", format)
        }
        Some(ProjectConfigAction::Delete { project, yes }) => {
            let project = project
                .or_else(|| linked_project().map(|(project, _)| project))
                .ok_or_else(|| eyre!("Pass a project ID or link one in helix.toml"))?;
            if !yes {
                if !prompts::is_interactive() {
                    return Err(eyre!(
                        "Project deletion requires --yes in non-interactive mode"
                    ));
                }
                if !prompts::confirm(&format!("Delete Cloud project {project}?"))? {
                    return Ok(());
                }
            }
            require_auth()
                .await?
                .delete(&format!("/v1/projects/{project}"), "delete project")
                .await?;
            crate::output::success(&format!("Deleted project {project}"));
            Ok(())
        }
        Some(ProjectConfigAction::Link { project, workspace }) => {
            let client = require_auth().await?;
            let remote = client
                .get(&format!("/v1/projects/{project}"), "get project")
                .await?;
            let remote_workspace = required_string(&remote, "workspaceId")?;
            if workspace
                .as_deref()
                .is_some_and(|workspace| workspace != remote_workspace)
            {
                return Err(eyre!(
                    "project does not belong to workspace {}",
                    workspace.unwrap()
                ));
            }
            let display_name = remote
                .get("displayName")
                .and_then(Value::as_str)
                .or_else(|| remote.get("slug").and_then(Value::as_str))
                .unwrap_or("Helix project");
            let mut context = ProjectContext::find_and_load(None)?;
            context.config.project.id = Some(project.clone());
            context.config.project.workspace_id = Some(remote_workspace.to_owned());
            context.config.project.name = display_name.to_owned();
            context
                .config
                .save_to_file(&context.root.join("helix.toml"))?;
            crate::output::success(&format!("Linked project {project}"));
            Ok(())
        }
        None => Err(eyre!("Specify 'helix project list|get|create|delete|link'")),
    }
}

pub async fn run_cluster(action: Option<ClusterConfigAction>) -> Result<()> {
    match action {
        Some(ClusterConfigAction::List {
            workspace_id,
            project_id,
            format,
        }) => {
            let linked = linked_project();
            let project_id =
                project_id.or_else(|| linked.as_ref().map(|(project, _)| project.clone()));
            let workspace_id = workspace_id.or_else(|| linked.and_then(|(_, workspace)| workspace));
            let client = require_auth().await?;
            let query = match (project_id, workspace_id) {
                (Some(project), workspace) => {
                    let workspace = match workspace {
                        Some(workspace) => workspace,
                        None => project_workspace(&client, &project).await?,
                    };
                    format!(
                        "workspace_id={}&project_id={}",
                        urlencoding::encode(&workspace),
                        urlencoding::encode(&project)
                    )
                }
                (None, Some(workspace)) => {
                    format!("workspace_id={}", urlencoding::encode(&workspace))
                }
                (None, None) => {
                    return Err(eyre!(
                        "Pass --project-id or --workspace-id, or link a project in helix.toml"
                    ));
                }
            };
            let response = client
                .get(&format!("/v1/clusters?{query}"), "list clusters")
                .await?;
            print_collection(&response, "clusters", "Clusters", format)
        }
        Some(ClusterConfigAction::Get { cluster_id, format }) => {
            let response = require_auth()
                .await?
                .get(&format!("/v1/clusters/{cluster_id}"), "get cluster")
                .await?;
            print_resource(&response, "Cluster", format)
        }
        Some(ClusterConfigAction::Indexes { cluster_id, format }) => {
            let database = match cluster_id {
                Some(cluster) => DatabaseReference::Cluster(cluster),
                None => {
                    linked_database().wrap_err("Pass --cluster-id or link a cluster database")?
                }
            };
            let DatabaseReference::Cluster(cluster) = database else {
                return Err(eyre!("linked database is a tenant; pass --cluster-id"));
            };
            let response = require_auth()
                .await?
                .get(
                    &format!("/v1/clusters/{cluster}/indexes"),
                    "list cluster indexes",
                )
                .await?;
            print_resource(&response, "Cluster indexes", format)
        }
        None => Err(eyre!("Specify 'helix cluster list|get|indexes'")),
    }
}

pub(crate) fn linked_project() -> Option<(String, Option<String>)> {
    let context = ProjectContext::find_and_load(None).ok()?;
    Some((
        context.config.project.id?,
        context.config.project.workspace_id,
    ))
}

pub(crate) fn linked_database() -> Result<DatabaseReference> {
    let context = ProjectContext::find_and_load(None)?;
    let mut databases = context
        .config
        .enterprise
        .values()
        .map(|instance| instance.database.clone());
    let database = databases
        .next()
        .ok_or_else(|| eyre!("No Cloud database is linked in helix.toml"))?;
    if databases.next().is_some() {
        return Err(eyre!(
            "Multiple Cloud databases are linked; specify an explicit database target"
        ));
    }
    Ok(database)
}

pub(crate) struct ResolvedCloudTarget {
    pub database: DatabaseReference,
    pub project_id: String,
    pub workspace_id: String,
}

pub(crate) async fn resolve_cloud_target(
    database: Option<String>,
    project_id: Option<String>,
    workspace_id: Option<String>,
) -> Result<ResolvedCloudTarget> {
    let client = require_auth().await?;
    if let Some(database) = database {
        let database: DatabaseReference =
            database.parse().map_err(|message: String| eyre!(message))?;
        let remote = match &database {
            DatabaseReference::Cluster(id) => {
                let cluster = client
                    .get(&format!("/v1/clusters/{id}"), "get cluster")
                    .await?;
                let access = cluster
                    .get("access")
                    .and_then(Value::as_str)
                    .unwrap_or_default();
                if access != "CLUSTER_ACCESS_DEDICATED" && access != "dedicated" {
                    return Err(eyre!("shared physical clusters are not database targets"));
                }
                cluster
            }
            DatabaseReference::Tenant(id) => {
                client
                    .get(&format!("/v1/tenants/{id}"), "get tenant")
                    .await?
            }
        };
        let owner_project = required_string(&remote, "projectId")?.to_owned();
        let owner_workspace = required_string(&remote, "workspaceId")?.to_owned();
        if project_id.as_deref().is_some_and(|id| id != owner_project)
            || workspace_id
                .as_deref()
                .is_some_and(|id| id != owner_workspace)
        {
            return Err(eyre!(
                "explicit database does not belong to the supplied owner"
            ));
        }
        return Ok(ResolvedCloudTarget {
            database,
            project_id: owner_project,
            workspace_id: owner_workspace,
        });
    }

    let linked = linked_project();
    let project_id = project_id
        .or_else(|| linked.as_ref().map(|(project, _)| project.clone()))
        .ok_or_else(|| eyre!("Pass --database or --project; no project is linked"))?;
    let workspace_id = workspace_id.or_else(|| linked.and_then(|(_, workspace)| workspace));
    let workspace_id = match workspace_id {
        Some(workspace_id) => workspace_id,
        None => project_workspace(&client, &project_id).await?,
    };
    let encoded_project = urlencoding::encode(&project_id);
    let encoded_workspace = urlencoding::encode(&workspace_id);
    let clusters = client
        .get(
            &format!("/v1/clusters?workspace_id={encoded_workspace}&project_id={encoded_project}"),
            "list project clusters",
        )
        .await?;
    let tenants = client
        .get(
            &format!("/v1/tenants?workspace_id={encoded_workspace}&project_id={encoded_project}"),
            "list project tenants",
        )
        .await?;
    let mut candidates = Vec::new();
    for cluster in clusters
        .get("clusters")
        .and_then(Value::as_array)
        .into_iter()
        .flatten()
    {
        let access = cluster
            .get("access")
            .and_then(Value::as_str)
            .unwrap_or_default();
        if (access == "CLUSTER_ACCESS_DEDICATED" || access == "dedicated")
            && let Some(id) = cluster.get("id").and_then(Value::as_str)
        {
            candidates.push(DatabaseReference::Cluster(id.to_owned()));
        }
    }
    candidates.extend(
        tenants
            .get("tenants")
            .and_then(Value::as_array)
            .into_iter()
            .flatten()
            .filter_map(|tenant| tenant.get("id").and_then(Value::as_str))
            .map(|id| DatabaseReference::Tenant(id.to_owned())),
    );
    let [database] = candidates.as_slice() else {
        let available = candidates
            .iter()
            .map(ToString::to_string)
            .collect::<Vec<_>>()
            .join(", ");
        return Err(eyre!(
            "Project {project_id} does not resolve to exactly one database. Candidates: {available}. Pass --database."
        ));
    };
    Ok(ResolvedCloudTarget {
        database: database.clone(),
        project_id,
        workspace_id,
    })
}

pub(crate) async fn project_workspace(client: &CloudClient, project: &str) -> Result<String> {
    let remote = client
        .get(&format!("/v1/projects/{project}"), "get project owner")
        .await?;
    Ok(required_string(&remote, "workspaceId")?.to_owned())
}

fn required_string<'a>(value: &'a Value, field: &str) -> Result<&'a str> {
    value
        .get(field)
        .and_then(Value::as_str)
        .filter(|value| !value.is_empty())
        .ok_or_else(|| eyre!("Cloud response omitted {field}"))
}

pub(crate) fn print_collection(
    response: &Value,
    field: &str,
    title: &str,
    format: ConfigOutputFormat,
) -> Result<()> {
    if format == ConfigOutputFormat::Json {
        println!("{}", serde_json::to_string_pretty(response)?);
        return Ok(());
    }
    println!("{}", title.bold());
    for resource in response
        .get(field)
        .and_then(Value::as_array)
        .into_iter()
        .flatten()
    {
        let id = resource
            .get("id")
            .and_then(Value::as_str)
            .unwrap_or("unknown");
        let name = resource
            .get("displayName")
            .or_else(|| resource.get("name"))
            .or_else(|| resource.get("slug"))
            .and_then(Value::as_str)
            .unwrap_or(id);
        println!("  {name} ({id})");
    }
    Ok(())
}

pub(crate) fn print_resource(
    response: &Value,
    title: &str,
    format: ConfigOutputFormat,
) -> Result<()> {
    if format == ConfigOutputFormat::Json {
        println!("{}", serde_json::to_string_pretty(response)?);
    } else {
        println!("{}", title.bold());
        println!("{}", serde_json::to_string_pretty(response)?);
    }
    Ok(())
}
