use crate::{
    cloud::{CloudClient, SessionCredentials},
    metrics_sender::{load_metrics_config, save_metrics_config},
    output, prompts, AuthAction,
};
use color_eyre::owo_colors::OwoColorize as _;
use eyre::{eyre, Result, WrapErr as _};
use serde::Deserialize;
use serde_json::json;
use std::io::{self, Write as _};
use tokio::{
    io::{AsyncReadExt as _, AsyncWriteExt as _},
    net::TcpListener,
    time::{timeout, Duration},
};

const LOGIN_TIMEOUT: Duration = Duration::from_secs(5 * 60);
const LOGIN_CALLBACK_ADDRESS: &str = "127.0.0.1:8765";
const LOGIN_CALLBACK_URI: &str = "http://localhost:8765/callback";

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct StartLoginResponse {
    url: String,
    session_id: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct LoginResponse {
    access_token: String,
    refresh_token: String,
    #[serde(deserialize_with = "crate::cloud::deserialize_i64")]
    expires_at: i64,
    email: String,
    #[serde(default)]
    email_verification_required: bool,
}

pub async fn run(action: AuthAction) -> Result<()> {
    match action {
        AuthAction::Login => login().await,
        AuthAction::Status => status().await,
        AuthAction::Logout => logout().await,
    }
}

pub async fn login() -> Result<()> {
    if !prompts::is_interactive() {
        return Err(eyre!("WorkOS login requires an interactive terminal"));
    }
    output::info("Logging into Helix Cloud with WorkOS");
    let client = CloudClient::new()?;
    let listener = TcpListener::bind(LOGIN_CALLBACK_ADDRESS)
        .await
        .wrap_err_with(|| {
            format!("bind the WorkOS callback listener at {LOGIN_CALLBACK_ADDRESS}")
        })?;
    let started: StartLoginResponse = serde_json::from_value(
        client
            .public_post(
                "/v1/auth/login:start",
                json!({"redirectUri": LOGIN_CALLBACK_URI, "provider": "AUTH_PROVIDER_GITHUB"}),
                "start WorkOS login",
            )
            .await?,
    )?;

    open::that(&started.url).wrap_err("open WorkOS login in browser")?;
    println!(
        "Open this URL if the browser did not start:\n{}",
        started.url.bold()
    );
    let (code, state) = timeout(LOGIN_TIMEOUT, receive_callback(listener))
        .await
        .map_err(|_| eyre!("WorkOS login timed out"))??;
    let exchanged: LoginResponse = serde_json::from_value(
        client
            .public_post(
                "/v1/auth/exchange",
                json!({"sessionId": started.session_id, "code": code, "state": state}),
                "exchange WorkOS authorization code",
            )
            .await?,
    )?;
    let session = if exchanged.email_verification_required {
        print!("Enter the verification code sent to {}: ", exchanged.email);
        io::stdout().flush()?;
        let mut code = String::new();
        io::stdin().read_line(&mut code)?;
        serde_json::from_value::<LoginResponse>(
            client
                .public_post(
                    "/v1/auth/verify-email",
                    json!({"sessionId": started.session_id, "code": code.trim()}),
                    "verify WorkOS email",
                )
                .await?,
        )?
    } else {
        exchanged
    };
    let credentials = SessionCredentials {
        access_token: session.access_token,
        refresh_token: session.refresh_token,
        expires_at: session.expires_at,
        email: session.email,
    };
    client.store_session(&credentials)?;

    let mut metrics = load_metrics_config()?;
    metrics.user_id = None;
    save_metrics_config(&metrics)?;
    output::success("Logged in successfully");
    output::info(&format!(
        "WorkOS session stored at {}",
        client.credentials_path().display()
    ));
    Ok(())
}

async fn receive_callback(listener: TcpListener) -> Result<(String, String)> {
    let (mut stream, _) = listener.accept().await?;
    let mut request = vec![0_u8; 8192];
    let read = stream.read(&mut request).await?;
    let first_line = std::str::from_utf8(&request[..read])?
        .lines()
        .next()
        .ok_or_else(|| eyre!("browser callback was empty"))?;
    let target = first_line
        .split_whitespace()
        .nth(1)
        .ok_or_else(|| eyre!("browser callback was malformed"))?;
    let url = reqwest::Url::parse(&format!("http://127.0.0.1{target}"))?;
    let code = url
        .query_pairs()
        .find_map(|(key, value)| (key == "code").then(|| value.into_owned()));
    let state = url
        .query_pairs()
        .find_map(|(key, value)| (key == "state").then(|| value.into_owned()));
    let result = match (code, state) {
        (Some(code), Some(state)) if !code.is_empty() && !state.is_empty() => Ok((code, state)),
        _ => Err(eyre!("WorkOS callback did not contain code and state")),
    };
    let (status, body) = if result.is_ok() {
        (
            "200 OK",
            "Helix CLI login complete. You can close this window.",
        )
    } else {
        (
            "400 Bad Request",
            "Helix CLI login failed. Return to the terminal.",
        )
    };
    stream
        .write_all(
            format!(
                "HTTP/1.1 {status}\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
                body.len()
            )
            .as_bytes(),
        )
        .await?;
    result
}

async fn status() -> Result<()> {
    let client = CloudClient::new()?;
    let response = client.get("/v1/whoami", "load WorkOS session").await?;
    let session = client.load_session()?;
    output::success("Authenticated with WorkOS");
    println!("Email: {}", session.email);
    let memberships = response
        .get("workspaces")
        .and_then(serde_json::Value::as_array)
        .map_or(0, Vec::len);
    println!("Workspace memberships: {memberships}");
    Ok(())
}

async fn logout() -> Result<()> {
    output::info("Logging out of Helix Cloud");
    let client = CloudClient::new()?;
    if client.load_session().is_ok()
        && let Err(error) = client
            .post("/v1/auth/logout", json!({}), "revoke WorkOS session")
            .await
    {
        output::warning(&format!(
            "Could not revoke the remote WorkOS session: {error}"
        ));
    }
    client.remove_session()?;
    output::success("Logged out successfully");
    Ok(())
}

pub async fn require_auth() -> Result<CloudClient> {
    let client = CloudClient::new()?;
    client.load_session().map_err(|error| {
        eyre!("{error}. Authentication required. Run 'helix auth login' first.")
    })?;
    Ok(client)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn callback_requires_code_and_state() {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let callback = tokio::spawn(receive_callback(listener));
        let mut stream = tokio::net::TcpStream::connect(address).await.unwrap();
        stream
            .write_all(b"GET /callback?code=abc&state=xyz HTTP/1.1\r\nHost: localhost\r\n\r\n")
            .await
            .unwrap();
        assert_eq!(
            callback.await.unwrap().unwrap(),
            ("abc".into(), "xyz".into())
        );
    }
}
