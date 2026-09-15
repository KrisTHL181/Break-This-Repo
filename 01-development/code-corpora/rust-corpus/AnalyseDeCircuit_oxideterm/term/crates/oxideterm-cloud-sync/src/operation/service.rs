// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

use super::*;

impl CloudSyncOperationService {
    pub fn new() -> Self {
        Self {
            backend: CloudSyncBackend::new(),
            guard: CloudSyncOperationGuard::default(),
        }
    }

    pub(super) async fn prepare_action_secrets(
        &self,
        settings: &CloudSyncSettings,
        secret_provider: &mut impl CloudSyncSecretProvider,
        secrets: &mut crate::secrets::CloudSyncSecrets,
    ) -> Result<()> {
        match settings.backend_type {
            BackendType::OneDrive => {
                let Some(refresh_token) = secrets
                    .microsoft_refresh_token
                    .as_ref()
                    .map(|token| token.as_str())
                    .filter(|token| !token.is_empty())
                else {
                    anyhow::bail!(
                        "missing_microsoft_refresh_token: Microsoft refresh token is not configured"
                    );
                };
                let refreshed = self
                    .backend
                    .refresh_microsoft_access_token(
                        &settings.microsoft_oauth_client_id,
                        refresh_token,
                    )
                    .await?;
                // Refreshed Microsoft tokens cross directly into the keychain-backed
                // provider and stay in zeroizing owners for the rest of this action.
                secret_provider
                    .store_secret(secret_keys::TOKEN, Some(refreshed.access_token.as_str()))?;
                if let Some(next_refresh_token) = refreshed.refresh_token.as_ref() {
                    secret_provider.store_secret(
                        secret_keys::MICROSOFT_REFRESH_TOKEN,
                        Some(next_refresh_token.as_str()),
                    )?;
                }
                secrets.token = Some(refreshed.access_token);
                if let Some(refresh_token) = refreshed.refresh_token {
                    secrets.microsoft_refresh_token = Some(refresh_token);
                }
            }
            BackendType::GoogleDrive => {
                let Some(refresh_token) = secrets
                    .google_refresh_token
                    .as_ref()
                    .map(|token| token.as_str())
                    .filter(|token| !token.is_empty())
                else {
                    anyhow::bail!(
                        "missing_google_refresh_token: Google refresh token is not configured"
                    );
                };
                let refreshed = self
                    .backend
                    .refresh_google_access_token(&settings.google_oauth_client_id, refresh_token)
                    .await?;
                // Google access tokens are short-lived bearer credentials; keep the
                // refreshed value in keychain/session state and zeroizing owners only.
                secret_provider
                    .store_secret(secret_keys::TOKEN, Some(refreshed.access_token.as_str()))?;
                if let Some(next_refresh_token) = refreshed.refresh_token.as_ref() {
                    secret_provider.store_secret(
                        secret_keys::GOOGLE_REFRESH_TOKEN,
                        Some(next_refresh_token.as_str()),
                    )?;
                }
                secrets.token = Some(refreshed.access_token);
                if let Some(refresh_token) = refreshed.refresh_token {
                    secrets.google_refresh_token = Some(refresh_token);
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub async fn check_remote(
        &self,
        settings: &CloudSyncSettings,
        secret_provider: &mut impl CloudSyncSecretProvider,
        skip_if_busy: bool,
        silent_secrets: bool,
        progress: Option<&mut dyn CloudSyncProgressSink>,
    ) -> Result<Option<RemoteMetadata>> {
        let Some(_permit) = self
            .guard
            .begin(CloudSyncOperationKind::Check, skip_if_busy)?
        else {
            return Ok(None);
        };
        let mut noop = |_| {};
        let progress = progress.unwrap_or(&mut noop);
        let mut secrets = get_action_secrets(
            settings,
            secret_provider,
            false,
            if silent_secrets {
                SecretReadMode::Silent
            } else {
                SecretReadMode::Prompt
            },
        )?;
        self.prepare_action_secrets(settings, secret_provider, &mut secrets)
            .await?;
        report_progress(progress, CloudSyncProgressStage::FetchMetadata, 1, 2);
        let metadata = self
            .backend
            .fetch_remote_metadata(settings, &secrets)
            .await?;
        report_progress(progress, CloudSyncProgressStage::Done, 2, 2);
        Ok(Some(metadata))
    }
}
