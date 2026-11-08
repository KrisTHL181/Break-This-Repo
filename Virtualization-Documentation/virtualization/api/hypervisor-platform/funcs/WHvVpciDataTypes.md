---
title: Virtual PCI Data Types
description: Learn about the virtual PCI (VPCI) data types used to allocate resources, create devices, and manage the lifecycle of assigned PCI devices.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Virtual PCI Data Types

The virtual PCI (VPCI) data types define the flags, property codes, and value structures used to allocate device resources, create virtual PCI devices, map MMIO ranges, access device registers, and target device interrupts.

## Syntax

```C
//
// Virtual PCI type definitions.
//

typedef enum WHV_ALLOCATE_VPCI_RESOURCE_FLAGS
{
    WHvAllocateVpciResourceFlagNone = 0x00000000,
    WHvAllocateVpciResourceFlagAllowDirectP2P = 0x00000001
} WHV_ALLOCATE_VPCI_RESOURCE_FLAGS;

// Enables bitwise operators on the WHV_ALLOCATE_VPCI_RESOURCE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_ALLOCATE_VPCI_RESOURCE_FLAGS);

#define WHV_MAX_DEVICE_ID_SIZE_IN_CHARS 200 // PnP manager limit

typedef struct WHV_SRIOV_RESOURCE_DESCRIPTOR
{
    WCHAR PnpInstanceId[WHV_MAX_DEVICE_ID_SIZE_IN_CHARS];
    LUID VirtualFunctionId;
    UINT16 VirtualFunctionIndex;
    UINT16 Reserved;
} WHV_SRIOV_RESOURCE_DESCRIPTOR;

typedef enum WHV_VPCI_DEVICE_NOTIFICATION_TYPE
{
    WHvVpciDeviceNotificationUndefined = 0,
    WHvVpciDeviceNotificationMmioRemapping = 1,
    WHvVpciDeviceNotificationSurpriseRemoval = 2
} WHV_VPCI_DEVICE_NOTIFICATION_TYPE;

typedef struct WHV_VPCI_DEVICE_NOTIFICATION
{
    WHV_VPCI_DEVICE_NOTIFICATION_TYPE NotificationType;
    UINT32 Reserved1;
    union
    {
        UINT64 Reserved2;
    };
} WHV_VPCI_DEVICE_NOTIFICATION;

typedef enum WHV_CREATE_VPCI_DEVICE_FLAGS
{
    WHvCreateVpciDeviceFlagNone = 0x00000000,
    WHvCreateVpciDeviceFlagPhysicallyBacked = 0x00000001,
    WHvCreateVpciDeviceFlagUseLogicalInterrupts = 0x00000002
} WHV_CREATE_VPCI_DEVICE_FLAGS;

// Enables bitwise operators on the WHV_CREATE_VPCI_DEVICE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_CREATE_VPCI_DEVICE_FLAGS);

typedef enum WHV_VPCI_DEVICE_PROPERTY_CODE
{
    WHvVpciDevicePropertyCodeUndefined   = 0,
    WHvVpciDevicePropertyCodeHardwareIDs = 1,
    WHvVpciDevicePropertyCodeProbedBARs  = 2
} WHV_VPCI_DEVICE_PROPERTY_CODE;

typedef struct WHV_VPCI_HARDWARE_IDS
{
    UINT16 VendorID;
    UINT16 DeviceID;
    UINT8 RevisionID;
    UINT8 ProgIf;
    UINT8 SubClass;
    UINT8 BaseClass;
    UINT16 SubVendorID;
    UINT16 SubSystemID;
} WHV_VPCI_HARDWARE_IDS;

#define WHV_VPCI_TYPE0_BAR_COUNT 6

typedef struct WHV_VPCI_PROBED_BARS
{
    UINT32 Value[WHV_VPCI_TYPE0_BAR_COUNT];
} WHV_VPCI_PROBED_BARS;

typedef enum WHV_VPCI_MMIO_RANGE_FLAGS
{
    WHvVpciMmioRangeFlagReadAccess = 0x00000001,
    WHvVpciMmioRangeFlagWriteAccess = 0x00000002
} WHV_VPCI_MMIO_RANGE_FLAGS;

// Enables bitwise operators on the WHV_VPCI_MMIO_RANGE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_VPCI_MMIO_RANGE_FLAGS);

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

typedef struct WHV_VPCI_MMIO_MAPPING
{
    WHV_VPCI_DEVICE_REGISTER_SPACE Location;
    WHV_VPCI_MMIO_RANGE_FLAGS Flags;
    UINT64 SizeInBytes;
    UINT64 OffsetInBytes;
    PVOID VirtualAddress;
} WHV_VPCI_MMIO_MAPPING;

typedef struct WHV_VPCI_DEVICE_REGISTER
{
    WHV_VPCI_DEVICE_REGISTER_SPACE Location;
    UINT32 SizeInBytes;
    UINT64 OffsetInBytes;
} WHV_VPCI_DEVICE_REGISTER;

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
```

