---
title: WHvDeleteVirtualProcessor
description: Learn about the WHvDeleteVirtualProcessor function that deletes a virtual processor in a partition.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# WHvDeleteVirtualProcessor

Deletes a virtual processor in a partition.

## Syntax

```C
HRESULT
WINAPI
WHvDeleteVirtualProcessor(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor that is deleted.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvDeleteVirtualProcessor` function deletes a virtual processor in a partition.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateVirtualProcessor`](WHvCreateVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
