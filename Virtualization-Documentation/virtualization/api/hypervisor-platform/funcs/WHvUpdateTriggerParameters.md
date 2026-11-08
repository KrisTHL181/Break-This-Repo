---
title: WHvUpdateTriggerParameters
description: Learn about the WHvUpdateTriggerParameters function that updates the parameters of an existing trigger object.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvUpdateTriggerParameters

Updates the parameters of an existing trigger object.

## Syntax

```C
typedef struct WHV_TRIGGER_PARAMETERS
{
    WHV_TRIGGER_TYPE TriggerType;
    UINT32 Reserved;
    union
    {
#if defined(_AMD64_)
        WHV_INTERRUPT_CONTROL Interrupt;
#endif
        WHV_SYNIC_EVENT_PARAMETERS SynicEvent;
        struct
        {
            UINT64 LogicalDeviceId;
            UINT64 MsiAddress;
            UINT32 MsiData;
            UINT32 Reserved;
        } DeviceInterrupt;
    };
} WHV_TRIGGER_PARAMETERS;

typedef PVOID WHV_TRIGGER_HANDLE;

HRESULT
WINAPI
WHvUpdateTriggerParameters(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_TRIGGER_PARAMETERS* Parameters,
    _In_ WHV_TRIGGER_HANDLE TriggerHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`Parameters`

Specifies the new parameters for the trigger. The `TriggerType` member must match the type the trigger was created with. See [Trigger Data Types](WHvTriggerDataTypes.md) for the full definition of each member.

`TriggerHandle`

Handle to the trigger object to update, as returned by [`WHvCreateTrigger`](WHvCreateTrigger.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if the parameters in `Parameters` are not valid for the trigger type. The trigger type is fixed when the trigger is created and cannot be changed by this function.

## Remarks

The `WHvUpdateTriggerParameters` function retargets a live trigger without re-creating it, so that the same `EventHandle` returned by [`WHvCreateTrigger`](WHvCreateTrigger.md) continues to deliver the updated action. The update is applied atomically with respect to delivery, so a signal that races with the update observes either the old or the new parameters.

The trigger type is fixed at creation time and cannot be changed by this function. Within the same trigger type, any of the parameter fields may be changed — for example, the target virtual processor, vector, or MSI address and data. To deliver a different kind of action, delete the trigger with [`WHvDeleteTrigger`](WHvDeleteTrigger.md) and create a new one.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateTrigger`](WHvCreateTrigger.md)
- [`WHvDeleteTrigger`](WHvDeleteTrigger.md)
- [Trigger Data Types](WHvTriggerDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
