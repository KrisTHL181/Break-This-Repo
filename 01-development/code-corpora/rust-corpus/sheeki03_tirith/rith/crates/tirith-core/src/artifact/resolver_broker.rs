//! Loopback-only CONNECT broker for package resolution.
//!
//! `uv` and `pip` are trusted executables, but their HTTP stacks must not decide
//! where resolver traffic may go. This broker receives every HTTPS connection,
//! authorizes the canonical `(host, port)` against the operator-approved index
//! and direct-reference origins, resolves once, rejects non-public addresses,
//! connects to the approved IP directly, and pins TLS SNI to the CONNECT host.
//!
//! The standard-library system resolver is not cancellable on every supported
//! OS. Lookups therefore run behind a process-global four-worker ceiling: a
//! wedged OS resolver can retain one of those bounded slots, and once all four
//! are retained future broker lookups fail closed instead of creating more
//! threads. Session and listener workers themselves remain owned and joined.

use std::collections::{BTreeMap, BTreeSet};
use std::io::{Read, Write};
use std::net::{IpAddr, Shutdown, SocketAddr, TcpListener, TcpStream, ToSocketAddrs};
use std::sync::atomic::{AtomicBool, AtomicU64, AtomicUsize, Ordering};
use std::sync::{mpsc, Arc, Mutex};
use std::thread::JoinHandle;
use std::time::{Duration, Instant};

use base64::Engine as _;

const MAX_REQUEST_HEAD: usize = 8 * 1024;
const MAX_CLIENT_HELLO: usize = 64 * 1024;
const MAX_RESOLVED_ADDRESSES: usize = 16;
const MAX_CONCURRENT_SESSIONS: usize = 32;
const MAX_CONCURRENT_DNS_LOOKUPS: usize = 4;
const MAX_CACHED_ORIGINS: usize = 64;
/// Per-direction byte ceiling for one proxied session. Reaching it ABORTS the
/// connection rather than truncating the body: a silently short download would
/// surface later as a digest mismatch, which reads like a tampered artifact
/// instead of a broker limit. Large ML wheels can legitimately approach this,
/// so a raise is a policy decision — but it must stay a decision, not a silent
/// corruption.
const MAX_TUNNEL_BYTES_PER_DIRECTION: u64 = 256 * 1024 * 1024;
const HANDSHAKE_TIMEOUT: Duration = Duration::from_secs(10);
const DNS_TIMEOUT: Duration = Duration::from_secs(5);
const IDLE_TIMEOUT: Duration = Duration::from_secs(120);
static ACTIVE_DNS_LOOKUPS: AtomicUsize = AtomicUsize::new(0);
/// Approved addresses for an origin already resolved during this broker's life.
///
/// A resolve normally targets one or two origins, but pip and uv open several
/// connections in parallel, and every session used to take one of the four DNS
/// worker slots. The fifth concurrent CONNECT to an ALREADY-resolved host then
/// failed with "resolver DNS worker capacity exhausted".
///
/// Only the post-approval list is stored, so a cache hit cannot admit an
/// address that `approve_resolved_addresses` would have refused. Reusing the
/// first approved answer is also strictly stronger against DNS rebinding than
/// re-resolving, which is why the entry is not re-validated on hit. The broker
/// clears this when it starts, so one run never inherits another's answers.
static RESOLVED_ORIGINS: Mutex<BTreeMap<(String, u16), Vec<SocketAddr>>> =
    Mutex::new(BTreeMap::new());

fn cached_origin_addresses(host: &str, port: u16) -> Option<Vec<SocketAddr>> {
    RESOLVED_ORIGINS
        .lock()
        .ok()?
        .get(&(host.to_string(), port))
        .cloned()
}

fn remember_origin_addresses(host: &str, port: u16, approved: &[SocketAddr]) {
    if let Ok(mut cache) = RESOLVED_ORIGINS.lock() {
        // A resolve targets a handful of origins; the bound only guards against
        // a pathological permitted-origin set.
        if cache.len() < MAX_CACHED_ORIGINS {
            cache.insert((host.to_string(), port), approved.to_vec());
        }
    }
}

fn forget_all_origin_addresses() {
    if let Ok(mut cache) = RESOLVED_ORIGINS.lock() {
        cache.clear();
    }
}

type ConnectionRegistry = Arc<Mutex<BTreeMap<u64, Vec<TcpStream>>>>;

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
struct PermittedOrigin {
    host: String,
    port: u16,
}

#[derive(Debug, Clone, Default)]
pub(super) struct PermittedOrigins {
    origins: BTreeSet<PermittedOrigin>,
}

impl PermittedOrigins {
    pub(super) fn from_urls(urls: &[url::Url]) -> Result<Self, String> {
        let mut origins = BTreeSet::new();
        for url in urls {
            let host = canonical_url_host(url)?;
            let port = url
                .port_or_known_default()
                .ok_or_else(|| format!("URL has no known port: {url}"))?;
            origins.insert(PermittedOrigin {
                host: host.clone(),
                port,
            });
            // PyPI's canonical Simple API serves wheel links from this one
            // exact CDN origin. Keep the compatibility mapping narrow: no
            // wildcard and no promotion of the CDN to an index argv.
            if host == "pypi.org" && port == 443 {
                origins.insert(PermittedOrigin {
                    host: "files.pythonhosted.org".to_string(),
                    port: 443,
                });
            }
            if host == "test.pypi.org" && port == 443 {
                origins.insert(PermittedOrigin {
                    host: "test-files.pythonhosted.org".to_string(),
                    port: 443,
                });
            }
        }
        Ok(Self { origins })
    }

