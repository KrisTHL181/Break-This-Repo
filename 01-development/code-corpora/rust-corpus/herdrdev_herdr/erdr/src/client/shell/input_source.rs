use super::*;

impl ClientShellState {
    fn wants_ascii_input(&self) -> bool {
        if let Some(overlay) = self.overlay.as_ref() {
            return matches!(
                overlay,
                ClientShellOverlay::ConfirmClose(_)
                    | ClientShellOverlay::Help(_)
                    | ClientShellOverlay::Navigator(_)
                    | ClientShellOverlay::WorktreeRemove(_)
                    | ClientShellOverlay::ContextMenu(_)
                    | ClientShellOverlay::GlobalMenu(_)
            );
        }
        matches!(
            self.mode,
            ClientShellMode::Prefix
                | ClientShellMode::Navigate
                | ClientShellMode::Resize
                | ClientShellMode::Copy
        )
    }

    pub(crate) fn reconcile_input_source(&mut self) {
        // Keep the platform restore token while another window has focus. Restoring
        // through a global key injection is only safe after this client regains focus.
        if self.outer_focused == Some(false) {
            return;
        }
        let desired = self.config.switch_ascii_input_source_in_prefix && self.wants_ascii_input();
        if desired != self.ascii_input_source_active {
            self.ascii_input_source_active = desired;
            self.pending_input_source_changes.push(desired);
        }
    }

    pub(crate) fn take_input_source_changes(&mut self) -> Vec<bool> {
        std::mem::take(&mut self.pending_input_source_changes)
    }
}
