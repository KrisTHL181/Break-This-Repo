// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

use super::*;

impl CloudSyncOperationService {
    pub async fn upload_now(
        &self,
        connection_store: &ConnectionStore,
        forwarding_registry: &ForwardingRegistry,
        settings_store: &SettingsStore,
        settings: &CloudSyncSettings,
        secret_provider: &mut impl CloudSyncSecretProvider,
        options: UploadOptions,
        progress: Option<&mut dyn CloudSyncProgressSink>,
    ) -> std::result::Result<Option<UploadOutcome>, CloudSyncUploadError> {
        let Some(_permit) = self
            .guard
            .begin(CloudSyncOperationKind::Upload, options.skip_if_busy)?
        else {
            return Ok(None);
        };

        let mut noop = |_| {};
        let progress = progress.unwrap_or(&mut noop);
        let local_snapshot = build_local_snapshot(
            connection_store,
            forwarding_registry,
            settings_store,
            options.last_synced_structured_state.as_ref(),
            options.raw_sync_scope.as_ref(),
        )?;
        let requires_password = local_snapshot.scope.sync_app_settings
            || local_snapshot.scope.sync_plugin_settings
            || local_snapshot.scope.sync_sensitive_credentials;
        let mut secrets = get_action_secrets(
            settings,
            secret_provider,
            requires_password,
            if options.automatic {
                SecretReadMode::Silent
            } else {
                SecretReadMode::Prompt
            },
        )?;
        self.prepare_action_secrets(settings, secret_provider, &mut secrets)
            .await?;
        if requires_password
            && secrets
                .sync_password
                .as_ref()
                .map(|password| password.as_str())
                .unwrap_or_default()
                .is_empty()
        {
            return Err(
                anyhow::anyhow!("missing_sync_password: cloud sync password is required").into(),
            );
        }

        let export_units = local_snapshot.upload_units;
        let upload_units = export_units + 1;
        let total = 4 + export_units + upload_units;
        report_progress(progress, CloudSyncProgressStage::FetchMetadata, 1, total);
        let mut effective_settings = settings.clone();
        let created_remote_id = if matches!(settings.backend_type, BackendType::GithubGist)
            && settings.git_repository.trim().is_empty()
        {
            let gist_id = self
                .backend
                .create_github_gist(&effective_settings, &secrets)
                .await
                .map_err(|error| upload_error_after_revision(error, options.revision_sequence))?;
            effective_settings.git_repository = gist_id.clone();
            Some(gist_id)
        } else {
            None
        };
        let remote_metadata = self
            .backend
            .fetch_remote_metadata(&effective_settings, &secrets)
            .await?;
        if !options.force && remote_metadata.exists {
            if let Err(error) = ensure_no_remote_conflict(
                &local_snapshot,
                &remote_metadata,
                options.previous_remote_revision.as_deref(),
                options.previous_remote_sections.as_ref(),
            ) {
                return Err(CloudSyncUploadError {
                    message: error.to_string(),
                    remote_metadata: Some(remote_metadata),
                    revision_sequence_consumed: None,
                });
            }
        }

        report_progress(progress, CloudSyncProgressStage::Preflight, 2, total);
        if local_snapshot.scope.sync_connections {
            let connection_ids = connection_store
                .connections()
                .iter()
                .map(|connection| connection.id.clone())
                .filter(|connection_id| {
                    options
                        .item_filter
                        .connection_ids
                        .as_ref()
                        .is_none_or(|ids| ids.contains(connection_id))
                })
                .collect::<Vec<_>>();
            let preflight = preflight_export(
                connection_store,
                &connection_ids,
                false,
                include_managed_keys_in_connection_preflight(&local_snapshot.scope),
                0,
            );
            if !preflight.can_export {
                return Err(anyhow::anyhow!("preflight_failed: export preflight failed").into());
            }
        }
        let revision = revision_id(Utc::now(), &options.device_id, options.revision_sequence);
        let uploaded_at = Utc::now().to_rfc3339();
        report_progress(progress, CloudSyncProgressStage::Exporting, 2, total);
        let plan = self
            .build_structured_upload_plan(
                connection_store,
                forwarding_registry,
                settings_store,
                &local_snapshot,
                &revision,
                &uploaded_at,
                &options.device_id,
                secrets
                    .sync_password
                    .as_ref()
                    .map(|password| password.as_str()),
                options.portable_secrets.clone(),
                &options.item_filter,
                progress,
                total,
            )
            .await
            .map_err(|error| upload_error_after_revision(error, options.revision_sequence))?;

        let mut completed_uploads = 0usize;
        report_progress(
            progress,
            CloudSyncProgressStage::UploadingBlob,
            2 + export_units,
            total,
        );
        let manifest_value = serde_json::to_value(&plan.manifest)
            .map_err(|error| upload_error_after_revision(error, options.revision_sequence))?;
        let metadata_write = if matches!(effective_settings.backend_type, BackendType::GithubGist) {
            let objects = plan
                .objects
                .iter()
                .map(|object| RemoteUploadObject {
                    path: object.path.clone(),
                    bytes: object.bytes.clone(),
                })
                .collect::<Vec<_>>();
            let write = self
                .backend
                .write_gist_objects_and_metadata(
                    &effective_settings,
                    &secrets,
                    &objects,
                    &manifest_value,
                    remote_metadata.etag.as_deref(),
                )
                .await
                .map_err(|error| upload_error_after_revision(error, options.revision_sequence))?;
            completed_uploads += plan.objects.len();
            report_progress(
                progress,
                CloudSyncProgressStage::UploadingBlob,
                2 + export_units + completed_uploads,
                total,
            );
            write
        } else {
            for object in &plan.objects {
                self.backend
                    .write_remote_object(
                        &effective_settings,
                        &secrets,
                        &object.path,
                        object.bytes.clone(),
                        Some(&object.content_type),
                    )
                    .await
                    .map_err(|error| {
                        upload_error_after_revision(error, options.revision_sequence)
                    })?;
                completed_uploads += 1;
                report_progress(
                    progress,
                    CloudSyncProgressStage::UploadingBlob,
                    2 + export_units + completed_uploads,
                    total,
                );
            }
            self.backend
                .write_remote_metadata(
                    &effective_settings,
                    &secrets,
                    &manifest_value,
                    remote_metadata.etag.as_deref(),
                )
                .await
                .map_err(|error| upload_error_after_revision(error, options.revision_sequence))?
        };
        completed_uploads += 1;
        report_progress(
            progress,
            CloudSyncProgressStage::UploadingBlob,
            2 + export_units + completed_uploads,
            total,
        );
        report_progress(progress, CloudSyncProgressStage::Done, total, total);

        Ok(Some(UploadOutcome {
            revision,
            revision_sequence: options.revision_sequence,
            etag: metadata_write.etag,
            local_snapshot,
            manifest: plan.manifest,
            created_remote_id,
        }))
    }
}