    pub(super) fn permits(&self, host: &str, port: u16) -> bool {
        self.origins.contains(&PermittedOrigin {
            host: canonical_host(host),
            port,
        })
    }
}

/// One resolver-session broker. Dropping it stops the listener; established
/// tunnels naturally close when the resolver children exit.
pub(super) struct ResolverBroker {
    address: SocketAddr,
    token: String,
    stop: Arc<AtomicBool>,
    connections: ConnectionRegistry,
    listener_thread: Option<JoinHandle<()>>,
}

impl ResolverBroker {
    pub(super) fn start(permitted: PermittedOrigins) -> Result<Self, String> {
        // Scope the approved-address cache to this broker: a long-lived process
        // (the MCP server) must not let one resolve inherit another's answers.
        forget_all_origin_addresses();
        let listener = TcpListener::bind(SocketAddr::from(([127, 0, 0, 1], 0)))
            .map_err(|e| format!("could not bind resolver broker: {e}"))?;
        listener
            .set_nonblocking(true)
            .map_err(|e| format!("could not configure resolver broker: {e}"))?;
        let address = listener
            .local_addr()
            .map_err(|e| format!("could not read resolver broker address: {e}"))?;

        let mut token_bytes = [0u8; 32];
        getrandom::fill(&mut token_bytes)
            .map_err(|e| format!("could not mint resolver broker token: {e}"))?;
        let token = hex::encode(token_bytes);
        let stop = Arc::new(AtomicBool::new(false));
        let connections: ConnectionRegistry = Arc::new(Mutex::new(BTreeMap::new()));
        let active = Arc::new(AtomicUsize::new(0));
        let next_connection_id = Arc::new(AtomicU64::new(1));
        let thread_stop = Arc::clone(&stop);
        let thread_token = token.clone();
        let thread_connections = Arc::clone(&connections);
        let thread_active = Arc::clone(&active);
        let thread_next_connection_id = Arc::clone(&next_connection_id);
        let listener_thread = std::thread::Builder::new()
            .name("tirith-resolver-broker".to_string())
            .spawn(move || {
                let mut workers: Vec<JoinHandle<()>> = Vec::new();
                while !thread_stop.load(Ordering::Acquire) {
                    reap_finished_workers(&mut workers);
                    match listener.accept() {
                        Ok((mut stream, peer)) if peer.ip().is_loopback() => {
                            // Some platforms propagate the listener's O_NONBLOCK
                            // state to accepted sockets. Session I/O relies on
                            // absolute socket deadlines; clear nonblocking so a
                            // partial request waits for that deadline instead of
                            // failing immediately with WouldBlock.
                            if stream.set_nonblocking(false).is_err() {
                                deny(&mut stream, 503, "Resolver Broker Busy");
                                continue;
                            }
                            if thread_active
                                .fetch_update(Ordering::AcqRel, Ordering::Acquire, |count| {
                                    (count < MAX_CONCURRENT_SESSIONS).then_some(count + 1)
                                })
                                .is_err()
                            {
                                deny(&mut stream, 503, "Resolver Broker Busy");
                                continue;
                            }
                            let connection_id =
                                thread_next_connection_id.fetch_add(1, Ordering::Relaxed);
                            let clone = match stream.try_clone() {
                                Ok(clone) => clone,
                                Err(_) => {
                                    thread_active.fetch_sub(1, Ordering::AcqRel);
                                    deny(&mut stream, 503, "Resolver Broker Busy");
                                    continue;
                                }
                            };
                            match thread_connections.lock() {
                                Ok(mut registry) => {
                                    registry.insert(connection_id, vec![clone]);
                                }
                                Err(_) => {
                                    thread_active.fetch_sub(1, Ordering::AcqRel);
                                    deny(&mut stream, 503, "Resolver Broker Busy");
                                    continue;
                                }
                            }
                            let permitted = permitted.clone();
                            let token = thread_token.clone();
                            let stop = Arc::clone(&thread_stop);
                            let connections = Arc::clone(&thread_connections);
                            let active = Arc::clone(&thread_active);
                            match std::thread::Builder::new()
                                .name("tirith-resolver-tunnel".to_string())
                                .spawn(move || {
                                    let _guard = SessionGuard {
                                        connection_id,
                                        active,
                                        connections: Arc::clone(&connections),
                                    };
                                    // Drop's cancellation is a stop store
                                    // followed by a registry shutdown sweep. A
                                    // session whose registration missed that
                                    // sweep would sit in its read deadline
                                    // unobserved and stall the listener join,
                                    // so complete the handshake from this side:
                                    // registration happened above, re-check the
                                    // flag, and self-cancel when it was already
                                    // raised.
                                    if stop.load(Ordering::Acquire) {
                                        let _ = stream.shutdown(Shutdown::Both);
                                        return;
                                    }
                                    let _ = serve_connection(
                                        stream,
                                        &permitted,
                                        &token,
                                        &stop,
                                        &connections,
                                        connection_id,
                                    );
                                }) {
                                Ok(worker) => workers.push(worker),
                                Err(_) => {
                                    thread_active.fetch_sub(1, Ordering::AcqRel);
                                    if let Ok(mut registry) = thread_connections.lock() {
                                        registry.remove(&connection_id);
                                    }
                                }
                            }
                        }
                        Ok((stream, _)) => drop(stream),
                        Err(error) if error.kind() == std::io::ErrorKind::WouldBlock => {
                            std::thread::sleep(Duration::from_millis(10));
                        }
                        Err(error) => {
                            // A client aborting mid-handshake, an interrupted
                            // call, or transient descriptor pressure must not
                            // retire the listener: every later pip/uv connection
                            // would get ECONNREFUSED for the rest of the resolve.
                            let retryable = matches!(
                                error.kind(),
                                std::io::ErrorKind::ConnectionAborted
                                    | std::io::ErrorKind::ConnectionReset
                                    | std::io::ErrorKind::Interrupted
                                    | std::io::ErrorKind::OutOfMemory
                            );
                            if !retryable {
                                break;
                            }
                            std::thread::sleep(Duration::from_millis(10));
                        }
                    }
                }
                for worker in workers {
                    let _ = worker.join();
                }
            })
            .map_err(|e| format!("could not start resolver broker: {e}"))?;

        Ok(Self {
            address,
            token,
            stop,
            connections,
            listener_thread: Some(listener_thread),
        })
    }

