---
title: WHvGetVpciDeviceInterruptTarget
description: Learn about the WHvGetVpciDeviceInterruptTarget function that queries the current vector and processor set of a mapped interrupt for an assigned virtual PCI device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVpciDeviceInterruptTarget

Queries the current vector and processor set of a mapped interrupt for an assigned virtual PCI device.

## Syntax

```C
typedef enum WHV_VPCI_INTERRUPT_TARGET_FLAGS
{
    WHvVpciInterruptTargetFlagNone      = 0x00000000,
    WHvVpciInterruptTargetFlagMulticast = 0x00000001,

} WHV_VPCI_INTERRUPT_TARGET_FLAGS;

// Enables bitwise operators on the WHV_VPCI_INTERRUPT_TARGET_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_VPCI_INTERRUPT_TARGET_FLAGS);

typedef struct WHV_VPCI_INTERRUPT_TARGET
{
    UINT32 Vector;
    WHV_VPCI_INTERRUPT_TARGET_FLAGS Flags;
    UINT32 ProcessorCount;
    UINT32 Processors[ANYSIZE_ARRAY];

} WHV_VPCI_INTERRUPT_TARGET;

HRESULT
WINAPI
WHvGetVpciDeviceInterruptTarget(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ UINT32 Index,
    _In_ UINT32 MultiMessageNumber,
    _Out_writes_bytes_to_(TargetSizeInBytes, *BytesWritten) WHV_VPCI_INTERRUPT_TARGET* Target,
    _In_ UINT32 TargetSizeInBytes,
    _Out_opt_ UINT32* BytesWritten
    );
```

### Parameters

`Partition`

Handle to the partition that owns the virtual PCI device.

`LogicalDeviceId`

Specifies the logical device ID of the virtual PCI device, as assigned when the device was created.

`Index`

Specifies the index of the interrupt entry to query. This is the same index that was supplied to [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) when the interrupt was mapped.

`MultiMessageNumber`

Specifies the message to query for a multi-message MSI interrupt, between 0 and 31, inclusive. For a single-message MSI or MSI-X interrupt, set this value to 0.

`Target`

Receives a `WHV_VPCI_INTERRUPT_TARGET` structure that describes the current vector and processor set of the interrupt.

`TargetSizeInBytes`

Specifies the size of the `Target` buffer, in bytes.

`BytesWritten`

Receives the number of bytes written to the `Target` buffer. If the function fails with `WHV_E_INSUFFICIENT_BUFFER`, this parameter receives the required buffer size instead. This parameter is optional and can be `NULL`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` when `Index` is greater than 65535 or when `MultiMessageNumber` is greater than 31. The function returns `WHV_E_INSUFFICIENT_BUFFER` when the `Target` buffer is too small to hold the result; in that case, `BytesWritten` receives the required size.

## Remarks

The `WHvGetVpciDeviceInterruptTarget` function returns the current target of an interrupt mapped with [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) or retargeted with [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md). Because `WHV_VPCI_INTERRUPT_TARGET` ends with a variable-length `Processors` array, size the `Target` buffer to hold the structure plus one `UINT32` per target processor.

To determine the required size, call the function with a buffer that is too small and read the required size from `BytesWritten`, then allocate a buffer of that size and call again.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md)
- [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md)
- [`WHvUnmapVpciDeviceInterrupt`](WHvUnmapVpciDeviceInterrupt.md)
- [`WHvRequestVpciDeviceInterrupt`](WHvRequestVpciDeviceInterrupt.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
