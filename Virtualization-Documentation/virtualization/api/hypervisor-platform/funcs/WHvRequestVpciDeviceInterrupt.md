---
title: WHvRequestVpciDeviceInterrupt
description: Learn about the WHvRequestVpciDeviceInterrupt function that delivers a logical interrupt to the partition that owns an assigned virtual PCI device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvRequestVpciDeviceInterrupt

Delivers a logical interrupt to the partition that owns an assigned virtual PCI device.

## Syntax

```C
HRESULT
WINAPI
WHvRequestVpciDeviceInterrupt(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ UINT64 MsiAddress,
    _In_ UINT32 MsiData
    );
```

### Parameters

`Partition`

Handle to the partition that owns the virtual PCI device.

`LogicalDeviceId`

Specifies the logical device ID of the virtual PCI device, as assigned when the device was created.

`MsiAddress`

Specifies the MSI address of the interrupt to deliver, as returned by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md).

`MsiData`

Specifies the MSI data payload of the interrupt to deliver, as returned by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

On x64, if `MsiAddress` is greater than `0xFFFFFFFF`, the function returns `HRESULT_FROM_WIN32(ERROR_HV_INVALID_PARAMETER)`. The hypervisor performs the remaining validation of the device and interrupt.

## Remarks

The `WHvRequestVpciDeviceInterrupt` function asserts an interrupt for a device that was created with logical interrupts enabled. The interrupt is identified by the `MsiAddress` and `MsiData` values returned by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md), and is delivered to the virtual processors that the mapping currently targets.

This call lets the virtualization stack inject the interrupt on behalf of the device instead of relying on the physical resources to signal it. Map the interrupt with [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) before requesting delivery.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md)
- [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md)
- [`WHvUnmapVpciDeviceInterrupt`](WHvUnmapVpciDeviceInterrupt.md)
- [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