    /// Authenticated loopback proxy URL. The random token is carried as Basic
    /// proxy credentials because both pip and uv honor standard proxy URLs.
    pub(super) fn proxy_url(&self) -> String {
        format!("http://tirith:{}@{}", self.token, self.address)
    }
}

impl Drop for ResolverBroker {
    fn drop(&mut self) {
        self.stop.store(true, Ordering::Release);
        if let Ok(registry) = self.connections.lock() {
            for sockets in registry.values() {
                for socket in sockets {
                    let _ = socket.shutdown(Shutdown::Both);
                }
            }
        }
        // Wake the nonblocking accept loop so shutdown does not wait on cadence.
        let _ = TcpStream::connect_timeout(&self.address, Duration::from_millis(50));
        if let Some(thread) = self.listener_thread.take() {
            let _ = thread.join();
        }
    }
}

struct SessionGuard {
    connection_id: u64,
    active: Arc<AtomicUsize>,
    connections: ConnectionRegistry,
}

impl Drop for SessionGuard {
    fn drop(&mut self) {
        if let Ok(mut registry) = self.connections.lock() {
            registry.remove(&self.connection_id);
        }
        self.active.fetch_sub(1, Ordering::AcqRel);
    }
}

fn reap_finished_workers(workers: &mut Vec<JoinHandle<()>>) {
    let mut index = 0usize;
    while index < workers.len() {
        if workers[index].is_finished() {
            let worker = workers.swap_remove(index);
            let _ = worker.join();
        } else {
            index += 1;
        }
    }
}

#[derive(Debug)]
struct ConnectRequest {
    host: String,
    port: u16,
    token: Option<String>,
}

fn serve_connection(
    mut client: TcpStream,
    permitted: &PermittedOrigins,
    expected_token: &str,
    stop: &AtomicBool,
    connections: &ConnectionRegistry,
    connection_id: u64,
) -> Result<(), String> {
    let deadline = Instant::now() + HANDSHAKE_TIMEOUT;
    set_socket_deadline(&client, deadline)?;

    let head = read_request_head(&mut client, deadline)?;
    let request = parse_connect_head(&head)?;
    if request
        .token
        .as_deref()
        .map(|token| constant_time_eq(token.as_bytes(), expected_token.as_bytes()))
        != Some(true)
    {
        deny_before(&mut client, 407, "Proxy Authentication Required", deadline);
        return Err("missing or invalid resolver broker token".to_string());
    }
    if !permitted.permits(&request.host, request.port) {
        deny_before(&mut client, 403, "Forbidden", deadline);
        return Err(format!(
            "resolver destination is not approved: {}:{}",
            request.host, request.port
        ));
    }

    if stop.load(Ordering::Acquire) {
        return Err("resolver broker is shutting down".to_string());
    }
    let approved = match cached_origin_addresses(&request.host, request.port) {
        Some(approved) => approved,
        None => {
            let dns_deadline = deadline.min(Instant::now() + DNS_TIMEOUT);
            let resolved =
                resolve_with_deadline(request.host.clone(), request.port, dns_deadline, stop)?;
            let approved = approve_resolved_addresses(&resolved)?;
            remember_origin_addresses(&request.host, request.port, &approved);
            approved
        }
    };
    let mut upstream = connect_with_fallback(&approved, deadline, stop)?;
    if let Ok(clone) = upstream.try_clone() {
        let mut registry = connections
            .lock()
            .map_err(|_| "resolver connection registry was poisoned".to_string())?;
        if let Some(sockets) = registry.get_mut(&connection_id) {
            sockets.push(clone);
        }
    }
    write_all_before(
        &mut client,
        b"HTTP/1.1 200 Connection Established\r\n\r\n",
        deadline,
    )
    .map_err(|e| format!("could not acknowledge resolver tunnel: {e}"))?;
    let client_hello = read_client_hello(&mut client, deadline)?;
    let sni = extract_sni(&client_hello);
    if !sni_matches_host(sni.as_deref(), &request.host) {
        let _ = client.shutdown(Shutdown::Both);
        return Err("resolver tunnel TLS SNI did not match approved host".to_string());
    }
    write_all_before(&mut upstream, &client_hello, deadline)
        .map_err(|e| format!("could not forward resolver ClientHello: {e}"))?;

    // Only after every handshake-stage byte has crossed under the one absolute
    // deadline may both sockets transition to tunnel idle timeouts.
    upstream
        .set_read_timeout(Some(IDLE_TIMEOUT))
        .map_err(|e| e.to_string())?;
    upstream
        .set_write_timeout(Some(IDLE_TIMEOUT))
        .map_err(|e| e.to_string())?;
    client
        .set_read_timeout(Some(IDLE_TIMEOUT))
        .map_err(|e| e.to_string())?;
    client
        .set_write_timeout(Some(IDLE_TIMEOUT))
        .map_err(|e| e.to_string())?;
    tunnel(client, upstream);
    Ok(())
}

