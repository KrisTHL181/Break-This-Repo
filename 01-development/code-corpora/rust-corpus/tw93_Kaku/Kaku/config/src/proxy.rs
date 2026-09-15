//! System proxy detection for `kaku update`-style curl invocations.
//!
//! When Kaku is launched from the menu bar or a notification, the process
//! env inherited from launchd has no proxy vars, so curl cannot reach the
//! internet through a user-configured proxy. This module resolves the
//! macOS system proxy from Network Settings via `/usr/sbin/scutil --proxy`
//! and exposes it for the update code paths in both `kaku` and `kaku-gui`.
//!
//! On non-macOS platforms `scutil` will simply fail to spawn and the
//! detection returns `None`, which is the correct fallback.

use std::process::Command;

const PROXY_ENV_VARS: &[&str] = &[
    "https_proxy",
    "HTTPS_PROXY",
    "http_proxy",
    "HTTP_PROXY",
    "ALL_PROXY",
    "all_proxy",
];

/// Detect the system proxy.
///
/// Returns `None` when any proxy env var is already set (curl picks it up
/// automatically) or when detection is unavailable on this platform.
pub fn detect_system_proxy() -> Option<String> {
    if PROXY_ENV_VARS.iter().any(|v| std::env::var(v).is_ok()) {
        return None;
    }

    let out = Command::new("/usr/sbin/scutil")
        .arg("--proxy")
        .output()
        .ok()?;
    if !out.status.success() {
        return None;
    }

    let text = String::from_utf8_lossy(&out.stdout);
    parse_scutil_proxy(&text)
}

/// Parse `scutil --proxy` output and return the preferred proxy URL
/// (HTTPS > HTTP > SOCKS). Returns `None` when no enabled proxy has both
/// host and port set.
pub fn parse_scutil_proxy(text: &str) -> Option<String> {
    let mut https_enabled = false;
    let mut https_host = String::new();
    let mut https_port = String::new();
    let mut http_enabled = false;
    let mut http_host = String::new();
    let mut http_port = String::new();
    let mut socks_enabled = false;
    let mut socks_host = String::new();
    let mut socks_port = String::new();

    for line in text.lines() {
        if let Some((key, val)) = line.trim().split_once(" : ") {
            match key.trim() {
                "HTTPSEnable" => https_enabled = val.trim() == "1",
                "HTTPSProxy" => https_host = val.trim().to_string(),
                "HTTPSPort" => https_port = val.trim().to_string(),
                "HTTPEnable" => http_enabled = val.trim() == "1",
                "HTTPProxy" => http_host = val.trim().to_string(),
                "HTTPPort" => http_port = val.trim().to_string(),
                "SOCKSEnable" => socks_enabled = val.trim() == "1",
                "SOCKSProxy" => socks_host = val.trim().to_string(),
                "SOCKSPort" => socks_port = val.trim().to_string(),
                _ => {}
            }
        }
    }

    if https_enabled && is_valid_host(&https_host) && is_valid_port(&https_port) {
        return Some(format!("http://{}:{}", https_host, https_port));
    }
    if http_enabled && is_valid_host(&http_host) && is_valid_port(&http_port) {
        return Some(format!("http://{}:{}", http_host, http_port));
    }
    if socks_enabled && is_valid_host(&socks_host) && is_valid_port(&socks_port) {
        return Some(format!("socks5h://{}:{}", socks_host, socks_port));
    }
    None
}

fn is_valid_port(s: &str) -> bool {
    s.parse::<u16>().map(|p| p > 0).unwrap_or(false)
}

fn is_valid_host(s: &str) -> bool {
    !s.is_empty() && !s.chars().any(|c| c.is_whitespace() || c == '/' || c == ':')
}

/// Detect the macOS system proxy bypass list (`scutil --proxy` ExceptionsList).
///
/// These are the hosts the user marked "Bypass proxy settings for these Hosts"
/// in Network Settings. Entries are normalized into reqwest `NoProxy` syntax
/// (partial CIDR masks like `169.254/16` are expanded to `169.254.0.0/16`).
/// Returns an empty vec on non-macOS or when no exceptions are configured.
pub fn system_proxy_exceptions() -> Vec<String> {
    let out = match Command::new("/usr/sbin/scutil").arg("--proxy").output() {
        Ok(o) if o.status.success() => o,
        _ => return Vec::new(),
    };
    parse_scutil_exceptions(&String::from_utf8_lossy(&out.stdout))
}

/// Parse the `ExceptionsList` array out of `scutil --proxy` output and
/// normalize each entry for reqwest `NoProxy::from_string`.
pub fn parse_scutil_exceptions(text: &str) -> Vec<String> {
    let mut entries = Vec::new();
    let mut in_list = false;
    for line in text.lines() {
        let trimmed = line.trim();
        if !in_list {
            if trimmed.starts_with("ExceptionsList") && trimmed.contains('{') {
                in_list = true;
            }
            continue;
        }
        if trimmed.starts_with('}') {
            break;
        }
        // Entries look like `0 : *.local`.
        if let Some((_, val)) = trimmed.split_once(" : ") {
            let val = val.trim();
            if !val.is_empty() {
                entries.push(normalize_no_proxy_entry(val));
            }
        }
    }
    entries
}

