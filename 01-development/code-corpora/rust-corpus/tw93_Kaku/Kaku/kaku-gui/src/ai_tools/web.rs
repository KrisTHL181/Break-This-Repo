//! HTTP fetch and web-request helpers.

use anyhow::{Context, Result};
use std::io::Read;
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, ToSocketAddrs};
use std::sync::OnceLock;
use std::time::Duration;

/// Shared HTTP client for all web tool calls (connection pool, keep-alive).
///
/// Built via `ai_client::build_client_with_proxy` so the user's system proxy
/// applies to `web_fetch` / `web_search` / `http_request` the same way it
/// already does for the chat client. The 60s timeout is intentionally
/// shorter than the chat client's: web tools should fail fast.
pub(super) fn web_client() -> &'static reqwest::blocking::Client {
    static CLIENT: OnceLock<reqwest::blocking::Client> = OnceLock::new();
    CLIENT.get_or_init(|| crate::ai_client::build_client_with_proxy(Duration::from_secs(60)))
}

/// Maximum bytes to buffer from any single HTTP fetch response.
const MAX_FETCH_BYTES: usize = 512 * 1024;

/// Read at most `MAX_FETCH_BYTES` from a reqwest blocking Response.
pub(super) fn read_response_capped(resp: reqwest::blocking::Response) -> Result<String> {
    let mut buf = Vec::with_capacity(MAX_FETCH_BYTES.min(64 * 1024));
    resp.take(MAX_FETCH_BYTES as u64)
        .read_to_end(&mut buf)
        .context("read HTTP response body")?;
    Ok(String::from_utf8_lossy(&buf).into_owned())
}

/// Read at most 4 KiB from an error response for diagnostic messages.
pub(super) fn read_error_body(resp: reqwest::blocking::Response) -> String {
    let mut buf = Vec::with_capacity(4096);
    let _ = resp.take(4096).read_to_end(&mut buf);
    String::from_utf8_lossy(&buf).into_owned()
}

/// Fetch a URL as Markdown. Primary: defuddle.md. Fallback: r.jina.ai.
pub(super) fn fetch_markdown_default(url: &str) -> Result<String> {
    let client = web_client();
    if let Ok(resp) = client.get(format!("https://defuddle.md/{}", url)).send() {
        if resp.status().is_success() {
            if let Ok(body) = read_response_capped(resp) {
                if !body.trim().is_empty() {
                    return Ok(body);
                }
            }
        }
    }
    let resp = client
        .get(format!("https://r.jina.ai/{}", url))
        .send()
        .context("both defuddle.md and r.jina.ai unreachable")?;
    if !resp.status().is_success() {
        anyhow::bail!(
            "fetch failed: defuddle.md and r.jina.ai both returned non-2xx (last: {})",
            resp.status()
        );
    }
    read_response_capped(resp).context("read fetch response body")
}

