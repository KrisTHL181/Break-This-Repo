//! Request-time authority for MCP transports and every OAuth HTTP operation.
//!
//! Direct public endpoints use validated DNS pins even when configured by an
//! operator. Explicit local endpoints/private-network opt-ins and selected
//! operator proxy routes carry authority only on their exact configured origin.
//! Model-added endpoints and server-selected secondary origins stay public.

use std::collections::HashMap;
use std::net::{IpAddr, SocketAddr};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use anyhow::{Context, Result, bail};
use reqwest::{Method, Request, Response, Url, header};

use crate::network_policy::{Decision, NetworkPolicyDecider};
use crate::tools::web::guard::{guarded_reqwest_client_builder, is_restricted_ip};

#[derive(Clone)]
pub(super) struct McpHttpClient {
    origin: String,
    operator_configured: bool,
    private_origin_allowed: bool,
    #[cfg(test)]
    dns_answers: Arc<Mutex<Option<std::collections::VecDeque<Vec<SocketAddr>>>>>,
    reviewed_plugin: bool,
    network_policy: Option<NetworkPolicyDecider>,
    connect_timeout: Duration,
    read_timeout: Duration,
    default_headers: header::HeaderMap,
    request_builder: reqwest::Client,
    clients: Arc<Mutex<HashMap<String, reqwest::Client>>>,
}

impl McpHttpClient {
    pub(super) fn new(
        url: &str,
        runtime_added: bool,
        reviewed_plugin: bool,
        allow_private_network: bool,
        network_policy: Option<&NetworkPolicyDecider>,
        connect_timeout: Duration,
        read_timeout: Duration,
    ) -> Result<Self> {
        let url = Url::parse(url).context("invalid MCP HTTP endpoint")?;
        validate_url(&url)?;
        validate_network_policy(&url, network_policy)?;
        if (runtime_added || reviewed_plugin) && url_has_credentials(&url) {
            bail!("MCP HTTP URL must not contain credentials; use configured headers");
        }
        Ok(Self {
            origin: url.origin().ascii_serialization(),
            operator_configured: !runtime_added,
            private_origin_allowed: !runtime_added
                && (allow_private_network || explicit_local_target(&url)),
            #[cfg(test)]
            dns_answers: Arc::new(Mutex::new(None)),
            reviewed_plugin,
            network_policy: network_policy.cloned(),
            connect_timeout,
            read_timeout,
            default_headers: header::HeaderMap::new(),
            request_builder: guarded_reqwest_client_builder().build()?,
            clients: Arc::new(Mutex::new(HashMap::new())),
        })
    }

    pub(super) fn with_default_headers(mut self, headers: header::HeaderMap) -> Self {
        self.default_headers = headers;
        self
    }

    pub(super) fn get(&self, url: &str) -> reqwest::RequestBuilder {
        self.request_builder.get(url)
    }

    pub(super) fn post(&self, url: &str) -> reqwest::RequestBuilder {
        self.request_builder.post(url)
    }

    pub(super) async fn send(&self, request: reqwest::RequestBuilder) -> Result<Response> {
        self.execute(request.build()?, true).await
    }

    pub(super) async fn execute(
        &self,
        mut request: Request,
        follow_redirects: bool,
    ) -> Result<Response> {
        if request.url().origin().ascii_serialization() == self.origin {
            for (name, value) in &self.default_headers {
                if !request.headers().contains_key(name) {
                    request.headers_mut().insert(name.clone(), value.clone());
                }
            }
        }
        let timeout = request.timeout().copied().unwrap_or(self.read_timeout);
        tokio::time::timeout(timeout, self.execute_inner(request, follow_redirects))
            .await
            .context("MCP HTTP request timed out")?
    }

