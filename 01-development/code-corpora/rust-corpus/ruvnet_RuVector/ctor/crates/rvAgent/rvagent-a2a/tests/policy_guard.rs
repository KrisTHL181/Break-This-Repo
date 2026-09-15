//! ADR-159 M1 r2 — `policy_guard.rs`.
//!
//! Three sub-tests, one per enforcement path:
//!   a) `allowed_skills` rejects a skill not in the set.
//!   b) `PolicyGuard::max_concurrency` caps in-flight slots per bucket.
//!   c) `enforce_budget_pre` rejects a pre-task estimate over `max_cost_usd`.

use rvagent_a2a::context::TaskContext;
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::policy::{PolicyError, PolicyGuard, TaskPolicy};
use rvagent_a2a::types::{Message, Part, Role, TaskSpec};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn agent_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

fn spec_with_skill(skill: &str) -> TaskSpec {
    TaskSpec {
        id: "t-test".into(),
        skill: skill.into(),
        message: Message {
            role: Role::User,
            parts: vec![Part::Text {
                text: "hello".into(),
            }],
            metadata: serde_json::Value::Null,
        },
        policy: None,
        context: TaskContext::new_root(agent_id()),
        metadata: serde_json::Value::Null,
    }
}

#[test]
fn allowed_skills_rejects_unlisted_skill() {
    // ADR-159 r2 — Policy and cost control.
    let policy = TaskPolicy {
        allowed_skills: Some(vec!["rag.query".into()]),
        ..TaskPolicy::default()
    };
    let spec = spec_with_skill("not.allowed");
    let res = policy.check_skill(&spec.skill);
    match res {
        Err(PolicyError::SkillDenied(s)) => assert_eq!(s, "not.allowed"),
        other => panic!("expected SkillDenied, got {:?}", other),
    }
}

#[test]
fn allowed_skills_admits_listed_skill() {
    let policy = TaskPolicy {
        allowed_skills: Some(vec!["rag.query".into()]),
        ..TaskPolicy::default()
    };
    assert!(policy.check_skill("rag.query").is_ok());
}

#[test]
fn max_concurrency_caps_tickets_and_release_recovers() {
    // ADR-159 r2 — `max_concurrency` per (caller, skill) bucket.
    let guard = PolicyGuard::new(TaskPolicy {
        max_concurrency: Some(2),
        ..TaskPolicy::default()
    });
    let caller = agent_id();
    let skill = "rag.query";

    let t1 = guard.acquire(caller.clone(), skill).expect("first ticket");
    let t2 = guard.acquire(caller.clone(), skill).expect("second ticket");
    // Third must be rejected while the first two are held.
    match guard.acquire(caller.clone(), skill) {
        Err(PolicyError::ConcurrencyFull { .. }) => {}
        other => panic!("expected ConcurrencyFull, got {:?}", other),
    }
    // Drop one ticket — next acquire succeeds.
    drop(t1);
    let _t3 = guard.acquire(caller, skill).expect("slot freed by drop");
    // Keep t2 live to prove the cap is still in force.
    let _ = t2;
}

#[test]
fn enforce_budget_pre_rejects_overestimate() {
    let policy = TaskPolicy {
        max_cost_usd: Some(0.05),
        ..TaskPolicy::default()
    };
    // 0.10 > 0.05 → reject.
    match policy.enforce_budget_pre(0.10, None) {
        Err(PolicyError::Exceeded { field, .. }) => assert_eq!(field, "max_cost_usd"),
        other => panic!("expected Exceeded(max_cost_usd), got {:?}", other),
    }
    // 0.04 ≤ 0.05 → ok.
    assert!(policy.enforce_budget_pre(0.04, None).is_ok());
}
