---
title: WHvSetVirtualProcessorInterruptControllerState2
description: Learn about the WHvSetVirtualProcessorInterruptControllerState2 function that sets the local APIC state of a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvSetVirtualProcessorInterruptControllerState2

Sets the local APIC state of a virtual processor.

> [!IMPORTANT]
> `WHvSetVirtualProcessorInterruptControllerState2` is deprecated. Use [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md) with the `WHvVirtualProcessorStateTypeInterruptControllerState2` state type instead.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
HRESULT
WINAPI
WHvSetVirtualProcessorInterruptControllerState2(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_bytes_(StateSize) const VOID* State,
    _In_ UINT32 StateSize
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose interrupt controller state is set.

`State`

Specifies the interrupt controller state to apply, in the standard external state format.

`StateSize`

Specifies the size of `State`, in bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

If `State` is `NULL`, or if `StateSize` is smaller than the required state size, the return value is `E_POINTER`. If `VpIndex` does not identify an existing virtual processor, the return value is `WHV_E_VP_DOES_NOT_EXIST`. If the virtual processor cannot be accessed in its current state, the return value is `WHV_E_INVALID_VP_STATE`.

## Remarks

The `WHvSetVirtualProcessorInterruptControllerState2` function restores the local APIC state of the specified virtual processor from a buffer in the standard external state format, typically one previously produced by [`WHvGetVirtualProcessorInterruptControllerState2`](WHvGetVirtualProcessorInterruptControllerState2.md). Because the task priority register (TPR) is not part of the hypervisor's interrupt controller state, the function also synchronizes the virtual processor's `CR8` register when the supplied TPR differs from the current value.

The function requires emulation of the local APIC to be configured for the partition.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 2004 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvGetVirtualProcessorInterruptControllerState2`](WHvGetVirtualProcessorInterruptControllerState2.md)
- [`WHvSetVirtualProcessorInterruptControllerState`](WHvSetVirtualProcessorInterruptControllerState.md)
- [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