pub(super) fn exec_http_request(
    method: &str,
    url: &str,
    headers: Option<&serde_json::Map<String, serde_json::Value>>,
    body: Option<&str>,
    query_params: Option<&serde_json::Map<String, serde_json::Value>>,
) -> Result<String> {
    let destination = validate_external_http_url(url)?;
    let client = pinned_web_client(&destination)?;
    let mut req = match method {
        "GET" => client.get(destination.url.clone()),
        "POST" => client.post(destination.url.clone()),
        "PUT" => client.put(destination.url.clone()),
        "PATCH" => client.patch(destination.url.clone()),
        "DELETE" => client.delete(destination.url.clone()),
        _ => anyhow::bail!("unsupported HTTP method: {}", method),
    };

    if let Some(params) = query_params {
        let pairs: Vec<(&str, &str)> = params
            .iter()
            .filter_map(|(k, v)| v.as_str().map(|s| (k.as_str(), s)))
            .collect();
        req = req.query(&pairs);
    }

    if let Some(hdrs) = headers {
        for (k, v) in hdrs {
            if let Some(val) = v.as_str() {
                req = req.header(k.as_str(), val);
            }
        }
    }

    if let Some(b) = body {
        if serde_json::from_str::<serde_json::Value>(b).is_ok() {
            req = req
                .header("Content-Type", "application/json")
                .body(b.to_string());
        } else {
            req = req.body(b.to_string());
        }
    }

    let resp = req
        .send()
        .with_context(|| format!("http_request {} {} failed", method, url))?;

    let status = resp.status();
    let resp_headers: Vec<String> = resp
        .headers()
        .iter()
        .filter(|(k, _)| {
            let name = k.as_str().to_ascii_lowercase();
            matches!(
                name.as_str(),
                "content-type" | "content-length" | "x-request-id" | "x-ratelimit-remaining"
            )
        })
        .map(|(k, v)| format!("{}: {}", k, v.to_str().unwrap_or("?")))
        .collect();
    let body_text = read_response_capped(resp).context("read http_request response body")?;

    let mut out = format!("HTTP {}\n", status.as_u16());
    if !resp_headers.is_empty() {
        out.push_str(&resp_headers.join("\n"));
        out.push('\n');
    }
    out.push('\n');
    if let Ok(json) = serde_json::from_str::<serde_json::Value>(&body_text) {
        out.push_str(&serde_json::to_string_pretty(&json).unwrap_or(body_text));
    } else {
        out.push_str(&body_text);
    }
    Ok(out)
}

