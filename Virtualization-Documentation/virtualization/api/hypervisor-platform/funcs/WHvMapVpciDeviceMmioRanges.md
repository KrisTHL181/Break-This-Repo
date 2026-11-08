---
title: WHvMapVpciDeviceMmioRanges
description: Learn about the WHvMapVpciDeviceMmioRanges function that maps the MMIO ranges of a virtual PCI (VPCI) device into the caller's process.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvMapVpciDeviceMmioRanges

Maps the MMIO ranges of a virtual PCI (VPCI) device into the caller's process.

## Syntax

```C
typedef enum WHV_VPCI_DEVICE_REGISTER_SPACE
{
    WHvVpciConfigSpace = -1,
    WHvVpciBar0 = 0,
    WHvVpciBar1 = 1,
    WHvVpciBar2 = 2,
    WHvVpciBar3 = 3,
    WHvVpciBar4 = 4,
    WHvVpciBar5 = 5
} WHV_VPCI_DEVICE_REGISTER_SPACE;

typedef enum WHV_VPCI_MMIO_RANGE_FLAGS
{
    WHvVpciMmioRangeFlagReadAccess = 0x00000001,
    WHvVpciMmioRangeFlagWriteAccess = 0x00000002
} WHV_VPCI_MMIO_RANGE_FLAGS;

// Enables bitwise operators on the WHV_VPCI_MMIO_RANGE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_VPCI_MMIO_RANGE_FLAGS);

typedef struct WHV_VPCI_MMIO_MAPPING
{
    WHV_VPCI_DEVICE_REGISTER_SPACE Location;
    WHV_VPCI_MMIO_RANGE_FLAGS Flags;
    UINT64 SizeInBytes;
    UINT64 OffsetInBytes;
    PVOID VirtualAddress;
} WHV_VPCI_MMIO_MAPPING;

HRESULT
WINAPI
WHvMapVpciDeviceMmioRanges(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _Out_ UINT32* MappingCount,
    _Outptr_result_buffer_(*MappingCount) WHV_VPCI_MMIO_MAPPING** Mappings
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

`MappingCount`

Receives the number of elements in the returned `Mappings` array.

`Mappings`

Receives a pointer to an array of [`WHV_VPCI_MMIO_MAPPING`](WHvVpciDataTypes.md) elements that describe the mapped MMIO ranges.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvMapVpciDeviceMmioRanges` function maps the physical MMIO ranges associated with a VPCI device into the virtual address space of the caller's process. The device must be in the D0 power state; set the power state with [`WHvSetVpciDevicePowerState`](WHvSetVpciDevicePowerState.md).

Each available base address register (BAR) is represented by one or more entries in the `Mappings` array. Entries are primarily sorted by BAR. Entries for the same BAR are sorted by offset, are non-overlapping, and cover the entire size of the BAR with no intervening gaps. Each entry identifies a sub-range of the BAR, starting at the specified offset from the base address and of the specified length, the virtual address at which the sub-range is mapped, and the level of access granted to the caller's process.

The returned array remains valid until the caller unmaps the device's MMIO ranges with [`WHvUnmapVpciDeviceMmioRanges`](WHvUnmapVpciDeviceMmioRanges.md), powers off the device with [`WHvSetVpciDevicePowerState`](WHvSetVpciDevicePowerState.md), or destroys the device with [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvUnmapVpciDeviceMmioRanges`](WHvUnmapVpciDeviceMmioRanges.md)
- [`WHvSetVpciDevicePowerState`](WHvSetVpciDevicePowerState.md)
- [`WHvReadVpciDeviceRegister`](WHvReadVpciDeviceRegister.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
