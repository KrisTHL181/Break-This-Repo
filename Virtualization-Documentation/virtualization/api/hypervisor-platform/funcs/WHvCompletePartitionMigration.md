---
title: WHvCompletePartitionMigration
description: Learn about the WHvCompletePartitionMigration function that finalizes a partition migration on the source host.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCompletePartitionMigration

Finalizes a partition migration on the source host.

## Syntax

```C
HRESULT
WINAPI
WHvCompletePartitionMigration(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object that is being migrated on the source host.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the partition is not in the source-migrating state, or if a virtual PCI device still has memory-mapped I/O mapped, the function returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

## Remarks

The `WHvCompletePartitionMigration` function completes the source side of a migration that was started with [`WHvStartPartitionMigration`](WHvStartPartitionMigration.md). It must be called after the destination has accepted the partition with [`WHvAcceptPartitionMigration`](WHvAcceptPartitionMigration.md).

The function finalizes the migration on the source host: it freezes partition time, prepares the partition's virtual processors and virtual PCI devices, closes the source partition handle, and signals the destination that it may proceed. Preparing the virtual PCI devices requires that no device still has memory-mapped I/O mapped; otherwise the call returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

After this call succeeds, the partition on the source is finalized, and all operations other than [`WHvDeletePartition`](WHvDeletePartition.md) are prohibited. The destination then calls [`WHvSetupPartition`](WHvSetupPartition.md) to complete the migration and resume the partition.

To abort a migration before completing it, call [`WHvCancelPartitionMigration`](WHvCancelPartitionMigration.md) instead.

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
- [`WHvCancelPartitionMigration`](WHvCancelPartitionMigration.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
