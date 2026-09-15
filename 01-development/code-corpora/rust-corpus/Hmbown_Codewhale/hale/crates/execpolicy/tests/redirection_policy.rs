use codewhale_execpolicy::{
    AskForApproval, ExecApprovalRequirement, ExecPolicyContext, ExecPolicyEngine, PermissionAction,
    Ruleset, ToolAskRule, shell_expand::expanded_commands,
};

fn context(command: &str, approval: AskForApproval) -> ExecPolicyContext<'_> {
    ExecPolicyContext {
        command,
        cwd: "/workspace",
        tool: Some("exec_shell"),
        path: None,
        ask_for_approval: approval,
        sandbox_mode: None,
    }
}

#[test]
fn redirection_syntax_preserves_prefix_and_typed_denials() {
    let engines = [
        ExecPolicyEngine::new(vec![], vec!["printf probe".to_string()]),
        ExecPolicyEngine::with_rulesets(vec![Ruleset::user(vec![], vec![]).with_ask_rules(vec![
            ToolAskRule {
                action: PermissionAction::Deny,
                ..ToolAskRule::exec_shell("printf probe")
            },
        ])]),
    ];
    for command in [
        "printf probe",
        "printf>marker probe",
        "printf>>marker probe",
        "printf<marker probe",
        "printf<>marker probe",
        "printf>|marker probe",
        "printf>&1 probe",
        "printf<&0 probe",
        "printf&>marker probe",
        "printf&>>marker probe",
        "printf<<END probe\ntext\nEND",
        "printf<<-END probe\n\ttext\nEND",
        "printf<<<text probe",
        ">marker printf probe",
        "2>marker printf probe",
        "{output}>marker printf probe",
        "printf 2>marker probe",
        "printf 2>&1 probe",
        "printf 3<&0 probe",
        "printf 3>&- probe",
        "printf >'marker with spaces' probe",
        "printf >\"marker with spaces\" probe",
        "printf >marker\\ with\\ spaces probe",
        "printf >one 2>two probe",
        "printf >$(echo marker) probe",
        "printf >`echo marker` probe",
        "printf >${marker:-out} probe",
        "printf > >(cat) probe",
        "env >marker printf probe",
        "sh >marker -c 'printf probe'",
        "$(echo) sh >marker -c 'printf probe'",
        "echo ok; >marker printf probe",
    ] {
        for engine in &engines {
            let decision = engine
                .check(context(command, AskForApproval::Never))
                .unwrap();
            assert!(!decision.allow, "{command:?}");
            assert!(!decision.requires_approval, "{command:?}");
            assert!(matches!(
                decision.requirement,
                ExecApprovalRequirement::Forbidden { .. }
            ));
        }
    }
}

#[test]
fn substitutions_in_redirection_operands_keep_their_own_denials() {
    let engine = ExecPolicyEngine::new(vec![], vec!["printf probe".to_string()]);
    for command in [
        "echo >$(printf probe)",
        "echo >`printf probe`",
        "echo >\"$(printf probe)\"",
        "echo >${output:-$(printf probe)}",
        "echo > >(printf probe)",
        "echo < <(printf probe)",
        "echo <<<$(printf probe)",
    ] {
        assert!(
            !engine
                .check(context(command, AskForApproval::Never))
                .unwrap()
                .allow,
            "{command:?}"
        );
    }
}

#[test]
fn quoted_operators_and_redirect_targets_remain_data() {
    let engine = ExecPolicyEngine::new(vec![], vec!["printf".to_string()]);
    for command in [
        "echo probe",
        "'printf>marker' probe",
        "\"printf<marker\" probe",
        "printf\\>marker probe",
        "echo >printf probe",
        ">printf echo probe",
        "echo >'$(printf probe)'",
        "echo >marker\\>printf probe",
    ] {
        assert!(
            engine
                .check(context(command, AskForApproval::Never))
                .unwrap()
                .allow,
            "{command:?}"
        );
    }
    for (command, expected) in [
        ("echo '2'>marker probe", "echo 2 probe"),
        ("echo \\2>marker probe", "echo 2 probe"),
        ("echo 2 >marker probe", "echo 2 probe"),
        ("printf2>marker probe", "printf2 probe"),
        ("echo '{output}'>marker probe", "echo {output} probe"),
    ] {
        assert!(expanded_commands(command).iter().any(|c| c == expected));
    }
}

#[test]
fn removing_redirections_does_not_expand_trusted_grants() {
    let engine = ExecPolicyEngine::new(vec!["printf".to_string()], vec![]);
    let decision = engine
        .check(context(
            ">marker printf probe",
            AskForApproval::UnlessTrusted,
        ))
        .unwrap();
    assert!(decision.allow && decision.requires_approval);
}

#[cfg(unix)]
#[test]
fn harmless_shell_reference_agrees_with_the_denied_command_candidates() {
    // Execute only this fixed harmless fixture in an owned temporary directory
    // to compare the real shell's words with the policy's candidate commands.
    let unique = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    let dir = std::env::temp_dir().join(format!("cw-policy-{}-{unique}", std::process::id()));
    std::fs::create_dir(&dir).unwrap();
    for command in [
        "printf>marker probe",
        ">marker printf probe",
        "printf 2>&1 >marker probe",
    ] {
        let status = std::process::Command::new("/bin/sh")
            .args(["-c", command])
            .current_dir(&dir)
            .status()
            .unwrap();
        assert!(status.success());
        assert_eq!(std::fs::read(dir.join("marker")).unwrap(), b"probe");
        assert!(
            expanded_commands(command)
                .iter()
                .any(|c| c == "printf probe")
        );
    }
    std::fs::remove_dir_all(dir).unwrap();
}
