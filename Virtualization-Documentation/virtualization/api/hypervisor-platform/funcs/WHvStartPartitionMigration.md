---
title: WHvStartPartitionMigration
description: Learn about the WHvStartPartitionMigration function that begins migrating a partition on the source host.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvStartPartitionMigration

Begins migrating a partition on the source host.

## Syntax

```C
HRESULT
WINAPI
WHvStartPartitionMigration(
    _In_ WHV_PARTITION_HANDLE Partition,
    _Out_ HANDLE* MigrationHandle
    );
```

### Parameters

`Partition`

Handle to the partition object to migrate.

`MigrationHandle`

Receives a handle that represents the in-progress migration. The source process transfers this handle to the destination process, which passes it to [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md) to receive the partition.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the partition is already involved in a migration, the function returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

## Remarks

The `WHvStartPartitionMigration` function starts the source side of a partition migration. It serializes the partition's state, properties, virtual processors, doorbells, and virtual PCI devices and sends them, together with the necessary handles, to the destination process. The send operation remains pending until the destination host accepts the partition with [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md).

After this call succeeds, the partition enters a migrating state in which operations that would invalidate the already-serialized state are blocked. From this point the source must either finish the migration with [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md) or abort it with [`WHvCancelPartitionMigration`](WHvCancelPartitionMigration.md). Deleting the partition with [`WHvDeletePartition`](WHvDeletePartition.md) also cancels the migration.

The handle returned in `MigrationHandle` is a standard Win32 handle. The source process is responsible for transferring it to the destination process by any suitable means (for example, handle duplication). [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md) closes the handle on the destination side when it succeeds.

The typical migration sequence is:

1. The source calls `WHvStartPartitionMigration` to begin migration and obtain `MigrationHandle`.
2. The destination calls [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md) with the transferred handle to create the target partition.
3. The source calls [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md) to finalize and release the source partition.
4. The destination calls [`WHvSetupPartition`](WHvSetupPartition.md) to complete the migration on the target and resume normal operation.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md)
- [`WHvCompletePartitionMigration`](WHvCompletePartitionMigration.md)
- [`WHvCancelPartitionMigration`](WHvCancelPartitionMigration.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
