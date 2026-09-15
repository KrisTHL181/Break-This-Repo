mod support;

use assert_cmd::assert::Assert;
use serde_json::Value;
use support::CliFixture;
use wiremock::matchers::{body_json, header, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

fn stdout(assert: Assert) -> String {
    String::from_utf8(assert.get_output().stdout.clone()).unwrap()
}

fn stderr(assert: Assert) -> String {
    String::from_utf8(assert.get_output().stderr.clone()).unwrap()
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn database_and_service_credentials_are_displayed_once_and_never_stored() {
    let server = MockServer::start().await;
    let fixture = CliFixture::new().with_http_base(server.uri());
    fixture.write_credentials("owner@example.com", "session-access");

    Mock::given(method("POST"))
        .and(path("/v1/tenants"))
        .and(header("authorization", "Bearer session-access"))
        .and(body_json(serde_json::json!({
            "projectId":"project-1", "clusterId":"", "name":"App", "slug":"app",
            "planCode":"starter"
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "tenant":{"id":"tenant-1","projectId":"project-1","name":"App"},
            "token":"default-database-secret", "key":{"id":"default-key-1"}
        })))
        .expect(1)
        .mount(&server)
        .await;
    let created = stdout(
        fixture
            .command()
            .args([
                "database",
                "create",
                "--project",
                "project-1",
                "--name",
                "App",
                "--slug",
                "app",
                "--plan",
                "starter",
                "--format",
                "json",
            ])
            .assert()
            .success(),
    );
    let created: Value = serde_json::from_str(&created).unwrap();
    assert_eq!(created["tenant"]["id"], "tenant-1");
    assert_eq!(created["token"], "default-database-secret");

    Mock::given(method("POST"))
        .and(path("/v1/tenants/tenant-1/keys"))
        .and(body_json(serde_json::json!({
            "name":"application", "access":"DATABASE_KEY_ACCESS_READ_WRITE"
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "token":"database-secret", "key":{"id":"key-1"}
        })))
        .expect(1)
        .mount(&server)
        .await;
    let key = stdout(
        fixture
            .command()
            .args([
                "database",
                "key",
                "create",
                "tenant:tenant-1",
                "--name",
                "application",
                "--access",
                "read-write",
            ])
            .assert()
            .success(),
    );
    assert_eq!(key.trim(), "database-secret");

    Mock::given(method("POST"))
        .and(path("/v1/workspaces/ws-1/service-credentials"))
        .and(body_json(serde_json::json!({
            "workspaceId":"ws-1",
            "name":"automation",
            "grants":[{"projectId":"project-1","permissions":["SERVICE_CREDENTIAL_PERMISSION_DATABASE_QUERY_READ"]}],
            "expiresAt":null
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "credential":{"id":"svc-1"}, "token":"service-secret"
        })))
        .expect(1)
        .mount(&server)
        .await;
    let credential = stdout(
        fixture
            .command()
            .args([
                "service-credential",
                "create",
                "--workspace",
                "ws-1",
                "--name",
                "automation",
                "--grant",
                "project-1=query-read",
            ])
            .assert()
            .success(),
    );
    assert_eq!(credential.trim(), "service-secret");
    let stored = std::fs::read_to_string(fixture.helix_home().join("credentials")).unwrap();
    assert!(!stored.contains("default-database-secret"));
    assert!(!stored.contains("database-secret"));
    assert!(!stored.contains("service-secret"));
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn discovery_uses_explicit_workspace_and_project_filters() {
    let server = MockServer::start().await;
    let fixture = CliFixture::new().with_http_base(server.uri());
    fixture.write_credentials("owner@example.com", "session-access");
    Mock::given(method("GET"))
        .and(path("/v1/projects"))
        .and(query_param("workspace_id", "ws-1"))
        .and(header("authorization", "Bearer session-access"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "projects":[{"id":"project-1","displayName":"Graph"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    let projects = stdout(
        fixture
            .command()
            .args([
                "project",
                "list",
                "--workspace-id",
                "ws-1",
                "--format",
                "json",
            ])
            .assert()
            .success(),
    );
    assert_eq!(
        serde_json::from_str::<Value>(&projects).unwrap()["projects"][0]["id"],
        "project-1"
    );
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn workspace_project_cluster_and_auth_commands_cover_crud_contracts() {
    let server = MockServer::start().await;
    let fixture = CliFixture::new().with_http_base(server.uri());
    fixture.write_credentials("owner@example.com", "session-access");

    Mock::given(method("GET"))
        .and(path("/v1/whoami"))
        .and(header("authorization", "Bearer session-access"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "workspaces":[{"id":"ws-1"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    let status = stdout(
        fixture
            .command()
            .args(["auth", "status"])
            .assert()
            .success(),
    );
    assert!(status.contains("owner@example.com"));
    assert!(status.contains("Workspace memberships: 1"));

    Mock::given(method("GET"))
        .and(path("/v1/workspaces"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "workspaces":[{"id":"ws-1","displayName":"Acme"}]
        })))
        .expect(2)
        .mount(&server)
        .await;
    let workspaces = stdout(
        fixture
            .command()
            .args(["workspace", "list"])
            .assert()
            .success(),
    );
    assert!(workspaces.contains("Acme (ws-1)"));
    fixture
        .command()
        .args(["config", "workspace", "list", "--format", "json"])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/workspaces/ws-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"ws-1","displayName":"Acme"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["workspace", "get", "ws-1", "--format", "json"])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/projects/project-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"project-1","workspaceId":"ws-1","displayName":"Graph"
        })))
        .expect(2)
        .mount(&server)
        .await;
    let project = stdout(
        fixture
            .command()
            .args(["project", "get", "project-1"])
            .assert()
            .success(),
    );
    assert!(project.contains("Project"));

    Mock::given(method("POST"))
        .and(path("/v1/projects"))
        .and(body_json(serde_json::json!({
            "workspaceId":"ws-1","slug":"graph","displayName":"Graph"
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"project-1","workspaceId":"ws-1","displayName":"Graph"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "project",
            "create",
            "--workspace",
            "ws-1",
            "--slug",
            "graph",
            "--name",
            "Graph",
            "--format",
            "json",
        ])
        .assert()
        .success();

    Mock::given(method("DELETE"))
        .and(path("/v1/projects/project-1"))
        .respond_with(ResponseTemplate::new(204))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["project", "delete", "project-1", "--yes"])
        .assert()
        .success();

    let project_dir = fixture.root().join("linked-project");
    fixture
        .command()
        .args(["init", "--path"])
        .arg(&project_dir)
        .args(["local", "--no-skills"])
        .assert()
        .success();
    fixture
        .command()
        .current_dir(&project_dir)
        .args(["project", "link", "project-1", "--workspace", "ws-1"])
        .assert()
        .success();
    let linked = std::fs::read_to_string(project_dir.join("helix.toml")).unwrap();
    assert!(linked.contains("project-1"));
    assert!(linked.contains("ws-1"));

    Mock::given(method("GET"))
        .and(path("/v1/clusters"))
        .and(query_param("workspace_id", "ws-1"))
        .and(query_param("project_id", "project-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "clusters":[{"id":"cluster-1","displayName":"Primary"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    let clusters = stdout(
        fixture
            .command()
            .args([
                "cluster",
                "list",
                "--workspace-id",
                "ws-1",
                "--project-id",
                "project-1",
            ])
            .assert()
            .success(),
    );
    assert!(clusters.contains("Primary (cluster-1)"));

    Mock::given(method("GET"))
        .and(path("/v1/clusters/cluster-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"cluster-1","displayName":"Primary"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["cluster", "get", "cluster-1", "--format", "json"])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/clusters/cluster-1/indexes"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "indexes":[{"name":"by_email"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "cluster",
            "indexes",
            "--cluster-id",
            "cluster-1",
            "--format",
            "json",
        ])
        .assert()
        .success();

    Mock::given(method("POST"))
        .and(path("/v1/auth/logout"))
        .respond_with(ResponseTemplate::new(204))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["auth", "logout"])
        .assert()
        .success();
    assert!(!fixture.helix_home().join("credentials").exists());
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn database_commands_cover_discovery_lifecycle_indexes_and_keys() {
    let server = MockServer::start().await;
    let fixture = CliFixture::new().with_http_base(server.uri());
    fixture.write_credentials("owner@example.com", "session-access");

    Mock::given(method("GET"))
        .and(path("/v1/projects/project-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"project-1","workspaceId":"ws-1"
        })))
        .expect(1)
        .mount(&server)
        .await;
    Mock::given(method("GET"))
        .and(path("/v1/clusters"))
        .and(query_param("workspace_id", "ws-1"))
        .and(query_param("project_id", "project-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "clusters":[
                {"id":"cluster-1","access":"dedicated"},
                {"id":"shared-1","access":"shared"}
            ]
        })))
        .expect(1)
        .mount(&server)
        .await;
    Mock::given(method("GET"))
        .and(path("/v1/tenants"))
        .and(query_param("workspace_id", "ws-1"))
        .and(query_param("project_id", "project-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "tenants":[{"id":"tenant-1","displayName":"App"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    let databases = stdout(
        fixture
            .command()
            .args([
                "database",
                "list",
                "--project",
                "project-1",
                "--format",
                "json",
            ])
            .assert()
            .success(),
    );
    let databases: Value = serde_json::from_str(&databases).unwrap();
    assert_eq!(databases["databases"].as_array().unwrap().len(), 2);

    Mock::given(method("GET"))
        .and(path("/v1/tenants/tenant-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"tenant-1","displayName":"App"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["database", "get", "tenant:tenant-1", "--format", "json"])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/clusters/cluster-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"cluster-1","access":"dedicated"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["database", "get", "cluster:cluster-1", "--format", "json"])
        .assert()
        .success();

    for endpoint in [
        "/v1/tenants/tenant-1/indexes",
        "/v1/clusters/cluster-1/indexes",
    ] {
        Mock::given(method("GET"))
            .and(path(endpoint))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "indexes":[{"name":"by_email"}]
            })))
            .expect(1)
            .mount(&server)
            .await;
    }
    fixture
        .command()
        .args(["database", "indexes", "tenant:tenant-1", "--format", "json"])
        .assert()
        .success();
    fixture
        .command()
        .args([
            "database",
            "indexes",
            "cluster:cluster-1",
            "--format",
            "json",
        ])
        .assert()
        .success();

    Mock::given(method("POST"))
        .and(path("/v1/tenants"))
        .and(body_json(serde_json::json!({
            "projectId":"project-1","clusterId":"cluster-1","name":"Dedicated",
            "slug":"dedicated","planCode":""
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "tenant":{"id":"tenant-2"},"token":"dedicated-secret"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "database",
            "create",
            "--project",
            "project-1",
            "--cluster",
            "cluster-1",
            "--name",
            "Dedicated",
            "--slug",
            "dedicated",
            "--format",
            "json",
        ])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/tenants/tenant-1/keys"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "keys":[{"id":"key-1","name":"application"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    let keys = stdout(
        fixture
            .command()
            .args(["database", "key", "list", "tenant:tenant-1"])
            .assert()
            .success(),
    );
    assert!(keys.contains("application (key-1)"));

    Mock::given(method("DELETE"))
        .and(path("/v1/tenants/tenant-1/keys/key-1"))
        .respond_with(ResponseTemplate::new(204))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "database",
            "key",
            "revoke",
            "tenant:tenant-1",
            "--key",
            "key-1",
            "--yes",
        ])
        .assert()
        .success();

    Mock::given(method("DELETE"))
        .and(path("/v1/tenants/tenant-1"))
        .respond_with(ResponseTemplate::new(204))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["database", "delete", "tenant:tenant-1", "--yes"])
        .assert()
        .success();
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn service_credential_and_generic_api_commands_cover_all_methods() {
    let server = MockServer::start().await;
    let fixture = CliFixture::new().with_http_base(server.uri());
    fixture.write_credentials("owner@example.com", "session-access");

    Mock::given(method("GET"))
        .and(path("/v1/workspaces/ws-1/service-credentials"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "credentials":[{"id":"svc-1","name":"automation"}]
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "service-credential",
            "list",
            "--workspace",
            "ws-1",
            "--format",
            "json",
        ])
        .assert()
        .success();

    Mock::given(method("GET"))
        .and(path("/v1/workspaces/ws-1/service-credentials/svc-1"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"svc-1","name":"automation"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args(["service-credential", "get", "--workspace", "ws-1", "svc-1"])
        .assert()
        .success();

    Mock::given(method("PATCH"))
        .and(path("/v1/workspaces/ws-1/service-credentials/svc-1"))
        .and(body_json(serde_json::json!({
            "workspaceId":"ws-1",
            "id":"svc-1",
            "replaceGrants":true,
            "grants":[{"projectId":"project-1","permissions":[
                "SERVICE_CREDENTIAL_PERMISSION_PROJECT_READ",
                "SERVICE_CREDENTIAL_PERMISSION_PROJECT_WRITE"
            ]}],
            "name":"renamed",
            "replaceExpiry":true,
            "expiresAt":"2030-01-01T00:00:00Z"
        })))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id":"svc-1","name":"renamed"
        })))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "service-credential",
            "update",
            "--workspace",
            "ws-1",
            "svc-1",
            "--name",
            "renamed",
            "--grant",
            "project-1=project-read,project-write",
            "--expires-at",
            "2030-01-01T00:00:00Z",
        ])
        .assert()
        .success();

    Mock::given(method("DELETE"))
        .and(path("/v1/workspaces/ws-1/service-credentials/svc-1"))
        .respond_with(ResponseTemplate::new(204))
        .expect(1)
        .mount(&server)
        .await;
    fixture
        .command()
        .args([
            "service-credential",
            "revoke",
            "--workspace",
            "ws-1",
            "svc-1",
            "--yes",
        ])
        .assert()
        .success();

    for (verb, endpoint) in [
        ("GET", "/v1/test-get"),
        ("POST", "/v1/test-post"),
        ("PATCH", "/v1/test-patch"),
        ("DELETE", "/v1/test-delete"),
    ] {
        Mock::given(method(verb))
            .and(path(endpoint))
            .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
                "method":verb
            })))
            .expect(1)
            .mount(&server)
            .await;
    }
    fixture
        .command()
        .args(["api", "get", "/v1/test-get"])
        .assert()
        .success();
    fixture
        .command()
        .args(["api", "post", "/v1/test-post", "--json", r#"{"value":1}"#])
        .assert()
        .success();
    fixture
        .command()
        .args(["api", "patch", "/v1/test-patch", "--json", r#"{"value":2}"#])
        .assert()
        .success();
    fixture
        .command()
        .args(["api", "delete", "/v1/test-delete"])
        .assert()
        .success();
}

#[test]
fn cloud_command_validation_rejects_unsafe_or_incomplete_requests_before_dispatch() {
    let fixture = CliFixture::new();
    for args in [
        vec![
            "database",
            "create",
            "--project",
            "project-1",
            "--name",
            "App",
            "--slug",
            "app",
        ],
        vec![
            "database",
            "create",
            "--project",
            "project-1",
            "--cluster",
            "cluster-1",
            "--plan",
            "starter",
            "--name",
            "App",
            "--slug",
            "app",
        ],
        vec!["database", "delete", "cluster:cluster-1", "--yes"],
        vec![
            "database",
            "key",
            "revoke",
            "tenant:tenant-1",
            "--key",
            "key-1",
        ],
        vec![
            "service-credential",
            "create",
            "--workspace",
            "ws-1",
            "--name",
            "svc",
        ],
        vec![
            "service-credential",
            "update",
            "--workspace",
            "ws-1",
            "svc-1",
        ],
        vec![
            "service-credential",
            "update",
            "--workspace",
            "ws-1",
            "svc-1",
            "--expires-at",
            "2030-01-01T00:00:00Z",
            "--clear-expiry",
        ],
        vec!["api", "get", "https://example.com/v1/projects"],
        vec!["api", "get", "/v2/query"],
        vec!["api", "post", "/v1/projects", "--json", "not-json"],
    ] {
        fixture.command().args(args).assert().failure();
    }

    let missing_project = stderr(
        fixture
            .command()
            .args(["database", "list"])
            .assert()
            .failure(),
    );
    assert!(missing_project.contains("Pass --project"));
}
