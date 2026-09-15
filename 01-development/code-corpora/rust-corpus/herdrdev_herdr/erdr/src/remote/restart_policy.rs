#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum RemoteServerRestartReason {
    EndpointProtocol,
    SurfaceInterest,
    HealthCheck,
    DaemonDetach,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum RemoteInstallRunningServerPlan {
    KeepRunning,
    LiveHandoff,
    StopRequired(RemoteServerRestartReason),
}

pub(super) fn remote_server_restart_reason(
    endpoint_protocol_generation: Option<u32>,
    detached_server_daemon: bool,
    require_surface_interest: bool,
    surface_interest: bool,
    health_check: bool,
) -> Option<RemoteServerRestartReason> {
    if endpoint_protocol_generation != Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION)
    {
        return Some(RemoteServerRestartReason::EndpointProtocol);
    }
    if require_surface_interest && !surface_interest {
        return Some(RemoteServerRestartReason::SurfaceInterest);
    }
    if require_surface_interest && !health_check {
        return Some(RemoteServerRestartReason::HealthCheck);
    }
    if !detached_server_daemon {
        return Some(RemoteServerRestartReason::DaemonDetach);
    }
    None
}

pub(super) fn remote_install_running_server_plan(
    endpoint_protocol_generation: Option<u32>,
    detached_server_daemon: bool,
    surface_interest: bool,
    health_check: bool,
    live_handoff: bool,
    live_handoff_enabled: bool,
    require_surface_interest: bool,
) -> RemoteInstallRunningServerPlan {
    let Some(reason) = remote_server_restart_reason(
        endpoint_protocol_generation,
        detached_server_daemon,
        require_surface_interest,
        surface_interest,
        health_check,
    ) else {
        return RemoteInstallRunningServerPlan::KeepRunning;
    };

    if live_handoff_enabled && live_handoff {
        return RemoteInstallRunningServerPlan::LiveHandoff;
    }
    RemoteInstallRunningServerPlan::StopRequired(reason)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pre_floor_server_requires_update() {
        assert_eq!(
            remote_server_restart_reason(None, true, false, false, false),
            Some(RemoteServerRestartReason::EndpointProtocol)
        );
    }

    #[test]
    fn compatible_server_can_keep_running() {
        assert_eq!(
            remote_server_restart_reason(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                true,
                false,
                false,
                false
            ),
            None
        );
    }

    #[test]
    fn saved_endpoint_requires_surface_interest() {
        assert_eq!(
            remote_server_restart_reason(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                true,
                true,
                false,
                false
            ),
            Some(RemoteServerRestartReason::SurfaceInterest)
        );
    }

    #[test]
    fn saved_endpoint_requires_health_checks() {
        assert_eq!(
            remote_server_restart_reason(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                true,
                true,
                true,
                false
            ),
            Some(RemoteServerRestartReason::HealthCheck)
        );
    }

    #[test]
    fn saved_endpoint_accepts_complete_lifecycle_support() {
        assert_eq!(
            remote_server_restart_reason(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                true,
                true,
                true,
                true
            ),
            None
        );
    }

    #[test]
    fn old_daemon_requires_restart() {
        assert_eq!(
            remote_server_restart_reason(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                false,
                false,
                false,
                false
            ),
            Some(RemoteServerRestartReason::DaemonDetach)
        );
    }

    #[test]
    fn saved_endpoint_can_explicitly_live_handoff_for_surface_support() {
        assert_eq!(
            remote_install_running_server_plan(
                Some(crate::protocol::endpoint::ENDPOINT_PROTOCOL_GENERATION),
                true,
                false,
                false,
                true,
                true,
                true
            ),
            RemoteInstallRunningServerPlan::LiveHandoff
        );
    }
}
