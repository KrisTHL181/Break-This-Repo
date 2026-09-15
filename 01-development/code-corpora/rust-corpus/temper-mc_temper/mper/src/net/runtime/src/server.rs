use temper_config::ServerConfig;
use temper_protocol::errors::NetError;
use tokio::net::TcpListener;
use tracing::{debug, error};

pub async fn create_server_listener(config: &ServerConfig) -> Result<TcpListener, NetError> {
    let server_addy = format!("{}:{}", config.host, config.port);
    let server_addy = server_addy.as_str();

    let listener = match TcpListener::bind(server_addy).await {
        Ok(l) => Ok::<TcpListener, std::io::Error>(l),
        Err(e) => {
            error!("Failed to bind to addy: {}", server_addy);
            error!("Perhaps the port {} is already in use?", config.port);

            Err(e)
        }
    };

    debug!("Server listening on {}", server_addy);

    Ok(listener?)
}