## Members

### WHV_ALLOCATE_VPCI_RESOURCE_FLAGS

Specifies options for the resource allocated by [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md).

`WHvAllocateVpciResourceFlagNone`

Specifies that no special options apply to the allocated resource.

`WHvAllocateVpciResourceFlagAllowDirectP2P`

Specifies that the allocated resource is permitted to participate in direct, translated peer-to-peer (P2P) transactions.

### WHV_SRIOV_RESOURCE_DESCRIPTOR

Describes a single-root I/O virtualization (SR-IOV) virtual function to allocate. Pass this structure as the `ResourceDescriptor` to [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md) when `ProviderId` is `NULL`.

`PnpInstanceId`

Specifies the Plug and Play (PnP) instance identifier of the physical function that hosts the virtual function. The identifier is at most `WHV_MAX_DEVICE_ID_SIZE_IN_CHARS` characters, matching the PnP manager limit.

`VirtualFunctionId`

Specifies the locally unique identifier (`LUID`) of the virtual function to allocate.

`VirtualFunctionIndex`

Specifies the index of the virtual function on its physical function.

`Reserved`

Reserved. Set to zero.

### WHV_VPCI_DEVICE_NOTIFICATION_TYPE

Identifies the type of an asynchronous notification reported for a VPCI device by [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md).

`WHvVpciDeviceNotificationUndefined`

Indicates that no notification is available. When [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md) returns this type, the caller has drained all pending notifications.

`WHvVpciDeviceNotificationMmioRemapping`

Indicates that the device's MMIO ranges have been remapped. The caller should remap the device's MMIO ranges with [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md).

`WHvVpciDeviceNotificationSurpriseRemoval`

Indicates that the device was removed unexpectedly (surprise removal).

### WHV_VPCI_DEVICE_NOTIFICATION

Receives the type and data of a VPCI device notification returned by [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md).

`NotificationType`

Receives a `WHV_VPCI_DEVICE_NOTIFICATION_TYPE` value that identifies the notification.

`Reserved1`

Reserved.

`Reserved2`

Reserved.

### WHV_CREATE_VPCI_DEVICE_FLAGS

Specifies optional characteristics of a device created by [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md).

`WHvCreateVpciDeviceFlagNone`

Specifies that no optional characteristics apply.

`WHvCreateVpciDeviceFlagPhysicallyBacked`

Specifies that the device is backed by the physical resources identified by the supplied VPCI resource handle.

`WHvCreateVpciDeviceFlagUseLogicalInterrupts`

Specifies that the device exposes logical interrupts to the guest operating system.

### WHV_VPCI_DEVICE_PROPERTY_CODE

Identifies a VPCI device property queried by [`WHvGetVpciDeviceProperty`](WHvGetVpciDeviceProperty.md).

`WHvVpciDevicePropertyCodeUndefined`

Reserved. Not a valid property code.

`WHvVpciDevicePropertyCodeHardwareIDs`

Selects the device's hardware identifiers. The property value is a `WHV_VPCI_HARDWARE_IDS` structure.

`WHvVpciDevicePropertyCodeProbedBARs`

Selects the device's probed base address registers (BARs). The property value is a `WHV_VPCI_PROBED_BARS` structure.

### WHV_VPCI_HARDWARE_IDS

Contains the PCI hardware identifiers of a VPCI device. Returned by [`WHvGetVpciDeviceProperty`](WHvGetVpciDeviceProperty.md) for the `WHvVpciDevicePropertyCodeHardwareIDs` property.

`VendorID`

Receives the PCI vendor identifier.

`DeviceID`

Receives the PCI device identifier.

`RevisionID`

Receives the PCI revision identifier.

`ProgIf`

Receives the programming interface byte of the PCI class code.

`SubClass`

Receives the subclass byte of the PCI class code.

`BaseClass`

Receives the base class byte of the PCI class code.

`SubVendorID`

Receives the PCI subsystem vendor identifier.

`SubSystemID`

Receives the PCI subsystem identifier.

### WHV_VPCI_PROBED_BARS

Contains the probed values of a VPCI device's type 0 base address registers (BARs). Returned by [`WHvGetVpciDeviceProperty`](WHvGetVpciDeviceProperty.md) for the `WHvVpciDevicePropertyCodeProbedBARs` property.

`Value`

Receives the probed value of each of the `WHV_VPCI_TYPE0_BAR_COUNT` (6) type 0 BARs.

### WHV_VPCI_MMIO_RANGE_FLAGS

Specifies the access granted to an MMIO range described by `WHV_VPCI_MMIO_MAPPING`.

