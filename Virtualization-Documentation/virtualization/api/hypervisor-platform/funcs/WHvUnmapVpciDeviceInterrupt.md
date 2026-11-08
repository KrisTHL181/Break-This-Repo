---
title: WHvUnmapVpciDeviceInterrupt
description: Learn about the WHvUnmapVpciDeviceInterrupt function that removes a previously mapped interrupt for an assigned virtual PCI device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvUnmapVpciDeviceInterrupt

Removes a previously mapped interrupt for an assigned virtual PCI device.

## Syntax

```C
HRESULT
WINAPI
WHvUnmapVpciDeviceInterrupt(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ UINT32 Index
    );
```

### Parameters

`Partition`

Handle to the partition that owns the virtual PCI device.

`LogicalDeviceId`

Specifies the logical device ID of the virtual PCI device, as assigned when the device was created.

`Index`

Specifies the index of the interrupt entry to unmap. This is the same index that was supplied to [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) when the interrupt was mapped.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvUnmapVpciDeviceInterrupt` function removes a mapping created by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md). After the interrupt is unmapped, the physical resources backing the device no longer route interrupts at that index to the partition's virtual processors.

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
- [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