/// Convert a macOS proxy-exception entry into reqwest `NoProxy` syntax.
///
/// macOS writes `*.local` for "this domain and subdomains" and abbreviated
/// CIDRs like `169.254/16` or `10/8`. reqwest expects `.local` and full
/// dotted CIDRs (`169.254.0.0/16`). Domains, wildcards (`*`), and already
/// well-formed IPs pass through unchanged.
fn normalize_no_proxy_entry(entry: &str) -> String {
    if let Some(rest) = entry.strip_prefix("*.") {
        return format!(".{rest}");
    }
    let Some((ip, mask)) = entry.split_once('/') else {
        return entry.to_string();
    };
    // Only pad dotted IPv4-style addresses; leave IPv6 and anything odd alone.
    let is_dotted_ipv4 = !ip.contains(':')
        && ip
            .split('.')
            .all(|o| !o.is_empty() && o.bytes().all(|b| b.is_ascii_digit()));
    let mut octets: Vec<&str> = ip.split('.').collect();
    if !is_dotted_ipv4 || octets.len() >= 4 {
        return entry.to_string();
    }
    while octets.len() < 4 {
        octets.push("0");
    }
    format!("{}/{}", octets.join("."), mask)
}

/// Apply `proxy` to `cmd` by setting the standard curl proxy env vars.
/// No-op when `proxy` is `None`.
pub fn apply_to_command(cmd: &mut Command, proxy: &Option<String>) {
    if let Some(p) = proxy {
        cmd.env("https_proxy", p)
            .env("HTTPS_PROXY", p)
            .env("http_proxy", p)
            .env("HTTP_PROXY", p)
            .env("all_proxy", p)
            .env("ALL_PROXY", p);
    }
}

#[cfg(test)]
mod tests {
    use super::{normalize_no_proxy_entry, parse_scutil_exceptions, parse_scutil_proxy};

    #[test]
    fn normalizes_macos_exception_forms() {
        assert_eq!(normalize_no_proxy_entry("169.254/16"), "169.254.0.0/16");
        assert_eq!(normalize_no_proxy_entry("10/8"), "10.0.0.0/8");
        assert_eq!(normalize_no_proxy_entry("*.local"), ".local");
        // Already well-formed entries pass through untouched.
        assert_eq!(normalize_no_proxy_entry("192.168.1.0/24"), "192.168.1.0/24");
        assert_eq!(normalize_no_proxy_entry("example.com"), "example.com");
        assert_eq!(normalize_no_proxy_entry("100.65.0.14"), "100.65.0.14");
    }

    #[test]
    fn parses_scutil_exceptions_list() {
        let text = r#"
<dictionary> {
  ExceptionsList : <array> {
    0 : *.local
    1 : 169.254/16
  }
  SOCKSEnable : 1
  SOCKSPort : 7070
  SOCKSProxy : 100.65.0.2
}
"#;
        assert_eq!(
            parse_scutil_exceptions(text),
            vec![".local".to_string(), "169.254.0.0/16".to_string()]
        );
    }

    #[test]
    fn parses_no_exceptions_when_list_absent() {
        let text = "<dictionary> {\n  SOCKSEnable : 1\n}\n";
        assert!(parse_scutil_exceptions(text).is_empty());
    }

    #[test]
    fn prefers_https_then_http_then_socks() {
        let text = r#"
<dictionary> {
  HTTPEnable : 1
  HTTPPort : 8080
  HTTPProxy : 127.0.0.1
  HTTPSEnable : 1
  HTTPSPort : 8443
  HTTPSProxy : proxy.example.com
  SOCKSEnable : 1
  SOCKSPort : 1080
  SOCKSProxy : socks.example.com
}
"#;
        assert_eq!(
            parse_scutil_proxy(text).as_deref(),
            Some("http://proxy.example.com:8443")
        );
    }

    #[test]
    fn uses_socks5h_for_socks_proxy() {
        let text = r#"
<dictionary> {
  HTTPEnable : 0
  HTTPSEnable : 0
  SOCKSEnable : 1
  SOCKSPort : 1080
  SOCKSProxy : 127.0.0.1
}
"#;
        assert_eq!(
            parse_scutil_proxy(text).as_deref(),
            Some("socks5h://127.0.0.1:1080")
        );
    }

    #[test]
    fn returns_none_when_no_enabled_proxy_has_endpoint() {
        let text = r#"
<dictionary> {
  HTTPEnable : 0
  HTTPSEnable : 0
  SOCKSEnable : 0
}
"#;
        assert_eq!(parse_scutil_proxy(text), None);
    }

    #[test]
    fn rejects_invalid_port() {
        let text = r#"
<dictionary> {
  HTTPSEnable : 1
  HTTPSPort : not-a-number
  HTTPSProxy : proxy.example.com
}
"#;
        assert_eq!(parse_scutil_proxy(text), None);
    }

    #[test]
    fn rejects_host_with_whitespace_or_separator() {
        let text = r#"
<dictionary> {
  HTTPSEnable : 1
  HTTPSPort : 8443
  HTTPSProxy : bad host/value
}
"#;
        assert_eq!(parse_scutil_proxy(text), None);
    }
}
