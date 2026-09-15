use super::auth::require_auth;
use super::config::{
    linked_database, linked_project, print_collection, print_resource, project_workspace,
};
use crate::config::DatabaseReference;
use crate::{
    prompts, CloudApiAction, ConfigOutputFormat, DatabaseAction, DatabaseKeyAction,
    ServiceCredentialAction,
};
use eyre::{eyre, Result};
use serde_json::{json, Map, Value};
use std::collections::HashSet;

pub async fn run_database(action: Option<DatabaseAction>) -> Result<()> {
    match action {
        Some(DatabaseAction::List { project, format }) => {
            let linked = linked_project();
            let project = project
                .or_else(|| linked.as_ref().map(|(project, _)| project.clone()))
                .ok_or_else(|| eyre!("Pass --project or link a project in helix.toml"))?;
            let client = require_auth().await?;
            let workspace = match linked.and_then(|(_, workspace)| workspace) {
                Some(workspace) => workspace,
                None => project_workspace(&client, &project).await?,
            };
            let encoded = urlencoding::encode(&project);
            let encoded_workspace = urlencoding::encode(&workspace);
            let clusters = client
                .get(
                    &format!("/v1/clusters?workspace_id={encoded_workspace}&project_id={encoded}"),
                    "list project clusters",
                )
                .await?;
            let tenants = client
                .get(
                    &format!("/v1/tenants?workspace_id={encoded_workspace}&project_id={encoded}"),
                    "list project tenants",
                )
                .await?;
            let dedicated = clusters
                .get("clusters")
                .and_then(Value::as_array)
                .into_iter()
                .flatten()
                .filter(|cluster| {
                    matches!(
                        cluster.get("access").and_then(Value::as_str),
                        Some("CLUSTER_ACCESS_DEDICATED" | "dedicated")
                    )
                })
                .cloned();
            let tenant_values = tenants
                .get("tenants")
                .and_then(Value::as_array)
                .into_iter()
                .flatten()
                .cloned();
            let databases = dedicated.chain(tenant_values).collect::<Vec<_>>();
            print_collection(
                &json!({"databases": databases}),
                "databases",
                "Databases",
                format,
            )
        }
        Some(DatabaseAction::Get { database, format }) => {
            let database = resolve_database(database)?;
            let value = get_database(&database).await?;
            print_resource(&value, "Database", format)
        }
        Some(DatabaseAction::Create {
            project,
            cluster,
            name,
            slug,
            plan,
            format,
        }) => {
            let project = project
                .or_else(|| linked_project().map(|(project, _)| project))
                .ok_or_else(|| eyre!("Pass --project or link a project in helix.toml"))?;
            if cluster.is_none() && plan.as_deref().is_none_or(str::is_empty) {
                return Err(eyre!("--plan is required for a shared tenant database"));
            }
            if cluster.is_some() && plan.is_some() {
                return Err(eyre!("--plan is not valid for a dedicated-cluster tenant"));
            }
            let response = require_auth()
                .await?
                .post(
                    "/v1/tenants",
                    json!({
                        "projectId": project,
                        "clusterId": cluster.unwrap_or_default(),
                        "name": name,
                        "slug": slug,
                        "planCode": plan.unwrap_or_default(),
                    }),
                    "create tenant database",
                )
                .await?;
            response
                .get("token")
                .and_then(Value::as_str)
                .filter(|token| !token.is_empty())
                .ok_or_else(|| eyre!("database response omitted its one-time application key"))?;
            print_resource(&response, "Database and application key created", format)?;
            eprintln!("This application key is shown once. The CLI did not store it.");
            Ok(())
        }
        Some(DatabaseAction::Delete { database, yes }) => {
            let database = resolve_database(database)?;
            let DatabaseReference::Tenant(id) = database else {
                return Err(eyre!(
                    "dedicated-cluster lifecycle is not supported by the CLI; delete only tenant:<id>"
                ));
            };
            confirm_or_require_yes(yes, &format!("Delete tenant database {id}?"))?;
            require_auth()
                .await?
                .delete(&format!("/v1/tenants/{id}"), "delete tenant database")
                .await?;
            crate::output::success(&format!("Deleted tenant database {id}"));
            Ok(())
        }
        Some(DatabaseAction::Indexes { database, format }) => {
            let database = resolve_database(database)?;
            let path = database_path(&database, "indexes");
            let response = require_auth()
                .await?
                .get(&path, "list database indexes")
                .await?;
            print_resource(&response, "Database indexes", format)
        }
        Some(DatabaseAction::Key { action }) => run_database_key(action).await,
        None => Err(eyre!(
            "Specify 'helix database list|get|create|delete|indexes|key'"
        )),
    }
}