    async fn execute_inner(
        &self,
        mut request: Request,
        follow_redirects: bool,
    ) -> Result<Response> {
        for redirect_count in 0..=5 {
            let url = request.url().clone();
            let client = self.client_for_target(&url).await?;
            // MCP and OAuth requests have buffered bodies. Keep the exact request
            // to replay only after the Location has passed the same guard.
            let next_request = request
                .try_clone()
                .context("MCP request body cannot be replayed")?;
            let response = client.execute(request).await?;
            if !follow_redirects
                || !matches!(response.status().as_u16(), 301 | 302 | 303 | 307 | 308)
            {
                return Ok(response);
            }
            let Some(location) = response.headers().get(header::LOCATION) else {
                return Ok(response);
            };
            if redirect_count == 5 {
                bail!("MCP HTTP redirect limit exceeded");
            }
            let next_url = url.join(location.to_str().context("invalid MCP redirect Location")?)?;
            validate_url(&next_url)?;
            if url_has_credentials(&next_url) {
                bail!("MCP HTTP redirect must not contain credentials");
            }
            if url.scheme() == "https" && next_url.scheme() != "https" {
                bail!("MCP HTTP redirect would downgrade HTTPS");
            }
            request = next_request;
            if (matches!(response.status().as_u16(), 301 | 302) && request.method() == Method::POST)
                || (response.status().as_u16() == 303 && request.method() != Method::HEAD)
            {
                *request.method_mut() = Method::GET;
                *request.body_mut() = None;
                request.headers_mut().remove(header::CONTENT_TYPE);
                request.headers_mut().remove(header::CONTENT_LENGTH);
                request.headers_mut().remove(header::TRANSFER_ENCODING);
            }
            if next_url.origin() != url.origin() {
                // Custom headers can contain credentials under arbitrary names;
                // retaining just Authorization/ Cookie exclusions is insufficient.
                let mut headers = header::HeaderMap::new();
                for name in [header::ACCEPT, header::CONTENT_TYPE] {
                    if let Some(value) = request.headers().get(&name) {
                        headers.insert(name, value.clone());
                    }
                }
                *request.headers_mut() = headers;
            }
            *request.url_mut() = next_url;
        }
        unreachable!("redirect loop is bounded")
    }

    async fn client_for_target(&self, url: &Url) -> Result<reqwest::Client> {
        validate_url(url)?;
        let same_origin = url.origin().ascii_serialization() == self.origin;
        if self.reviewed_plugin && !super::reviewed_redirect_matches_origin(url, &self.origin) {
            bail!("MCP redirect leaves the reviewed plugin origin");
        }
        validate_network_policy(url, self.network_policy.as_ref())?;
        let operator_origin = self.operator_configured && same_origin;
        if !operator_origin && url_has_credentials(url) {
            bail!("MCP HTTP discovered URL must not contain credentials");
        }
        let proxy =
            super::configured_mcp_proxy(url, !operator_origin || self.reviewed_plugin, |key| {
                std::env::var(key)
            })?;
        // A selected operator proxy resolves its own destinations. This is
        // delegated proxy authority, never evidence of a local DNS pin.
        let pin = if (self.private_origin_allowed && same_origin) || proxy.is_some() {
            None
        } else {
            self.public_dns_pin(url).await?
        };
        // Validate DNS before reusing a client too: a new private answer revokes
        // this request. Each cached client itself remains pinned to its old public
        // address, including reconnects after a keep-alive socket expires.
        let key = format!("{}:{pin:?}", url.origin().ascii_serialization());
        if proxy.is_none()
            && let Some(client) = self
                .clients
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner)
                .get(&key)
        {
            return Ok(client.clone());
        }
        let mut builder = guarded_reqwest_client_builder()
            .redirect(reqwest::redirect::Policy::none())
            .connect_timeout(self.connect_timeout)
            .timeout(self.read_timeout);
        let proxied = proxy.is_some();
        if let Some(proxy) = proxy {
            builder = builder.proxy(proxy);
        } else if let Some((host, address)) = pin {
            builder = builder.resolve(&host, address);
        }
        let client = builder
            .build()
            .context("building guarded MCP HTTP client")?;
        let mut clients = self
            .clients
            .lock()
            .unwrap_or_else(std::sync::PoisonError::into_inner);
        if !proxied && clients.len() < 32 {
            clients.insert(key, client.clone());
        }
        Ok(client)
    }
}

fn validate_network_policy(url: &Url, network_policy: Option<&NetworkPolicyDecider>) -> Result<()> {
    let host = url.host_str().context("MCP URL has no host")?;
    if let Some(policy) = network_policy {
        match policy.evaluate(host, "mcp") {
            Decision::Allow => {}
            Decision::Deny => bail!("MCP HTTP destination blocked by network policy"),
            Decision::Prompt => bail!("MCP HTTP destination requires network approval"),
        }
    }
    Ok(())
}

fn validate_url(url: &Url) -> Result<()> {
    if !matches!(url.scheme(), "http" | "https") || url.host_str().is_none() {
        bail!("MCP HTTP requires an http:// or https:// URL with a host");
    }
    Ok(())
}

