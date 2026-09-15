// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

use super::*;

impl CloudSyncOperationService {
    pub(super) async fn read_required_object(
        &self,
        settings: &CloudSyncSettings,
        secrets: &crate::secrets::CloudSyncSecrets,
        entry: &StructuredObjectEntry,
    ) -> Result<crate::backend::RemoteObject> {
        self.backend
            .read_remote_object(settings, secrets, &entry.path)
            .await?
            .ok_or_else(|| {
                anyhow::anyhow!("remote_not_found: missing remote object {}", entry.path)
            })
    }

    pub(super) async fn read_optional_object(
        &self,
        settings: &CloudSyncSettings,
        secrets: &crate::secrets::CloudSyncSecrets,
        path: &str,
    ) -> Result<Option<crate::backend::RemoteObject>> {
        self.backend
            .read_remote_object(settings, secrets, path)
            .await
    }
}
