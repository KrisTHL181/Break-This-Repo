use super::common::{create_config, fixture_path};

#[test]
fn inheritance_propagates_this_accesses_to_children() {
    let root = fixture_path("inheritance-project");
    let mut config = create_config(root);
    config.rules.unused_class_members = fallow_config::Severity::Error;
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_members: Vec<String> = results
        .unused_class_members
        .iter()
        .map(|m| format!("{}.{}", m.member.parent_name, m.member.member_name))
        .collect();

    assert!(
        !unused_members.contains(&"Circle.kind".to_string()),
        "Circle.kind should be used via this.kind in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Circle.getArea".to_string()),
        "Circle.getArea should be used via this.getArea() in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Circle.getPerimeter".to_string()),
        "Circle.getPerimeter should be used via this.getPerimeter() in BaseShape: {unused_members:?}"
    );

    assert!(
        !unused_members.contains(&"Rectangle.kind".to_string()),
        "Rectangle.kind should be used via this.kind in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Rectangle.getArea".to_string()),
        "Rectangle.getArea should be used via this.getArea() in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Rectangle.getPerimeter".to_string()),
        "Rectangle.getPerimeter should be used via this.getPerimeter() in BaseShape: {unused_members:?}"
    );

    assert!(
        !unused_members.contains(&"default.kind".to_string()),
        "default export class kind should be used via this.kind in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"default.getArea".to_string()),
        "default export class getArea should be used via this.getArea() in BaseShape: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"default.getPerimeter".to_string()),
        "default export class getPerimeter should be used via this.getPerimeter() in BaseShape: {unused_members:?}"
    );
}

#[test]
fn inheritance_and_interface_members_follow_barrel_origins() {
    let root = fixture_path("inheritance-barrel-project");
    let mut config = create_config(root);
    config.rules.unused_class_members = fallow_config::Severity::Error;
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_members: Vec<String> = results
        .unused_class_members
        .iter()
        .map(|m| format!("{}.{}", m.member.parent_name, m.member.member_name))
        .collect();

    assert!(
        !unused_members.contains(&"Circle.kind".to_string()),
        "Circle.kind should be credited from BaseShape.describe through the barrel import: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Circle.area".to_string()),
        "Circle.area should be credited from BaseShape.describe through the barrel import: {unused_members:?}"
    );
    assert!(
        !unused_members.contains(&"Circle.render".to_string()),
        "Circle.render should be credited from interface-typed access when implements uses a barrel import: {unused_members:?}"
    );
    assert!(
        unused_members.contains(&"Circle.unusedHelper".to_string()),
        "unrelated class members should still be reported: {unused_members:?}"
    );
}
