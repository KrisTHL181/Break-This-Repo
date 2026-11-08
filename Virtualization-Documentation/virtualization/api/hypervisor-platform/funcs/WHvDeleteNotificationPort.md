---
title: WHvDeleteNotificationPort
description: Learn about the WHvDeleteNotificationPort function that deletes a notification port.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvDeleteNotificationPort

Deletes a notification port.

## Syntax

```C
typedef PVOID WHV_NOTIFICATION_PORT_HANDLE;

HRESULT
WINAPI
WHvDeleteNotificationPort(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_NOTIFICATION_PORT_HANDLE PortHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`PortHandle`

Handle to the notification port to delete, as returned by [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

The function fails if `PortHandle` does not refer to a valid notification port.

## Remarks

The `WHvDeleteNotificationPort` function deletes a notification port that was created with [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md) and releases the resources associated with it. After the port is deleted, the partition no longer signals the associated Windows event object, and `PortHandle` is no longer valid for use with [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md).

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
- [`WHvSetNotificationPortProperty`](WHvSetNotificationPortProperty.md)
- [Notification Port Data Types](WHvNotificationPortDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
