---
title: WHvRetargetVpciDeviceInterrupt
description: Learn about the WHvRetargetVpciDeviceInterrupt function that changes the target vector and processor set for a previously mapped interrupt of an assigned virtual PCI device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvRetargetVpciDeviceInterrupt

Changes the target vector and processor set for a previously mapped interrupt of an assigned virtual PCI device.

## Syntax

```C
typedef enum WHV_VPCI_INTERRUPT_TARGET_FLAGS
{
    WHvVpciInterruptTargetFlagNone      = 0x00000000,
    WHvVpciInterruptTargetFlagMulticast = 0x00000001,

} WHV_VPCI_INTERRUPT_TARGET_FLAGS;

// Enables bitwise operators on the WHV_VPCI_INTERRUPT_TARGET_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_VPCI_INTERRUPT_TARGET_FLAGS);

typedef struct WHV_VPCI_INTERRUPT_TARGET
{
    UINT32 Vector;
    WHV_VPCI_INTERRUPT_TARGET_FLAGS Flags;
    UINT32 ProcessorCount;
    UINT32 Processors[ANYSIZE_ARRAY];

} WHV_VPCI_INTERRUPT_TARGET;

HRESULT
WINAPI
WHvRetargetVpciDeviceInterrupt(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ UINT64 MsiAddress,
    _In_ UINT32 MsiData,
    _In_ const WHV_VPCI_INTERRUPT_TARGET* Target
    );
```

### Parameters

`Partition`

Handle to the partition that owns the virtual PCI device.

`LogicalDeviceId`

Specifies the logical device ID of the virtual PCI device, as assigned when the device was created.

`MsiAddress`

Specifies the MSI address of the interrupt to retarget, as returned by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md).

`MsiData`

Specifies the MSI data payload of the interrupt to retarget, as returned by [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md).

`Target`

Specifies a pointer to a `WHV_VPCI_INTERRUPT_TARGET` structure that supplies the new target vector and processor set for the interrupt.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` when `Target->ProcessorCount` is 0 or exceeds the number of virtual processors in the partition, when `Target->Flags` specifies an undefined flag, when `WHvVpciInterruptTargetFlagMulticast` is set together with fewer than two target processors, or when a processor in `Target->Processors` is greater than or equal to the maximum number of virtual processors that a partition can have. On Arm64, the function returns `E_INVALIDARG` when `Target->ProcessorCount` is greater than 1, because the hypervisor targets each MSI to a single virtual processor on Arm64.

## Remarks

The `WHvRetargetVpciDeviceInterrupt` function moves an interrupt that was previously mapped with [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md) to a new vector and set of target virtual processors. The interrupt is identified by the `MsiAddress` and `MsiData` values returned when it was mapped, so the device's MSI or MSI-X capability does not need to be reprogrammed.

Retargeting an individual message of a multi-message MSI interrupt lets a guest distribute the messages of a single interrupt across different processor sets, after the messages were all initialized with the same target by `WHvMapVpciDeviceInterrupt`. To direct an interrupt at more than one processor, set `WHvVpciInterruptTargetFlagMulticast` in `Target->Flags` and supply at least two processors in `Target->Processors`; multicast targeting is supported on x64 only.

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
- [`WHvUnmapVpciDeviceInterrupt`](WHvUnmapVpciDeviceInterrupt.md)
- [`WHvRequestVpciDeviceInterrupt`](WHvRequestVpciDeviceInterrupt.md)
- [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
