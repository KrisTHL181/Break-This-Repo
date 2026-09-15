use tirith_core::data;
use tirith_core::parse;

pub fn run(url: &str, json: bool) -> i32 {
    let parsed = parse::parse_url(url);

    let host = parsed.host().map(String::from);
    let raw_host = parsed.raw_host().map(String::from);
    let scheme = parsed.scheme().map(String::from);
    let path = parsed.path().map(String::from);
    let port = parsed.port();
    let userinfo = parsed.userinfo().map(String::from);

    let is_known = host.as_deref().map(data::is_known_domain).unwrap_or(false);

    // host/raw_host diverge when IDNA normalization rewrote the host.
    let host_divergence = match (host.as_deref(), raw_host.as_deref()) {
        (Some(h), Some(rh)) => h != rh,
        _ => false,
    };

    let scheme_warning = match scheme.as_deref() {
        Some("http") => Some("HTTP (unencrypted) — consider HTTPS"),
        Some("ftp") => Some("FTP (unencrypted)"),
        Some("javascript") => Some("JavaScript URI — potential XSS vector"),
        Some("data") => Some("Data URI — content embedded in URL"),
        _ => None,
    };

    let port_warning = match port {
        Some(p) if p != 80 && p != 443 && p != 22 && p != 9418 && is_known => {
            Some(format!("Non-standard port {p} on known domain"))
        }
        _ => None,
    };

    let userinfo_warning = userinfo.as_ref().and_then(|ui| {
        if ui.contains('.') {
            Some("Userinfo contains dot — may be domain impersonation")
        } else {
            None
        }
    });

    if json {
        #[derive(serde::Serialize)]
        struct DiffOutput<'a> {
            url: &'a str,
            host: Option<String>,
            raw_host: Option<String>,
            scheme: Option<String>,
            path: Option<String>,
            port: Option<u16>,
            userinfo: Option<String>,
            is_known_domain: bool,
            host_divergence: bool,
            warnings: Vec<String>,
        }

        let mut warnings = Vec::new();
        if host_divergence {
            warnings.push(format!(
                "Host divergence: parsed='{}' raw='{}'",
                host.as_deref().unwrap_or(""),
                raw_host.as_deref().unwrap_or("")
            ));
        }
        if let Some(sw) = scheme_warning {
            warnings.push(sw.to_string());
        }
        if let Some(pw) = &port_warning {
            warnings.push(pw.clone());
        }
        if let Some(uw) = userinfo_warning {
            warnings.push(uw.to_string());
        }

        let out = DiffOutput {
            url,
            host,
            raw_host,
            scheme,
            path,
            port,
            userinfo,
            is_known_domain: is_known,
            host_divergence,
            warnings,
        };
        if serde_json::to_writer_pretty(std::io::stdout().lock(), &out).is_err() {
            eprintln!("tirith: failed to write JSON output");
            return 1;
        }
        println!();
    } else {
        // repo-0377: the URL and every parsed component are attacker-controlled;
        // scrub terminal controls before human rendering (JSON stays raw —
        // serde escapes it).
        eprintln!(
            "tirith diff: {}",
            super::sanitize_for_human_output(url, false)
        );
        if let Some(h) = &host {
            eprintln!(
                "  host:       {}",
                super::sanitize_for_human_output(h, false)
            );
        }
        if let Some(rh) = &raw_host {
            eprintln!(
                "  raw_host:   {}",
                super::sanitize_for_human_output(rh, false)
            );
        }
        if host_divergence {
            eprintln!("  WARNING:    host/raw_host diverge (IDNA normalization applied)");
        }
        if let Some(s) = &scheme {
            eprintln!(
                "  scheme:     {}",
                super::sanitize_for_human_output(s, false)
            );
        }
        if let Some(sw) = scheme_warning {
            eprintln!(
                "  WARNING:    {}",
                super::sanitize_for_human_output(sw, false)
            );
        }
        if let Some(p) = &path {
            eprintln!(
                "  path:       {}",
                super::sanitize_for_human_output(p, false)
            );
        }
        if let Some(port) = port {
            eprintln!("  port:       {port}");
        }
        if let Some(pw) = &port_warning {
            eprintln!(
                "  WARNING:    {}",
                super::sanitize_for_human_output(pw, false)
            );
        }
        if let Some(ui) = &userinfo {
            eprintln!(
                "  userinfo:   {}",
                super::sanitize_for_human_output(ui, false)
            );
        }
        if let Some(uw) = userinfo_warning {
            eprintln!(
                "  WARNING:    {}",
                super::sanitize_for_human_output(uw, false)
            );
        }
        eprintln!("  known_domain: {is_known}");
    }

    0
}
