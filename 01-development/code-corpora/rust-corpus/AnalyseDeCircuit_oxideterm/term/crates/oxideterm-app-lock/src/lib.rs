// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

//! Password verification storage for the application-level workspace lock.

use anyhow::{Context, Result, anyhow};
use argon2::{Algorithm, Argon2, Params, Version};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
use oxideterm_portable_runtime::keystore::{self as portable_keystore, PortableKeystoreError};
use oxideterm_secret_store::NativeSecretStore;
use rand::RngCore;
use serde::{Deserialize, Serialize};
use subtle::ConstantTimeEq;
use zeroize::Zeroizing;

mod biometric;

pub use biometric::{
    BiometricAvailability, BiometricOutcome, authenticate_biometric, biometric_availability,
};

const APP_LOCK_SERVICE: &str = "com.oxideterm.app-lock";
const APP_LOCK_ACCOUNT_SUFFIX: &str = "workspace-lock-verifier";
const VERIFIER_VERSION: u32 = 1;
const SALT_LENGTH: usize = 16;
const DIGEST_LENGTH: usize = 32;
// OWASP's baseline Argon2id profile keeps interactive verification practical
// while making offline guesses materially more expensive than a fast hash.
const ARGON_MEMORY_KIB: u32 = 19 * 1024;
const ARGON_ITERATIONS: u32 = 2;
const ARGON_PARALLELISM: u32 = 1;

/// Owns the platform-specific persistence boundary for the application lock verifier.
#[derive(Clone)]
pub struct AppLockStore {
    service: String,
    account: String,
}

impl Default for AppLockStore {
    fn default() -> Self {
        Self::new()
    }
}

impl AppLockStore {
    pub fn new() -> Self {
        Self {
            service: APP_LOCK_SERVICE.to_string(),
            account: format!("{}@{APP_LOCK_ACCOUNT_SUFFIX}", whoami::username()),
        }
    }

    /// Checks verifier metadata without retrieving it, so settings rendering cannot prompt.
    pub fn is_configured(&self) -> Result<bool> {
        if portable_mode()? {
            return portable_keystore::secret_exists(&self.service, &self.account)
                .or_else(|error| match error {
                    PortableKeystoreError::NotFound(_) => Ok(false),
                    other => Err(other),
                })
                .context("failed to inspect the portable application lock verifier");
        }

        NativeSecretStore::new(&self.service)
            .exists(&self.account)
            .context("failed to inspect the application lock verifier")
    }

    /// Replaces the verifier. The password is zeroized by the caller-owned wrapper on return.
    pub fn set_password(&self, password: Zeroizing<String>) -> Result<()> {
        if password.is_empty() {
            return Err(anyhow!("application lock password cannot be empty"));
        }
        let verifier = PasswordVerifier::create(password.as_str())?;
        let encoded = Zeroizing::new(
            serde_json::to_string(&verifier)
                .context("failed to encode the application lock verifier")?,
        );
        self.store_encoded_verifier(encoded.as_str())
    }

    /// Verifies a supplied password in constant time after the Argon2id derivation.
    pub fn verify_password(&self, password: Zeroizing<String>) -> Result<bool> {
        let Some(encoded) = self.load_encoded_verifier()? else {
            return Ok(false);
        };
        let verifier: PasswordVerifier = serde_json::from_str(encoded.as_str())
            .context("application lock verifier is invalid")?;
        verifier.verify(password.as_str())
    }

    /// Changes the password only after proving knowledge of the current password.
    pub fn change_password(
        &self,
        current_password: Zeroizing<String>,
        new_password: Zeroizing<String>,
    ) -> Result<bool> {
        if !self.verify_password(current_password)? {
            return Ok(false);
        }
        self.set_password(new_password)?;
        Ok(true)
    }

    /// Removes the verifier only after proving knowledge of the current password.
    pub fn remove_password(&self, current_password: Zeroizing<String>) -> Result<bool> {
        if !self.verify_password(current_password)? {
            return Ok(false);
        }
        if portable_mode()? {
            portable_keystore::delete_secret(&self.service, &self.account)
                .context("failed to remove the portable application lock verifier")?;
        } else {
            NativeSecretStore::new(&self.service)
                .delete(&self.account)
                .context("failed to remove the application lock verifier")?;
        }
        Ok(true)
    }

