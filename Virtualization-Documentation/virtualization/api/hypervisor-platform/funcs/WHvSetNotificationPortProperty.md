---
title: WHvSetNotificationPortProperty
description: Learn about the WHvSetNotificationPortProperty function that sets a property on an existing notification port.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvSetNotificationPortProperty

Sets a property on an existing notification port.

## Syntax

```C
typedef enum WHV_NOTIFICATION_PORT_PROPERTY_CODE
{
    WHvNotificationPortPropertyPreferredTargetVp = 1,
    WHvNotificationPortPropertyPreferredTargetDuration = 5,
} WHV_NOTIFICATION_PORT_PROPERTY_CODE;

typedef UINT64 WHV_NOTIFICATION_PORT_PROPERTY;

typedef PVOID WHV_NOTIFICATION_PORT_HANDLE;

HRESULT
WINAPI
WHvSetNotificationPortProperty(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_NOTIFICATION_PORT_HANDLE PortHandle,
    _In_ WHV_NOTIFICATION_PORT_PROPERTY_CODE PropertyCode,
    _In_ WHV_NOTIFICATION_PORT_PROPERTY PropertyValue
    );
```

### Parameters

`Partition`

Handle to the partition object.

`PortHandle`

Handle to the notification port, as returned by [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md).

`PropertyCode`

Specifies the property to set, as a `WHV_NOTIFICATION_PORT_PROPERTY_CODE` value.

`PropertyValue`

Specifies the value to assign to the property identified by `PropertyCode`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function fails if `PropertyCode` is not one of the supported `WHV_NOTIFICATION_PORT_PROPERTY_CODE` values, if `PropertyValue` is out of range for the property — for example, a target virtual processor index that is neither a valid index nor `WHV_ANY_VP` — or if `PortHandle` does not refer to a valid notification port.

## Remarks

The `WHvSetNotificationPortProperty` function sets a property on a notification port that was created with [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md).

`WHvNotificationPortPropertyPreferredTargetVp` sets the index of the virtual processor that the hypervisor prefers as the target for the port's notifications. The default value is `WHV_ANY_VP`, which lets the hypervisor select any virtual processor.

`WHvNotificationPortPropertyPreferredTargetDuration` sets the duration, in 100-nanosecond units, for which the preferred target virtual processor remains the affinity target. The default value is `WHV_NOTIFICATION_PORT_PREFERRED_DURATION_MAX`.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md)
- [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md)
- [Notification Port Data Types](WHvNotificationPortDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
