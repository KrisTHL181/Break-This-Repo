use crate::prelude::*;

use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use rocket::tls::{ClientHello, Resolver, ServerConfig, TlsConfig};

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

fn test_tls_resolver() -> Result<()> {
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

register!(test_tls_resolver);

// TODO: Implement an `UpdatingResolver`. Expose `SniResolver` and
// `UpdatingResolver` in a `contrib` library or as part of `rocket`.
//
// struct UpdatingResolver {
//     timestamp: AtomicU64,
//     config: ArcSwap<ServerConfig>
// }
//
// #[crate::async_trait]
// impl Resolver for UpdatingResolver {
//     async fn resolve(&self, _: ClientHello<'_>) -> Option<Arc<ServerConfig>> {
//         if let Either::Left(path) = self.tls_config.certs() {
//             let metadata = tokio::fs::metadata(&path).await.ok()?;
//             let modtime = metadata.modified().ok()?;
//             let timestamp = modtime.duration_since(UNIX_EPOCH).ok()?.as_secs();
//             let old_timestamp = self.timestamp.load(Ordering::Acquire);
//             if timestamp > old_timestamp {
//                 let new_config = self.tls_config.to_server_config().await.ok()?;
//                 self.server_config.store(Arc::new(new_config));
//                 self.timestamp.store(timestamp, Ordering::Release);
//             }
//         }
//
//         Some(self.server_config.load_full())
//     }
// }
