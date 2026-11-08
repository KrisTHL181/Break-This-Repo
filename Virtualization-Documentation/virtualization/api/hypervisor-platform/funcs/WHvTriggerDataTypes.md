---
title: Trigger Data Types
description: Learn about the trigger data types used to describe and create triggers that deliver interrupts and events to a partition.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Trigger Data Types

The trigger data types describe the type of a trigger, the action it delivers to a partition, and the handle used to identify it.

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
```

## Members

### WHV_TRIGGER_TYPE

Identifies the kind of action a trigger delivers when its event is signaled.

`WHvTriggerTypeInterrupt`

Injects a virtual interrupt described by a `WHV_INTERRUPT_CONTROL` structure. This trigger type is available only on x64.

`WHvTriggerTypeSynicEvent`

Sets a synthetic interrupt controller (SynIC) event flag described by a `WHV_SYNIC_EVENT_PARAMETERS` structure.

`WHvTriggerTypeDeviceInterrupt`

Asserts a device (MSI) interrupt for a logical device that is present in the partition.

### WHV_SYNIC_EVENT_PARAMETERS

Describes the SynIC event flag that a `WHvTriggerTypeSynicEvent` trigger sets.

`VpIndex`

Specifies the index of the virtual processor that receives the event.

`TargetSint`

Specifies the synthetic interrupt source (SINT) to signal.

`TargetVtl`

Specifies the virtual trust level that receives the event. For triggers, this must be VTL 0.

`FlagNumber`

Specifies the SynIC event flag to set.

### WHV_TRIGGER_PARAMETERS

Describes a trigger: its type and the parameters of the action it delivers.

`TriggerType`

Specifies the trigger type, which selects the active member of the union.

`Reserved`

Reserved. Set to 0.

`Interrupt`

Specifies the interrupt to inject when `TriggerType` is `WHvTriggerTypeInterrupt`. This member is available only on x64. See [`WHvRequestInterrupt`](WHvRequestInterrupt.md) for the definition of `WHV_INTERRUPT_CONTROL`.

`SynicEvent`

Specifies the SynIC event to signal when `TriggerType` is `WHvTriggerTypeSynicEvent`.

`DeviceInterrupt`

Specifies the device interrupt to assert when `TriggerType` is `WHvTriggerTypeDeviceInterrupt`. `LogicalDeviceId` identifies the logical device in the partition, and `MsiAddress` and `MsiData` specify the message-signaled interrupt to deliver. The nested `Reserved` field must be set to 0.

### WHV_TRIGGER_HANDLE

Identifies a trigger object created by [`WHvCreateTrigger`](WHvCreateTrigger.md). The handle is passed to [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md) and [`WHvDeleteTrigger`](WHvDeleteTrigger.md).

## Remarks

`WHV_TRIGGER_PARAMETERS` is passed to [`WHvCreateTrigger`](WHvCreateTrigger.md) to create a trigger and to [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md) to retarget one. The `TriggerType` member selects which member of the union is read; the trigger type is fixed at creation time and cannot be changed by an update.

`WHvTriggerTypeInterrupt` and the `Interrupt` member of `WHV_TRIGGER_PARAMETERS` are available only on x64.

A `WHvTriggerTypeDeviceInterrupt` trigger asserts an interrupt for a logical device that already exists in the partition; the device must be present and its interrupt mapped — for example, through [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) — before the trigger is signaled.

## See also

- [`WHvCreateTrigger`](WHvCreateTrigger.md)
- [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md)
- [`WHvDeleteTrigger`](WHvDeleteTrigger.md)
- [`WHvRequestInterrupt`](WHvRequestInterrupt.md)
- [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