/// Validate direct HTTP destinations before connecting. This intentionally
/// rejects names that resolve to any non-public address, rather than only
/// checking URL text, so DNS aliases cannot be used to reach local services.
struct ValidatedHttpDestination {
    url: url::Url,
    domain: Option<String>,
    addresses: Vec<SocketAddr>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum HttpRequestRoute {
    DirectPinned,
}

fn http_request_route(scheme: &str) -> HttpRequestRoute {
    debug_assert!(matches!(scheme, "http" | "https"));
    HttpRequestRoute::DirectPinned
}

fn validate_external_http_url(raw: &str) -> Result<ValidatedHttpDestination> {
    let parsed = url::Url::parse(raw).context("invalid http_request URL")?;
    if !matches!(parsed.scheme(), "http" | "https") {
        anyhow::bail!("http_request URL must use http or https");
    }
    if !parsed.username().is_empty() || parsed.password().is_some() {
        anyhow::bail!("http_request URL must not contain credentials");
    }
    let port = parsed
        .port_or_known_default()
        .ok_or_else(|| anyhow::anyhow!("http_request URL has no usable port"))?;
    let (domain, addresses) = match parsed.host() {
        Some(url::Host::Ipv4(address)) => {
            reject_non_public_ip(IpAddr::V4(address))?;
            (None, vec![SocketAddr::new(IpAddr::V4(address), port)])
        }
        Some(url::Host::Ipv6(address)) => {
            reject_non_public_ip(IpAddr::V6(address))?;
            (None, vec![SocketAddr::new(IpAddr::V6(address), port)])
        }
        Some(url::Host::Domain(domain)) => {
            let normalized = domain.trim_end_matches('.').to_ascii_lowercase();
            if normalized == "localhost" || normalized.ends_with(".localhost") {
                anyhow::bail!("http_request refuses loopback destinations");
            }
            if normalized == "local" || normalized.ends_with(".local") {
                anyhow::bail!("http_request refuses .local destinations");
            }
            let addresses = (domain, port)
                .to_socket_addrs()
                .with_context(|| format!("resolve http_request host `{domain}`"))?
                .collect::<Vec<_>>();
            if addresses.is_empty() {
                anyhow::bail!("http_request host `{domain}` resolved to no addresses");
            }
            for address in &addresses {
                reject_non_public_ip(address.ip())?;
            }
            (Some(domain.to_string()), addresses)
        }
        None => anyhow::bail!("http_request URL is missing a host"),
    };
    Ok(ValidatedHttpDestination {
        url: parsed,
        domain,
        addresses,
    })
}

fn pinned_web_client(destination: &ValidatedHttpDestination) -> Result<reqwest::blocking::Client> {
    let mut builder = reqwest::blocking::Client::builder()
        .connect_timeout(Duration::from_secs(30))
        .timeout(Duration::from_secs(60))
        .redirect(reqwest::redirect::Policy::none());

    // The security decision and the connection must use the same address set.
    // A proxy would re-resolve the hostname after validation, reopening DNS
    // rebinding against private services. This tool therefore stays direct
    // and pinned for both HTTP and HTTPS; chat and fetch clients retain their
    // normal system-proxy behavior.
    let HttpRequestRoute::DirectPinned = http_request_route(destination.url.scheme());
    builder = builder.no_proxy();
    if let Some(domain) = destination.domain.as_deref() {
        builder = builder.resolve_to_addrs(domain, &destination.addresses);
    }
    builder.build().context("build pinned http_request client")
}

fn reject_non_public_ip(address: IpAddr) -> Result<()> {
    let public = match address {
        IpAddr::V4(address) => ipv4_is_public(address),
        IpAddr::V6(address) => ipv6_is_public(address),
    };
    if !public {
        anyhow::bail!("http_request refuses non-public destination `{address}`");
    }
    Ok(())
}

fn ipv4_is_public(address: Ipv4Addr) -> bool {
    let octets = address.octets();
    !(address.is_private()
        || address.is_loopback()
        || address.is_link_local()
        || address.is_multicast()
        || address.is_broadcast()
        || address.is_unspecified()
        || octets[0] == 0
        || (octets[0] == 100 && (64..=127).contains(&octets[1]))
        || (octets[0] == 192 && octets[1] == 0 && octets[2] == 0)
        || (octets[0] == 198 && matches!(octets[1], 18 | 19))
        || (octets[0] == 192 && octets[1] == 0 && octets[2] == 2)
        || (octets[0] == 198 && octets[1] == 51 && octets[2] == 100)
        || (octets[0] == 203 && octets[1] == 0 && octets[2] == 113)
        // 6to4 relay anycast (RFC 7526): a relay would forward onward.
        || (octets[0] == 192 && octets[1] == 88 && octets[2] == 99)
        || octets[0] >= 240)
}

fn ipv6_is_public(address: Ipv6Addr) -> bool {
    if let Some(mapped) = address.to_ipv4_mapped() {
        return ipv4_is_public(mapped);
    }
    let segments = address.segments();

    // IPv4-embedding transition prefixes: a NAT64/6to4 gateway forwards the
    // request to the embedded IPv4 address, so judge that address, not the
    // IPv6 wrapper. Covers the well-known NAT64 prefix 64:ff9b::/96
    // (RFC 6052) and 6to4 2002::/16 (RFC 3056); without this, on a DNS64
    // network `[64:ff9b::a9fe:a9fe]` reaches 169.254.169.254.
    if segments[0] == 0x0064 && segments[1] == 0xff9b && segments[2..6] == [0, 0, 0, 0] {
        let embedded = Ipv4Addr::new(
            (segments[6] >> 8) as u8,
            segments[6] as u8,
            (segments[7] >> 8) as u8,
            segments[7] as u8,
        );
        return ipv4_is_public(embedded);
    }
    if segments[0] == 0x2002 {
        let embedded = Ipv4Addr::new(
            (segments[1] >> 8) as u8,
            segments[1] as u8,
            (segments[2] >> 8) as u8,
            segments[2] as u8,
        );
        return ipv4_is_public(embedded);
    }

    !(address.is_loopback()
        || address.is_unspecified()
        || address.is_multicast()
        || (segments[0] & 0xfe00) == 0xfc00
        || (segments[0] & 0xffc0) == 0xfe80
        || (segments[0] & 0xffc0) == 0xfec0
        || (segments[0] == 0x0064 && segments[1] == 0xff9b && segments[2] == 0x0001)
        || (segments[0] == 0x2001 && segments[1] == 0x0db8))
}

/// Read a URL and return clean extracted text.
/// Uses provider-native readers where available, falls back to generic fetchers.
pub(super) fn exec_read_url(url: &str, provider: &str, api_key: &str) -> Result<String> {
    match provider {
        "pipellm" => {
            let domains = ["https://api.pipellm.ai", "https://api.pipellm.com"];
            let mut last_err = String::new();
            for base in &domains {
                let resp = match web_client()
                    .get(format!("{}/v1/websearch/reader", base))
                    .query(&[("url", url)])
                    .bearer_auth(api_key)
                    .send()
                {
                    Ok(r) => r,
                    Err(e) => {
                        last_err = e.to_string();
                        continue;
                    }
                };
                if !resp.status().is_success() {
                    let status = resp.status();
                    let body = read_error_body(resp);
                    last_err = format!(
                        "{} from {}: {}",
                        status,
                        base,
                        body.chars().take(300).collect::<String>()
                    );
                    continue;
                }
                let json: serde_json::Value =
                    resp.json().context("parse pipellm reader response")?;
                let text = json["content"]
                    .as_str()
                    .or_else(|| json["text"].as_str())
                    .or_else(|| json.as_str())
                    .unwrap_or("")
                    .to_string();
                if !text.trim().is_empty() {
                    return Ok(text);
                }
                return Ok("Page returned empty content.".into());
            }
            log::warn!(
                "pipellm reader failed ({}), falling back to generic fetch",
                last_err
            );
            fetch_markdown_default(url)
        }
        "tavily" => {
            let resp = web_client()
                .post("https://api.tavily.com/extract")
                .bearer_auth(api_key)
                .json(&serde_json::json!({ "urls": [url] }))
                .send()
                .context("tavily extract request failed")?;
            if !resp.status().is_success() {
                let status = resp.status();
                let body = read_error_body(resp);
                log::warn!(
                    "tavily extract returned {} ({}), falling back to generic fetch",
                    status,
                    body.trim().chars().take(200).collect::<String>()
                );
                return fetch_markdown_default(url);
            }
            let json: serde_json::Value = resp.json().context("parse tavily extract response")?;
            let content = json["results"]
                .as_array()
                .and_then(|a| a.first())
                .and_then(|r| r["raw_content"].as_str().or_else(|| r["content"].as_str()))
                .unwrap_or("")
                .to_string();
            if content.trim().is_empty() {
                return fetch_markdown_default(url);
            }
            Ok(content)
        }
        _ => fetch_markdown_default(url),
    }
}

/// Content above this raw-bytes threshold is passed through a cheap
/// summarizer before being returned to the main agent. Picked so a typical
/// docs page (under 4 KB clean markdown) skips the second LLM hop, while
/// long blog posts and reference pages get compressed.
const SUMMARIZE_FETCH_THRESHOLD: usize = 4_000;

const WEBFETCH_SUMMARIZE_PROMPT: &str =
    include_str!("../../../assets/prompts/webfetch_summarize.txt");

pub(super) fn should_return_raw_fetch(detail: &str, raw_requested: bool) -> bool {
    raw_requested || detail == "full"
}

/// Compress a verbose web_fetch result so the main agent context stays cheap.
///
/// - `raw_passthrough`: return fetched content verbatim. Used when the caller
///   needs exact source text for quoting, debugging, or a full-detail read.
/// - Below `SUMMARIZE_FETCH_THRESHOLD` bytes: passthrough.
/// - Otherwise: build a small `AiClient` from the active config, call
///   `complete_once` with the webfetch-summarizer prompt, return the
///   summary. On any error, fall back to the raw content so the agent loop
///   never breaks just because the summarizer was misconfigured.
///
/// Uses `fast_model` when present (this is a low-stakes compression step
/// and should not bill against the deep model).
pub(super) fn maybe_summarize_fetched(
    url: &str,
    content: String,
    config: &crate::ai_client::AssistantConfig,
    raw_passthrough: bool,
) -> String {
    if raw_passthrough {
        return content;
    }
    if content.len() < SUMMARIZE_FETCH_THRESHOLD {
        return content;
    }
    let model = config
        .fast_model
        .as_deref()
        .filter(|s| !s.is_empty())
        .unwrap_or(&config.chat_model)
        .to_string();
    if model.is_empty() {
        return content;
    }
    let prompt = crate::ai_chat_engine::strip_prompt_metadata(WEBFETCH_SUMMARIZE_PROMPT)
        .replace("${URL}", url)
        .replace("${WEB_CONTENT}", &content);
    let client = crate::ai_client::AiClient::new(config.clone());
    match client.complete_once(&model, &[crate::ai_client::ApiMessage::system(prompt)]) {
        Ok(s) if !s.trim().is_empty() => s,
        Ok(_) => {
            log::warn!("maybe_summarize_fetched: empty summary, returning raw");
            content
        }
        Err(e) => {
            log::warn!("maybe_summarize_fetched: model call failed: {e}; returning raw");
            content
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn direct_http_requests_reject_local_and_private_destinations() {
        for url in [
            "http://127.0.0.1/admin",
            "http://[::1]/admin",
            "http://10.0.0.8/",
            "http://100.64.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "https://service.local/",
            "https://localhost/",
        ] {
            assert!(
                validate_external_http_url(url).is_err(),
                "must reject {}",
                url
            );
        }
    }

    #[test]
    fn public_ip_classification_rejects_reserved_ranges() {
        assert!(ipv4_is_public("8.8.8.8".parse().unwrap()));
        assert!(!ipv4_is_public("192.168.1.2".parse().unwrap()));
        assert!(!ipv4_is_public("100.127.255.254".parse().unwrap()));
        assert!(!ipv4_is_public("192.88.99.1".parse().unwrap()));
        assert!(!ipv6_is_public("fe80::1".parse().unwrap()));
        assert!(!ipv6_is_public("fec0::1".parse().unwrap()));
        assert!(!ipv6_is_public("64:ff9b:1::1".parse().unwrap()));
        assert!(ipv6_is_public("2606:4700:4700::1111".parse().unwrap()));
        // Well-known NAT64 prefix (RFC 6052): judge the embedded IPv4.
        // 64:ff9b::a9fe:a9fe embeds 169.254.169.254 (metadata endpoint).
        assert!(!ipv6_is_public("64:ff9b::a9fe:a9fe".parse().unwrap()));
        assert!(!ipv6_is_public("64:ff9b::7f00:1".parse().unwrap()));
        assert!(ipv6_is_public("64:ff9b::101:101".parse().unwrap()));
        // 6to4 (RFC 3056): 2002:V4ADDR::/48 embeds the IPv4 in segments 1-2.
        assert!(!ipv6_is_public("2002:a9fe:a9fe::1".parse().unwrap()));
        assert!(!ipv6_is_public("2002:c0a8:101::1".parse().unwrap()));
        assert!(ipv6_is_public("2002:101:101::1".parse().unwrap()));
    }

    #[test]
    fn validated_ip_destination_is_pinned_to_the_checked_socket() {
        let destination = validate_external_http_url("https://8.8.8.8/example").unwrap();
        assert!(destination.domain.is_none());
        assert_eq!(destination.addresses.len(), 1);
        assert_eq!(
            destination.addresses[0].ip(),
            "8.8.8.8".parse::<IpAddr>().unwrap()
        );
        assert_eq!(destination.addresses[0].port(), 443);
    }

    #[test]
    fn https_http_request_uses_direct_pinned_routing() {
        assert_eq!(
            http_request_route("https"),
            HttpRequestRoute::DirectPinned,
            "validated HTTPS destinations must not be re-resolved by a proxy"
        );
    }

    #[test]
    fn raw_fetch_policy_respects_full_detail_and_explicit_raw() {
        assert!(should_return_raw_fetch("full", false));
        assert!(should_return_raw_fetch("default", true));
        assert!(!should_return_raw_fetch("default", false));
        assert!(!should_return_raw_fetch("brief", false));
    }
}
