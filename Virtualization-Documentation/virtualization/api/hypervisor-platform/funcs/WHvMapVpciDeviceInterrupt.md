---
title: WHvMapVpciDeviceInterrupt
description: Learn about the WHvMapVpciDeviceInterrupt function that maps an MSI or MSI-X interrupt for an assigned virtual PCI device and returns the MSI address and data to program into the device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvMapVpciDeviceInterrupt

Maps an MSI or MSI-X interrupt for an assigned virtual PCI device and returns the MSI address and data to program into the device.

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
WHvMapVpciDeviceInterrupt(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT64 LogicalDeviceId,
    _In_ UINT32 Index,
    _In_ UINT32 MessageCount,
    _In_ const WHV_VPCI_INTERRUPT_TARGET* Target,
    _Out_ UINT64* MsiAddress,
    _Out_ UINT32* MsiData
    );
```

### Parameters

`Partition`

Handle to the partition that owns the virtual PCI device.

`LogicalDeviceId`

Specifies the logical device ID of the virtual PCI device, as assigned when the device was created.

`Index`

Specifies the index of the interrupt mapping entry to create. Each mapping request must specify a unique, unused index.

`MessageCount`

Specifies the number of messages requested for the interrupt. For a multi-message MSI interrupt, this is a power of two between 1 and 32, inclusive. For an MSI-X interrupt, set this value to 1.

`Target`

Specifies a pointer to a `WHV_VPCI_INTERRUPT_TARGET` structure that supplies the target vector and processor set for the interrupt. When `MessageCount` is greater than 1, every message is initialized with the same target processor set.

`MsiAddress`

Receives the MSI address assigned to the interrupt. Program this value into the MSI or MSI-X capability of the device.

`MsiData`

Receives the MSI data payload assigned to the interrupt. Program this value into the MSI or MSI-X capability of the device.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` when `Index` is greater than 65535, when `MessageCount` is 0 or greater than 32, when `Target->Flags` specifies an undefined flag, when `Target->ProcessorCount` is 0 or exceeds the number of virtual processors in the partition, when a processor in `Target->Processors` is not a valid virtual processor index, or when `WHvVpciInterruptTargetFlagMulticast` is set together with a single target processor. On Arm64, the function returns `E_INVALIDARG` when `Target->ProcessorCount` is greater than 1 or when `WHvVpciInterruptTargetFlagMulticast` is set, because the hypervisor targets each MSI to a single virtual processor on Arm64.

## Remarks

The `WHvMapVpciDeviceInterrupt` function establishes a mapping so that interrupts issued by the physical resources backing the assigned device are routed to the target virtual processors of the partition that owns the device. After the mapping is created, program the returned `MsiAddress` and `MsiData` values into the MSI or MSI-X capability of the device.

When the device was created with logical interrupts enabled, the caller can also assert the interrupt directly by passing the returned `MsiAddress` and `MsiData` to [`WHvRequestVpciDeviceInterrupt`](WHvRequestVpciDeviceInterrupt.md).

When mapping a multi-message MSI interrupt (`MessageCount` greater than 1), all messages are initialized with the same target processor set. Use [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md) afterward to retarget individual messages to a different processor set. Query the current target of a mapped interrupt with [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md), and remove the mapping with [`WHvUnmapVpciDeviceInterrupt`](WHvUnmapVpciDeviceInterrupt.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvUnmapVpciDeviceInterrupt`](WHvUnmapVpciDeviceInterrupt.md)
- [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md)
- [`WHvRequestVpciDeviceInterrupt`](WHvRequestVpciDeviceInterrupt.md)
- [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
