---
title: WHvGetVirtualProcessorInterruptControllerState
description: Learn about the WHvGetVirtualProcessorInterruptControllerState function that retrieves the state of a virtual processor's interrupt controller.
author: jstarks
ms.author: jostarks
ms.date: 06/06/2019
ms.topic: reference
---

# WHvGetVirtualProcessorInterruptControllerState

Retrieves the state of a virtual processor's interrupt controller.

> [!IMPORTANT]
> `WHvGetVirtualProcessorInterruptControllerState` is deprecated. Use [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md) with the `WHvVirtualProcessorStateTypeInterruptControllerState2` state type instead.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
HRESULT
WINAPI
WHvGetVirtualProcessorInterruptControllerState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _Out_writes_bytes_to_(StateSize, *WrittenSize) VOID* State,
    _In_ UINT32 StateSize,
    _Out_opt_ UINT32* WrittenSize
    );
```

### Parameters

`Partition`

Specifies the partition of the virtual processor.

`VpIndex`

Specifies the index of the virtual processor whose interrupt controller should be retrieved.

`State`

Specifies a buffer to write the interrupt controller state into.

`StateSize`

Specifies the size of the buffer, in bytes.

`WrittenSize`

If non-NULL, receives the number of bytes written to the buffer.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the buffer is too small to contain the interrupt controller state, the return value is `WHV_E_INSUFFICIENT_BUFFER`. In this case, `WrittenSize` receives the number of bytes necessary to fit the interrupt controller state.

## Remarks

The `WHvGetVirtualProcessorInterruptControllerState` function retrieves the state of the specified virtual processor's interrupt controller.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1809 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvSetVirtualProcessorInterruptControllerState`](WHvSetVirtualProcessorInterruptControllerState.md)
- [`WHvRequestInterrupt`](WHvRequestInterrupt.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
