---
title: WHvGetVirtualProcessorInterruptControllerState2
description: Learn about the WHvGetVirtualProcessorInterruptControllerState2 function that retrieves the local APIC state of a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVirtualProcessorInterruptControllerState2

Retrieves the local APIC state of a virtual processor.

> [!IMPORTANT]
> `WHvGetVirtualProcessorInterruptControllerState2` is deprecated. Use [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md) with the `WHvVirtualProcessorStateTypeInterruptControllerState2` state type instead.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
HRESULT
WINAPI
WHvGetVirtualProcessorInterruptControllerState2(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _Out_writes_bytes_to_(StateSize, *WrittenSize) VOID* State,
    _In_ UINT32 StateSize,
    _Out_opt_ UINT32* WrittenSize
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose interrupt controller state is retrieved.

`State`

Receives the interrupt controller state.

`StateSize`

Specifies the size of `State`, in bytes.

`WrittenSize`

If non-NULL, receives the number of bytes written to `State`. When the buffer is too small, it receives the number of bytes required.

## Return Value

If the function succeeds, the return value is `S_OK`.

If `State` is too small to contain the interrupt controller state, the return value is `WHV_E_INSUFFICIENT_BUFFER`, and `WrittenSize` receives the number of bytes required. If `VpIndex` does not identify an existing virtual processor, the return value is `WHV_E_VP_DOES_NOT_EXIST`. If the virtual processor cannot be accessed in its current state, the return value is `WHV_E_INVALID_VP_STATE`.

## Remarks

The `WHvGetVirtualProcessorInterruptControllerState2` function retrieves the local APIC state of the specified virtual processor in the standard external state format. It supersedes the deprecated [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md), which returns a legacy format that packs the interrupt-request, in-service, and trigger-mode vectors differently from the standard external state format and does not include the processor priority register.

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

- [`WHvSetVirtualProcessorInterruptControllerState2`](WHvSetVirtualProcessorInterruptControllerState2.md)
- [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md)
- [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
