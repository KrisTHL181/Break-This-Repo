---
title: Notification Port Data Types
description: Learn about the notification port data types used to create and configure notification ports on a partition.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Notification Port Data Types

The notification port data types define the port types, parameters, property codes, and handle used to create and configure notification ports on a partition.

## Syntax

```C
typedef enum WHV_NOTIFICATION_PORT_TYPE
{
    WHvNotificationPortTypeEvent = 2,
    WHvNotificationPortTypeDoorbell = 4,
} WHV_NOTIFICATION_PORT_TYPE;

typedef struct WHV_DOORBELL_MATCH_DATA
{
    WHV_GUEST_PHYSICAL_ADDRESS GuestAddress;
    UINT64 Value;
    UINT32 Length;
    UINT32 MatchOnValue:1;
    UINT32 MatchOnLength:1;
    UINT32 Reserved:30;
} WHV_DOORBELL_MATCH_DATA;

typedef struct WHV_NOTIFICATION_PORT_PARAMETERS
{
    WHV_NOTIFICATION_PORT_TYPE NotificationPortType;
    UINT16 Reserved;
    UINT8 Reserved1;
    UINT8 ConnectionVtl;

    union
    {
        WHV_DOORBELL_MATCH_DATA Doorbell;
        struct
        {
            UINT32 ConnectionId;
        } Event;
    };

} WHV_NOTIFICATION_PORT_PARAMETERS;

typedef enum WHV_NOTIFICATION_PORT_PROPERTY_CODE
{
    WHvNotificationPortPropertyPreferredTargetVp = 1,
    WHvNotificationPortPropertyPreferredTargetDuration = 5,
} WHV_NOTIFICATION_PORT_PROPERTY_CODE;

typedef UINT64 WHV_NOTIFICATION_PORT_PROPERTY;

//
// The preferred target VP defaults to any VP.
//

#define WHV_ANY_VP (0xFFFFFFFF)

//
// Define the maximum possible value for the preferred target duration.
//

#define WHV_NOTIFICATION_PORT_PREFERRED_DURATION_MAX (0xFFFFFFFFFFFFFFFFUI64)

typedef PVOID WHV_NOTIFICATION_PORT_HANDLE;
```

## Members

`WHV_NOTIFICATION_PORT_TYPE`

Specifies the type of notification port to create.

- `WHvNotificationPortTypeEvent` — A SynIC event port. The port's event object is signaled when the guest invokes the `HvCallSignalEvent` hypercall with a connection ID that matches the `ConnectionId` supplied in `WHV_NOTIFICATION_PORT_PARAMETERS`.
- `WHvNotificationPortTypeDoorbell` — A doorbell port. The port's event object is signaled when a virtual processor performs a write to the guest physical address described by `WHV_DOORBELL_MATCH_DATA` that matches the configured value and length.

`WHV_DOORBELL_MATCH_DATA`

Describes the guest physical address write that triggers a doorbell-type notification port.

- `GuestAddress` — The guest physical address that is monitored for writes.
- `Value` — The value that a write must contain to trigger the port. Used only when `MatchOnValue` is set; otherwise it must be zero.
- `Length` — The size, in bytes, of the monitored write. Valid values are 1, 2, 4, and 8. Used only when `MatchOnLength` is set; otherwise it must be zero.
- `MatchOnValue` — When set, the port triggers only when the write value equals `Value`. Can be set only when `MatchOnLength` is also set.
- `MatchOnLength` — When set, the port triggers only when the write length equals `Length`.
- `Reserved` — Reserved. Must be zero.

`WHV_NOTIFICATION_PORT_PARAMETERS`

Specifies the type and configuration of a notification port that is passed to [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md).

- `NotificationPortType` — The port type, one of the `WHV_NOTIFICATION_PORT_TYPE` values.
- `Reserved`, `Reserved1` — Reserved. Must be zero.
- `ConnectionVtl` — The guest virtual trust level (VTL) the port is connected to. Must be zero.
- `Doorbell` — For a doorbell port, the `WHV_DOORBELL_MATCH_DATA` that describes the triggering write.
- `Event` — For an event port, the `ConnectionId` that the guest passes to `HvCallSignalEvent` to signal the port.

`WHV_NOTIFICATION_PORT_PROPERTY_CODE`

Identifies a notification port property that is set with [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md).

- `WHvNotificationPortPropertyPreferredTargetVp` — The index of the virtual processor that is the preferred target for the port's notifications. The default value is `WHV_ANY_VP`, which lets the hypervisor select any virtual processor.
- `WHvNotificationPortPropertyPreferredTargetDuration` — The duration, in 100-nanosecond units, for which the preferred target virtual processor remains the affinity target. The default value is `WHV_NOTIFICATION_PORT_PREFERRED_DURATION_MAX`.

`WHV_NOTIFICATION_PORT_PROPERTY`

A `UINT64` that holds the value of a notification port property. The interpretation of the value depends on the associated `WHV_NOTIFICATION_PORT_PROPERTY_CODE`.

`WHV_NOTIFICATION_PORT_HANDLE`

An opaque handle to a notification port. The handle is returned by [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md) and is used to identify the port in [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md) and [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md).

## Remarks

Notification ports provide a unified mechanism for receiving asynchronous notifications from a partition through a Windows event object. The notification port API supersedes the deprecated `WHvRegisterPartitionDoorbellEvent` function, which is equivalent to creating a doorbell-type notification port.

A doorbell port (`WHvNotificationPortTypeDoorbell`) signals its event object when a virtual processor writes a matching value to a monitored guest physical address. The `MatchOnValue` and `MatchOnLength` flags in `WHV_DOORBELL_MATCH_DATA` control whether the value and length, respectively, must match for the port to trigger. When `MatchOnValue` is clear, `Value` must be zero; when `MatchOnLength` is clear, `Length` must be zero.

An event port (`WHvNotificationPortTypeEvent`) signals its event object when the guest invokes the `HvCallSignalEvent` hypercall with a connection ID equal to the `ConnectionId` field of the `Event` member.

`WHV_ANY_VP` is the default value of the `WHvNotificationPortPropertyPreferredTargetVp` property and indicates that the hypervisor may target any virtual processor. `WHV_NOTIFICATION_PORT_PREFERRED_DURATION_MAX` is the default value of the `WHvNotificationPortPropertyPreferredTargetDuration` property.

## See also

- [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md)
- [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md)
- [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