fn read_request_head(stream: &mut TcpStream, deadline: Instant) -> Result<Vec<u8>, String> {
    let mut bytes = Vec::with_capacity(1024);
    let mut byte = [0u8; 1];
    while bytes.len() <= MAX_REQUEST_HEAD {
        set_read_deadline(stream, deadline)?;
        let count = stream
            .read(&mut byte)
            .map_err(|e| format!("could not read resolver proxy request: {e}"))?;
        if count == 0 {
            return Err("resolver proxy request closed before headers".to_string());
        }
        bytes.push(byte[0]);
        if bytes.ends_with(b"\r\n\r\n") {
            return Ok(bytes);
        }
    }
    Err("resolver proxy request head exceeded limit".to_string())
}

fn parse_connect_head(head: &[u8]) -> Result<ConnectRequest, String> {
    let head = std::str::from_utf8(head)
        .map_err(|_| "resolver proxy request head was not UTF-8".to_string())?;
    let mut lines = head.split("\r\n");
    let request_line = lines
        .next()
        .ok_or_else(|| "empty resolver proxy request".to_string())?;
    let parts = request_line.split_whitespace().collect::<Vec<_>>();
    if parts.len() != 3 || !parts[0].eq_ignore_ascii_case("CONNECT") {
        return Err("resolver broker accepts CONNECT only".to_string());
    }
    let (host, port) = split_authority(parts[1])?;
    let mut token = None;
    for line in lines {
        let Some((name, value)) = line.split_once(':') else {
            continue;
        };
        if !name.trim().eq_ignore_ascii_case("proxy-authorization") {
            continue;
        }
        let value = value.trim();
        let Some(encoded) = value
            .strip_prefix("Basic ")
            .or_else(|| value.strip_prefix("basic "))
        else {
            continue;
        };
        let decoded = base64::engine::general_purpose::STANDARD
            .decode(encoded.trim())
            .map_err(|_| "invalid resolver proxy credentials".to_string())?;
        let decoded = std::str::from_utf8(&decoded)
            .map_err(|_| "invalid resolver proxy credentials".to_string())?;
        if let Some((username, password)) = decoded.split_once(':') {
            if username == "tirith" {
                token = Some(password.to_string());
            }
        }
    }
    Ok(ConnectRequest { host, port, token })
}

fn split_authority(authority: &str) -> Result<(String, u16), String> {
    let (host, port) = if let Some(rest) = authority.strip_prefix('[') {
        let (host, suffix) = rest
            .split_once(']')
            .ok_or_else(|| "malformed IPv6 CONNECT authority".to_string())?;
        let port = suffix
            .strip_prefix(':')
            .ok_or_else(|| "CONNECT authority has no port".to_string())?;
        (host, port)
    } else {
        authority
            .rsplit_once(':')
            .ok_or_else(|| "CONNECT authority has no port".to_string())?
    };
    if host.is_empty() {
        return Err("CONNECT authority has no host".to_string());
    }
    let port = port
        .parse::<u16>()
        .map_err(|_| "CONNECT authority has an invalid port".to_string())?;
    Ok((canonical_host(host), port))
}

fn approve_resolved_addresses(resolved: &[SocketAddr]) -> Result<Vec<SocketAddr>, String> {
    if resolved.is_empty() {
        return Err("resolver destination resolved to no addresses".to_string());
    }
    if resolved.len() > MAX_RESOLVED_ADDRESSES {
        return Err("resolver destination resolved to too many addresses".to_string());
    }
    let approved = resolved
        .iter()
        .copied()
        .filter(crate::url_validate::is_public_addr)
        .collect::<Vec<_>>();
    if approved.is_empty() {
        Err("resolver destination resolved only to non-global addresses".to_string())
    } else {
        Ok(approved)
    }
}

fn read_client_hello(stream: &mut TcpStream, deadline: Instant) -> Result<Vec<u8>, String> {
    let mut header = [0u8; 5];
    read_exact_before(stream, &mut header, deadline)
        .map_err(|e| format!("could not read resolver TLS header: {e}"))?;
    if header[0] != 22 {
        return Err("resolver tunnel was not TLS".to_string());
    }
    let record_len = u16::from_be_bytes([header[3], header[4]]) as usize;
    if record_len == 0 || record_len > MAX_CLIENT_HELLO {
        return Err("resolver TLS ClientHello exceeded limit".to_string());
    }
    let mut bytes = Vec::with_capacity(5 + record_len);
    bytes.extend_from_slice(&header);
    bytes.resize(5 + record_len, 0);
    read_exact_before(stream, &mut bytes[5..], deadline)
        .map_err(|e| format!("could not read resolver TLS ClientHello: {e}"))?;
    Ok(bytes)
}

