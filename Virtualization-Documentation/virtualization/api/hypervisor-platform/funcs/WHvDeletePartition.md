---
title: WHvDeletePartition
description: Learn about the WHvDeletePartition function that lets you delete a partition, remove the partition object, and release the resources that the partition was using.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# WHvDeletePartition

Deletes a partition, removes the partition object, and releases the resources that the partition was using.

## Syntax
```C
HRESULT
WINAPI
WHvDeletePartition(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object that is deleted.


## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

Deleting a partition tears down the partition object and releases all resources that the partition was using.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreatePartition`](WHvCreatePartition.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