async fn run_database_key(action: DatabaseKeyAction) -> Result<()> {
    match action {
        DatabaseKeyAction::Create {
            database,
            name,
            access,
        } => {
            let database = resolve_database(database)?;
            let response = require_auth()
                .await?
                .post(
                    &database_path(&database, "keys"),
                    json!({
                        "name": name.unwrap_or_default(),
                        "access": access.protobuf_name(),
                    }),
                    "create database key",
                )
                .await?;
            let token = response
                .get("token")
                .and_then(Value::as_str)
                .ok_or_else(|| eyre!("key response omitted its one-time token"))?;
            println!("{token}");
            eprintln!("This application key is shown once. The CLI did not store it.");
            Ok(())
        }
        DatabaseKeyAction::List { database, format } => {
            let database = resolve_database(database)?;
            let response = require_auth()
                .await?
                .get(&database_path(&database, "keys"), "list database keys")
                .await?;
            print_collection(&response, "keys", "Database keys", format)
        }
        DatabaseKeyAction::Revoke { database, key, yes } => {
            let database = resolve_database(database)?;
            confirm_or_require_yes(yes, &format!("Revoke database key {key}?"))?;
            require_auth()
                .await?
                .delete(
                    &format!("{}/{key}", database_path(&database, "keys")),
                    "revoke database key",
                )
                .await?;
            crate::output::success(&format!("Revoked database key {key}"));
            Ok(())
        }
    }
}

pub async fn run_service_credential(action: Option<ServiceCredentialAction>) -> Result<()> {
    match action {
        Some(ServiceCredentialAction::Create {
            workspace,
            name,
            grants,
            expires_at,
        }) => {
            let grants = parse_grants(&grants)?;
            if grants.is_empty() {
                return Err(eyre!("at least one --grant is required"));
            }
            let response = require_auth()
                .await?
                .post(
                    &format!("/v1/workspaces/{workspace}/service-credentials"),
                    json!({
                        "workspaceId": workspace,
                        "name": name,
                        "grants": grants,
                        "expiresAt": expires_at,
                    }),
                    "create service credential",
                )
                .await?;
            let token = response
                .get("token")
                .and_then(Value::as_str)
                .ok_or_else(|| eyre!("credential response omitted its one-time token"))?;
            println!("{token}");
            eprintln!("This service-credential token is shown once. The CLI did not store it.");
            Ok(())
        }
        Some(ServiceCredentialAction::List { workspace, format }) => {
            let response = require_auth()
                .await?
                .get(
                    &format!("/v1/workspaces/{workspace}/service-credentials"),
                    "list service credentials",
                )
                .await?;
            print_collection(&response, "credentials", "Service credentials", format)
        }
        Some(ServiceCredentialAction::Get {
            workspace,
            credential,
            format,
        }) => {
            let response = require_auth()
                .await?
                .get(
                    &format!("/v1/workspaces/{workspace}/service-credentials/{credential}"),
                    "get service credential",
                )
                .await?;
            print_resource(&response, "Service credential", format)
        }
        Some(ServiceCredentialAction::Update {
            workspace,
            credential,
            name,
            grants,
            expires_at,
            clear_expiry,
        }) => {
            if clear_expiry && expires_at.is_some() {
                return Err(eyre!("--clear-expiry conflicts with --expires-at"));
            }
            let replace_grants = !grants.is_empty();
            let grants = parse_grants(&grants)?;
            if name.is_none() && !replace_grants && expires_at.is_none() && !clear_expiry {
                return Err(eyre!("specify at least one field to update"));
            }
            let mut body = Map::new();
            body.insert("workspaceId".into(), Value::String(workspace.clone()));
            body.insert("id".into(), Value::String(credential.clone()));
            body.insert("replaceGrants".into(), Value::Bool(replace_grants));
            if replace_grants {
                body.insert("grants".into(), Value::Array(grants));
            }
            if let Some(name) = name {
                body.insert("name".into(), Value::String(name));
            }
            if expires_at.is_some() || clear_expiry {
                body.insert("replaceExpiry".into(), Value::Bool(true));
                if let Some(expires_at) = expires_at {
                    body.insert("expiresAt".into(), Value::String(expires_at));
                }
            }
            let response = require_auth()
                .await?
                .patch(
                    &format!("/v1/workspaces/{workspace}/service-credentials/{credential}"),
                    Value::Object(body),
                    "update service credential",
                )
                .await?;
            print_resource(
                &response,
                "Service credential updated; its secret was not rotated",
                ConfigOutputFormat::Human,
            )
        }
        Some(ServiceCredentialAction::Revoke {
            workspace,
            credential,
            yes,
        }) => {
            confirm_or_require_yes(yes, &format!("Revoke service credential {credential}?"))?;
            require_auth()
                .await?
                .delete(
                    &format!("/v1/workspaces/{workspace}/service-credentials/{credential}"),
                    "revoke service credential",
                )
                .await?;
            crate::output::success(&format!("Revoked service credential {credential}"));
            Ok(())
        }
        None => Err(eyre!(
            "Specify 'helix service-credential create|list|get|update|revoke'"
        )),
    }
}

