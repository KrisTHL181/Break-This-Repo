---
title: WHvCancelPartitionMigration
description: Learn about the WHvCancelPartitionMigration function that aborts an in-progress partition migration on the source host.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCancelPartitionMigration

Aborts an in-progress partition migration on the source host.

## Syntax

```C
HRESULT
WINAPI
WHvCancelPartitionMigration(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object whose migration is to be canceled on the source host.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the partition is not in the source-migrating state, the function returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

## Remarks

The `WHvCancelPartitionMigration` function aborts a migration that was started on the source with [`WHvStartPartitionMigration`](WHvStartPartitionMigration.md). It cancels the pending transfer to the destination and returns the partition to its normal, non-migrating state, after which all operations are permitted again.

This function operates only on the source side of a migration. It can be called while the migration is in progress, including after the destination has accepted the partition with [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md), provided the source has not yet called [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md). Once the source completes the migration, it can no longer be canceled.

Deleting the partition with [`WHvDeletePartition`](WHvDeletePartition.md) while a migration is in progress also cancels the migration.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvStartPartitionMigration`](WHvStartPartitionMigration.md)
- [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md)
- [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md)
- [`WHvDeletePartition`](WHvDeletePartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
