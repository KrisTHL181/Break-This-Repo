use rand::prelude::IndexedRandom;
use std::net::{Ipv4Addr, SocketAddrV4};
use temper_config::ServerConfig;
use tokio::net::UdpSocket;
use tracing::error;

pub struct LanPinger {
    socket: UdpSocket,
    addr: SocketAddrV4,
}

impl LanPinger {
    pub async fn new() -> std::io::Result<Self> {
        const ADDR: Ipv4Addr = Ipv4Addr::new(224, 0, 2, 60); // mojang's UDP multicast address
        const PORT: u16 = 4445;

        Ok(Self {
            socket: UdpSocket::bind("0.0.0.0:0").await?,
            addr: SocketAddrV4::new(ADDR, PORT),
        })
    }

    pub fn announcement(&self, config: &ServerConfig) -> String {
        let motd = config.motd.choose(&mut rand::rng()).unwrap();
        let port = config.port;

        format!("[MOTD]{motd}[/MOTD][AD]{port}[/AD]")
    }

    pub async fn send(&mut self, config: &ServerConfig) {
        let announcement = self.announcement(config);

        if let Err(err) = self
            .socket
            .send_to(announcement.as_bytes(), self.addr)
            .await
        {
            error!("Failed sending LAN UDP Packet: {err}")
        }
    }
}
