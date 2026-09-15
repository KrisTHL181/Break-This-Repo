use crate::prelude::*;

#[get("/")]
fn infinite() -> TextStream![&'static str] {
    TextStream! {
        loop {
            yield rocket::futures::future::pending::<&str>().await;
        }
    }
}

pub fn test_inifinite_streams_end() -> Result<()> {
    let mut server = spawn! {
        Rocket::default().mount("/", routes![infinite])
    }?;

    let client = Client::default();
    client.get(&server, "/")?.send()?;
    server.terminate()?;

    let stdout = server.read_stdout()?;
    assert!(stdout.contains("Rocket has launched on http"));
    assert!(stdout.contains("GET /"));
    assert!(stdout.contains("Graceful shutdown completed"));

    Ok(())
}

register!(test_inifinite_streams_end);
