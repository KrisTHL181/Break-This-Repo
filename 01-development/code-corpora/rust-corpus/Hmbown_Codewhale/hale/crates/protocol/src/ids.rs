//! Typed `ThreadId` / `SessionId` for the `crates/core` boundary (issue #5261).
//!
//! `codewhale`'s `Session` is really a thread. The new boundary introduces
//! two ids so every consumer — TUI, CLI, app-server, tests — can name the
//! right scope:
//! - `ThreadId` — long-lived conversation (persisted in `state.json` / `threads/`)
//! - `SessionId` — one turn/session within a thread (ephemeral engine handle)
//!
//! Both are thin wrappers around the existing `"thread-…"` string id so the
//! persisted JSON shape stays unchanged. They serialize as plain strings,
//! deserialize from plain strings or `{ "id": "…" }`, and parse from either.

use std::fmt;
use std::str::FromStr;

use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Long-lived conversation id. Backwards compatible with the existing
/// `thread-{uuid}` string form used in `crates/state` and `runtime_threads`.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct ThreadId(pub String);

impl ThreadId {
    #[must_use]
    pub fn new() -> Self {
        Self(format!("thread-{}", Uuid::new_v4()))
    }

    #[must_use]
    pub fn from_string(s: impl Into<String>) -> Self {
        Self(s.into())
    }

    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    #[must_use]
    pub fn into_string(self) -> String {
        self.0
    }
}

impl Default for ThreadId {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Display for ThreadId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for ThreadId {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl From<ThreadId> for String {
    fn from(id: ThreadId) -> Self {
        id.0
    }
}

impl FromStr for ThreadId {
    type Err = std::convert::Infallible;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        Ok(Self(s.to_string()))
    }
}

/// One engine session within a thread (a single `Op` turn or a supervised
/// engine lifetime). Distinct from `ThreadId` so the thread manager can
/// start a session with no TUI attached and so tests can assert headless
/// == TUI byte-identical requests for the same `ThreadId` + `SessionId` pair.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct SessionId(pub String);

impl SessionId {
    #[must_use]
    pub fn new() -> Self {
        Self(format!("session-{}", Uuid::new_v4()))
    }

    #[must_use]
    pub fn from_string(s: impl Into<String>) -> Self {
        Self(s.into())
    }

    #[must_use]
    pub fn as_str(&self) -> &str {
        &self.0
    }

    #[must_use]
    pub fn into_string(self) -> String {
        self.0
    }
}

impl Default for SessionId {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Display for SessionId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for SessionId {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl From<SessionId> for String {
    fn from(id: SessionId) -> Self {
        id.0
    }
}

impl FromStr for SessionId {
    type Err = std::convert::Infallible;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        Ok(Self(s.to_string()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn thread_id_roundtrip() {
        let id = ThreadId::new();
        let s = id.to_string();
        assert!(s.starts_with("thread-"));
        let parsed: ThreadId = s.parse().unwrap();
        assert_eq!(parsed.as_str(), id.as_str());
    }

    #[test]
    fn session_id_display() {
        let id = SessionId::from_string("session-abc");
        assert_eq!(format!("{id}"), "session-abc");
        let json = serde_json::to_string(&id).unwrap();
        assert_eq!(json, "\"session-abc\"");
        let back: SessionId = serde_json::from_str(&json).unwrap();
        assert_eq!(back, id);
    }
}
