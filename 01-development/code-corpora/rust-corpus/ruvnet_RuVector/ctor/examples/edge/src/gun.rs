//! GUN Decentralized Database Integration
//!
//! Provides decentralized P2P sync for swarm intelligence using GUN protocol.
//!
//! GUN Features:
//! - Offline-first: Works without internet
//! - Real-time sync: Changes propagate instantly
//! - Decentralized: No central server needed
//! - Graph database: Perfect for relationship data
//! - Conflict resolution: HAM (Hypothetical Amnesia Machine)

use crate::intelligence::{LearningState, Pattern};
use crate::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

/// GUN-backed swarm state that syncs across all peers
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GunSwarmState {
    /// Swarm identifier
    pub swarm_id: String,
    /// Connected peers
    pub peers: HashMap<String, GunPeerInfo>,
    /// Shared learning patterns (synced via GUN)
    pub patterns: HashMap<String, Pattern>,
    /// Shared memories (synced via GUN)
    pub memories: Vec<GunMemory>,
    /// Global swarm config
    pub config: GunSwarmConfig,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GunPeerInfo {
    pub agent_id: String,
    pub public_key: Option<String>,
    pub last_seen: u64,
    pub patterns_count: usize,
    pub memories_count: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GunMemory {
    pub id: String,
    pub content: String,
    pub embedding_hash: String, // Hash of embedding for dedup
    pub owner: String,
    pub timestamp: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GunSwarmConfig {
    /// GUN relay peers to connect to
    pub relays: Vec<String>,
    /// Enable encryption (SEA)
    pub encrypted: bool,
    /// Sync interval in ms
    pub sync_interval_ms: u64,
    /// Max patterns to sync
    pub max_patterns: usize,
}

impl Default for GunSwarmConfig {
    fn default() -> Self {
        Self {
            relays: vec![
                "https://gun-manhattan.herokuapp.com/gun".to_string(),
                "https://gun-us.herokuapp.com/gun".to_string(),
            ],
            encrypted: true,
            sync_interval_ms: 1000,
            max_patterns: 10000,
        }
    }
}

/// GUN-powered decentralized swarm sync
pub struct GunSync {
    swarm_id: String,
    agent_id: String,
    state: Arc<RwLock<GunSwarmState>>,
    config: GunSwarmConfig,
}

impl GunSync {
    /// Create new GUN sync manager
    pub fn new(swarm_id: &str, agent_id: &str, config: GunSwarmConfig) -> Self {
        let state = GunSwarmState {
            swarm_id: swarm_id.to_string(),
            peers: HashMap::new(),
            patterns: HashMap::new(),
            memories: Vec::new(),
            config: config.clone(),
        };

        Self {
            swarm_id: swarm_id.to_string(),
            agent_id: agent_id.to_string(),
            state: Arc::new(RwLock::new(state)),
            config,
        }
    }

    /// Connect to GUN network
    pub async fn connect(&mut self) -> Result<()> {
        tracing::warn!(
            "The vulnerable Rust GUN backend has been retired; using local-only mode for swarm {}",
            self.swarm_id
        );
        Ok(())
    }

    /// Publish pattern to GUN network
    pub async fn publish_pattern(&self, pattern: &Pattern) -> Result<()> {
        let key = format!("{}|{}", pattern.state, pattern.action);

        // Update local state
        {
            let mut state = self.state.write().await;
            state.patterns.insert(key.clone(), pattern.clone());
        }

        Ok(())
    }

    /// Publish memory to GUN network
    pub async fn publish_memory(&self, memory: GunMemory) -> Result<()> {
        // Update local state
        {
            let mut state = self.state.write().await;
            state.memories.push(memory.clone());
        }

        Ok(())
    }

    /// Announce peer presence
    pub async fn announce_peer(&self) -> Result<()> {
        let peer_info = GunPeerInfo {
            agent_id: self.agent_id.clone(),
            public_key: None, // Would be set with SEA encryption
            last_seen: chrono::Utc::now().timestamp_millis() as u64,
            patterns_count: self.state.read().await.patterns.len(),
            memories_count: self.state.read().await.memories.len(),
        };

        // Update local state
        {
            let mut state = self.state.write().await;
            state.peers.insert(self.agent_id.clone(), peer_info.clone());
        }

        Ok(())
    }

    /// Get all patterns from swarm
    pub async fn get_patterns(&self) -> HashMap<String, Pattern> {
        self.state.read().await.patterns.clone()
    }

    /// Get all peers in swarm
    pub async fn get_peers(&self) -> Vec<GunPeerInfo> {
        self.state.read().await.peers.values().cloned().collect()
    }

    /// Get swarm statistics
    pub async fn get_stats(&self) -> GunSwarmStats {
        let state = self.state.read().await;
        GunSwarmStats {
            swarm_id: state.swarm_id.clone(),
            total_peers: state.peers.len(),
            total_patterns: state.patterns.len(),
            total_memories: state.memories.len(),
            relays: self.config.relays.len(),
            encrypted: self.config.encrypted,
        }
    }

    /// Sync learning state to GUN
    pub async fn sync_learning_state(&self, learning_state: &LearningState) -> Result<usize> {
        let mut synced = 0;

        for (_key, pattern) in &learning_state.patterns {
            self.publish_pattern(pattern).await?;
            synced += 1;
        }

        Ok(synced)
    }

    /// Import patterns from GUN to local learning state
    pub async fn import_to_learning_state(&self, learning_state: &mut LearningState) -> usize {
        let state = self.state.read().await;
        let mut imported = 0;

        for (key, pattern) in &state.patterns {
            if !learning_state.patterns.contains_key(key) {
                learning_state.patterns.insert(key.clone(), pattern.clone());
                imported += 1;
            } else {
                // Merge patterns (take higher Q-value or more visits)
                if let Some(existing) = learning_state.patterns.get_mut(key) {
                    if pattern.visits > existing.visits {
                        *existing = pattern.clone();
                        imported += 1;
                    }
                }
            }
        }

        imported
    }
}

/// GUN swarm statistics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GunSwarmStats {
    pub swarm_id: String,
    pub total_peers: usize,
    pub total_patterns: usize,
    pub total_memories: usize,
    pub relays: usize,
    pub encrypted: bool,
}

/// Builder for GUN-enabled swarm
pub struct GunSwarmBuilder {
    swarm_id: String,
    relays: Vec<String>,
    encrypted: bool,
    sync_interval_ms: u64,
}

impl GunSwarmBuilder {
    pub fn new(swarm_id: &str) -> Self {
        Self {
            swarm_id: swarm_id.to_string(),
            relays: vec![],
            encrypted: true,
            sync_interval_ms: 1000,
        }
    }

    /// Add a GUN relay peer
    pub fn with_relay(mut self, url: &str) -> Self {
        self.relays.push(url.to_string());
        self
    }

    /// Add default public relays
    pub fn with_public_relays(mut self) -> Self {
        self.relays.extend(vec![
            "https://gun-manhattan.herokuapp.com/gun".to_string(),
            "https://gun-us.herokuapp.com/gun".to_string(),
            "https://gun-eu.herokuapp.com/gun".to_string(),
        ]);
        self
    }

    /// Enable/disable encryption
    pub fn encrypted(mut self, enabled: bool) -> Self {
        self.encrypted = enabled;
        self
    }

    /// Set sync interval
    pub fn sync_interval(mut self, ms: u64) -> Self {
        self.sync_interval_ms = ms;
        self
    }

    /// Build GunSync instance
    pub fn build(self, agent_id: &str) -> GunSync {
        let config = GunSwarmConfig {
            relays: if self.relays.is_empty() {
                GunSwarmConfig::default().relays
            } else {
                self.relays
            },
            encrypted: self.encrypted,
            sync_interval_ms: self.sync_interval_ms,
            max_patterns: 10000,
        };

        GunSync::new(&self.swarm_id, agent_id, config)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_gun_sync_local() {
        let sync = GunSwarmBuilder::new("test-swarm")
            .encrypted(false)
            .build("agent-001");

        // Test pattern publishing (local only, no GUN feature)
        let pattern = Pattern::new("edit_ts", "typescript-developer");
        sync.publish_pattern(&pattern).await.unwrap();

        let patterns = sync.get_patterns().await;
        assert_eq!(patterns.len(), 1);

        // Test stats
        let stats = sync.get_stats().await;
        assert_eq!(stats.swarm_id, "test-swarm");
        assert_eq!(stats.total_patterns, 1);
    }

    #[tokio::test]
    async fn test_gun_peer_announce() {
        let sync = GunSwarmBuilder::new("test-swarm").build("agent-001");

        sync.announce_peer().await.unwrap();

        let peers = sync.get_peers().await;
        assert_eq!(peers.len(), 1);
        assert_eq!(peers[0].agent_id, "agent-001");
    }
}
