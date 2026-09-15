use std::future::Future;
use std::net::{Ipv4Addr, SocketAddr};
use std::time::Duration;
use std::sync::Once;
use std::process::Stdio;
use std::io::Read;

use rocket::fairing::AdHoc;
use rocket::listener::{Bind, DefaultListener};
use rocket::serde::{Deserialize, DeserializeOwned, Serialize};
use rocket::{Build, Ignite, Rocket};
use rocket::trace::Trace;

use ipc_channel::ipc::{IpcOneShotServer, IpcReceiver, IpcSender};

use crate::{Result, Error};

#[derive(Debug)]
pub struct Server {
    proc: procspawn::JoinHandle<Launched>,
    pub tls: bool,
    pub port: u16,
    _rx: IpcReceiver<Message>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(crate = "rocket::serde")]
pub enum Message {
    Liftoff(bool, u16),
    Failure,
}

#[derive(Serialize, Deserialize)]
#[serde(crate = "rocket::serde")]
pub struct Token(String);

#[derive(Serialize, Deserialize)]
#[serde(crate = "rocket::serde")]
pub struct Launched(());

fn stdio() -> Stdio {
    std::env::var_os("NOCAPTURE")
        .map(|_| Stdio::inherit())
        .unwrap_or_else(Stdio::piped)
}

fn read<T: Read>(io: Option<T>) -> Result<String> {
    if let Some(mut io) = io {
        let mut string = String::new();
        io.read_to_string(&mut string)?;
        return Ok(string);
    }

    Ok(String::new())
}

impl Server {
    pub fn spawn<T>(ctxt: T, f: fn((Token, T)) -> Launched) -> Result<Server>
        where T: Serialize + DeserializeOwned
    {
        static INIT: Once = Once::new();
        INIT.call_once(procspawn::init);

        let (ipc, server) = IpcOneShotServer::new()?;
        let mut proc = procspawn::Builder::new()
            .stdin(Stdio::null())
            .stdout(stdio())
            .stderr(stdio())
            .spawn((Token(server), ctxt), f);

        let (rx, _) = ipc.accept().unwrap();
        match rx.recv()? {
            Message::Liftoff(tls, port) => {
                Ok(Server { proc, tls, port, _rx: rx })
            },
            Message::Failure => {
                Err(Error::Liftoff(read(proc.stdout())?, read(proc.stderr())?))
            }
        }
    }

    pub fn socket_addr(&self) -> SocketAddr {
        let ip = Ipv4Addr::LOCALHOST;
        SocketAddr::new(ip.into(), self.port)
    }

    pub fn read_stdout(&mut self) -> Result<String> {
        read(self.proc.stdout())
    }

    pub fn read_stderr(&mut self) -> Result<String> {
        read(self.proc.stderr())
    }

    pub fn kill(&mut self) -> Result<()> {
        Ok(self.proc.kill()?)
    }

    pub fn terminate(&mut self) -> Result<()> {
        use nix::{sys::signal, unistd::Pid};

        let pid = Pid::from_raw(self.proc.pid().unwrap() as i32);
        Ok(signal::kill(pid, signal::SIGTERM)?)
    }

    pub fn join(&mut self, duration: Duration) -> Result<()> {
        match self.proc.join_timeout(duration) {
            Ok(_) => Ok(()),
            Err(e) if e.is_remote_close() => Ok(()),
            Err(e) => Err(e.into()),
        }
    }
}

impl Token {
    pub fn with_launch<F, Fut>(self, rocket: Rocket<Build>, launch: F) -> Launched
        where F: FnOnce(Rocket<Ignite>) -> Fut + Send + Sync + 'static,
              Fut: Future<Output = Result<Rocket<Ignite>, rocket::Error>> + Send
    {
        let server = self.0.clone();
        let rocket = rocket.attach(AdHoc::on_liftoff("Liftoff", move |rocket| Box::pin(async move {
            let tcp = rocket.endpoints().find_map(|e| e.tcp()).unwrap();
            let tls = rocket.endpoints().any(|e| e.is_tls());
            let sender = IpcSender::<Message>::connect(server).unwrap();
            let _ = sender.send(Message::Liftoff(tls, tcp.port()));
            let _ = sender.send(Message::Liftoff(tls, tcp.port()));
        })));

        let server = self.0.clone();
        let launch = async move {
            let rocket = rocket.ignite().await?;
            launch(rocket).await
        };

        if let Err(e) = rocket::execute(launch) {
            let sender = IpcSender::<Message>::connect(server).unwrap();
            let _ = sender.send(Message::Failure);
            let _ = sender.send(Message::Failure);
            e.trace_error();
            std::process::exit(1);
        }

        Launched(())
    }

    pub fn launch_with<B: Bind>(self, rocket: Rocket<Build>) -> Launched
        where B: Send + Sync + 'static
    {
        self.with_launch(rocket, |rocket| rocket.launch_with::<B>())
    }

    pub fn launch(self, rocket: Rocket<Build>) -> Launched {
        self.launch_with::<DefaultListener>(rocket)
    }
}

impl Drop for Server {
    fn drop(&mut self) {
        let _ = self.terminate();
        if self.join(Duration::from_secs(3)).is_err() {
            let _ = self.kill();
        }
    }
}

#[macro_export]
macro_rules! spawn {
    ($($arg:ident : $t:ty),* => $rocket:block) => {{
        #[allow(unused_parens)]
        fn _server((token, $($arg),*): ($crate::Token, $($t),*)) -> $crate::Launched {
            let rocket: rocket::Rocket<rocket::Build> = $rocket;
            token.launch(rocket)
        }

        Server::spawn(($($arg),*), _server)
    }};

    ($($token:tt)*) => {{
        let _unit = ();
        spawn!(_unit: () => { $($token)* } )
    }};
}
