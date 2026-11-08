---
title: WHvGetVpciDeviceProperty
description: Learn about the WHvGetVpciDeviceProperty function that retrieves a property of a virtual PCI (VPCI) device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVpciDeviceProperty

Retrieves a property of a virtual PCI (VPCI) device.

## Syntax

```C
typedef enum WHV_VPCI_DEVICE_PROPERTY_CODE
{
    WHvVpciDevicePropertyCodeUndefined   = 0,
    WHvVpciDevicePropertyCodeHardwareIDs = 1,
    WHvVpciDevicePropertyCodeProbedBARs  = 2
} WHV_VPCI_DEVICE_PROPERTY_CODE;

HRESULT
WINAPI
WHvGetVpciDeviceProperty(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ WHV_VPCI_DEVICE_PROPERTY_CODE PropertyCode,
    _Out_writes_bytes_to_(PropertyBufferSizeInBytes, *WrittenSizeInBytes) VOID* PropertyBuffer,
    _In_ UINT32 PropertyBufferSizeInBytes,
    _Out_opt_ UINT32* WrittenSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition that owns the VPCI device.

`LogicalDeviceId`

Specifies the logical device identifier of the VPCI device.

`PropertyCode`

Specifies the property to query, as a [`WHV_VPCI_DEVICE_PROPERTY_CODE`](WHvVpciDataTypes.md) value.

`PropertyBuffer`

Receives the value of the requested property. For `WHvVpciDevicePropertyCodeHardwareIDs`, the buffer receives a [`WHV_VPCI_HARDWARE_IDS`](WHvVpciDataTypes.md) structure. For `WHvVpciDevicePropertyCodeProbedBARs`, it receives a [`WHV_VPCI_PROBED_BARS`](WHvVpciDataTypes.md) structure.

`PropertyBufferSizeInBytes`

Specifies the size, in bytes, of the `PropertyBuffer` buffer.

`WrittenSizeInBytes`

Receives the number of bytes written to `PropertyBuffer`. This parameter is optional and can be `NULL`.

## Return Value

If the function succeeds, the return value is `S_OK`.

If `PropertyBufferSizeInBytes` is smaller than the structure required for the requested property, the function returns `WHV_E_INSUFFICIENT_BUFFER`. If `PropertyCode` is not a recognized property, the function returns `WHV_E_UNKNOWN_PROPERTY`.

## Remarks

The `WHvGetVpciDeviceProperty` function retrieves a property of a VPCI device created with [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md). Use `WHvVpciDevicePropertyCodeHardwareIDs` to retrieve the device's PCI vendor, device, revision, class, and subsystem identifiers, and `WHvVpciDevicePropertyCodeProbedBARs` to retrieve the probed values of the device's type 0 base address registers (BARs).

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
