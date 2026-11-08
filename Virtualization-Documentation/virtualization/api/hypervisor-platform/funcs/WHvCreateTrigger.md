---
title: WHvCreateTrigger
description: Learn about the WHvCreateTrigger function that creates a trigger object that delivers a pre-configured interrupt or event to a partition when a host event is signaled.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCreateTrigger

Creates a trigger object that delivers a pre-configured interrupt or event to a partition when a host event is signaled.

## Syntax

```C
typedef enum WHV_TRIGGER_TYPE
{
#if defined(_AMD64_)
    WHvTriggerTypeInterrupt = 0,
#endif
    WHvTriggerTypeSynicEvent = 1,
    WHvTriggerTypeDeviceInterrupt = 2,
} WHV_TRIGGER_TYPE;

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
WHvCreateTrigger(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_TRIGGER_PARAMETERS* Parameters,
    _Out_ WHV_TRIGGER_HANDLE* TriggerHandle,
    _Out_ HANDLE* EventHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`Parameters`

Specifies the type of the trigger and the action to deliver to the partition. The `TriggerType` member selects which member of the union is used: `WHvTriggerTypeInterrupt` (x64 only) uses `Interrupt`, `WHvTriggerTypeSynicEvent` uses `SynicEvent`, and `WHvTriggerTypeDeviceInterrupt` uses `DeviceInterrupt`. See [Trigger Data Types](WHvTriggerDataTypes.md) for the full definition of each member.

`TriggerHandle`

Receives the handle to the newly created trigger object. Use this handle with [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md) to retarget the trigger and with [`WHvDeleteTrigger`](WHvDeleteTrigger.md) to delete it.

`EventHandle`

Receives a Win32 event handle. The caller activates the trigger by signaling this event — for example, with `SetEvent` — which causes the configured action in `Parameters` to be delivered to the partition. The caller owns this handle and must release it with `CloseHandle` when it is no longer needed.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if the parameters in `Parameters` are not valid for the selected trigger type. For a `WHvTriggerTypeDeviceInterrupt` trigger, an invalid `LogicalDeviceId` returns `HRESULT_FROM_WIN32(ERROR_HV_INVALID_DEVICE_ID)`.

## Remarks

The `WHvCreateTrigger` function creates a partition-scoped object that binds a host event to a pre-configured guest-targeting action. After the trigger is created, the host signals the returned `EventHandle` — for example, from an I/O completion routine or another thread — to deliver the configured interrupt or event to the partition without making a separate API call or hypercall on each delivery. This makes triggers well suited to data-path scenarios such as virtual NIC receive queues, storage completion rings, and channel signaling.

The `TriggerType` member of `Parameters` selects the action that is delivered when the event is signaled:

- `WHvTriggerTypeInterrupt` (x64 only) injects a virtual interrupt described by the `WHV_INTERRUPT_CONTROL` structure — the same effect as [`WHvRequestInterrupt`](WHvRequestInterrupt.md), but pre-armed so that it can be re-delivered each time the event is signaled.
- `WHvTriggerTypeSynicEvent` sets a synthetic interrupt controller (SynIC) event flag on the virtual processor, virtual trust level, and SINT named by the `WHV_SYNIC_EVENT_PARAMETERS` structure. `TargetVtl` must be VTL 0.
- `WHvTriggerTypeDeviceInterrupt` asserts a device (MSI) interrupt for the logical device named by `LogicalDeviceId`, using `MsiAddress` and `MsiData`. The logical device must already be present in the partition and the interrupt mapped — for example, through [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) — before the trigger is signaled.

Delivery is asynchronous. Multiple signals that arrive before a delivery completes may coalesce into a single delivery. The guest must be prepared to drain the queue or ring that the trigger represents on each delivery.

`WHvTriggerTypeInterrupt` and the `Interrupt` member of `WHV_TRIGGER_PARAMETERS` are available only on x64. On Arm64, only `WHvTriggerTypeSynicEvent` and `WHvTriggerTypeDeviceInterrupt` are available.

The trigger and the event handle have independent lifetimes. Deleting the trigger with [`WHvDeleteTrigger`](WHvDeleteTrigger.md) does not invalidate `EventHandle`; the caller must still close it with `CloseHandle`. Triggers are not preserved across live migration.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md)
- [`WHvDeleteTrigger`](WHvDeleteTrigger.md)
- [Trigger Data Types](WHvTriggerDataTypes.md)
- [`WHvRequestInterrupt`](WHvRequestInterrupt.md)
- [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
