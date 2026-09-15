// Realistic reproduction of David's `received fatal alert: BadRecordMac`.
//
// Architecture (mimics a corrupting middlebox / flaky VPN):
//
//   reqwest client (rustls)  ->  corrupting TCP proxy  ->  real TLS server (rustls)
//
// The proxy flips one byte inside the first large client->server TLS
// application_data record (the HTTP request body). The server's AEAD
// authentication then fails, so it returns a TLS `bad_record_mac` fatal alert,
// which the client surfaces as: "received fatal alert: BadRecordMac".
//
// We then wrap that error exactly like jcode's anthropic provider does
// (`.context("Failed to send request to Anthropic API")`) and show how the
// cause is masked from `to_string()` but visible via the error source chain.

use std::sync::Arc;

use anyhow::Context;
use rustls::pki_types::{CertificateDer, PrivateKeyDer};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpListener, TcpStream};
use tokio_rustls::TlsAcceptor;

fn classify(error_str: &str) -> bool {
    // VERBATIM copy of jcode's shared is_transient_transport_error
    // (crates/jcode-base/src/provider/routing.rs @ v0.24.0).
    let lower = error_str.to_ascii_lowercase();
    lower.contains("connection reset")
        || lower.contains("connection closed")
        || lower.contains("connection refused")
        || lower.contains("connection aborted")
        || lower.contains("broken pipe")
        || lower.contains("timed out")
        || lower.contains("timeout")
        || lower.contains("operation timed out")
        || lower.contains("error decoding")
        || lower.contains("error reading")
        || lower.contains("unexpected eof")
        || lower.contains("tls handshake eof")
        || lower.contains("badrecordmac")
        || lower.contains("bad_record_mac")
        || lower.contains("fatal alert: badrecordmac")
        || lower.contains("fatal alert: bad_record_mac")
        || lower.contains("received fatal alert: badrecordmac")
        || lower.contains("received fatal alert: bad_record_mac")
        || lower.contains("decryption failed or bad record mac")
        || lower.contains("temporary failure in name resolution")
        || lower.contains("failed to lookup address information")
        || lower.contains("dns error")
        || lower.contains("name or service not known")
        || lower.contains("no route to host")
        || lower.contains("network is unreachable")
        || lower.contains("host is unreachable")
        || lower.contains("http2 error")
        || lower.contains("stream error")
        || lower.contains("protocol error")
        || lower.contains("refused_stream")
        || lower.contains("refused stream")
        || lower.contains("enhance_your_calm")
        || lower.contains("goaway")
        || lower.contains("go away")
        || lower.contains("sendrequest")
}

/// OLD OpenAI is_retryable_error (pre-fix): standalone list, NO TLS terms,
/// does NOT call the shared classifier. This is what shipped in v0.24.0 and
/// caused David's BadRecordMac to fail immediately.
fn openai_classify_old(error_str: &str) -> bool {
    error_str.contains("connection reset")
        || error_str.contains("connection closed")
        || error_str.contains("connection refused")
        || error_str.contains("broken pipe")
        || error_str.contains("timed out")
        || error_str.contains("timeout")
        || error_str.contains("failed to send request to openai api")
        || error_str.contains("error decoding")
        || error_str.contains("error reading")
        || error_str.contains("unexpected eof")
        || error_str.contains("incomplete message")
        || error_str.contains("stream disconnected before completion")
        || error_str.contains("ended before message completion marker")
        || error_str.contains("falling back from websockets to https transport")
        || error_str.contains("500 internal server error")
        || error_str.contains("502 bad gateway")
        || error_str.contains("503 service unavailable")
        || error_str.contains("504 gateway timeout")
        || error_str.contains("overloaded")
        || error_str.contains("api_error")
        || error_str.contains("server_error")
        || error_str.contains("internal server error")
        || error_str.contains("an error occurred while processing your request")
        || error_str.contains("please include the request id")
}

/// NEW OpenAI is_retryable_error (post-fix): delegates to the shared classifier.
fn openai_classify_new(error_str: &str) -> bool {
    classify(error_str) || openai_classify_old(error_str)
}