pub async fn run_api(action: CloudApiAction) -> Result<()> {
    let client = require_auth().await?;
    let value = match action {
        CloudApiAction::Get { path } => {
            client
                .get(validate_api_path(&path)?, "call Cloud API")
                .await?
        }
        CloudApiAction::Post { path, json } => {
            client
                .post(
                    validate_api_path(&path)?,
                    parse_body(&json)?,
                    "call Cloud API",
                )
                .await?
        }
        CloudApiAction::Patch { path, json } => {
            client
                .patch(
                    validate_api_path(&path)?,
                    parse_body(&json)?,
                    "call Cloud API",
                )
                .await?
        }
        CloudApiAction::Delete { path } => {
            client
                .delete(validate_api_path(&path)?, "call Cloud API")
                .await?
        }
    };
    println!("{}", serde_json::to_string_pretty(&value)?);
    Ok(())
}

fn resolve_database(database: Option<String>) -> Result<DatabaseReference> {
    match database {
        Some(database) => database.parse().map_err(|message: String| eyre!(message)),
        None => linked_database(),
    }
}

async fn get_database(database: &DatabaseReference) -> Result<Value> {
    let path = match database {
        DatabaseReference::Cluster(id) => format!("/v1/clusters/{id}"),
        DatabaseReference::Tenant(id) => format!("/v1/tenants/{id}"),
    };
    require_auth().await?.get(&path, "get database").await
}

fn database_path(database: &DatabaseReference, suffix: &str) -> String {
    match database {
        DatabaseReference::Cluster(id) => format!("/v1/clusters/{id}/{suffix}"),
        DatabaseReference::Tenant(id) => format!("/v1/tenants/{id}/{suffix}"),
    }
}

fn confirm_or_require_yes(yes: bool, question: &str) -> Result<()> {
    if yes {
        return Ok(());
    }
    if !prompts::is_interactive() {
        return Err(eyre!(
            "this operation requires --yes in non-interactive mode"
        ));
    }
    if !prompts::confirm(question)? {
        return Err(eyre!("operation cancelled"));
    }
    Ok(())
}

#[derive(Clone, Copy, Debug, Eq, Hash, PartialEq)]
enum ServiceCredentialPermission {
    ProjectRead,
    ProjectWrite,
    QueryRead,
    QueryWrite,
}

impl ServiceCredentialPermission {
    fn parse(value: &str) -> Result<Self> {
        match value {
            "project-read" => Ok(Self::ProjectRead),
            "project-write" => Ok(Self::ProjectWrite),
            "query-read" => Ok(Self::QueryRead),
            "query-write" => Ok(Self::QueryWrite),
            permission => Err(eyre!(
                "unknown service-credential permission '{permission}'"
            )),
        }
    }

    fn api_name(self) -> &'static str {
        match self {
            Self::ProjectRead => "SERVICE_CREDENTIAL_PERMISSION_PROJECT_READ",
            Self::ProjectWrite => "SERVICE_CREDENTIAL_PERMISSION_PROJECT_WRITE",
            Self::QueryRead => "SERVICE_CREDENTIAL_PERMISSION_DATABASE_QUERY_READ",
            Self::QueryWrite => "SERVICE_CREDENTIAL_PERMISSION_DATABASE_QUERY_WRITE",
        }
    }
}

