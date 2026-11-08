---
title: WHvGetInterruptTargetVpSet
description: Learn about the WHvGetInterruptTargetVpSet function that resolves an interrupt destination to the set of target virtual processors.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetInterruptTargetVpSet

Resolves an interrupt destination to the set of target virtual processors.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
typedef enum WHV_INTERRUPT_DESTINATION_MODE
{
    WHvX64InterruptDestinationModePhysical,
    WHvX64InterruptDestinationModeLogical,
} WHV_INTERRUPT_DESTINATION_MODE;

HRESULT
WINAPI
WHvGetInterruptTargetVpSet(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 Destination,
    _In_ WHV_INTERRUPT_DESTINATION_MODE DestinationMode,
    _Out_writes_to_(VpCount, *TargetVpCount) UINT32* TargetVps,
    _In_ UINT32 VpCount,
    _Out_ UINT32* TargetVpCount
    );
```

### Parameters

`Partition`

Handle to the partition object.

`Destination`

Specifies the APIC destination value to resolve.

`DestinationMode`

Specifies how `Destination` is interpreted: `WHvX64InterruptDestinationModePhysical` for a physical APIC ID, or `WHvX64InterruptDestinationModeLogical` for a logical destination.

`TargetVps`

Receives the indices of the virtual processors targeted by the destination.

`VpCount`

Specifies the number of elements in the `TargetVps` array. This value must be at least the partition's processor count.

`TargetVpCount`

Receives the number of virtual processor indices written to `TargetVps`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `DestinationMode` is not a valid value, or if `VpCount` is smaller than the partition's processor count. If the partition is not configured to emulate the local APIC, the function returns `HRESULT_FROM_WIN32(ERROR_HV_OPERATION_DENIED)`.

## Remarks

The `WHvGetInterruptTargetVpSet` function computes the set of virtual processors that an interrupt with the specified destination and destination mode would target, applying the same APIC routing rules the hypervisor uses to deliver interrupts. This lets a virtualization stack determine interrupt routing without delivering an interrupt.

The `TargetVps` buffer must be large enough to hold one entry for every virtual processor in the partition; the function does not use an incremental size-query protocol, and an undersized buffer returns `E_INVALIDARG` rather than reporting the required size.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvRequestInterrupt`](WHvRequestInterrupt.md)
- [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
