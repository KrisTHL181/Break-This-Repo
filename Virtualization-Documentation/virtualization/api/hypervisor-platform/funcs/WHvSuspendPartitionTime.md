---
title: WHvSuspendPartitionTime
description: Learn about the WHvSuspendPartitionTime function that suspends the passage of time for a partition.
author: nschonni
ms.author: roharwoo
ms.date: 06/03/2019
ms.topic: reference
---

# WHvSuspendPartitionTime

Suspends the passage of time for a partition.

## Syntax

```C
HRESULT
WINAPI
WHvSuspendPartitionTime(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvSuspendPartitionTime` function suspends time for the partition.

No virtual processor may be running when this is called. Time will resume when [`WHvResumePartitionTime`](WHvResumePartitionTime.md) or
[`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md) is called.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1903 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvResumePartitionTime`](WHvResumePartitionTime.md)
- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
