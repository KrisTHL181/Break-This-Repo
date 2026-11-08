---
title: WHvGetVirtualProcessorState
description: Learn about the WHvGetVirtualProcessorState function that retrieves a category of saved state from a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVirtualProcessorState

Retrieves a category of saved state from a virtual processor.

## Syntax

```C
// WHvGetVirtualProcessorState and WHvSetVirtualProcessorState types.
#if defined(_AMD64_)

typedef enum WHV_VIRTUAL_PROCESSOR_STATE_TYPE
{
    WHvVirtualProcessorStateTypeSynicMessagePage          = 0x00000000,
    WHvVirtualProcessorStateTypeSynicEventFlagPage        = 0x00000001,
    WHvVirtualProcessorStateTypeSynicTimerState           = 0x00000002,

    WHvVirtualProcessorStateTypeInterruptControllerState2 = 0x00001000,
    WHvVirtualProcessorStateTypeXsaveState                = 0x00001001,
    WHvVirtualProcessorStateTypeNestedState               = 0x00001002,
} WHV_VIRTUAL_PROCESSOR_STATE_TYPE;

#elif defined (_ARM64_)

#define WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN    (1ui32 << 31)
#define WHV_VIRTUAL_PROCESSOR_STATE_TYPE_ANY_VP (1ui32 << 30)

typedef enum WHV_VIRTUAL_PROCESSOR_STATE_TYPE
{
    WHvVirtualProcessorStateTypeInterruptControllerState  = 0x00000000 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,

    WHvVirtualProcessorStateTypeSynicMessagePage          = 0x00000002 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
    WHvVirtualProcessorStateTypeSynicEventFlagPage        = 0x00000003 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
    WHvVirtualProcessorStateTypeSynicTimerState           = 0x00000004,

    WHvVirtualProcessorStateTypeGlobalInterruptState      = 0x00000006 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_ANY_VP,
    WHvVirtualProcessorStateTypeSveState                  = 0x00000007 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
} WHV_VIRTUAL_PROCESSOR_STATE_TYPE;

#endif

HRESULT
WINAPI
WHvGetVirtualProcessorState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ WHV_VIRTUAL_PROCESSOR_STATE_TYPE StateType,
    _Out_writes_bytes_to_(BufferSizeInBytes, *BytesWritten) VOID* Buffer,
    _In_ UINT32 BufferSizeInBytes,
    _Out_opt_ UINT32* BytesWritten
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose state is retrieved.

`StateType`

Specifies the category of state to retrieve. See the Remarks section for the meaning of each value.

`Buffer`

Receives the requested state. The format of the buffer depends on `StateType`.

`BufferSizeInBytes`

Specifies the size of `Buffer`, in bytes.

`BytesWritten`

If non-NULL, receives the number of bytes written to `Buffer`. When the buffer is too small, it receives the number of bytes required to hold the state.

## Return Value

If the function succeeds, the return value is `S_OK`.

If `Buffer` is too small to contain the requested state, the return value is `WHV_E_INSUFFICIENT_BUFFER`. In this case, `BytesWritten` receives the number of bytes required. If `StateType` is not a valid state type, the return value is `E_INVALIDARG`.

## Remarks

The `WHvGetVirtualProcessorState` function retrieves a category of virtual processor state identified by `StateType`. It provides a single, architecture-aware interface for saving virtual processor state and supersedes the deprecated [`WHvGetVirtualProcessorXsaveState`](WHvGetVirtualProcessorXsaveState.md) and [`WHvGetVirtualProcessorInterruptControllerState2`](WHvGetVirtualProcessorInterruptControllerState2.md) functions.

### State types common to both architectures

- `WHvVirtualProcessorStateTypeSynicMessagePage` and `WHvVirtualProcessorStateTypeSynicEventFlagPage` retrieve the synthetic interrupt controller (SynIC) message and event-flag pages.
- `WHvVirtualProcessorStateTypeSynicTimerState` retrieves the SynIC synthetic timer state.

### x64 state types

- `WHvVirtualProcessorStateTypeInterruptControllerState2` retrieves the local APIC state.
- `WHvVirtualProcessorStateTypeXsaveState` retrieves the processor extended (xsave) state.
- `WHvVirtualProcessorStateTypeNestedState` retrieves nested virtualization state.

### Arm64 state types

- `WHvVirtualProcessorStateTypeInterruptControllerState` retrieves the per-processor GIC state.
- `WHvVirtualProcessorStateTypeGlobalInterruptState` retrieves partition-wide interrupt-controller state.
- `WHvVirtualProcessorStateTypeSveState` retrieves the Scalable Vector Extension (SVE) state.

To determine the required buffer size, call the function with a buffer that is too small and read the value returned in `BytesWritten` when the function returns `WHV_E_INSUFFICIENT_BUFFER`, then call the function again with a buffer of that size.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md)
- [`WHvGetVirtualProcessorXsaveState`](WHvGetVirtualProcessorXsaveState.md)
- [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
