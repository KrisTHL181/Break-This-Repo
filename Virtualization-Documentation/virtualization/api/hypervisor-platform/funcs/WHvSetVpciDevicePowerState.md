---
title: WHvSetVpciDevicePowerState
description: Learn about the WHvSetVpciDevicePowerState function that changes the power state of a virtual PCI (VPCI) device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvSetVpciDevicePowerState

Changes the power state of a virtual PCI (VPCI) device.

## Syntax

```C
HRESULT
WINAPI
WHvSetVpciDevicePowerState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ DEVICE_POWER_STATE PowerState
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

`PowerState`

Specifies the power state to transition to, as a `DEVICE_POWER_STATE` value (for example, `PowerDeviceD0`).

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvSetVpciDevicePowerState` function requests a power state change for the PCIe physical or virtual function associated with a VPCI device. The device must be in the D0 power state before its MMIO ranges can be mapped with [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md). Transitioning the device out of D0 invalidates the MMIO mapping array previously returned for the device.

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
- [`WHvUnmapVpciDeviceMmioRanges`](WHvUnmapVpciDeviceMmioRanges.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
