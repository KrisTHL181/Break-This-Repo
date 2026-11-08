---
title: WHvUnmapVpciDeviceMmioRanges
description: Learn about the WHvUnmapVpciDeviceMmioRanges function that unmaps the MMIO ranges of a virtual PCI (VPCI) device from the caller's process.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvUnmapVpciDeviceMmioRanges

Unmaps the MMIO ranges of a virtual PCI (VPCI) device from the caller's process.

## Syntax

```C
HRESULT
WINAPI
WHvUnmapVpciDeviceMmioRanges(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvUnmapVpciDeviceMmioRanges` function unmaps all of the MMIO ranges of the specified device from the caller's virtual address space. After this call, the array of [`WHV_VPCI_MMIO_MAPPING`](WHvVpciDataTypes.md) elements previously returned by [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md) is no longer valid.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md)
- [`WHvSetVpciDevicePowerState`](WHvSetVpciDevicePowerState.md)
- [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