/// Walk the full anyhow error source chain and classify on the joined text.
fn classify_chain(err: &anyhow::Error) -> bool {
    // 1) alternate Display includes the cause chain
    if classify(&format!("{err:#}")) {
        return true;
    }
    // 2) explicit chain walk (most robust)
    for cause in err.chain() {
        if classify(&cause.to_string()) {
            return true;
        }
    }
    false
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let _ = rustls::crypto::ring::default_provider().install_default();

    // ---- 1. Self-signed cert for 127.0.0.1 ----
    let cert = rcgen::generate_simple_self_signed(vec!["127.0.0.1".to_string()])?;
    let cert_der = CertificateDer::from(cert.cert.der().to_vec());
    let key_der = PrivateKeyDer::try_from(cert.key_pair.serialize_der())
        .map_err(|e| anyhow::anyhow!("key: {e}"))?;

    let server_config = rustls::ServerConfig::builder()
        .with_no_client_auth()
        .with_single_cert(vec![cert_der.clone()], key_der)?;
    let acceptor = TlsAcceptor::from(Arc::new(server_config));

    // ---- 2. Real TLS server ----
    let server_listener = TcpListener::bind("127.0.0.1:0").await?;
    let server_addr = server_listener.local_addr()?;
    tokio::spawn(async move {
        loop {
            let Ok((tcp, _)) = server_listener.accept().await else {
                continue;
            };
            let acceptor = acceptor.clone();
            tokio::spawn(async move {
                match acceptor.accept(tcp).await {
                    Ok(mut tls) => {
                        // Try to read the request; AEAD failure shows up here and
                        // rustls automatically emits a bad_record_mac fatal alert.
                        let mut buf = [0u8; 4096];
                        let _ = tls.read(&mut buf).await;
                        let _ = tls
                            .write_all(b"HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok")
                            .await;
                        let _ = tls.shutdown().await;
                    }
                    Err(e) => {
                        eprintln!("[server] handshake/accept error: {e}");
                    }
                }
            });
        }
    });

    // ---- 3. Corrupting proxy ----
    let proxy_listener = TcpListener::bind("127.0.0.1:0").await?;
    let proxy_addr = proxy_listener.local_addr()?;
    tokio::spawn(async move {
        loop {
            let Ok((client, _)) = proxy_listener.accept().await else {
                continue;
            };
            tokio::spawn(async move {
                if let Err(e) = corrupt_proxy(client, server_addr).await {
                    eprintln!("[proxy] {e}");
                }
            });
        }
    });

    // ---- 4. Client (mirrors jcode's reqwest+rustls config) ----
    let mut roots = rustls::RootCertStore::empty();
    roots.add(cert_der)?;
    let client = reqwest::Client::builder()
        .use_preconfigured_tls(
            rustls::ClientConfig::builder()
                .with_root_certificates(roots)
                .with_no_client_auth(),
        )
        .build()?;

    let url = format!("https://127.0.0.1:{}/v1/messages", proxy_addr.port());
    let body = serde_json_body();
    let result = client
        .post(&url)
        .header("content-type", "application/json")
        .header("x-api-key", "sk-test-1234567890")
        .body(body)
        .send()
        .await;

    let raw = match &result {
        Ok(_) => "<no error: request unexpectedly succeeded>".to_string(),
        Err(e) => e.to_string(),
    };
    println!("=== Reproduction result ===");
    println!("raw reqwest to_string : {raw:?}");

    // Wrap exactly like the anthropic provider does.
    let wrapped: anyhow::Result<()> = result
        .map(|_| ())
        .context("Failed to send request to Anthropic API");
    if let Err(err) = wrapped {
        let masked = err.to_string();
        let alt = format!("{err:#}");
        println!("provider to_string()  : {masked:?}");
        println!("provider {{:#}} (chain) : {alt:?}");
        println!();

        let reproduced = alt.to_lowercase().contains("badrecordmac")
            || raw.to_lowercase().contains("badrecordmac");
        println!("REPRODUCED BadRecordMac: {reproduced}");
        println!();

        // Anthropic-style WS/HTTP error where the cause is INLINE in the message
        // (this is what David actually saw from the OpenAI websocket path).
        let openai_inline = "stream error: io error: received fatal alert: badrecordmac";

        println!("=== OpenAI websocket path (inline error, as David saw) ===");
        println!("  error string: {openai_inline:?}");
        println!("  OLD is_retryable = {}  (immediate fail, will_retry=false)",
            openai_classify_old(openai_inline));
        println!("  NEW is_retryable = {}  (retried)", openai_classify_new(openai_inline));
        println!();

        println!("=== Send path with anyhow .context() masking ===");
        println!("  to_string (masked) -> shared classify = {}", classify(&masked));
        println!("  {{:#}} chain         -> shared classify = {}", classify(&alt));
        println!("  chain-aware walk    -> classify        = {}", classify_chain(&err));
        println!();

        let bug_confirmed = !openai_classify_old(openai_inline)
            && openai_classify_new(openai_inline)
            && !classify(&masked)
            && classify_chain(&err);
        println!("BUG CONFIRMED (old misses, fix catches; masking confirmed): {bug_confirmed}");
        if reproduced && bug_confirmed {
            println!("\nSUCCESS: realistic BadRecordMac reproduced and both fixes validated.");
            std::process::exit(0);
        } else {
            println!("\nINCOMPLETE: reproduced={reproduced} bug_confirmed={bug_confirmed}");
            std::process::exit(1);
        }
    }

    Ok(())
}

