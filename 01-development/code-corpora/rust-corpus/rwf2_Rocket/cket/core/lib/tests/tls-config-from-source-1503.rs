#![cfg(feature = "tls")]

macro_rules! relative {
    ($path:expr) => {
        std::path::Path::new(concat!(env!("CARGO_MANIFEST_DIR"), "/", $path))
    };
}

#[test]
fn tls_config_from_source() {
    use rocket::tls::TlsConfig;
    use rocket::figment::{Figment, providers::Serialized};

    let cert_path = relative!("examples/tls/private/cert.pem");
    let key_path = relative!("examples/tls/private/key.pem");
    let config = TlsConfig::from_paths(cert_path, key_path);

    let tls: TlsConfig = Figment::from(Serialized::globals(config)).extract().unwrap();
    assert_eq!(tls.certs().unwrap_left(), cert_path);
    assert_eq!(tls.key().unwrap_left(), key_path);
}