fn set_socket_deadline(stream: &TcpStream, deadline: Instant) -> Result<(), String> {
    let remaining = remaining_until(deadline)?;
    stream
        .set_read_timeout(Some(remaining))
        .map_err(|error| error.to_string())?;
    stream
        .set_write_timeout(Some(remaining))
        .map_err(|error| error.to_string())
}

fn set_read_deadline(stream: &TcpStream, deadline: Instant) -> Result<(), String> {
    stream
        .set_read_timeout(Some(remaining_until(deadline)?))
        .map_err(|error| error.to_string())
}

fn remaining_until(deadline: Instant) -> Result<Duration, String> {
    deadline
        .checked_duration_since(Instant::now())
        .filter(|remaining| !remaining.is_zero())
        .ok_or_else(|| "resolver broker handshake deadline exceeded".to_string())
}

fn read_exact_before(
    stream: &mut TcpStream,
    mut output: &mut [u8],
    deadline: Instant,
) -> Result<(), std::io::Error> {
    while !output.is_empty() {
        let remaining = deadline
            .checked_duration_since(Instant::now())
            .filter(|remaining| !remaining.is_zero())
            .ok_or_else(|| std::io::Error::new(std::io::ErrorKind::TimedOut, "deadline"))?;
        stream.set_read_timeout(Some(remaining))?;
        let count = stream.read(output)?;
        if count == 0 {
            return Err(std::io::Error::new(
                std::io::ErrorKind::UnexpectedEof,
                "connection closed",
            ));
        }
        output = &mut output[count..];
    }
    Ok(())
}

fn write_all_before(
    stream: &mut TcpStream,
    mut input: &[u8],
    deadline: Instant,
) -> Result<(), std::io::Error> {
    while !input.is_empty() {
        let remaining = deadline
            .checked_duration_since(Instant::now())
            .filter(|remaining| !remaining.is_zero())
            .ok_or_else(|| std::io::Error::new(std::io::ErrorKind::TimedOut, "deadline"))?;
        stream.set_write_timeout(Some(remaining))?;
        let count = stream.write(input)?;
        if count == 0 {
            return Err(std::io::Error::new(
                std::io::ErrorKind::WriteZero,
                "connection accepted no bytes",
            ));
        }
        input = &input[count..];
    }
    Ok(())
}

fn resolve_with_deadline(
    host: String,
    port: u16,
    deadline: Instant,
    stop: &AtomicBool,
) -> Result<Vec<SocketAddr>, String> {
    resolve_with_deadline_using(
        host,
        port,
        deadline,
        stop,
        &ACTIVE_DNS_LOOKUPS,
        |host, port| {
            (host.as_str(), port)
                .to_socket_addrs()
                .map(|addresses| {
                    addresses
                        .take(MAX_RESOLVED_ADDRESSES + 1)
                        .collect::<Vec<_>>()
                })
                .map_err(|error| format!("resolver destination lookup failed: {error}"))
        },
    )
}

struct DnsSlotGuard(&'static AtomicUsize);

impl Drop for DnsSlotGuard {
    fn drop(&mut self) {
        self.0.fetch_sub(1, Ordering::AcqRel);
    }
}

fn resolve_with_deadline_using<F>(
    host: String,
    port: u16,
    deadline: Instant,
    stop: &AtomicBool,
    slots: &'static AtomicUsize,
    lookup: F,
) -> Result<Vec<SocketAddr>, String>
where
    F: FnOnce(String, u16) -> Result<Vec<SocketAddr>, String> + Send + 'static,
{
    reserve_dns_slot(slots)?;
    let (sender, receiver) = mpsc::sync_channel(1);
    let spawn_result = std::thread::Builder::new()
        .name("tirith-resolver-dns".to_string())
        .spawn(move || {
            let _slot = DnsSlotGuard(slots);
            let result = lookup(host, port);
            let _ = sender.send(result);
        });
    if let Err(error) = spawn_result {
        slots.fetch_sub(1, Ordering::AcqRel);
        return Err(format!("could not start resolver DNS worker: {error}"));
    }
    loop {
        if stop.load(Ordering::Acquire) {
            return Err("resolver broker shut down during DNS lookup".to_string());
        }
        let wait = remaining_until(deadline)?.min(Duration::from_millis(50));
        match receiver.recv_timeout(wait) {
            Ok(result) => return result,
            Err(mpsc::RecvTimeoutError::Timeout) => continue,
            Err(mpsc::RecvTimeoutError::Disconnected) => {
                return Err("resolver DNS worker stopped without a result".to_string())
            }
        }
    }
}

fn reserve_dns_slot(slots: &AtomicUsize) -> Result<(), String> {
    slots
        .fetch_update(Ordering::AcqRel, Ordering::Acquire, |count| {
            (count < MAX_CONCURRENT_DNS_LOOKUPS).then_some(count + 1)
        })
        .map(|_| ())
        .map_err(|_| "resolver DNS worker capacity exhausted".to_string())
}

fn connect_with_fallback(
    addresses: &[SocketAddr],
    deadline: Instant,
    stop: &AtomicBool,
) -> Result<TcpStream, String> {
    let mut last_error = None;
    loop {
        for address in addresses {
            if stop.load(Ordering::Acquire) {
                return Err("resolver broker shut down during destination connect".to_string());
            }
            let attempt_timeout = remaining_until(deadline)?.min(Duration::from_millis(500));
            match TcpStream::connect_timeout(address, attempt_timeout) {
                Ok(stream) => return Ok(stream),
                Err(error) => last_error = Some(error),
            }
        }
        if Instant::now() >= deadline {
            break;
        }
        // A refused port returns in microseconds, so retrying without a pause
        // burns a core and floods the destination with SYNs until the deadline.
        let backoff = Duration::from_millis(20).min(remaining_until(deadline)?);
        std::thread::sleep(backoff);
    }
    Err(format!(
        "resolver destination connect failed for every approved address: {}",
        last_error
            .map(|error| error.to_string())
            .unwrap_or_else(|| "no address".to_string())
    ))
}

fn tunnel(client: TcpStream, upstream: TcpStream) {
    let Ok(mut client_reader) = client.try_clone() else {
        return;
    };
    let Ok(mut upstream_writer) = upstream.try_clone() else {
        return;
    };
    let up = std::thread::spawn(move || {
        let outcome = copy_capped(&mut client_reader, &mut upstream_writer);
        // A clean write-shutdown after a capped copy would look like a complete
        // body to the peer. Abort instead, so the transfer visibly fails.
        let _ = match outcome {
            CopyOutcome::Complete => upstream_writer.shutdown(Shutdown::Write),
            CopyOutcome::CapReached => upstream_writer.shutdown(Shutdown::Both),
        };
    });
    let mut upstream_reader = upstream;
    let mut client_writer = client;
    let outcome = copy_capped(&mut upstream_reader, &mut client_writer);
    let _ = match outcome {
        CopyOutcome::Complete => client_writer.shutdown(Shutdown::Write),
        CopyOutcome::CapReached => {
            eprintln!(
                "tirith: resolver broker aborted a session at its {} MiB per-direction limit; \
                 the artifact was NOT truncated into the install",
                MAX_TUNNEL_BYTES_PER_DIRECTION / (1024 * 1024)
            );
            client_writer.shutdown(Shutdown::Both)
        }
    };
    let _ = up.join();
}

/// Whether a proxied copy ended on its own or ran into the per-direction cap.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum CopyOutcome {
    Complete,
    CapReached,
}

