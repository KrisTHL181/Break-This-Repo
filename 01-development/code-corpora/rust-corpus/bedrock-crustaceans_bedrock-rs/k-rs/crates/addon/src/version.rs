use std::fmt::Debug;

use serde::{Deserialize, Serialize};

/// A version used in Addons that is either a Vector [a, b, c] or SemVer String.
#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(untagged)]
pub enum AddonSemanticVersion {
    Vector([u32; 3]),
    SemVer(semver::Version),
}
