/// Return the current system hostname without assuming it is stable for the
/// lifetime of the app.
pub(crate) fn current() -> Option<String> {
    hostname::get()
        .ok()
        .map(|hostname| hostname.to_string_lossy().into_owned())
        .filter(|hostname| !hostname.is_empty())
}

fn without_fqdn_dot(hostname: &str) -> &str {
    hostname.strip_suffix('.').unwrap_or(hostname)
}

/// Whether a `file://` URL host is proven to identify this machine.
///
/// Short hostnames are intentionally not derived from an FQDN: a local
/// `mac.local` and a remote `mac` are distinct machines even though their
/// first labels match.
pub(crate) fn is_local_file_host(host: Option<&str>, local_hostname: Option<&str>) -> bool {
    let Some(host) = host else {
        return true;
    };
    let host = without_fqdn_dot(host);
    if host.eq_ignore_ascii_case("localhost") {
        return true;
    }

    let Some(local_hostname) = local_hostname else {
        return false;
    };
    let local_hostname = without_fqdn_dot(local_hostname);
    !local_hostname.is_empty() && host.eq_ignore_ascii_case(local_hostname)
}

#[cfg(test)]
mod tests {
    use super::is_local_file_host;

    #[test]
    fn local_file_hosts_require_an_exact_identity() {
        assert!(is_local_file_host(None, Some("mac.local")));
        assert!(is_local_file_host(Some("localhost"), Some("mac.local")));
        assert!(is_local_file_host(Some("MAC.LOCAL."), Some("mac.local")));

        assert!(!is_local_file_host(Some("mac"), Some("mac.local")));
        assert!(!is_local_file_host(
            Some("mac.corp.example"),
            Some("mac.local")
        ));
        assert!(!is_local_file_host(Some("mac.local"), None));
    }
}
