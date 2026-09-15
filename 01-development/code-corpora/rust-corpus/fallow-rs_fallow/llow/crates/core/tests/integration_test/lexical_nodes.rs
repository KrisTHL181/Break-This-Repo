//! End-to-end test for the Lexical plugin against the `tests/fixtures/lexical-nodes/`
//! fixture. Verifies that the framework-invoked lifecycle methods on custom
//! `DecoratorNode` / `ElementNode` / `TextNode` subclasses are credited as
//! used, while genuinely-unused non-lifecycle methods on the same classes are
//! still reported. The plugin only activates because `lexical` is listed in the
//! fixture's `package.json` dependencies.

use super::common::{create_config, fixture_path};

#[test]
fn lexical_node_lifecycle_members_are_credited_but_real_dead_members_survive() {
    let root = fixture_path("lexical-nodes");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_members: Vec<String> = results
        .unused_class_members
        .iter()
        .map(|finding| {
            format!(
                "{}.{}",
                finding.member.parent_name, finding.member.member_name
            )
        })
        .collect();

    let shared = [
        "getType",
        "clone",
        "importJSON",
        "importDOM",
        "exportJSON",
        "exportDOM",
        "createDOM",
        "updateDOM",
        "updateFromJSON",
        "getTextContent",
    ];
    for class in ["VideoNode", "CustomParagraphNode", "ColoredTextNode"] {
        for member in shared {
            assert!(
                !unused_members.contains(&format!("{class}.{member}")),
                "{class}.{member} is a Lexical runtime hook and must not surface \
                 as unused-class-member; unused_class_members = {unused_members:?}"
            );
        }
    }

    for class in ["VideoNode", "CustomParagraphNode"] {
        assert!(
            !unused_members.contains(&format!("{class}.isInline")),
            "{class}.isInline is an inline-layout hook and must not surface as \
             unused-class-member; unused_class_members = {unused_members:?}"
        );
    }
    assert!(
        unused_members.contains(&"ColoredTextNode.isInline".to_string()),
        "TextNode has no isInline() hook, so ColoredTextNode.isInline must still \
         be reported; unused_class_members = {unused_members:?}"
    );

    assert!(
        !unused_members.contains(&"VideoNode.decorate".to_string()),
        "VideoNode.decorate is a DecoratorNode render hook and must not surface \
         as unused-class-member; unused_class_members = {unused_members:?}"
    );
    assert!(
        unused_members.contains(&"CustomParagraphNode.decorate".to_string()),
        "ElementNode has no decorate() hook, so CustomParagraphNode.decorate must \
         still be reported; unused_class_members = {unused_members:?}"
    );

    for dead in [
        "VideoNode.helperNeverCalled",
        "CustomParagraphNode.paragraphHelper",
        "ColoredTextNode.textHelper",
    ] {
        assert!(
            unused_members.contains(&dead.to_string()),
            "{dead} is genuinely unused and must still be reported; \
             unused_class_members = {unused_members:?}"
        );
    }
}
