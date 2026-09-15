//! Ensure that responses with a status of 204 or 304 do not have a body, and
//! for the former, do not have a Content-Length header.

use crate::prelude::*;

use rocket::http::Status;

#[get("/<code>")]
fn status(code: u16) -> (Status, &'static [u8]) {
    (Status::new(code), &[1, 2, 3, 4])
}

pub fn test_no_content() -> Result<()> {
    let server = spawn!(Rocket::default().mount("/", routes![status]))?;

    let client = Client::default();
    let response = client.get(&server, "/204")?.send()?;
    assert_eq!(response.status(), 204);
    assert!(response.headers().get("Content-Length").is_none());
    assert!(response.bytes()?.is_empty());

    let response = client.get(&server, "/304")?.send()?;
    assert_eq!(response.status(), 304);
    assert_eq!(response.headers().get("Content-Length").unwrap(), "4");
    assert!(response.bytes()?.is_empty());

    Ok(())
}

register!(test_no_content);