fn url_has_credentials(url: &Url) -> bool {
    !url.username().is_empty() || url.password().is_some()
}

impl McpHttpClient {
    async fn public_dns_pin(&self, url: &Url) -> Result<Option<(String, SocketAddr)>> {
        let host = url.host_str().context("MCP URL has no host")?;
        let literal = host.trim_start_matches('[').trim_end_matches(']');
        if let Ok(ip) = literal.parse::<IpAddr>() {
            if is_restricted_ip(&ip) {
                bail!("MCP HTTP destination is a restricted IP address");
            }
            return Ok(None);
        }
        let port = url.port_or_known_default().context("MCP URL has no port")?;
        #[cfg(test)]
        let injected = self
            .dns_answers
            .lock()
            .unwrap()
            .as_mut()
            .map(|answers| answers.pop_front().expect("DNS fixture answer available"));
        #[cfg(not(test))]
        let injected: Option<Vec<SocketAddr>> = None;
        let addresses: Vec<_> = if let Some(addresses) = injected {
            addresses
        } else {
            tokio::time::timeout(self.connect_timeout, tokio::net::lookup_host((host, port)))
                .await
                .context("MCP HTTP DNS resolution timed out")?
                .context("MCP HTTP DNS resolution failed")?
                .collect()
        };
        let address = validated_public_address(&addresses)?;
        Ok(Some((host.to_string(), address)))
    }
}

fn explicit_local_target(url: &Url) -> bool {
    let Some(host) = url.host_str() else {
        return false;
    };
    let host = host.trim_end_matches('.');
    host.eq_ignore_ascii_case("localhost")
        || host.to_ascii_lowercase().ends_with(".localhost")
        || host
            .trim_start_matches('[')
            .trim_end_matches(']')
            .parse::<IpAddr>()
            .is_ok_and(|ip| is_restricted_ip(&ip))
}

fn validated_public_address(addresses: &[SocketAddr]) -> Result<SocketAddr> {
    if addresses
        .iter()
        .any(|address| is_restricted_ip(&address.ip()))
    {
        bail!("MCP HTTP DNS resolved to a restricted IP address");
    }
    addresses
        .first()
        .copied()
        .context("MCP HTTP DNS resolved to no addresses")
}

#[cfg(test)]
mod tests {
    use super::*;
    use tokio::io::{AsyncReadExt, AsyncWriteExt};
    use tokio::net::TcpListener;

    fn client(url: &str, runtime_added: bool) -> McpHttpClient {
        McpHttpClient::new(
            url,
            runtime_added,
            false,
            false,
            None,
            Duration::from_secs(1),
            Duration::from_secs(2),
        )
        .unwrap()
    }

    async fn reply_once(listener: TcpListener, response: String) -> String {
        let (mut socket, _) = listener.accept().await.unwrap();
        let mut bytes = Vec::new();
        let mut buffer = [0u8; 2048];
        loop {
            let n = socket.read(&mut buffer).await.unwrap();
            assert!(n > 0);
            bytes.extend_from_slice(&buffer[..n]);
            if bytes.windows(4).any(|part| part == b"\r\n\r\n") {
                break;
            }
        }
        socket.write_all(response.as_bytes()).await.unwrap();
        String::from_utf8(bytes).unwrap()
    }

    #[tokio::test]
    async fn model_added_http_rejects_private_literals_and_local_dns_before_connecting() {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let port = listener.local_addr().unwrap().port();
        for host in [
            "127.0.0.1",
            "127.1",
            "2130706433",
            "0x7f000001",
            "localhost",
            "[::1]",
            "[::ffff:127.0.0.1]",
            "169.254.169.254",
            "10.0.0.1",
        ] {
            let url = format!("http://{host}:{port}/mcp");
            let client = client(&url, true);
            for method in [Method::GET, Method::POST] {
                let request = client.request_builder.request(method, &url);
                let error = client.send(request).await.unwrap_err();
                assert!(
                    format!("{error:#}").contains("restricted"),
                    "{host}: {error:#}"
                );
            }
        }
        assert!(
            tokio::time::timeout(Duration::from_millis(30), listener.accept())
                .await
                .is_err()
        );
    }