`WHvVpciMmioRangeFlagReadAccess`

Indicates that the caller's process has read access to the range.

`WHvVpciMmioRangeFlagWriteAccess`

Indicates that the caller's process has write access to the range.

### WHV_VPCI_DEVICE_REGISTER_SPACE

Identifies a register space of a VPCI device, used by `WHV_VPCI_MMIO_MAPPING` and `WHV_VPCI_DEVICE_REGISTER`.

`WHvVpciConfigSpace`

Identifies the device's PCI configuration space.

`WHvVpciBar0` through `WHvVpciBar5`

Identify the device's base address registers (BARs) 0 through 5.

### WHV_VPCI_MMIO_MAPPING

Describes a single MMIO sub-range mapped into the caller's process by [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md).

`Location`

Identifies the BAR that the sub-range belongs to, as a `WHV_VPCI_DEVICE_REGISTER_SPACE` value.

`Flags`

Specifies the access granted to the caller's process for this sub-range, as a combination of `WHV_VPCI_MMIO_RANGE_FLAGS` values.

`SizeInBytes`

Specifies the length, in bytes, of the sub-range.

`OffsetInBytes`

Specifies the offset, in bytes, of the sub-range from the base address of the BAR identified by `Location`.

`VirtualAddress`

Specifies the virtual address in the caller's process at which the sub-range is mapped.

### WHV_VPCI_DEVICE_REGISTER

Identifies a register to read with [`WHvReadVpciDeviceRegister`](WHvReadVpciDeviceRegister.md) or write with [`WHvWriteVpciDeviceRegister`](WHvWriteVpciDeviceRegister.md).

`Location`

Specifies the register space, as a `WHV_VPCI_DEVICE_REGISTER_SPACE` value (configuration space or a specific BAR).

`SizeInBytes`

Specifies the size, in bytes, of the register access.

`OffsetInBytes`

Specifies the offset, in bytes, of the register within the space identified by `Location`.

### WHV_VPCI_INTERRUPT_TARGET_FLAGS

Specifies options for an interrupt target described by `WHV_VPCI_INTERRUPT_TARGET`.

`WHvVpciInterruptTargetFlagNone`

Specifies that no special options apply to the interrupt target.

`WHvVpciInterruptTargetFlagMulticast`

Specifies that the interrupt is delivered to the set of target processors as a multicast.

### WHV_VPCI_INTERRUPT_TARGET

Specifies the target of a VPCI device interrupt: a vector and a set of virtual processors. This structure has a variable length because `Processors` is a flexible array member.

`Vector`

Specifies the interrupt vector to deliver to the target processors.

`Flags`

Specifies options for the target, as a combination of `WHV_VPCI_INTERRUPT_TARGET_FLAGS` values.

`ProcessorCount`

Specifies the number of valid entries in the `Processors` array.

`Processors`

Specifies the indexes of the virtual processors that the interrupt targets.

## Remarks

A VPCI device assigned to a partition represents a single-root I/O virtualization (SR-IOV) virtual function, a discretely assignable physical function, or a virtual device that is not backed by any physical resources. The typical lifecycle is to allocate a resource with [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md), create the device with [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md), map the device's MMIO ranges with [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md), map its interrupts with [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md), and finally destroy the device with [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md).

The `WHV_VPCI_INTERRUPT_TARGET` and `WHV_VPCI_INTERRUPT_TARGET_FLAGS` types are shared by the device interrupt functions [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md), [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md), and [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md). Because `Processors` is a flexible array member, callers must allocate a buffer large enough to hold `ProcessorCount` processor indexes following the fixed portion of the structure.

## See also

- [`WHvAllocateVpciResource`](WHvAllocateVpciResource.md)
- [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md)
- [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md)
- [`WHvGetVpciDeviceProperty`](WHvGetVpciDeviceProperty.md)
- [`WHvGetVpciDeviceNotification`](WHvGetVpciDeviceNotification.md)
- [`WHvMapVpciDeviceMmioRanges`](WHvMapVpciDeviceMmioRanges.md)
- [`WHvUnmapVpciDeviceMmioRanges`](WHvUnmapVpciDeviceMmioRanges.md)
- [`WHvSetVpciDevicePowerState`](WHvSetVpciDevicePowerState.md)
- [`WHvReadVpciDeviceRegister`](WHvReadVpciDeviceRegister.md)
- [`WHvWriteVpciDeviceRegister`](WHvWriteVpciDeviceRegister.md)
- [`WHvMapVpciDeviceInterrupt`](WHvMapVpciDeviceInterrupt.md)
- [`WHvRetargetVpciDeviceInterrupt`](WHvRetargetVpciDeviceInterrupt.md)
- [`WHvGetVpciDeviceInterruptTarget`](WHvGetVpciDeviceInterruptTarget.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
