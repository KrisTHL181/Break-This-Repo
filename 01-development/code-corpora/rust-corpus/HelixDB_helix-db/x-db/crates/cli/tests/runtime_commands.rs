mod support;

use assert_cmd::assert::Assert;
use support::CliFixture;
use wiremock::matchers::{method, path};
use wiremock::{Mock, MockServer, ResponseTemplate};

fn stdout(assert: Assert) -> String {
    String::from_utf8(assert.get_output().stdout.clone()).expect("stdout should be utf8")
}

fn stderr(assert: Assert) -> String {
    String::from_utf8(assert.get_output().stderr.clone()).expect("stderr should be utf8")
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn disk_runtime_commands_cover_resource_reuse_status_cleanup_and_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/healthz"))
        .respond_with(ResponseTemplate::new(200))
        .expect(2)
        .mount(&server)
        .await;

    let fixture = CliFixture::new_with_fake_runtime();
    let project = fixture.root().join("disk-command-project");
    fixture
        .command()
        .args(["init", "--path"])
        .arg(&project)
        .args(["local", "--port"])
        .arg(server.address().port().to_string())
        .args(["--disk", "--no-skills"])
        .assert()
        .success();

    fixture
        .command()
        .current_dir(&project)
        .args(["start", "dev"])
        .assert()
        .success();

    let container = "helix-disk-command-project-dev";
    let ps_output = format!(
        "{container}\tUp 1 minute\tlocalhost:{}",
        server.address().port()
    );
    let status = stdout(
        fixture
            .command()
            .current_dir(&project)
            .args(["status", "dev"])
            .env("HELIX_TEST_RUNTIME_PS_OUTPUT", &ps_output)
            .assert()
            .success(),
    );
    assert!(status.contains("Up 1 minute"));
    assert!(status.contains("storage: disk"));
    assert!(status.contains(&server.address().port().to_string()));

    fixture
        .command()
        .current_dir(&project)
        .args(["logs", "dev", "--follow"])
        .assert()
        .success();
    fixture
        .command()
        .current_dir(&project)
        .args(["restart", "dev"])
        .env("HELIX_TEST_RUNTIME_RESOURCES_EXIST", "1")
        .assert()
        .success();

    let stopped = stdout(
        fixture
            .command()
            .current_dir(&project)
            .args(["stop", "dev"])
            .env("HELIX_TEST_RUNTIME_RESOURCES_EXIST", "1")
            .assert()
            .success(),
    );
    assert!(stopped.contains("Stopped 'dev' successfully"));
    fixture
        .command()
        .current_dir(&project)
        .args(["prune", "--all", "--yes"])
        .env("HELIX_TEST_RUNTIME_RESOURCES_EXIST", "1")
        .assert()
        .success();

    let status_error = stderr(
        fixture
            .command()
            .current_dir(&project)
            .args(["status", "dev"])
            .env("HELIX_TEST_RUNTIME_FAIL_COMMAND", "ps")
            .assert()
            .failure(),
    );
    assert!(status_error.contains("simulated runtime failure"));

    let log = fixture.runtime_log();
    assert!(log.contains("network create"));
    assert!(log.contains("volume create"));
    assert!(log.contains("minio/mc:latest"));
    assert!(log.contains("logs -f"));
    assert!(log.contains("network inspect"));
}

#[test]
fn logs_and_status_report_a_missing_runtime_like_stop_does() {
    let fixture = CliFixture::new().with_missing_runtime();
    let project = fixture.root().join("missing-runtime-project");
    fixture
        .command()
        .args(["init", "--path"])
        .arg(&project)
        .args(["local", "--no-skills"])
        .assert()
        .success();

    for command in [["logs", "dev"], ["status", "dev"], ["stop", "dev"]] {
        let message = stderr(
            fixture
                .command()
                .current_dir(&project)
                .args(command)
                .assert()
                .failure(),
        );
        assert!(
            message.contains("Docker is not installed"),
            "`helix {}` should name the missing runtime, got: {message}",
            command.join(" ")
        );
        // The number is platform-specific: ENOENT is 2, while Windows reports 3
        // (ERROR_PATH_NOT_FOUND) when the parent directory is absent too. What
        // matters is that the originating error survives as the cause.
        assert!(
            message.contains("os error"),
            "`helix {}` should keep the underlying cause, got: {message}",
            command.join(" ")
        );
    }
}

/// A runtime that is present but refuses to launch is a different failure from
/// one that is not installed, and must not be reported as a missing install.
#[cfg(unix)]
#[test]
fn a_present_but_unspawnable_runtime_keeps_its_command_error() {
    let fixture = CliFixture::new().with_unspawnable_runtime();
    let project = fixture.root().join("unspawnable-runtime-project");
    fixture
        .command()
        .args(["init", "--path"])
        .arg(&project)
        .args(["local", "--no-skills"])
        .assert()
        .success();

    for command in [["logs", "dev"], ["status", "dev"]] {
        let message = stderr(
            fixture
                .command()
                .current_dir(&project)
                .args(command)
                .assert()
                .failure(),
        );
        assert!(
            !message.contains("is not installed"),
            "`helix {}` should not blame the install for a permission failure, got: {message}",
            command.join(" ")
        );
    }
}
