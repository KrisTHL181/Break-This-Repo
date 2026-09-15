mod runner;
mod servers;
mod config;

pub mod prelude {
    pub use rocket::*;
    pub use rocket::fairing::*;
    pub use rocket::response::stream::*;

    pub use testbench::{Error, Result, *};
    pub use crate::register;
    pub use crate::config::*;
}

pub use runner::Test;

fn main() -> std::process::ExitCode {
    runner::run()
}
