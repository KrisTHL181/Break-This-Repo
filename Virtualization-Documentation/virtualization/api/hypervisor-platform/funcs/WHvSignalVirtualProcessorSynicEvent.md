---
title: WHvSignalVirtualProcessorSynicEvent
description: Learn about the WHvSignalVirtualProcessorSynicEvent function that signals a synthetic interrupt controller event flag on a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvSignalVirtualProcessorSynicEvent

Signals a synthetic interrupt controller (SynIC) event flag on a virtual processor.

## Syntax

```C
typedef UINT8 WHV_VTL;

typedef struct WHV_SYNIC_EVENT_PARAMETERS
{
    UINT32 VpIndex;
    UINT8 TargetSint;
    WHV_VTL TargetVtl;
    UINT16 FlagNumber;
} WHV_SYNIC_EVENT_PARAMETERS;

HRESULT
WINAPI
WHvSignalVirtualProcessorSynicEvent(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_SYNIC_EVENT_PARAMETERS SynicEvent,
    _Out_opt_ BOOL* NewlySignaled
    );
```

### Parameters

`Partition`

Handle to the partition object.

`SynicEvent`

Specifies the target of the event. `VpIndex` identifies the virtual processor, `TargetSint` identifies the synthetic interrupt source (SINT), `TargetVtl` identifies the target virtual trust level, and `FlagNumber` identifies the event flag to set.

`NewlySignaled`

If non-NULL, receives `TRUE` if the event flag transitioned from clear to set as a result of this call, and `FALSE` if the flag was already set.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `TargetVtl` is not `0`. If the synthetic interrupt controller of the target virtual processor is not configured to receive events, the function returns `HRESULT_FROM_WIN32(ERROR_HV_INVALID_SYNIC_STATE)`.

## Remarks

The `WHvSignalVirtualProcessorSynicEvent` function sets the event flag identified by `FlagNumber` in the SynIC event-flag page of the target virtual processor and delivers an edge-triggered interrupt on the specified SINT. This is the mechanism used to signal a guest through a SynIC event port.

The `NewlySignaled` output distinguishes a flag that this call set from one that was already pending.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvPostVirtualProcessorSynicMessage`](WHvPostVirtualProcessorSynicMessage.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
