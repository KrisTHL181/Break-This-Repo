---
title: WHvCreatePartition
description: Learn about the WHvCreatePartition function that creates a new partition object.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# WHvCreatePartition

Creates a new partition object.

## Syntax

```C
typedef VOID* WHV_PARTITION_HANDLE;

HRESULT
WINAPI
WHvCreatePartition(
    _Out_ WHV_PARTITION_HANDLE* Partition
    );
```

### Parameters

`Partition`

Receives the partition handle to the newly created partition object. All operations on the partition are performed through this handle.

To delete a partition created by `WHvCreatePartition`, use the [`WHvDeletePartition`](WHvDeletePartition.md) function.


## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvCreatePartition` function creates a new partition object.

`WHvCreatePartition` only creates the partition object and does not yet create the actual partition in the hypervisor. After creation of the partition object, the partition object should be configured using [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md). After the partition object is configured, the [`WHvSetupPartition`](WHvSetupPartition.md) function should be called to create the hypervisor partition.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSetupPartition`](WHvSetupPartition.md)
- [`WHvDeletePartition`](WHvDeletePartition.md)
- [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
