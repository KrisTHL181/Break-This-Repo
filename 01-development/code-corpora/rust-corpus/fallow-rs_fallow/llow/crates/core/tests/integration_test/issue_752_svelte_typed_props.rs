use super::common::{create_config, fixture_path};

#[test]
fn svelte_typed_prop_member_access_credits_class_members() {
    let root = fixture_path("issue-752-svelte-template-typed-props");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused: Vec<(&str, &str)> = results
        .unused_class_members
        .iter()
        .map(|m| (m.member.parent_name.as_str(), m.member.member_name.as_str()))
        .collect();

    assert!(
        !unused.contains(&("ResultState", "onOpen")),
        "ResultState.onOpen is called from the component script via the typed prop, found: {unused:?}"
    );
    assert!(
        !unused.contains(&("ResultState", "pin")),
        "ResultState.pin is called from markup `onclick={{() => resultState.pin(...)}}`, found: {unused:?}"
    );
    assert!(
        !unused.contains(&("ResultState", "addSkipRule")),
        "ResultState.addSkipRule is called from markup inside `{{#if}}`, found: {unused:?}"
    );
    assert!(
        !unused.contains(&("ResultState", "updateLabel")),
        "ResultState.updateLabel is called from markup inside `{{#if}}`, found: {unused:?}"
    );
    assert!(
        !unused.contains(&("ResultState", "labelInput")),
        "ResultState.labelInput is bound via `bind:value={{resultState.labelInput}}`, found: {unused:?}"
    );
    assert!(
        !unused.contains(&("ResultState", "labelMessage")),
        "ResultState.labelMessage is read in `{{#if resultState.labelMessage}}`, found: {unused:?}"
    );

    assert!(
        unused.contains(&("ResultState", "neverCalled")),
        "ResultState.neverCalled is never referenced and should be flagged, found: {unused:?}"
    );
}
