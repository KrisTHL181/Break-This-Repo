---
title: WHvAcceptPartitionMigration
description: Learn about the WHvAcceptPartitionMigration function that accepts a migrating partition on the destination host.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvAcceptPartitionMigration

Accepts a migrating partition on the destination host.

## Syntax

```C
HRESULT
WINAPI
WHvAcceptPartitionMigration(
    _In_ HANDLE MigrationHandle,
    _Out_ WHV_PARTITION_HANDLE* Partition
    );
```

### Parameters

`MigrationHandle`

Specifies the migration handle that the source process obtained from [`WHvStartPartitionMigration`](WHvStartPartitionMigration.md) and transferred to the destination process.

`Partition`

Receives the handle to the newly created partition object that represents the migrated partition on the destination.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if the serialized partition data cannot be parsed. If the transferred handles are missing or invalid, the function returns `HRESULT_FROM_WIN32(ERROR_INVALID_DATA)`. If the serialized partition uses a migration version that this version of the platform does not support, the function returns `HRESULT_FROM_WIN32(ERROR_NOT_SUPPORTED)`.

## Remarks

The `WHvAcceptPartitionMigration` function receives the serialized partition state and handles that the source sent with [`WHvStartPartitionMigration`](WHvStartPartitionMigration.md), and reconstructs the partition as a new partition object on the destination host. The new partition is created in a migrating state in which only [`WHvSetupPartition`](WHvSetupPartition.md) and [`WHvDeletePartition`](WHvDeletePartition.md) are permitted until the migration completes.

On success, the function closes `MigrationHandle`. The caller should not close the handle again.

After this call, the source finalizes the migration with [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md). Once the source has completed, the destination calls [`WHvSetupPartition`](WHvSetupPartition.md) on the partition returned in `Partition` to finish the migration and resume normal operation. Calling [`WHvSetupPartition`](WHvSetupPartition.md) before the source completes returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

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
- [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md)
- [`WHvCancelPartitionMigration`](WHvCancelPartitionMigration.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
