use std::str::FromStr;
use std::thread;
use voxel_rs_network::{Client, Server, ServerEvent, SocketAddr, MessageDelivery};

mod common;
use self::common::{DummySocket, NO_LOSS_CONFIG};

// Server sends 42 to client, client sends back 43 to server, unreliable
#[test]
fn test_connection_no_loss() {
    let client_addr = SocketAddr::from_str("127.0.0.1:42").unwrap();
    let server_addr = SocketAddr::from_str("127.0.0.1:43").unwrap();
    thread::spawn(move || {
        let client_socket = DummySocket::new(client_addr, NO_LOSS_CONFIG);
        let mut client = Client::new(client_socket, server_addr);
        client.connect();

        loop {
            client.tick();
            let mut send_back = false;
            for message in client.get_messages() {
                if message.1 == vec![42] {
                    send_back = true;
                }
            }
            if send_back {
                client.send_message(vec![43], MessageDelivery::Unreliable);
            }
        }
    });

    let server_thread = thread::spawn(move || {
        let server_socket = DummySocket::new(server_addr, NO_LOSS_CONFIG);
        let mut server = Server::new(server_socket);

        loop {
            server.tick();
            let mut send_back_id = None;
            for event in server.get_events() {
                match event {
                    ServerEvent::Connected { id } => {
                        send_back_id = Some(id);
                    }
                    ServerEvent::Message { data, .. } => {
                        if data == vec![43] {
                            return true;
                        }
                    }
                    _ => {}
                }
            }
            if let Some(id) = send_back_id {
                server.send_message(id, vec![42], MessageDelivery::Unreliable);
            }
        }
    });

    let join_result = server_thread.join();
    assert!(join_result.unwrap(), "Server received the client's message");
}