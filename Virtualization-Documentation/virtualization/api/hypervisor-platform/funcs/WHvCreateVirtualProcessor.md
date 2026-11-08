---
title: WHvCreateVirtualProcessor
description: Learn about the WHvCreateVirtualProcessor function that creates a new virtual processor in a partition.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# WHvCreateVirtualProcessor

Creates a new virtual processor in a partition.

## Syntax

```C
HRESULT
WINAPI
WHvCreateVirtualProcessor(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ UINT32 Flags
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the new virtual processor.

`Flags`

Unused, must be zero.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvCreateVirtualProcessor` function creates a new virtual processor in a partition. On x64, the index of the virtual processor is used to set the APIC ID of the processor.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvDeleteVirtualProcessor`](WHvDeleteVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
