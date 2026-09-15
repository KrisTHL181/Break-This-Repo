pub(crate) fn response(kind: &str, data: String) -> Option<crate::protocol::ServerMessage> {
    (kind == crate::protocol::endpoint::HEALTH_PING_KIND).then(|| {
        crate::protocol::ServerMessage::EndpointControl {
            kind: crate::protocol::endpoint::HEALTH_PONG_KIND.into(),
            data,
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn health_ping_echoes_a_pong_without_entering_server_state() {
        let pong = response(crate::protocol::endpoint::HEALTH_PING_KIND, "probe".into()).unwrap();
        assert!(matches!(
            pong,
            crate::protocol::ServerMessage::EndpointControl { kind, data }
                if kind == crate::protocol::endpoint::HEALTH_PONG_KIND && data == "probe"
        ));
        assert!(response("future.control", String::new()).is_none());
    }
}
