---
title: WHvCreateNotificationPort
description: Learn about the WHvCreateNotificationPort function that creates a notification port that signals a Windows event object when the partition generates a matching doorbell or SynIC event.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCreateNotificationPort

Creates a notification port that signals a Windows event object when the partition generates a matching doorbell or SynIC event.

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

typedef PVOID WHV_NOTIFICATION_PORT_HANDLE;

HRESULT
WINAPI
WHvCreateNotificationPort(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_NOTIFICATION_PORT_PARAMETERS* Parameters,
    _In_ HANDLE EventHandle,
    _Out_ WHV_NOTIFICATION_PORT_HANDLE* PortHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`Parameters`

Specifies the type and configuration of the notification port to create. See [`WHV_NOTIFICATION_PORT_PARAMETERS`](WHvNotificationPortDataTypes.md).

`EventHandle`

Handle to a Windows event object that is signaled when the notification port is triggered.

`PortHandle`

Receives the handle to the newly created notification port. The handle is used in subsequent calls to [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md) and [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `NotificationPortType` is not a valid `WHV_NOTIFICATION_PORT_TYPE` value, or if the doorbell match data is not valid for a doorbell port. For the doorbell match data fields and the rules that govern them, see [Doorbell Data Types](WHvDoorbellDataTypes.md).

## Remarks

The `WHvCreateNotificationPort` function creates a notification port that signals the supplied Windows event object when the partition generates a matching event. Notification ports supersede the deprecated `WHvRegisterPartitionDoorbellEvent` function, which is equivalent to creating a doorbell-type notification port.

When `NotificationPortType` is `WHvNotificationPortTypeDoorbell`, the port signals `EventHandle` when a virtual processor writes a matching value to the guest physical address described by the `Doorbell` member. The `MatchOnValue` and `MatchOnLength` flags control whether the written value and length must match.

When `NotificationPortType` is `WHvNotificationPortTypeEvent`, the port signals `EventHandle` when the guest invokes the `HvCallSignalEvent` hypercall with a connection ID equal to the `Event.ConnectionId` member.

Create the partition with [`WHvCreatePartition`](WHvCreatePartition.md), configure it, and call [`WHvSetupPartition`](WHvSetupPartition.md) before creating notification ports. After the port is created, adjust its preferred target virtual processor or affinity duration with [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md), and delete it with [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md)
- [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md)
- [Notification Port Data Types](WHvNotificationPortDataTypes.md)
- [`WHvSetupPartition`](WHvSetupPartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
