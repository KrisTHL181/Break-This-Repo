//! Stable typed identities used by actions and traces.

use std::num::{NonZeroU32, NonZeroU64};

use serde::{Deserialize, Serialize};

use crate::{Result, TestkitError};

macro_rules! string_id {
    ($name:ident, $kind:literal, $doc:literal) => {
        #[doc = $doc]
        #[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
        #[serde(try_from = "String", into = "String")]
        pub struct $name(String);

        impl $name {
            #[doc = concat!("Constructs a non-empty ", $kind, ".")]
            pub fn try_new(value: impl Into<String>) -> Result<Self> {
                let value = value.into();
                if value.is_empty() {
                    return Err(TestkitError::EmptyIdentifier { kind: $kind });
                }
                Ok(Self(value))
            }

            #[doc = concat!("Borrows the ", $kind, ".")]
            pub fn as_str(&self) -> &str {
                &self.0
            }
        }

        impl TryFrom<String> for $name {
            type Error = TestkitError;

            fn try_from(value: String) -> Result<Self> {
                Self::try_new(value)
            }
        }

        impl From<$name> for String {
            fn from(value: $name) -> Self {
                value.0
            }
        }
    };
}

string_id!(TenantId, "tenant ID", "Non-empty workload tenant identity.");
string_id!(
    PropertyName,
    "property name",
    "Non-empty graph property name."
);
string_id!(LabelName, "label name", "Non-empty graph label name.");
string_id!(IndexName, "index name", "Non-empty logical index name.");
string_id!(
    DatabaseName,
    "database name",
    "Non-empty logical database name used by a fixture."
);

macro_rules! non_zero_u64_id {
    ($name:ident, $kind:literal, $doc:literal) => {
        #[doc = $doc]
        #[derive(
            Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize,
        )]
        pub struct $name(NonZeroU64);

        impl $name {
            #[doc = concat!("Constructs a non-zero ", $kind, ".")]
            pub fn new(value: u64) -> Result<Self> {
                let Some(value) = NonZeroU64::new(value) else {
                    return Err(TestkitError::ZeroIdentifier { kind: $kind });
                };
                Ok(Self(value))
            }

            #[doc = concat!("Returns the raw ", $kind, ".")]
            pub const fn get(self) -> u64 {
                self.0.get()
            }
        }
    };
}

non_zero_u64_id!(RequestId, "request ID", "Stable non-zero request identity.");
non_zero_u64_id!(
    GenerationId,
    "generation ID",
    "Stable non-zero physical index generation identity."
);
non_zero_u64_id!(
    CommitId,
    "commit ID",
    "Stable non-zero committed transaction identity."
);

impl GenerationId {
    /// Returns the next generation or an exhaustion error.
    pub fn checked_next(self) -> Result<Self> {
        let Some(next) = self.get().checked_add(1) else {
            return Err(TestkitError::ModelViolation(
                "index generation domain exhausted".to_string(),
            ));
        };
        Self::new(next)
    }
}

/// Runtime identity used to distinguish writer and reader processes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct RuntimeId(NonZeroU32);

impl RuntimeId {
    /// Constructs a non-zero runtime identity.
    pub fn new(value: u32) -> Result<Self> {
        let Some(value) = NonZeroU32::new(value) else {
            return Err(TestkitError::ZeroIdentifier { kind: "runtime ID" });
        };
        Ok(Self(value))
    }

    /// Returns the raw runtime identity.
    pub const fn get(self) -> u32 {
        self.0.get()
    }
}

/// Graph entity identity; zero is valid because graph allocators start at zero.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct EntityId(u64);

impl EntityId {
    /// Wraps any graph entity identity.
    pub const fn new(value: u64) -> Self {
        Self(value)
    }

    /// Returns the raw graph entity identity.
    pub const fn get(self) -> u64 {
        self.0
    }
}

/// Monotonic database sequence; zero represents the empty initial snapshot.
#[derive(
    Debug, Clone, Copy, Default, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize,
)]
pub struct Sequence(u64);

impl Sequence {
    /// Returns the empty initial sequence.
    pub const fn initial() -> Self {
        Self(0)
    }

    /// Wraps a recorded sequence.
    pub const fn new(value: u64) -> Self {
        Self(value)
    }

    /// Returns the raw sequence.
    pub const fn get(self) -> u64 {
        self.0
    }

    /// Returns the next sequence or an exhaustion error.
    pub fn checked_next(self) -> Result<Self> {
        let Some(next) = self.0.checked_add(1) else {
            return Err(TestkitError::ModelViolation(
                "database sequence domain exhausted".to_string(),
            ));
        };
        Ok(Self(next))
    }
}

/// Seed printed and serialized with every randomized run.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct StableSeed(u64);

impl StableSeed {
    /// Wraps a deterministic seed, including zero.
    pub const fn new(value: u64) -> Self {
        Self(value)
    }

    /// Returns the raw seed.
    pub const fn get(self) -> u64 {
        self.0
    }
}

/// Finite f32 stored by its exact IEEE bits so equality is reflexive.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(try_from = "f32", into = "f32")]
pub struct FiniteF32(u32);

impl FiniteF32 {
    /// Validates and stores a finite floating-point value.
    pub fn try_new(value: f32) -> Result<Self> {
        if !value.is_finite() {
            return Err(TestkitError::NonFinite {
                kind: "vector component",
            });
        }
        Ok(Self(value.to_bits()))
    }

    /// Returns the original floating-point value.
    pub fn get(self) -> f32 {
        f32::from_bits(self.0)
    }
}

impl TryFrom<f32> for FiniteF32 {
    type Error = TestkitError;

    fn try_from(value: f32) -> Result<Self> {
        Self::try_new(value)
    }
}

impl From<FiniteF32> for f32 {
    fn from(value: FiniteF32) -> Self {
        value.get()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn string_numeric_sequence_and_float_boundaries_are_closed() {
        assert!(TenantId::try_new("").is_err());
        assert_eq!(TenantId::try_new("a").unwrap().as_str(), "a");
        assert!(RequestId::new(0).is_err());
        assert_eq!(RequestId::new(1).unwrap().get(), 1);
        assert_eq!(Sequence::initial().checked_next().unwrap().get(), 1);
        assert!(Sequence::new(u64::MAX).checked_next().is_err());
        assert!(FiniteF32::try_new(f32::NAN).is_err());
        assert_eq!(FiniteF32::try_new(1.5).unwrap().get(), 1.5);
    }

    #[test]
    fn serde_cannot_bypass_validated_identity_and_float_boundaries() {
        assert!(serde_json::from_str::<TenantId>(r#"""#).is_err());
        assert!(serde_json::from_str::<RuntimeId>("0").is_err());
        assert!(serde_json::from_str::<FiniteF32>("1e100").is_err());
        let id = GenerationId::new(9).unwrap();
        assert_eq!(
            serde_json::from_str::<GenerationId>(&serde_json::to_string(&id).unwrap()).unwrap(),
            id
        );
    }
}
