use std::sync::Arc;
use std::collections::HashMap;
use std::sync::atomic::{Ordering, AtomicUsize};

use rocket::http::uri::Host;
use rocket::tls::{Resolver, TlsConfig, ClientHello, ServerConfig};
use reqwest::tls::TlsInfo;

use crate::prelude::*;

static SNI_TLS_CONFIG: &str = r#"
    [default.tls]
    certs = "{ROCKET}/examples/tls/private/rsa_sha256_cert.pem"
    key = "{ROCKET}/examples/tls/private/rsa_sha256_key.pem"

    [default.tls.sni."sni1.dev"]
    certs = "{ROCKET}/examples/tls/private/ecdsa_nistp256_sha256_cert.pem"
    key = "{ROCKET}/examples/tls/private/ecdsa_nistp256_sha256_key_pkcs8.pem"

    [default.tls.sni."sni2.dev"]
    certs = "{ROCKET}/examples/tls/private/ed25519_cert.pem"
    key = "{ROCKET}/examples/tls/private/ed25519_key.pem"
"#;

struct SniResolver {
    default: Arc<ServerConfig>,
    map: HashMap<Host<'static>, Arc<ServerConfig>>
}

#[rocket::async_trait]
impl Resolver for SniResolver {
    async fn init(rocket: &Rocket<Build>) -> rocket::tls::Result<Self> {
        let default: TlsConfig = rocket.figment().extract_inner("tls")?;
        let sni: HashMap<Host<'_>, TlsConfig> = rocket.figment().extract_inner("tls.sni")?;

        let default = Arc::new(default.server_config().await?);
        let mut map = HashMap::new();
        for (host, config) in sni {
            let config = config.server_config().await?;
            map.insert(host, Arc::new(config));
        }

        Ok(SniResolver { default, map })
    }

    async fn resolve(&self, hello: ClientHello<'_>) -> Option<Arc<ServerConfig>> {
        if let Some(Ok(host)) = hello.server_name().map(Host::parse) {
            if let Some(config) = self.map.get(&host) {
                return Some(config.clone());
            }
        }

        Some(self.default.clone())
    }
}

fn sni_resolver() -> Result<()> {
    let server = spawn! {
        #[get("/")] fn index() { }

        Rocket::default()
            .reconfigure_with_toml(SNI_TLS_CONFIG)
            .mount("/", routes![index])
            .attach(SniResolver::fairing())
    }?;

    let client: Client = Client::build()
        .resolve("unknown.dev", server.socket_addr())
        .resolve("sni1.dev", server.socket_addr())
        .resolve("sni2.dev", server.socket_addr())
        .try_into()?;

    let response = client.get(&server, "https://unknown.dev")?.send()?;
    let tls = response.extensions().get::<TlsInfo>().unwrap();
    let expected = cert("{ROCKET}/examples/tls/private/rsa_sha256_cert.pem")?;
    assert_eq!(tls.peer_certificate().unwrap(), expected);

    let response = client.get(&server, "https://sni1.dev")?.send()?;
    let tls = response.extensions().get::<TlsInfo>().unwrap();
    let expected = cert("{ROCKET}/examples/tls/private/ecdsa_nistp256_sha256_cert.pem")?;
    assert_eq!(tls.peer_certificate().unwrap(), expected);

    let response = client.get(&server, "https://sni2.dev")?.send()?;
    let tls = response.extensions().get::<TlsInfo>().unwrap();
    let expected = cert("{ROCKET}/examples/tls/private/ed25519_cert.pem")?;
    assert_eq!(tls.peer_certificate().unwrap(), expected);
    Ok(())
}

struct CountingResolver {
    config: Arc<ServerConfig>,
    counter: Arc<AtomicUsize>,
}

#[rocket::async_trait]
impl Resolver for CountingResolver {
    async fn init(rocket: &Rocket<Build>) -> rocket::tls::Result<Self> {
        let config: TlsConfig = rocket.figment().extract_inner("tls")?;
        let config = Arc::new(config.server_config().await?);
        let counter = rocket.state::<Arc<AtomicUsize>>().unwrap().clone();
        Ok(Self { config, counter })
    }

    async fn resolve(&self, _: ClientHello<'_>) -> Option<Arc<ServerConfig>> {
        self.counter.fetch_add(1, Ordering::Release);
        Some(self.config.clone())
    }
}

#[get("/count")]
fn count(counter: &State<Arc<AtomicUsize>>) -> String {
    counter.load(Ordering::Acquire).to_string()
}

fn counting_resolver() -> Result<()> {
    let server = spawn! {
        let counter = Arc::new(AtomicUsize::new(0));
        Rocket::tls_default()
            .manage(counter)
            .mount("/", routes![count])
            .attach(CountingResolver::fairing())
    }?;

    let client = Client::default();
    let response = client.get(&server, "/count")?.send()?;
    assert_eq!(response.text()?, "1");

    // Use a new client so we get a new TLS session.
    let client = Client::default();
    let response = client.get(&server, "/count")?.send()?;
    assert_eq!(response.text()?, "2");
    Ok(())
}

register!(counting_resolver);
register!(sni_resolver);
