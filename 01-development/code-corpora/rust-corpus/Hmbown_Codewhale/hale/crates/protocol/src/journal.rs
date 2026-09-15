//! Session tree journal placeholder (issue #5262).
//!
//! The journal is append-only with an in-memory tree projection. Every
//! non-header entry carries `id` + `parentId`; the active position is a
//! `leafId`; appending creates a child of the leaf; branching only moves the
//! leaf — it never rewrites history. This file lands the *entry shape* that
//! #5262's tree operations (`/tree`, `/branch`, `/fork`, `/resume`) and the
//! deferred compaction/branch-summary entry kinds hang off of. The strategies
//! themselves are deferred, but the shape must be stable now so no migration
//! is needed later.

use serde::{Deserialize, Serialize};
use serde_json::Value;

/// One journal entry. All entries except the root header have `id` + `parent_id`.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct JournalEntry {
    /// Stable entry id (`entry-{uuid}`).
    pub id: String,
    /// Parent entry id; `None` only for the root header.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// Entry kind (`header`, `user`, `assistant`, `tool_result`, `compaction`, `branch_summary`, …).
    pub kind: String,
    /// Payload (text, tool output, compaction summary, etc).
    #[serde(default)]
    pub payload: Value,
    /// When the entry was created (unix seconds).
    pub created_at: i64,
}

/// Append-only journal with a `leafId` cursor. The tree projection is
/// derived root→leaf; moving `leaf_id` branches without rewriting history.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Default)]
pub struct Journal {
    pub entries: Vec<JournalEntry>,
    /// Active position. `None` before the header is appended.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub leaf_id: Option<String>,
}

impl Journal {
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    #[must_use]
    pub fn len(&self) -> usize {
        self.entries.len()
    }

    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.entries.is_empty()
    }

    /// Append a new entry as a child of the current leaf. Returns the new id.
    pub fn append(&mut self, kind: impl Into<String>, payload: Value) -> String {
        let id = format!("entry-{}", uuid::Uuid::new_v4());
        let parent_id = self.leaf_id.clone();
        let entry = JournalEntry {
            id: id.clone(),
            parent_id,
            kind: kind.into(),
            payload,
            created_at: chrono::Utc::now().timestamp(),
        };
        self.entries.push(entry);
        self.leaf_id = Some(id.clone());
        id
    }

    /// Branch: move `leaf_id` to an existing ancestor without rewriting.
    /// Returns `false` when `target` is not found.
    pub fn branch_to(&mut self, target: &str) -> bool {
        if self.entries.iter().any(|e| e.id == target) {
            self.leaf_id = Some(target.to_string());
            true
        } else {
            false
        }
    }

    /// Project the active path root→leaf as a slice of entries in order.
    #[must_use]
    pub fn active_path(&self) -> Vec<&JournalEntry> {
        let Some(leaf) = self.leaf_id.as_deref() else {
            return Vec::new();
        };
        // Build id→parent map for walk.
        let mut by_id = std::collections::HashMap::new();
        for e in &self.entries {
            by_id.insert(e.id.as_str(), e);
        }
        let mut path = Vec::new();
        let mut cur: Option<&str> = Some(leaf);
        while let Some(id) = cur {
            if let Some(entry) = by_id.get(id) {
                path.push(*entry);
                cur = entry.parent_id.as_deref();
            } else {
                break;
            }
        }
        path.reverse();
        path
    }

    /// Find entry by id.
    #[must_use]
    pub fn get(&self, id: &str) -> Option<&JournalEntry> {
        self.entries.iter().find(|e| e.id == id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn append_sets_parent_and_leaf() {
        let mut j = Journal::new();
        let a = j.append("header", json!({}));
        assert_eq!(j.leaf_id.as_deref(), Some(a.as_str()));
        let b = j.append("user", json!("hi"));
        let entry = j.get(&b).unwrap();
        assert_eq!(entry.parent_id.as_deref(), Some(a.as_str()));
        assert_eq!(j.leaf_id.as_deref(), Some(b.as_str()));
    }

    #[test]
    fn branching_only_moves_leaf() {
        let mut j = Journal::new();
        let a = j.append("header", json!({}));
        let b = j.append("user", json!("b"));
        let c = j.append("assistant", json!("c"));
        assert_eq!(j.entries.len(), 3);
        assert!(j.branch_to(&b));
        assert_eq!(j.leaf_id.as_deref(), Some(b.as_str()));
        // History untouched.
        assert_eq!(j.entries.len(), 3);
        // Active path is now a→b.
        let path = j.active_path();
        assert_eq!(path.len(), 2);
        assert_eq!(path[0].id, a);
        assert_eq!(path[1].id, b);
        let d = j.append("user", json!("d after branch"));
        let ent = j.get(&d).unwrap();
        assert_eq!(ent.parent_id.as_deref(), Some(b.as_str()));
        // Old c still exists as a sibling branch that is no longer on the active path.
        assert!(j.get(&c).is_some());
        let path2 = j.active_path();
        assert_eq!(path2.len(), 3);
        assert_eq!(path2[2].id, d);
    }

    #[test]
    fn journal_is_serializable_and_preserves_shape() {
        let mut j = Journal::new();
        j.append("header", json!({}));
        j.append("user", json!("hello"));
        let s = serde_json::to_string(&j).unwrap();
        let back: Journal = serde_json::from_str(&s).unwrap();
        assert_eq!(back, j);
    }
}
