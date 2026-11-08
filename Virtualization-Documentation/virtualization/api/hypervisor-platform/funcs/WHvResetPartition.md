---
title: WHvResetPartition
description: Learn about the WHvResetPartition function that resets a partition to its initial state.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvResetPartition

Resets a partition to its initial state.

## Syntax

```C
HRESULT
WINAPI
WHvResetPartition(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object to reset.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the partition is in a state that does not permit a reset, the function returns `HRESULT_FROM_WIN32(ERROR_INVALID_STATE)`.

## Remarks

The `WHvResetPartition` function resets the partition, returning its virtual processors to the state they had immediately after the partition was set up with [`WHvSetupPartition`](WHvSetupPartition.md). The register state of each existing virtual processor is restored to its initial values, and per-virtual-processor state such as pending suspend, cancel, and dispatch-notification state is cleared.

To carry out the reset, the function blocks all of the partition's virtual processors and freezes partition time; time is thawed again when a virtual processor is next run. Existing virtual processors are reinitialized in place rather than deleted, so the number of virtual processors and the partition's configured properties are preserved across the reset.

Resetting a partition does not remove its guest physical address (GPA) mappings or alter the contents of the backing memory established with [`WHvMapGpaRange`](WHvMapGpaRange.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 11, version 21H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreatePartition`](WHvCreatePartition.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [`WHvDeletePartition`](WHvDeletePartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