    #[test]
    fn mixed_dns_answers_and_empty_resolution_fail_closed() {
        let public = "8.8.8.8:443".parse().unwrap();
        for private in [
            "127.0.0.1:443",
            "10.0.0.2:443",
            "169.254.169.254:443",
            "[fc00::1]:443",
        ] {
            let private = private.parse().unwrap();
            assert!(validated_public_address(&[public, private]).is_err());
            assert!(validated_public_address(&[private, public]).is_err());
        }
        assert!(validated_public_address(&[]).is_err());
        assert_eq!(validated_public_address(&[public]).unwrap(), public);
    }

    #[tokio::test]
    async fn operator_origin_remains_usable_but_does_not_authorize_private_redirects() {
        let _env = crate::test_support::lock_test_env();
        let _proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "*");
        let destination = TcpListener::bind("127.0.0.1:0").await.unwrap();
        for status in [301, 302, 303, 307, 308] {
            let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
            let url = format!("http://{}/mcp", listener.local_addr().unwrap());
            let response = format!(
                "HTTP/1.1 {status} Redirect\r\nLocation: http://{}/private\r\nConnection: close\r\nContent-Length: 0\r\n\r\n",
                destination.local_addr().unwrap()
            );
            let server = tokio::spawn(reply_once(listener, response));
            let client = client(&url, false);
            let error = client
                .send(
                    client
                        .post(&url)
                        .header("Authorization", "Bearer fixture")
                        .body("{}"),
                )
                .await
                .unwrap_err();
            assert!(format!("{error:#}").contains("restricted"), "{error:#}");
            assert!(server.await.unwrap().contains("Bearer fixture"));
        }
        assert!(
            tokio::time::timeout(Duration::from_millis(30), destination.accept())
                .await
                .is_err()
        );
    }

    #[tokio::test]
    async fn redirect_stop_returns_response_without_following_even_same_origin() {
        let _env = crate::test_support::lock_test_env();
        let _proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "*");
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        let url = format!("http://{addr}/token");
        let server = tokio::spawn(async move {
            let (mut socket, _) = listener.accept().await.unwrap();
            let mut buf = [0u8; 2048];
            let read = socket.read(&mut buf).await.unwrap();
            assert!(read > 0, "fixture request must contain bytes");
            socket.write_all(b"HTTP/1.1 307 Redirect\r\nLocation: /capture\r\nConnection: close\r\nContent-Length: 0\r\n\r\n").await.unwrap();
            drop(socket);
            tokio::time::timeout(Duration::from_millis(80), listener.accept())
                .await
                .is_err()
        });
        let client = client(&url, false);
        let response = client
            .execute(
                client.post(&url).body("code=fixture").build().unwrap(),
                false,
            )
            .await
            .unwrap();
        assert_eq!(response.status(), 307);
        assert!(server.await.unwrap());
    }

    #[tokio::test]
    async fn configured_local_same_origin_redirect_and_connection_reuse_work() {
        let _env = crate::test_support::lock_test_env();
        let _proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "*");
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let url = format!("http://{}/mcp", listener.local_addr().unwrap());
        let server = tokio::spawn(async move {
            let (mut socket, _) = listener.accept().await.unwrap();
            let mut buf = [0u8; 2048];
            let read = socket.read(&mut buf).await.unwrap();
            assert!(read > 0, "fixture request must contain bytes");
            socket
                .write_all(
                    b"HTTP/1.1 307 Redirect\r\nLocation: /mcp/v2\r\nContent-Length: 0\r\n\r\n",
                )
                .await
                .unwrap();
            let size = socket.read(&mut buf).await.unwrap();
            assert!(String::from_utf8_lossy(&buf[..size]).starts_with("GET /mcp/v2 "));
            socket
                .write_all(b"HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: 2\r\n\r\nok")
                .await
                .unwrap();
        });
        let client = client(&url, false);
        let response = client.send(client.get(&url)).await.unwrap();
        assert_eq!(response.text().await.unwrap(), "ok");
        server.await.unwrap();
    }

    #[tokio::test]
    async fn model_added_configuration_marker_cannot_be_spoofed_or_lost_on_clone() {
        let config: super::super::McpServerConfig = serde_json::from_value(serde_json::json!({
            "url":"http://127.0.0.1:1/mcp", "runtime_added": false, "allow_private_network": true
        }))
        .unwrap();
        let pool = super::super::McpPool::new(super::super::McpConfig::default());
        pool.add_runtime_server_config("dynamic".to_string(), config)
            .unwrap();
        let config = pool.dynamic_servers.read().get("dynamic").unwrap().clone();
        assert!(config.runtime_added);
        assert!(config.allow_private_network);
        let client = McpHttpClient::new(
            config.url.as_deref().unwrap(),
            config.runtime_added,
            false,
            config.allow_private_network,
            None,
            Duration::from_secs(1),
            Duration::from_secs(2),
        )
        .unwrap();
        assert!(
            client
                .client_for_target(&Url::parse(config.url.as_deref().unwrap()).unwrap())
                .await
                .is_err()
        );
        assert!(
            serde_json::to_value(&config)
                .unwrap()
                .get("runtime_added")
                .is_none()
        );
    }

    #[tokio::test]
    async fn model_added_endpoint_cannot_use_ambient_proxy_but_operator_can() {
        let _env = crate::test_support::lock_test_env();
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let proxy_url = format!("http://{}", listener.local_addr().unwrap());
        let _https_proxy = crate::test_support::EnvVarGuard::set("HTTPS_PROXY", &proxy_url);
        let _http_proxy = crate::test_support::EnvVarGuard::set("HTTP_PROXY", &proxy_url);
        let _no_proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "");
        let _lower_no_proxy = crate::test_support::EnvVarGuard::set("no_proxy", "");
        let url = "http://mcp-guard-fixture.invalid/mcp";
        let strict = client(url, true);
        assert!(strict.send(strict.get(url)).await.is_err());
        // This documentation-only address passes the public-IP classifier. A
        // mistakenly enabled proxy would receive it without any DNS lookup.
        let public_literal = "http://192.0.2.1:9/mcp";
        let strict_literal = client(public_literal, true);
        assert!(
            strict_literal
                .send(strict_literal.get(public_literal))
                .await
                .is_err()
        );
        // A mistakenly enabled proxy would connect immediately; the generous
        // window only absorbs full-suite scheduler load.
        assert!(
            tokio::time::timeout(Duration::from_millis(500), listener.accept())
                .await
                .is_err()
        );
        let server = tokio::spawn(reply_once(
            listener,
            "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: 2\r\n\r\nok".to_string(),
        ));
        let configured = client(url, false);
        assert_eq!(
            configured
                .send(configured.get(url))
                .await
                .unwrap()
                .text()
                .await
                .unwrap(),
            "ok"
        );
        assert!(
            server
                .await
                .unwrap()
                .starts_with("GET http://mcp-guard-fixture.invalid/mcp ")
        );
    }

    #[tokio::test]
    async fn configured_public_origin_rejects_rebinding_before_reusing_its_pinned_client() {
        let _env = crate::test_support::lock_test_env();
        let _proxy = crate::test_support::EnvVarGuard::set("HTTPS_PROXY", "http://127.0.0.1:9");
        let _no_proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "mcp-guard-fixture.test");
        let url = Url::parse("https://mcp-guard-fixture.test/mcp").unwrap();
        let configured = client(url.as_str(), false);
        *configured.dns_answers.lock().unwrap() = Some(std::collections::VecDeque::from([
            vec!["8.8.8.8:443".parse().unwrap()],
            vec!["127.0.0.1:443".parse().unwrap()],
        ]));
        configured.client_for_target(&url).await.unwrap();
        assert_eq!(configured.clients.lock().unwrap().len(), 1);
        let error = configured.client_for_target(&url).await.unwrap_err();
        assert!(error.to_string().contains("restricted"), "{error:#}");
        assert!(
            configured
                .dns_answers
                .lock()
                .unwrap()
                .as_ref()
                .unwrap()
                .is_empty()
        );
    }

    #[tokio::test]
    async fn private_dns_requires_an_explicit_operator_opt_in() {
        let _env = crate::test_support::lock_test_env();
        let _proxy = crate::test_support::EnvVarGuard::set("NO_PROXY", "*");
        let url = Url::parse("https://internal-service.example.test/mcp").unwrap();
        let configured = client(url.as_str(), false);
        *configured.dns_answers.lock().unwrap() = Some(std::collections::VecDeque::from([vec![
            "10.0.0.3:443".parse().unwrap(),
        ]]));
        assert!(configured.client_for_target(&url).await.is_err());
        let approved = McpHttpClient::new(
            url.as_str(),
            false,
            false,
            true,
            None,
            Duration::from_secs(1),
            Duration::from_secs(2),
        )
        .unwrap();
        approved.client_for_target(&url).await.unwrap();
    }
}
