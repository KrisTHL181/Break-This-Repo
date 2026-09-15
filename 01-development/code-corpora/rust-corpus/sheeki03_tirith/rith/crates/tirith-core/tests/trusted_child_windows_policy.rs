use tirith_core::capsule::windows::command_line_from_parts;
use tirith_core::trusted_child::{
    evaluate_windows_trust, windows_access_mask_grants_replacement,
    windows_provenance_is_system_helper_approved, WindowsExecutableSource, WindowsOwnerClass,
    WindowsTrustFacts, WindowsTrustProvenance,
};

fn secure_facts() -> WindowsTrustFacts {
    WindowsTrustFacts {
        broad_write_access: false,
        leaf_owner: WindowsOwnerClass::CurrentUser,
        owner_chain_trusted: true,
        secure_user_install: true,
        protected_install_root: false,
        authenticode_trusted: false,
    }
}

#[test]
fn windows_path_discovery_rejects_broad_write_even_when_signed() {
    let facts = WindowsTrustFacts {
        broad_write_access: true,
        authenticode_trusted: true,
        ..secure_facts()
    };
    assert!(evaluate_windows_trust(WindowsExecutableSource::PathSearch, facts).is_err());
}

#[test]
fn windows_path_discovery_accepts_valid_authenticode_with_a_secure_acl() {
    let facts = WindowsTrustFacts {
        secure_user_install: false,
        authenticode_trusted: true,
        ..secure_facts()
    };
    assert_eq!(
        evaluate_windows_trust(WindowsExecutableSource::PathSearch, facts).unwrap(),
        WindowsTrustProvenance::Authenticode
    );
}

#[test]
fn windows_path_discovery_accepts_a_secure_user_owned_install() {
    assert_eq!(
        evaluate_windows_trust(WindowsExecutableSource::PathSearch, secure_facts()).unwrap(),
        WindowsTrustProvenance::SecureUserInstall
    );
}

#[test]
fn windows_path_discovery_accepts_trusted_ownership_without_further_provenance() {
    // The Windows collector derives `secure_user_install` from the same
    // ownership facts the gate above already requires, so this branch is
    // reached whenever that gate passes: an unsigned PATH executable outside a
    // protected root is accepted on owner-chain evidence alone. Pin that so
    // narrowing it — which would refuse unsigned user-local installs — has to
    // be a deliberate change rather than a silent one.
    let facts = WindowsTrustFacts {
        leaf_owner: WindowsOwnerClass::CurrentUser,
        owner_chain_trusted: true,
        secure_user_install: true,
        protected_install_root: false,
        authenticode_trusted: false,
        broad_write_access: false,
    };
    // This revision has no system-helper predicate yet, so the pin covers the
    // provenance decision only.
    let provenance = evaluate_windows_trust(WindowsExecutableSource::PathSearch, facts).unwrap();
    assert_eq!(provenance, WindowsTrustProvenance::SecureUserInstall);
}

#[test]
fn windows_path_discovery_rejects_unknown_unsigned_provenance() {
    let facts = WindowsTrustFacts {
        leaf_owner: WindowsOwnerClass::Other,
        owner_chain_trusted: false,
        secure_user_install: false,
        ..secure_facts()
    };
    assert!(evaluate_windows_trust(WindowsExecutableSource::PathSearch, facts).is_err());
}

#[test]
fn windows_explicit_absolute_still_requires_secure_ownership() {
    let facts = WindowsTrustFacts {
        owner_chain_trusted: false,
        secure_user_install: false,
        ..secure_facts()
    };
    assert!(evaluate_windows_trust(WindowsExecutableSource::ExplicitAbsolute, facts).is_err());
    assert_eq!(
        evaluate_windows_trust(WindowsExecutableSource::ExplicitAbsolute, secure_facts()).unwrap(),
        WindowsTrustProvenance::ExplicitAbsolute
    );
    let signed_but_untrusted_owner = WindowsTrustFacts {
        owner_chain_trusted: false,
        secure_user_install: false,
        authenticode_trusted: true,
        ..secure_facts()
    };
    assert!(evaluate_windows_trust(
        WindowsExecutableSource::ExplicitAbsolute,
        signed_but_untrusted_owner
    )
    .is_err());
}

#[test]
fn windows_system_candidate_requires_protected_or_signed_provenance() {
    let protected = WindowsTrustFacts {
        leaf_owner: WindowsOwnerClass::Administrators,
        secure_user_install: false,
        protected_install_root: true,
        ..secure_facts()
    };
    assert_eq!(
        evaluate_windows_trust(WindowsExecutableSource::SystemCandidate, protected).unwrap(),
        WindowsTrustProvenance::SystemCandidate
    );
}

#[test]
fn windows_current_user_owner_is_not_upgraded_by_a_protected_root_label() {
    let facts = WindowsTrustFacts {
        protected_install_root: true,
        ..secure_facts()
    };
    assert_eq!(
        evaluate_windows_trust(WindowsExecutableSource::PathSearch, facts).unwrap(),
        WindowsTrustProvenance::SecureUserInstall
    );
}

#[test]
fn windows_system_helper_policy_rejects_same_user_only_provenance() {
    for approved in [
        WindowsTrustProvenance::SystemCandidate,
        WindowsTrustProvenance::Authenticode,
        WindowsTrustProvenance::ProtectedInstall,
    ] {
        assert!(windows_provenance_is_system_helper_approved(approved));
    }
    for rejected in [
        WindowsTrustProvenance::ExplicitAbsolute,
        WindowsTrustProvenance::CurrentProcess,
        WindowsTrustProvenance::SecureUserInstall,
    ] {
        assert!(!windows_provenance_is_system_helper_approved(rejected));
    }
}

#[test]
fn windows_read_and_synchronize_masks_are_not_classified_as_writers() {
    const READ_CONTROL: u32 = 0x0002_0000;
    const SYNCHRONIZE: u32 = 0x0010_0000;
    const FILE_GENERIC_READ: u32 = 0x0012_0089;
    const FILE_WRITE_DATA: u32 = 0x0000_0002;
    const FILE_DELETE_CHILD: u32 = 0x0000_0040;
    const GENERIC_WRITE: u32 = 0x4000_0000;

    assert!(!windows_access_mask_grants_replacement(READ_CONTROL, true));
    assert!(!windows_access_mask_grants_replacement(SYNCHRONIZE, true));
    assert!(!windows_access_mask_grants_replacement(
        FILE_GENERIC_READ,
        true
    ));
    assert!(windows_access_mask_grants_replacement(
        FILE_WRITE_DATA,
        true
    ));
    assert!(windows_access_mask_grants_replacement(
        FILE_DELETE_CHILD,
        true
    ));
    assert!(windows_access_mask_grants_replacement(GENERIC_WRITE, true));
}

#[test]
fn shared_windows_command_line_quotes_program_and_adversarial_arguments() {
    let args = vec![
        "plain".to_string(),
        "two words".to_string(),
        r#"quote\"inside"#.to_string(),
        r#"trailing\\"#.to_string(),
        String::new(),
    ];
    let line = command_line_from_parts(r#"C:\Program Files\Tirith\tirith.exe"#, &args);
    assert_eq!(
        line,
        r#""C:\Program Files\Tirith\tirith.exe" plain "two words" "quote\\\"inside" trailing\\ """#
    );
}
