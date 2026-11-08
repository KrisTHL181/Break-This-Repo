---
title: WHvSetVirtualProcessorInterruptControllerState
description: Learn about the WHvSetVirtualProcessorInterruptControllerState function that sets the state of a virtual processor's interrupt controller.
author: jstarks
ms.author: jostarks
ms.date: 06/06/2019
ms.topic: reference
---

# WHvSetVirtualProcessorInterruptControllerState

Sets the state of a virtual processor's interrupt controller.

> [!IMPORTANT]
> `WHvSetVirtualProcessorInterruptControllerState` is deprecated. Use [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md) with the `WHvVirtualProcessorStateTypeInterruptControllerState2` state type instead.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
HRESULT
WINAPI
WHvSetVirtualProcessorInterruptControllerState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_bytes_(StateSize) const VOID* State,
    _In_ UINT32 StateSize
    );
```

### Parameters

`Partition`

Specifies the partition of the virtual processor.

`VpIndex`

Specifies the index of the virtual processor whose interrupt controller should be set.

`State`

Specifies a buffer containing the interrupt controller state.

`StateSize`

Specifies the size of the buffer, in bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the virtual processor is currently running, the return value is `WHV_E_INVALID_VP_STATE`.

## Remarks

The `WHvSetVirtualProcessorInterruptControllerState` function sets the state of the specified virtual processor's interrupt controller.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1809 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md)
- [`WHvRequestInterrupt`](WHvRequestInterrupt.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
