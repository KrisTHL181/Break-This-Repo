use super::common::{create_config, fixture_path};

#[test]
fn new_expression_receivers_credit_class_members() {
    let root = fixture_path("issue-605-new-class-member");
    let mut config = create_config(root);
    config.rules.unused_class_members = fallow_config::Severity::Error;
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused: Vec<String> = results
        .unused_class_members
        .iter()
        .map(|m| format!("{}.{}", m.member.parent_name, m.member.member_name))
        .collect();

    for credited in ["TracesRepository.search", "TracesRepository.getTrace"] {
        assert!(
            !unused.contains(&credited.to_string()),
            "{credited} is called via `new TracesRepository(client).<method>()` and must be \
             credited (issue #605), found: {unused:?}"
        );
    }

    for credited in [
        "OptionBuilder.addDefault",
        "OptionBuilder.addFromConfig",
        "OptionBuilder.addFromCli",
        "OptionBuilder.build",
    ] {
        assert!(
            !unused.contains(&credited.to_string()),
            "{credited} is reached through a fluent chain off `new OptionBuilder()` and must be \
             credited (issue #605), found: {unused:?}"
        );
    }

    assert!(
        !unused.contains(&"OptionBuilder.peek".to_string()),
        "OptionBuilder.peek is called directly off `new OptionBuilder()` and must be credited, \
         found: {unused:?}"
    );

    assert!(
        !unused.contains(&"URL.parse".to_string()),
        "URL.parse is called via `new URL().parse()` on a USER class named like a builtin and \
         must be credited (issue #605), found: {unused:?}"
    );

    for flagged in [
        "TracesRepository.unusedRepoMethod",
        "OptionBuilder.addUnused",
        "OptionBuilder.afterPeek",
        "URL.unusedOnUrl",
    ] {
        assert!(
            unused.contains(&flagged.to_string()),
            "{flagged} has no crediting call site and must remain flagged unused (no blanket \
             over-credit), found: {unused:?}"
        );
    }
}
