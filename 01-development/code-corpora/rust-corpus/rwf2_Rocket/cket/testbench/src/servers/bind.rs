use rocket::{tokio::net::TcpListener};

use crate::prelude::*;

#[cfg(unix)]
fn tcp_unix_listener_fail() -> Result<()> {
    use rocket::listener::unix::UnixListener;

    let server = spawn! {
        Rocket::default().reconfigure_with_toml("[default]\naddress = 123")
    };

    if let Err(Error::Liftoff(stdout, _)) = server {
        assert!(stdout.contains("expected: valid TCP (ip) or unix (path)"));
        assert!(stdout.contains("default.address"));
    } else {
        panic!("unexpected result: {server:#?}");
    }

    let server = Server::spawn((), |(token, _)| {
        let rocket = Rocket::default().reconfigure_with_toml("[default]\naddress = \"unix:foo\"");
        token.launch_with::<TcpListener>(rocket)
    });

    if let Err(Error::Liftoff(stdout, _)) = server {
        assert!(stdout.contains("invalid tcp endpoint: unix:foo"));
    } else {
        panic!("unexpected result: {server:#?}");
    }

    let server = Server::spawn((), |(token, _)| {
        token.launch_with::<UnixListener>(Rocket::default())
    });

    if let Err(Error::Liftoff(stdout, _)) = server {
        assert!(stdout.contains("invalid unix endpoint: tcp:127.0.0.1:8000"));
    } else {
        panic!("unexpected result: {server:#?}");
    }

    Ok(())
}

#[cfg(unix)]
register!(tcp_unix_listener_fail);
