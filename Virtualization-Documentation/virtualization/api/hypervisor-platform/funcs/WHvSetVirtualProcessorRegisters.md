---
title: WHvSetVirtualProcessorRegisters
description: Learn about the WHvSetVirtualProcessorRegisters function that sets the values of the specified registers of a virtual processor.
author: Juarezhm
ms.author: hajuarez
ms.date: 06/22/2018
ms.topic: reference
---

# WHvSetVirtualProcessorRegisters

Sets the values of the specified registers of a virtual processor.

## Syntax

```C
HRESULT
WINAPI
WHvSetVirtualProcessorRegisters(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_(RegisterCount) const WHV_REGISTER_NAME* RegisterNames,
    _In_ UINT32 RegisterCount,
    _In_reads_(RegisterCount) const WHV_REGISTER_VALUE* RegisterValues
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose registers are set.

`RegisterNames`

Array specifying the names of the registers that are set.

`RegisterCount`

Specifies the number of elements in the `RegisterNames` array.

`RegisterValues`

Array specifying the values of the registers that are set.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvSetVirtualProcessorRegisters` function sets the values of the specified registers of a virtual processor.

On Arm64, the GIC redistributor base address register (`WHvArm64RegisterGicrBaseGpa`) must be set on each virtual processor before it is run.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvGetVirtualProcessorRegisters`](WHvGetVirtualProcessorRegisters.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