fn serde_json_body() -> Vec<u8> {
    // Large enough that the HTTP request becomes a sizable application_data record.
    let payload = format!(
        "{{\"model\":\"claude-opus-4\",\"messages\":[{{\"role\":\"user\",\"content\":\"{}\"}}]}}",
        "x".repeat(800)
    );
    payload.into_bytes()
}

/// TCP proxy that forwards both directions but flips a byte inside the first
/// large client->server TLS application_data record, corrupting the ciphertext.
async fn corrupt_proxy(client: TcpStream, server_addr: std::net::SocketAddr) -> anyhow::Result<()> {
    let server = TcpStream::connect(server_addr).await?;
    let (mut cr, mut cw) = client.into_split();
    let (mut sr, mut sw) = server.into_split();

    // server -> client: pass through untouched
    let s2c = tokio::spawn(async move {
        let mut buf = [0u8; 8192];
        loop {
            match sr.read(&mut buf).await {
                Ok(0) | Err(_) => break,
                Ok(n) => {
                    if cw.write_all(&buf[..n]).await.is_err() {
                        break;
                    }
                }
            }
        }
    });

    // client -> server: parse TLS records, corrupt first big application_data
    let c2s = tokio::spawn(async move {
        let mut acc: Vec<u8> = Vec::new();
        let mut buf = [0u8; 8192];
        let mut corrupted = false;
        loop {
            match cr.read(&mut buf).await {
                Ok(0) | Err(_) => break,
                Ok(n) => {
                    acc.extend_from_slice(&buf[..n]);
                    // Emit complete TLS records, corrupting as configured.
                    while acc.len() >= 5 {
                        let content_type = acc[0];
                        let len = ((acc[3] as usize) << 8) | (acc[4] as usize);
                        let total = 5 + len;
                        if acc.len() < total {
                            break;
                        }
                        let mut rec: Vec<u8> = acc.drain(..total).collect();
                        // application_data == 23; corrupt the first big one (the
                        // HTTP request), flipping a ciphertext byte so the server
                        // AEAD check fails -> bad_record_mac alert.
                        if !corrupted && content_type == 23 && len > 80 {
                            let mid = 5 + len / 2;
                            rec[mid] ^= 0xff;
                            corrupted = true;
                            eprintln!("[proxy] corrupted application_data record ({len} bytes)");
                        }
                        if sw.write_all(&rec).await.is_err() {
                            return;
                        }
                    }
                }
            }
        }
        let _ = sw.write_all(&acc).await;
    });

    let _ = tokio::join!(s2c, c2s);
    Ok(())
}
