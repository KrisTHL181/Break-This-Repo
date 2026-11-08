---
title: WHvGetVirtualProcessorRegisters
description: Learn about the WHvGetVirtualProcessorRegisters function that gets the values of the specified registers of a virtual processor.
author: Juarezhm
ms.author: hajuarez
ms.date: 06/22/2018
ms.topic: reference
---

# WHvGetVirtualProcessorRegisters

Gets the values of the specified registers of a virtual processor.

## Syntax

```C
HRESULT
WINAPI
WHvGetVirtualProcessorRegisters(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_(RegisterCount) const WHV_REGISTER_NAME* RegisterNames,
    _In_ UINT32 RegisterCount,
    _Out_writes_(RegisterCount) WHV_REGISTER_VALUE* RegisterValues
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose registers are queried.

`RegisterNames`

Array specifying the names of the registers that are queried.

`RegisterCount`

Specifies the number of elements in the `RegisterNames` array.

`RegisterValues`

Receives the values of the queried registers.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvGetVirtualProcessorRegisters` function gets the values of the specified registers of a virtual processor.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSetVirtualProcessorRegisters`](WHvSetVirtualProcessorRegisters.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
