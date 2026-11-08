---
title: WHvCreateVpciDevice
description: Learn about the WHvCreateVpciDevice function that creates a virtual PCI (VPCI) device and assigns its resources to a partition.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCreateVpciDevice

Creates a virtual PCI (VPCI) device and assigns its resources to a partition.

## Syntax

```C
typedef enum WHV_CREATE_VPCI_DEVICE_FLAGS
{
    WHvCreateVpciDeviceFlagNone = 0x00000000,
    WHvCreateVpciDeviceFlagPhysicallyBacked = 0x00000001,
    WHvCreateVpciDeviceFlagUseLogicalInterrupts = 0x00000002
} WHV_CREATE_VPCI_DEVICE_FLAGS;

// Enables bitwise operators on the WHV_CREATE_VPCI_DEVICE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_CREATE_VPCI_DEVICE_FLAGS);

HRESULT
WINAPI
WHvCreateVpciDevice(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ HANDLE VpciResource,
    _In_ WHV_CREATE_VPCI_DEVICE_FLAGS Flags,
    _In_opt_ HANDLE NotificationEventHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`LogicalDeviceId`

Specifies a unique identifier for the device within the scope of the partition.

`VpciResource`

Handle to a previously allocated VPCI resource that backs the virtual device. Obtain this handle from [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md).

`Flags`

Specifies optional characteristics of the device to create, as a combination of [`WHV_CREATE_VPCI_DEVICE_FLAGS`](WHvVpciDataTypes.md) values.

`NotificationEventHandle`

Specifies an optional handle to an event that is signaled to notify the caller of asynchronous events associated with the VPCI device. When the event is signaled, retrieve the pending notifications with [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvCreateVpciDevice` function creates a virtual PCI device and assigns the physical or virtual resources identified by `VpciResource` to the partition. After this call completes, the IOMMU is programmed so that the PCI requestor ID of the physical or virtual function backing the device is assigned to the partition domain, remapping any DMA operations that originate from that function to the partition's address space.

Allocate the resource with [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md) before calling this function. After the device is created, map its MMIO ranges with [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md) and its interrupts with [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md). Destroy the device with [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md)
- [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md)
- [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md)
- [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