    fn store_encoded_verifier(&self, encoded: &str) -> Result<()> {
        if portable_mode()? {
            return portable_keystore::store_secret(&self.service, &self.account, encoded)
                .context("failed to store the portable application lock verifier");
        }
        NativeSecretStore::new(&self.service)
            .store(&self.account, encoded)
            .context("failed to store the application lock verifier")
    }

    fn load_encoded_verifier(&self) -> Result<Option<Zeroizing<String>>> {
        if portable_mode()? {
            return match portable_keystore::get_secret(&self.service, &self.account) {
                Ok(verifier) => Ok(Some(verifier)),
                Err(PortableKeystoreError::NotFound(_)) => Ok(None),
                Err(error) => {
                    Err(error).context("failed to load the portable application lock verifier")
                }
            };
        }
        NativeSecretStore::new(&self.service)
            .get(&self.account)
            .context("failed to load the application lock verifier")
    }
}

#[derive(Serialize, Deserialize)]
struct PasswordVerifier {
    version: u32,
    salt: String,
    digest: String,
}

impl PasswordVerifier {
    fn create(password: &str) -> Result<Self> {
        let mut salt = [0_u8; SALT_LENGTH];
        rand::rngs::OsRng.fill_bytes(&mut salt);
        let digest = derive_digest(password, &salt)?;
        Ok(Self {
            version: VERIFIER_VERSION,
            salt: BASE64.encode(salt),
            digest: BASE64.encode(*digest),
        })
    }

    fn verify(&self, password: &str) -> Result<bool> {
        if self.version != VERIFIER_VERSION {
            return Err(anyhow!(
                "unsupported application lock verifier version {}",
                self.version
            ));
        }
        let salt = BASE64
            .decode(&self.salt)
            .context("application lock verifier salt is invalid")?;
        let expected = Zeroizing::new(
            BASE64
                .decode(&self.digest)
                .context("application lock verifier digest is invalid")?,
        );
        if salt.len() != SALT_LENGTH || expected.len() != DIGEST_LENGTH {
            return Err(anyhow!("application lock verifier has invalid lengths"));
        }
        let actual = derive_digest(password, &salt)?;
        Ok(bool::from(actual.as_slice().ct_eq(expected.as_slice())))
    }
}

fn derive_digest(password: &str, salt: &[u8]) -> Result<Zeroizing<[u8; DIGEST_LENGTH]>> {
    let params = Params::new(
        ARGON_MEMORY_KIB,
        ARGON_ITERATIONS,
        ARGON_PARALLELISM,
        Some(DIGEST_LENGTH),
    )
    .map_err(|_| anyhow!("invalid application lock KDF parameters"))?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);
    let mut digest = Zeroizing::new([0_u8; DIGEST_LENGTH]);
    argon2
        .hash_password_into(password.as_bytes(), salt, &mut *digest)
        .map_err(|_| anyhow!("application lock password derivation failed"))?;
    Ok(digest)
}

fn portable_mode() -> Result<bool> {
    oxideterm_portable_runtime::is_portable_mode()
        .context("failed to determine application lock storage mode")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn password_verifier_accepts_only_the_original_password() {
        let verifier =
            PasswordVerifier::create("correct horse battery staple").expect("create verifier");

        assert!(
            verifier
                .verify("correct horse battery staple")
                .expect("verify password")
        );
        assert!(!verifier.verify("incorrect").expect("reject password"));
    }

    #[test]
    fn password_verifier_uses_a_fresh_random_salt() {
        let first = PasswordVerifier::create("same password").expect("first verifier");
        let second = PasswordVerifier::create("same password").expect("second verifier");

        assert_ne!(first.salt, second.salt);
        assert_ne!(first.digest, second.digest);
    }

    #[test]
    fn malformed_verifier_is_rejected_without_panicking() {
        let verifier = PasswordVerifier {
            version: VERIFIER_VERSION,
            salt: BASE64.encode([0_u8; 3]),
            digest: BASE64.encode([0_u8; DIGEST_LENGTH]),
        };

        assert!(verifier.verify("password").is_err());
    }
}
