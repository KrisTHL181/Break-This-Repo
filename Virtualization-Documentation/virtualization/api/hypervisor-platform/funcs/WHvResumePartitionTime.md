---
title: WHvResumePartitionTime
description: Learn about the WHvResumePartitionTime function that resumes the passage of time for a suspended partition.
author: sethmanheim
ms.author: roharwoo
ms.date: 03/15/2019
ms.topic: reference
---

# WHvResumePartitionTime

Resumes the passage of time for a suspended partition.

## Syntax

```C
HRESULT
WINAPI
WHvResumePartitionTime(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvResumePartitionTime` function resumes time for a partition suspended by [`WHvSuspendPartitionTime`](WHvSuspendPartitionTime.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1903 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSuspendPartitionTime`](WHvSuspendPartitionTime.md)
- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
