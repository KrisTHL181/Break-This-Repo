---
title: WHvGetVpciDeviceNotification
description: Learn about the WHvGetVpciDeviceNotification function that retrieves the next pending notification for a virtual PCI (VPCI) device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVpciDeviceNotification

Retrieves the next pending notification for a virtual PCI (VPCI) device.

## Syntax

```C
typedef enum WHV_VPCI_DEVICE_NOTIFICATION_TYPE
{
    WHvVpciDeviceNotificationUndefined = 0,
    WHvVpciDeviceNotificationMmioRemapping = 1,
    WHvVpciDeviceNotificationSurpriseRemoval = 2
} WHV_VPCI_DEVICE_NOTIFICATION_TYPE;

typedef struct WHV_VPCI_DEVICE_NOTIFICATION
{
    WHV_VPCI_DEVICE_NOTIFICATION_TYPE NotificationType;
    UINT32 Reserved1;
    union
    {
        UINT64 Reserved2;
    };
} WHV_VPCI_DEVICE_NOTIFICATION;

HRESULT
WINAPI
WHvGetVpciDeviceNotification(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _Out_writes_bytes_(NotificationSizeInBytes) WHV_VPCI_DEVICE_NOTIFICATION* Notification,
    _In_ UINT32 NotificationSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

`Notification`

Receives the notification type and data, as a [`WHV_VPCI_DEVICE_NOTIFICATION`](WHvVpciDataTypes.md) structure.

`NotificationSizeInBytes`

Specifies the size, in bytes, of the `Notification` buffer.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvGetVpciDeviceNotification` function retrieves the next available asynchronous notification for a VPCI device. Call this function after the device notification event supplied to [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md) is signaled, and call the function repeatedly until the received notification type is `WHvVpciDeviceNotificationUndefined`, which indicates that no more notifications are available.

A `WHvVpciDeviceNotificationMmioRemapping` notification indicates that the device's MMIO ranges have been remapped and should be re-acquired with [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md). A `WHvVpciDeviceNotificationSurpriseRemoval` notification indicates that the device was removed unexpectedly.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md)
- [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
