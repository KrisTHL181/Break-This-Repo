---
title: WHvSetVirtualProcessorState
description: Learn about the WHvSetVirtualProcessorState function that sets a category of saved state on a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvSetVirtualProcessorState

Sets a category of saved state on a virtual processor.

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
WHvSetVirtualProcessorState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ WHV_VIRTUAL_PROCESSOR_STATE_TYPE StateType,
    _In_reads_bytes_(BufferSizeInBytes) const VOID* Buffer,
    _In_ UINT32 BufferSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose state is set.

`StateType`

Specifies the category of state to set. See [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md) for the meaning of each value.

`Buffer`

Specifies the state to apply. The format of the buffer depends on `StateType`.

`BufferSizeInBytes`

Specifies the size of `Buffer`, in bytes. The size must match the size of the state identified by `StateType`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `StateType` is not a valid state type.

## Remarks

The `WHvSetVirtualProcessorState` function applies a category of virtual processor state identified by `StateType`. It is the counterpart of [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md).

Unlike the get operation, the buffer supplied to the set operation must be exactly the size of the state being restored.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md)
- [`WHvSetVirtualProcessorXsaveState`](WHvSetVirtualProcessorXsaveState.md)
- [`WHvSetVirtualProcessorInterruptControllerState`](WHvSetVirtualProcessorInterruptControllerState.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
