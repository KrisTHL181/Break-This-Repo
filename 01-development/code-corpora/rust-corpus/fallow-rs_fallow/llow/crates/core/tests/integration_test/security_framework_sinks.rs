//! Integration tests for framework-scoped security-sink catalogue rows (#861).
//!
//! Framework-scoped rows carry an `enabler` package gate so a per-framework
//! idiom is recognized with higher precision: it fires only when the framework
//! is active (the enabler dependency is declared). These tests pin both
//! directions against two fixtures sharing the SAME call shape:
//!   - `security-framework-sinks-angular/`: declares `@angular/platform-browser`,
//!     so `sanitizer.bypassSecurityTrustHtml(userInput)` fires an
//!     `angular-trusted-html` candidate.
//!   - `security-framework-sinks-plain/`: NO Angular dep, so the same
//!     `bypassSecurityTrustHtml` call does NOT fire (precision), while a global
//!     `innerHTML` sink still does (global rows are not framework-gated).

use fallow_config::Severity;
use fallow_core::results::{AnalysisResults, SecurityFindingKind};

use super::common::{create_config_with_rules, fixture_path};

fn analyze_fixture(name: &str) -> AnalysisResults {
    let root = fixture_path(name);
    let config = create_config_with_rules(root, |rules| {
        rules.security_sink = Severity::Warn;
    });
    fallow_core::analyze(&config).expect("analysis should succeed")
}

#[test]
fn angular_bypass_security_trust_fires_when_enabler_present() {
    // The @angular/platform-browser enabler is declared, so the framework-scoped
    // row fires and carries category `angular-trusted-html` + CWE-79.
    let results = analyze_fixture("security-framework-sinks-angular");
    let finding = results
        .security_findings
        .iter()
        .find(|f| matches!(f.kind, SecurityFindingKind::TaintedSink))
        .expect("an Angular bypassSecurityTrust* sink should fire");
    assert_eq!(finding.category.as_deref(), Some("angular-trusted-html"));
    assert_eq!(finding.cwe, Some(79));
    assert!(
        finding
            .path
            .to_string_lossy()
            .replace('\\', "/")
            .ends_with("src/sink.ts"),
        "candidate anchors on the sink file"
    );
}

#[test]
fn bypass_security_trust_does_not_fire_without_enabler() {
    // The plain fixture has the SAME bypassSecurityTrustHtml call shape but no
    // @angular/platform-browser dependency. The framework-scoped row is gated on
    // that enabler, so it must NOT produce an angular-trusted-html candidate.
    let results = analyze_fixture("security-framework-sinks-plain");
    assert!(
        results
            .security_findings
            .iter()
            .all(|f| f.category.as_deref() != Some("angular-trusted-html")),
        "a bypassSecurityTrustHtml call without the Angular enabler must not fire"
    );
}

#[test]
fn global_rows_still_fire_without_any_enabler() {
    // The plain fixture also has a non-literal `el.innerHTML = userInput`. The
    // global dangerous-html row carries no enabler, so framework gating does not
    // suppress it: a global candidate is still produced.
    let results = analyze_fixture("security-framework-sinks-plain");
    assert!(
        results
            .security_findings
            .iter()
            .any(|f| f.category.as_deref() == Some("dangerous-html")),
        "the global dangerous-html row must still fire regardless of framework gating"
    );
}
