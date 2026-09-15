// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

use super::*;

pub(super) fn forward_changed_fields(
    before: &PersistedForwardDto,
    after: &PersistedForwardDto,
) -> Vec<CloudSyncFieldDiffField> {
    let mut fields = Vec::new();
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.forward_type",
        Some(before.forward_type.clone()),
        Some(after.forward_type.clone()),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.bind_address",
        Some(before.bind_address.clone()),
        Some(after.bind_address.clone()),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.bind_port",
        Some(before.bind_port.to_string()),
        Some(after.bind_port.to_string()),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.target_host",
        Some(before.target_host.clone()),
        Some(after.target_host.clone()),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.target_port",
        Some(before.target_port.to_string()),
        Some(after.target_port.to_string()),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.description",
        before.description.clone(),
        after.description.clone(),
    );
    push_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.auto_start",
        Some(before.auto_start.to_string()),
        Some(after.auto_start.to_string()),
    );
    fields
}

pub(super) fn forward_merge_fields(
    base: &PersistedForwardDto,
    local: &PersistedForwardDto,
    remote: &PersistedForwardDto,
    effective: &PersistedForwardDto,
    conflict_strategy: &ConflictStrategy,
) -> Vec<CloudSyncFieldDiffField> {
    let mut fields = Vec::new();
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.forward_type",
        Some(base.forward_type.clone()),
        Some(local.forward_type.clone()),
        Some(remote.forward_type.clone()),
        Some(effective.forward_type.clone()),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.bind_address",
        Some(base.bind_address.clone()),
        Some(local.bind_address.clone()),
        Some(remote.bind_address.clone()),
        Some(effective.bind_address.clone()),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.bind_port",
        Some(base.bind_port.to_string()),
        Some(local.bind_port.to_string()),
        Some(remote.bind_port.to_string()),
        Some(effective.bind_port.to_string()),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.target_host",
        Some(base.target_host.clone()),
        Some(local.target_host.clone()),
        Some(remote.target_host.clone()),
        Some(effective.target_host.clone()),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.target_port",
        Some(base.target_port.to_string()),
        Some(local.target_port.to_string()),
        Some(remote.target_port.to_string()),
        Some(effective.target_port.to_string()),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.description",
        base.description.clone(),
        local.description.clone(),
        remote.description.clone(),
        effective.description.clone(),
        conflict_strategy,
    );
    push_merge_changed(
        &mut fields,
        "plugin.cloud_sync.diff_fields.auto_start",
        Some(base.auto_start.to_string()),
        Some(local.auto_start.to_string()),
        Some(remote.auto_start.to_string()),
        Some(effective.auto_start.to_string()),
        conflict_strategy,
    );
    fields
}

pub(super) fn forward_summary_fields(value: &PersistedForwardDto) -> Vec<CloudSyncFieldDiffField> {
    vec![
        field(
            "plugin.cloud_sync.diff_fields.forward_type",
            None,
            Some(value.forward_type.clone()),
        ),
        field(
            "plugin.cloud_sync.diff_fields.bind_address",
            None,
            Some(value.bind_address.clone()),
        ),
        field(
            "plugin.cloud_sync.diff_fields.bind_port",
            None,
            Some(value.bind_port.to_string()),
        ),
        field(
            "plugin.cloud_sync.diff_fields.target_host",
            None,
            Some(value.target_host.clone()),
        ),
        field(
            "plugin.cloud_sync.diff_fields.target_port",
            None,
            Some(value.target_port.to_string()),
        ),
    ]
}
