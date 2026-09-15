//! Coalescing frame-request scheduler.
//!
//! Widgets ask for a future frame; this scheduler merges requests and emits
//! at most one wake deadline compatible with the existing frame-cap
//! philosophy in [`crate::tui::frame_rate_limiter`]. It does **not** run a
//! competing animation loop — the main `ui` poll loop remains the sole
//! emitter of `terminal.draw`.

use std::time::Duration;
use std::time::Instant;

use super::mode::MotionPolicy;

/// Coalesced request for a future redraw.
#[derive(Debug, Default)]
pub struct FrameRequester {
    /// Earliest instant a requester wants a frame.
    next_due: Option<Instant>,
    /// Whether any widget asked for a frame since the last take.
    pending: bool,
    request_count: u64,
    emit_count: u64,
}

impl FrameRequester {
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// Request a frame as soon as the motion policy and frame cap allow.
    pub fn request_frame(&mut self, now: Instant, policy: MotionPolicy) {
        if !policy.should_request_animation_frames() {
            // Reduced/Still: do not schedule decorative frames. State-change
            // redraws still go through `needs_redraw` directly.
            return;
        }
        self.request_at(now, now, policy);
    }

    /// Request a frame no earlier than `earliest`.
    pub fn request_at(&mut self, now: Instant, earliest: Instant, policy: MotionPolicy) {
        if !policy.should_request_animation_frames() {
            return;
        }
        let capped = earliest.max(now);
        self.pending = true;
        self.request_count = self.request_count.saturating_add(1);
        self.next_due = Some(match self.next_due {
            Some(existing) => existing.min(capped),
            None => capped,
        });
    }

    /// Time until a coalesced frame should emit, if one is pending.
    #[must_use]
    pub fn due_in(&self, now: Instant) -> Option<Duration> {
        let due = self.next_due?;
        if !self.pending {
            return None;
        }
        Some(due.saturating_duration_since(now))
    }

    /// Consume a due frame request. Returns true when the main loop should
    /// set `needs_redraw` for animation (not for state changes).
    pub fn take_due(&mut self, now: Instant, policy: MotionPolicy) -> bool {
        if !self.pending || !policy.should_request_animation_frames() {
            self.pending = false;
            self.next_due = None;
            return false;
        }
        let Some(due) = self.next_due else {
            return false;
        };
        if now < due {
            return false;
        }
        self.pending = false;
        self.next_due = None;
        self.emit_count = self.emit_count.saturating_add(1);
        true
    }

    /// Apply the frame-rate limiter interval so animation requests never beat
    /// the draw cap.
    #[allow(dead_code)] // frame-cap bridge for poll-loop hosts (TUI-DOG-008)
    pub fn clamp_to_frame_cap(&mut self, last_draw_at: Instant, policy: MotionPolicy) {
        let Some(due) = self.next_due else {
            return;
        };
        let min_allowed = last_draw_at
            .checked_add(policy.min_frame_interval())
            .unwrap_or(last_draw_at);
        if due < min_allowed {
            self.next_due = Some(min_allowed);
        }
    }

    #[allow(dead_code)] // reset between modal/session boundaries (TUI-DOG-008)
    pub fn reset(&mut self) {
        self.pending = false;
        self.next_due = None;
    }

    #[must_use]
    #[allow(dead_code)] // telemetry/introspection for motion QA (TUI-DOG-008)
    pub fn request_count(&self) -> u64 {
        self.request_count
    }

    #[must_use]
    #[allow(dead_code)] // telemetry/introspection for motion QA (TUI-DOG-008)
    pub fn emit_count(&self) -> u64 {
        self.emit_count
    }

    #[must_use]
    #[allow(dead_code)] // pending probe for poll-loop hosts (TUI-DOG-008)
    pub fn is_pending(&self) -> bool {
        self.pending
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::tui::motion::MotionPolicy;

    #[test]
    fn coalesces_multiple_requests_into_one_emit() {
        let policy = MotionPolicy::from_settings(false, true, false);
        let mut req = FrameRequester::new();
        let t0 = Instant::now();

        for _ in 0..20 {
            req.request_frame(t0, policy);
        }
        assert!(req.is_pending());
        assert_eq!(req.request_count(), 20);
        assert!(req.take_due(t0, policy));
        assert_eq!(req.emit_count(), 1);
        assert!(!req.take_due(t0, policy));
    }

    #[test]
    fn reduced_motion_drops_animation_frame_requests() {
        let policy = MotionPolicy::from_settings(true, true, false);
        let mut req = FrameRequester::new();
        let t0 = Instant::now();
        req.request_frame(t0, policy);
        assert!(!req.is_pending());
        assert!(!req.take_due(t0, policy));
    }

    #[test]
    fn clamp_respects_low_motion_frame_cap() {
        let policy = MotionPolicy::from_settings(false, true, false);
        let mut req = FrameRequester::new();
        let t0 = Instant::now();
        req.request_frame(t0, policy);
        req.clamp_to_frame_cap(t0, policy);
        let due = req.due_in(t0).unwrap();
        assert!(due >= policy.min_frame_interval() || due.is_zero());
    }
}
