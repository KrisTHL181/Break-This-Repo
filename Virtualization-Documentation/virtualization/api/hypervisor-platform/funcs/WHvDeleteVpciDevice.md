---
title: WHvDeleteVpciDevice
description: Learn about the WHvDeleteVpciDevice function that destroys a virtual PCI (VPCI) device and releases its resources.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvDeleteVpciDevice

Destroys a virtual PCI (VPCI) device and releases its resources.

## Syntax

```C
HRESULT
WINAPI
WHvDeleteVpciDevice(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device to destroy.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvDeleteVpciDevice` function destroys a VPCI device that was created with [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md). The call unmaps the device's MMIO ranges and interrupts, resets the physical resources, and reverts the IOMMU mappings of the associated requestor IDs (RIDs) to the root partition.

Any MMIO mapping array previously returned by [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md) for this device is no longer valid after this call.

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
- [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md)
- [`WHvUnmapVpciDeviceMmioRanges`](WHvUnmapVpciDeviceMmioRanges.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
