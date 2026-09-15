//! Production contracts for the stable V2 failpoint control boundary.
//!
//! This feature-gated child of the production failpoint module covers stable
//! name parsing, environment-triggered errors, one-shot reset, and poisoned
//! synchronization. The caller serializes the process-wide environment and
//! failpoint slot with the lifecycle acceptance lock.

use std::thread;

use super::*;

/// Enters the environment-selected abort boundary for a subprocess contract.
pub(crate) fn abort_probe() -> ! {
    let _ = trip(IndexOutboxFailpoint::CommitBefore);
    panic!("abort failpoint unexpectedly returned");
}

/// Runs failpoint parsing, environment, reset, and poisoned-lock contracts.
pub(crate) fn run() {
    for failpoint in IndexOutboxFailpoint::ALL {
        assert_eq!(
            IndexOutboxFailpoint::parse(failpoint.as_str()),
            Some(failpoint)
        );
    }
    assert_eq!(IndexOutboxFailpoint::parse("unknown"), None);

    inject_once(IndexOutboxFailpoint::ClaimBefore).expect("one-shot failpoint installs");
    assert!(trip(IndexOutboxFailpoint::ClaimBefore).is_err());
    assert!(was_triggered());
    assert!(trip(IndexOutboxFailpoint::ClaimBefore).is_ok());

    // SAFETY: the acceptance runner holds its process-global serialization
    // lock, and both variables are removed before this function returns.
    unsafe {
        std::env::set_var("HELIX_INDEX_OUTBOX_FAILPOINT", "commit_before");
        std::env::remove_var("HELIX_INDEX_OUTBOX_FAIL_ACTION");
    }
    assert!(matches!(
        trip(IndexOutboxFailpoint::CommitBefore),
        Err(HelixDbError::InvariantViolation(reason))
            if reason.contains("commit_before")
    ));
    // SAFETY: this restores the serialized process environment immediately
    // after the single production call that observes it.
    unsafe {
        std::env::remove_var("HELIX_INDEX_OUTBOX_FAILPOINT");
    }

    let poisoner = thread::spawn(|| {
        let _guard = INJECTED_FAILPOINT
            .lock()
            .expect("failpoint mutex starts healthy");
        panic!("poison failpoint mutex for the fail-closed contract");
    });
    assert!(poisoner.join().is_err());
    assert!(matches!(
        inject_once(IndexOutboxFailpoint::ClaimAfter),
        Err(HelixDbError::InvariantViolation(reason))
            if reason.contains("mutex was poisoned")
    ));
    assert!(matches!(
        trip(IndexOutboxFailpoint::ClaimAfter),
        Err(HelixDbError::InvariantViolation(reason))
            if reason.contains("mutex was poisoned")
    ));
    INJECTED_FAILPOINT.clear_poison();
    *INJECTED_FAILPOINT
        .lock()
        .expect("cleared failpoint mutex locks") = None;
}
