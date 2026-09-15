use crate::errors::PortError;
use std::net::TcpListener;

pub const DEFAULT_PORT: u16 = 6969;
const MAX_PORT_ATTEMPTS: u16 = 100;

/// Probes whether the IPv4 loopback address can bind `port` right now.
///
/// The probe does not reserve the port; callers must still handle a later bind
/// losing the race to another process.
pub fn is_port_available(port: u16) -> bool {
    TcpListener::bind(("127.0.0.1", port)).is_ok()
}

/// Finds the first currently available port in a bounded ascending search.
///
/// The search includes `starting_port`, checks at most 100 ports, and never
/// wraps past [`u16::MAX`].
pub fn find_available_port(starting_port: u16) -> Result<u16, PortError> {
    find_available_port_with(starting_port, &mut is_port_available)
}

fn find_available_port_with(
    starting_port: u16,
    is_available: &mut impl FnMut(u16) -> bool,
) -> Result<u16, PortError> {
    let end = starting_port.saturating_add(MAX_PORT_ATTEMPTS - 1);
    for port in starting_port..=end {
        if is_available(port) {
            return Ok(port);
        }
    }

    Err(PortError::NoAvailablePort {
        start: starting_port,
        end,
    })
}

/// Returns the requested port when available, otherwise the next bounded match.
///
/// The boolean is `true` only when a different port was selected. Like
/// [`is_port_available`], this probes availability without reserving the result.
pub fn ensure_port_available(requested_port: u16) -> Result<(u16, bool), PortError> {
    ensure_port_available_with(requested_port, &mut is_port_available)
}

fn ensure_port_available_with(
    requested_port: u16,
    is_available: &mut impl FnMut(u16) -> bool,
) -> Result<(u16, bool), PortError> {
    if is_available(requested_port) {
        return Ok((requested_port, false));
    }

    let Some(next_port) = requested_port.checked_add(1) else {
        return Err(PortError::NoAvailablePort {
            start: requested_port,
            end: requested_port,
        });
    };
    let available = find_available_port_with(next_port, is_available)?;
    Ok((available, true))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn available_port_is_returned_without_change() {
        const PORT: u16 = 12_345;

        assert_eq!(
            ensure_port_available_with(PORT, &mut |candidate| candidate == PORT).unwrap(),
            (PORT, false)
        );
    }

    #[test]
    fn occupied_port_falls_forward() {
        const OCCUPIED_PORT: u16 = 12_345;
        const NEXT_PORT: u16 = OCCUPIED_PORT + 1;
        let (selected, changed) =
            ensure_port_available_with(OCCUPIED_PORT, &mut |candidate| candidate == NEXT_PORT)
                .unwrap();

        assert!(changed);
        assert_eq!(selected, NEXT_PORT);
    }

    #[test]
    fn unavailable_maximum_port_returns_a_bounded_error() {
        let error = ensure_port_available_with(u16::MAX, &mut |_| false).unwrap_err();

        assert!(matches!(
            error,
            PortError::NoAvailablePort {
                start: u16::MAX,
                end: u16::MAX
            }
        ));
    }

    #[test]
    fn search_range_stops_at_maximum_port() {
        let error = find_available_port_with(u16::MAX - 1, &mut |_| false).unwrap_err();

        assert!(matches!(
            error,
            PortError::NoAvailablePort {
                start,
                end: u16::MAX
            } if start == u16::MAX - 1
        ));
    }
}
