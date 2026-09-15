use std::{
    collections::{HashSet, VecDeque},
    sync::Arc,
};

use parking_lot::Mutex;
use tokio::sync::watch;

use super::{AgentError, AgentRunRef};

pub const DEFAULT_AGENT_CONCURRENCY: usize = 2;
pub const MAX_AGENT_CONCURRENCY: usize = 4;

struct Schedule {
    limit: usize,
    queued: VecDeque<AgentRunRef>,
    running: HashSet<AgentRunRef>,
}

#[derive(Clone)]
pub(super) struct AgentScheduler {
    state: Arc<Mutex<Schedule>>,
    changes: watch::Sender<u64>,
}

/// Held only during runnable work, never across approval, parent or resource waits.
pub struct AgentPermit {
    scheduler: AgentScheduler,
    run: AgentRunRef,
}

impl Drop for AgentPermit {
    fn drop(&mut self) {
        self.scheduler.state.lock().running.remove(&self.run);
        self.scheduler.changed();
    }
}

struct QueuedRun {
    scheduler: AgentScheduler,
    run: AgentRunRef,
}

impl Drop for QueuedRun {
    fn drop(&mut self) {
        let mut state = self.scheduler.state.lock();
        let count = state.queued.len();
        state.queued.retain(|run| *run != self.run);
        let removed = state.queued.len() != count;
        drop(state);
        if removed {
            self.scheduler.changed();
        }
    }
}

impl AgentScheduler {
    pub(super) fn new(limit: usize) -> Self {
        Self {
            state: Arc::new(Mutex::new(Schedule {
                limit: limit.clamp(1, MAX_AGENT_CONCURRENCY),
                queued: VecDeque::new(),
                running: HashSet::new(),
            })),
            changes: watch::channel(0).0,
        }
    }

    fn changed(&self) {
        self.changes
            .send_modify(|value| *value = value.wrapping_add(1));
    }

    pub(super) fn set_limit(&self, limit: usize) {
        self.state.lock().limit = limit.clamp(1, MAX_AGENT_CONCURRENCY);
        self.changed();
    }

    pub(super) async fn acquire(
        &self,
        run: AgentRunRef,
        mut cancelled: watch::Receiver<bool>,
    ) -> Result<AgentPermit, AgentError> {
        let mut changes = self.changes.subscribe();
        {
            let mut state = self.state.lock();
            if state.running.contains(&run) || state.queued.contains(&run) {
                return Err(AgentError::InvalidState);
            }
            state.queued.push_back(run.clone());
        }
        // An aborted future must not leave a ticket blocking all later requests.
        let _queued = QueuedRun {
            scheduler: self.clone(),
            run: run.clone(),
        };
        loop {
            if *cancelled.borrow() {
                return Err(AgentError::Cancelled);
            }
            {
                let mut state = self.state.lock();
                if state.running.len() < state.limit && state.queued.front() == Some(&run) {
                    state.queued.pop_front();
                    state.running.insert(run.clone());
                    drop(state);
                    self.changed();
                    return Ok(AgentPermit {
                        scheduler: self.clone(),
                        run,
                    });
                }
            }
            tokio::select! {
                _ = cancelled.changed() => return Err(AgentError::Cancelled),
                result = changes.changed() => { result.map_err(|_| AgentError::Cancelled)?; }
            }
        }
    }
}
