use std::error::Error;

use crate::listener::{Endpoint, Listener};
use crate::{Rocket, Ignite};

pub trait Bind: Listener + 'static {
    type Error: Error + Send + 'static;

    #[crate::async_bound(Send)]
    async fn bind(rocket: &Rocket<Ignite>) -> Result<Self, Self::Error>;

    fn bind_endpoint(to: &Rocket<Ignite>) -> Result<Endpoint, Self::Error>;
}