fn parse_grants(grants: &[String]) -> Result<Vec<Value>> {
    let mut seen_projects = HashSet::with_capacity(grants.len());
    grants
        .iter()
        .map(|grant| {
            let (project, permissions) = grant.split_once('=').ok_or_else(|| {
                eyre!("grant must be PROJECT_ID=project-read,project-write,query-read,query-write")
            })?;
            let project = project.trim();
            if project.is_empty() {
                return Err(eyre!("grant project ID cannot be empty"));
            }
            if !seen_projects.insert(project) {
                return Err(eyre!("duplicate project grant '{project}'"));
            }
            if permissions.trim().is_empty() {
                return Err(eyre!("grant permissions cannot be empty"));
            }
            let permissions = permissions
                .split(',')
                .map(str::trim)
                .map(ServiceCredentialPermission::parse)
                .collect::<Result<Vec<_>>>()?;
            let unique_permissions = permissions.iter().copied().collect::<HashSet<_>>();
            if unique_permissions.len() != permissions.len() {
                return Err(eyre!("grant permissions cannot contain duplicates"));
            }
            if unique_permissions.contains(&ServiceCredentialPermission::ProjectWrite)
                && !unique_permissions.contains(&ServiceCredentialPermission::ProjectRead)
            {
                return Err(eyre!("project-write requires project-read"));
            }
            if unique_permissions.contains(&ServiceCredentialPermission::QueryWrite)
                && !unique_permissions.contains(&ServiceCredentialPermission::QueryRead)
            {
                return Err(eyre!("query-write requires query-read"));
            }
            Ok(json!({
                "projectId": project,
                "permissions": permissions
                    .into_iter()
                    .map(ServiceCredentialPermission::api_name)
                    .collect::<Vec<_>>(),
            }))
        })
        .collect()
}

fn validate_api_path(path: &str) -> Result<&str> {
    if !path.starts_with("/v1/")
        || path.contains("://")
        || path.contains('\n')
        || path.contains('\r')
    {
        return Err(eyre!("Cloud API path must be an absolute /v1/... path"));
    }
    Ok(path)
}

fn parse_body(body: &str) -> Result<Value> {
    serde_json::from_str(body).map_err(|error| eyre!("invalid JSON body: {error}"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn grants_are_project_scoped_and_map_every_permission() {
        let grants =
            parse_grants(&[" project-1 =project-read,project-write,query-read,query-write".into()])
                .unwrap();
        assert_eq!(grants[0]["projectId"], "project-1");
        assert_eq!(
            grants[0]["permissions"],
            json!([
                "SERVICE_CREDENTIAL_PERMISSION_PROJECT_READ",
                "SERVICE_CREDENTIAL_PERMISSION_PROJECT_WRITE",
                "SERVICE_CREDENTIAL_PERMISSION_DATABASE_QUERY_READ",
                "SERVICE_CREDENTIAL_PERMISSION_DATABASE_QUERY_WRITE",
            ])
        );
    }

    #[test]
    fn grants_require_matching_read_permissions() {
        assert!(parse_grants(&["project-1=project-write".into()]).is_err());
        assert!(parse_grants(&["project-1=query-write".into()]).is_err());
        assert!(
            parse_grants(&["project-1=project-read,project-write,query-write".into()]).is_err()
        );
    }

    #[test]
    fn grants_reject_duplicate_and_malformed_values() {
        assert!(parse_grants(&[]).unwrap().is_empty());
        assert!(parse_grants(&[
            "project-1=query-read".into(),
            "project-1=project-read".into(),
        ])
        .is_err());
        assert!(parse_grants(&["project-1=query-read,query-read".into()]).is_err());
        assert!(parse_grants(&["project-1=".into()]).is_err());
        assert!(parse_grants(&["=query-read".into()]).is_err());
        assert!(parse_grants(&["project-1".into()]).is_err());
        assert!(parse_grants(&["project-1=service-credentials-manage".into()]).is_err());
    }

    #[test]
    fn generic_api_rejects_absolute_urls_and_non_v1_paths() {
        assert!(validate_api_path("https://gateway.example/v1/query").is_err());
        assert!(validate_api_path("/v2/query").is_err());
        assert_eq!(
            validate_api_path("/v1/workspaces").unwrap(),
            "/v1/workspaces"
        );
    }
}
