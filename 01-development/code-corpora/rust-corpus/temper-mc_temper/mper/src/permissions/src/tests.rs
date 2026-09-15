use crate::player::PlayerPermission;
use crate::{Access, Permissions};

#[test]
fn denies_permission_when_unset() {
    let player = PlayerPermission::new();

    assert!(!player.can(Permissions::Kill));
}

#[test]
fn allows_explicitly_allowed_permission() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::Kill, Access::Allow);

    assert!(player.can(Permissions::Kill));
    assert!(!player.can(Permissions::Teleport));
}

#[test]
fn denies_explicitly_denied_permission() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::Kill, Access::Deny);

    assert!(!player.can(Permissions::Kill));
}

#[test]
fn all_allow_grants_unset_permissions() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::ALL, Access::Allow);

    assert!(player.can(Permissions::Kill));
    assert!(player.can(Permissions::Teleport));
}

#[test]
fn specific_deny_overrides_all_allow() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::ALL, Access::Allow);
    player.set_permission(Permissions::Kill, Access::Deny);

    assert!(!player.can(Permissions::Kill));
    assert!(player.can(Permissions::Teleport));
}

#[test]
fn all_deny_does_not_block_specific_allow() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::ALL, Access::Deny);
    player.set_permission(Permissions::Kill, Access::Allow);

    assert!(player.can(Permissions::Kill));
    assert!(!player.can(Permissions::Teleport));
}

#[test]
fn setting_permission_replaces_previous_value() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::Kill, Access::Deny);
    assert!(!player.can(Permissions::Kill));

    player.set_permission(Permissions::Kill, Access::Allow);
    assert!(player.can(Permissions::Kill));

    player.set_permission(Permissions::Kill, Access::Deny);
    assert!(!player.can(Permissions::Kill));
}

#[test]
fn removing_specific_permission_revokes_or_falls_back_to_all() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::Kill, Access::Allow);
    assert!(player.can(Permissions::Kill));

    player.remove_permission(&Permissions::Kill);
    assert!(!player.can(Permissions::Kill));

    player.set_permission(Permissions::ALL, Access::Allow);
    player.set_permission(Permissions::Kill, Access::Deny);
    assert!(!player.can(Permissions::Kill));

    player.remove_permission(&Permissions::Kill);
    assert!(player.can(Permissions::Kill));
}

#[test]
fn removing_all_permission_revokes_blanket_access() {
    let mut player = PlayerPermission::new();

    player.set_permission(Permissions::ALL, Access::Allow);
    assert!(player.can(Permissions::Ban));

    player.remove_permission(&Permissions::ALL);
    assert!(!player.can(Permissions::Ban));
}
