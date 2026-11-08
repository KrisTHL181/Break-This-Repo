---
title: WHvReadVpciDeviceRegister
description: Learn about the WHvReadVpciDeviceRegister function that reads a configuration space or MMIO register of a virtual PCI (VPCI) device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvReadVpciDeviceRegister

Reads a configuration space or MMIO register of a virtual PCI (VPCI) device.

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

typedef struct WHV_VPCI_DEVICE_REGISTER
{
    WHV_VPCI_DEVICE_REGISTER_SPACE Location;
    UINT32 SizeInBytes;
    UINT64 OffsetInBytes;
} WHV_VPCI_DEVICE_REGISTER;

HRESULT
WINAPI
WHvReadVpciDeviceRegister(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ const WHV_VPCI_DEVICE_REGISTER* Register,
    _Out_writes_bytes_(Register->SizeInBytes) VOID* Data
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

`Register`

Specifies the register to read, as a [`WHV_VPCI_DEVICE_REGISTER`](WHvVpciDataTypes.md) structure that identifies the register space, offset, and size.

`Data`

Receives the data read from the register. The buffer must be at least `Register->SizeInBytes` bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvReadVpciDeviceRegister` function reads a register from the configuration space or MMIO space of the PCIe physical or virtual function associated with a VPCI device. For configuration space, access to certain registers may be filtered by the underlying virtual PCI stack to guarantee secure VM assignment of PCIe functions. For MMIO space, this function can read registers that have not been mapped directly into the caller's address space because the underlying virtual PCI stack must enforce mitigations for those registers.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvWriteVpciDeviceRegister`](WHvWriteVpciDeviceRegister.md)
- [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