fn copy_capped(reader: &mut TcpStream, writer: &mut TcpStream) -> CopyOutcome {
    let mut remaining = MAX_TUNNEL_BYTES_PER_DIRECTION;
    let mut buffer = [0u8; 16 * 1024];
    while remaining > 0 {
        let take = usize::try_from(remaining.min(buffer.len() as u64)).unwrap_or(buffer.len());
        let count = match reader.read(&mut buffer[..take]) {
            Ok(0) | Err(_) => return CopyOutcome::Complete,
            Ok(count) => count,
        };
        if writer.write_all(&buffer[..count]).is_err() {
            return CopyOutcome::Complete;
        }
        remaining -= count as u64;
    }
    // The reader still had bytes to give when the budget ran out.
    CopyOutcome::CapReached
}

fn deny(stream: &mut TcpStream, status: u16, reason: &str) {
    deny_before(
        stream,
        status,
        reason,
        Instant::now() + Duration::from_millis(250),
    );
}

fn deny_before(stream: &mut TcpStream, status: u16, reason: &str, deadline: Instant) {
    let response =
        format!("HTTP/1.1 {status} {reason}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
    let _ = write_all_before(stream, response.as_bytes(), deadline);
    // FIN the response direction after the full status is queued. An immediate
    // `Shutdown::Both` can turn a deterministic 403/407 into a client-side RST.
    let _ = stream.shutdown(Shutdown::Write);
    // Keep the read half alive just long enough for the peer to observe that FIN
    // and close its side. Dropping a socket with unread peer data can still turn
    // the queued response into ECONNRESET on macOS. This drain is absolutely
    // bounded in both time and bytes, so a hostile loopback peer cannot pin the
    // listener/session worker.
    let mut drained = 0usize;
    let mut buffer = [0u8; 512];
    while drained < MAX_REQUEST_HEAD {
        let Some(remaining) = deadline.checked_duration_since(Instant::now()) else {
            break;
        };
        if remaining.is_zero() || stream.set_read_timeout(Some(remaining)).is_err() {
            break;
        }
        match stream.read(&mut buffer) {
            Ok(0) | Err(_) => break,
            Ok(count) => drained += count,
        }
    }
}

fn canonical_host(host: &str) -> String {
    host.trim_matches(['[', ']'])
        .trim_end_matches('.')
        .to_ascii_lowercase()
}

fn canonical_url_host(url: &url::Url) -> Result<String, String> {
    match url.host() {
        Some(url::Host::Domain(domain)) => Ok(canonical_host(domain)),
        Some(url::Host::Ipv4(address)) => Ok(address.to_string()),
        Some(url::Host::Ipv6(address)) => Ok(address.to_string()),
        None => Err(format!("URL has no host: {url}")),
    }
}

fn constant_time_eq(left: &[u8], right: &[u8]) -> bool {
    let mut difference = left.len() ^ right.len();
    let length = left.len().max(right.len());
    for index in 0..length {
        difference |= usize::from(
            left.get(index).copied().unwrap_or(0) ^ right.get(index).copied().unwrap_or(0),
        );
    }
    difference == 0
}

fn extract_sni(bytes: &[u8]) -> Option<String> {
    if bytes.len() < 9 || bytes[0] != 22 {
        return None;
    }
    let record_len = u16::from_be_bytes([bytes[3], bytes[4]]) as usize;
    let record = bytes.get(5..5 + record_len)?;
    if record.len() < 4 || record[0] != 1 {
        return None;
    }
    let hello_len = ((record[1] as usize) << 16) | ((record[2] as usize) << 8) | record[3] as usize;
    let hello = record.get(4..4 + hello_len)?;
    let mut cursor = 34usize;
    let session_len = *hello.get(cursor)? as usize;
    cursor = cursor.checked_add(1 + session_len)?;
    let cipher_len = u16::from_be_bytes([*hello.get(cursor)?, *hello.get(cursor + 1)?]) as usize;
    cursor = cursor.checked_add(2 + cipher_len)?;
    let compression_len = *hello.get(cursor)? as usize;
    cursor = cursor.checked_add(1 + compression_len)?;
    let extensions_len =
        u16::from_be_bytes([*hello.get(cursor)?, *hello.get(cursor + 1)?]) as usize;
    cursor += 2;
    let extensions_end = cursor.checked_add(extensions_len)?;
    if extensions_end > hello.len() {
        return None;
    }
    while cursor + 4 <= extensions_end {
        let kind = u16::from_be_bytes([hello[cursor], hello[cursor + 1]]);
        let length = u16::from_be_bytes([hello[cursor + 2], hello[cursor + 3]]) as usize;
        cursor += 4;
        let data = hello.get(cursor..cursor.checked_add(length)?)?;
        if kind == 0 {
            return parse_server_name(data);
        }
        cursor += length;
    }
    None
}

fn parse_server_name(data: &[u8]) -> Option<String> {
    let list_len = u16::from_be_bytes([*data.first()?, *data.get(1)?]) as usize;
    let list = data.get(2..2 + list_len)?;
    let mut cursor = 0usize;
    while cursor + 3 <= list.len() {
        let kind = list[cursor];
        let length = u16::from_be_bytes([list[cursor + 1], list[cursor + 2]]) as usize;
        cursor += 3;
        let name = list.get(cursor..cursor.checked_add(length)?)?;
        if kind == 0 {
            return std::str::from_utf8(name).ok().map(canonical_host);
        }
        cursor += length;
    }
    None
}

fn sni_matches_host(sni: Option<&str>, host: &str) -> bool {
    if canonical_host(host).parse::<IpAddr>().is_ok() {
        return true;
    }
    sni.map(canonical_host).as_deref() == Some(canonical_host(host).as_str())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn origins() -> PermittedOrigins {
        PermittedOrigins::from_urls(&[
            url::Url::parse("https://INDEX.Example.:443/simple").unwrap(),
            url::Url::parse("https://artifacts.example/files").unwrap(),
        ])
        .unwrap()
    }

    #[test]
    fn canonical_index_aliases_map_to_one_origin() {
        let origins = origins();
        assert!(origins.permits("index.example", 443));
        assert!(origins.permits("INDEX.EXAMPLE.", 443));
        assert!(!origins.permits("index.example", 8443));
    }

    #[test]
    fn redirect_destination_must_be_separately_permitted() {
        let origins = origins();
        assert!(origins.permits("artifacts.example", 443));
        assert!(!origins.permits("redirect.attacker.example", 443));
        assert!(!origins.permits("127.0.0.1", 443));
    }

    #[test]
    fn pypi_adds_only_its_exact_artifact_cdn_origin() {
        let origins = PermittedOrigins::from_urls(&[url::Url::parse(
            "https://PYPI.ORG.:443/simple",
        )
        .unwrap()])
        .unwrap();
        assert!(origins.permits("pypi.org", 443));
        assert!(origins.permits("files.pythonhosted.org", 443));
        assert!(!origins.permits("evil.pythonhosted.org", 443));
        assert!(!origins.permits("files.pythonhosted.org.attacker.test", 443));
        assert!(!origins.permits("files.pythonhosted.org", 8443));
    }

    #[test]
    fn typed_ipv6_hosts_are_bracket_independent() {
        let url = url::Url::parse("https://[2607:f8b0:4004:800::200e]/simple").unwrap();
        let origins = PermittedOrigins::from_urls(&[url]).unwrap();
        assert!(origins.permits("2607:f8b0:4004:800::200e", 443));
        assert!(origins.permits("[2607:f8b0:4004:800::200e]", 443));
    }

    #[test]
    fn connect_parser_accepts_authenticated_canonical_host() {
        let encoded = base64::engine::general_purpose::STANDARD.encode("tirith:secret");
        let head = format!(
            "CONNECT INDEX.Example.:443 HTTP/1.1\r\nProxy-Authorization: Basic {encoded}\r\n\r\n"
        );
        let request = parse_connect_head(head.as_bytes()).unwrap();
        assert_eq!(request.host, "index.example");
        assert_eq!(request.port, 443);
        assert_eq!(request.token.as_deref(), Some("secret"));
    }

    #[test]
    fn connect_time_address_filter_rejects_private_and_metadata() {
        for address in ["127.0.0.1:443", "10.0.0.1:443", "169.254.169.254:443"] {
            let address = address.parse::<SocketAddr>().unwrap();
            assert!(approve_resolved_addresses(&[address]).is_err(), "{address}");
        }
    }

    #[test]
    fn address_filter_retains_every_global_fallback() {
        let addresses = [
            "10.0.0.1:443".parse().unwrap(),
            "93.184.216.34:443".parse().unwrap(),
            "8.8.8.8:443".parse().unwrap(),
        ];
        assert_eq!(
            approve_resolved_addresses(&addresses).unwrap(),
            vec![addresses[1], addresses[2]]
        );
    }

    #[test]
    fn connector_falls_back_after_an_unreachable_address() {
        let closed = TcpListener::bind("127.0.0.1:0").unwrap();
        let closed_address = closed.local_addr().unwrap();
        drop(closed);
        let reachable = TcpListener::bind("127.0.0.1:0").unwrap();
        let reachable_address = reachable.local_addr().unwrap();
        let connected = connect_with_fallback(
            &[closed_address, reachable_address],
            Instant::now() + Duration::from_secs(2),
            &AtomicBool::new(false),
        )
        .unwrap();
        assert_eq!(connected.peer_addr().unwrap(), reachable_address);
    }

    #[test]
    fn dns_worker_pool_is_bounded_before_lookup() {
        let slots = AtomicUsize::new(MAX_CONCURRENT_DNS_LOOKUPS);
        let error = reserve_dns_slot(&slots).unwrap_err();
        assert!(error.contains("capacity"), "{error}");
    }

    #[test]
    fn timed_out_dns_workers_remain_process_globally_bounded() {
        static TEST_SLOTS: AtomicUsize = AtomicUsize::new(0);
        assert_eq!(TEST_SLOTS.swap(0, Ordering::AcqRel), 0);
        let release = Arc::new((std::sync::Mutex::new(false), std::sync::Condvar::new()));
        let stop = AtomicBool::new(false);

        for _ in 0..MAX_CONCURRENT_DNS_LOOKUPS {
            let worker_release = Arc::clone(&release);
            let error = resolve_with_deadline_using(
                "blocked.example".to_string(),
                443,
                Instant::now() + Duration::from_millis(20),
                &stop,
                &TEST_SLOTS,
                move |_, _| {
                    let (lock, condition) = &*worker_release;
                    let mut released = lock.lock().unwrap();
                    while !*released {
                        released = condition.wait(released).unwrap();
                    }
                    Ok(Vec::new())
                },
            )
            .unwrap_err();
            assert!(error.contains("deadline"), "{error}");
        }
        assert_eq!(
            TEST_SLOTS.load(Ordering::Acquire),
            MAX_CONCURRENT_DNS_LOOKUPS
        );
        let exhausted = resolve_with_deadline_using(
            "blocked.example".to_string(),
            443,
            Instant::now() + Duration::from_millis(20),
            &stop,
            &TEST_SLOTS,
            |_, _| Ok(Vec::new()),
        )
        .unwrap_err();
        assert!(exhausted.contains("capacity"), "{exhausted}");

        let (lock, condition) = &*release;
        *lock.lock().unwrap() = true;
        condition.notify_all();
        let cleanup_deadline = Instant::now() + Duration::from_secs(1);
        while TEST_SLOTS.load(Ordering::Acquire) != 0 && Instant::now() < cleanup_deadline {
            std::thread::yield_now();
        }
        assert_eq!(TEST_SLOTS.load(Ordering::Acquire), 0);
    }

    #[test]
    fn resolver_broker_denies_unapproved_connect_before_dns() {
        // Repeat to exercise the FIN/read-close ordering that otherwise becomes
        // a scheduler-sensitive ECONNRESET on some platforms.
        for _ in 0..8 {
            let broker = ResolverBroker::start(origins()).unwrap();
            let mut stream = TcpStream::connect(broker.address).unwrap();
            let encoded = base64::engine::general_purpose::STANDARD
                .encode(format!("tirith:{}", broker.token));
            write!(
                stream,
                "CONNECT unapproved.invalid:443 HTTP/1.1\r\nProxy-Authorization: Basic {encoded}\r\n\r\n"
            )
            .unwrap();
            let mut response = String::new();
            stream.read_to_string(&mut response).unwrap();
            assert!(response.starts_with("HTTP/1.1 403"), "{response:?}");
        }
    }

    #[test]
    fn broker_drop_cancels_partial_unauthenticated_session() {
        let broker = ResolverBroker::start(origins()).unwrap();
        let mut stream = TcpStream::connect(broker.address).unwrap();
        stream.write_all(b"CON").unwrap();
        let started = Instant::now();
        drop(broker);
        assert!(
            started.elapsed() < Duration::from_secs(2),
            "drop must cancel and join partial sessions promptly"
        );
    }

    #[test]
    fn approved_origin_addresses_are_reused_and_scoped_to_one_broker() {
        // Parallel sessions to an already-resolved origin must not each take a
        // DNS worker slot, and one broker's answers must not survive into the
        // next.
        forget_all_origin_addresses();
        let approved = vec![SocketAddr::from(([93, 184, 216, 34], 443))];
        remember_origin_addresses("example.com", 443, &approved);
        assert_eq!(
            cached_origin_addresses("example.com", 443),
            Some(approved.clone()),
            "a resolved origin is reused without another lookup"
        );
        assert_eq!(
            cached_origin_addresses("example.com", 8443),
            None,
            "the port is part of the key"
        );
        assert_eq!(
            cached_origin_addresses("other.example", 443),
            None,
            "the host is part of the key"
        );

        forget_all_origin_addresses();
        assert_eq!(
            cached_origin_addresses("example.com", 443),
            None,
            "a new broker must not inherit the previous run's addresses"
        );
    }
}
