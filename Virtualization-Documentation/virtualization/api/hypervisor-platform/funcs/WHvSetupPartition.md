---
title: WHvSetupPartition
description: Learn about the WHvSetupPartition function that sets up the partition, which causes the actual partition to be created in the hypervisor.
author: Juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# WHvSetupPartition

Sets up the partition, which causes the actual partition to be created in the hypervisor.

## Syntax

```C
HRESULT
WINAPI
WHvSetupPartition(
    _In_ WHV_PARTITION_HANDLE Partition
    );
```

### Parameters

`Partition`

Handle to the partition object.


## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `WHV_E_INVALID_PARTITION_CONFIG` when a partition property that is required before setup has not been configured appropriately.

## Remarks

The `WHvSetupPartition` function sets up the partition, which causes the actual partition to be created in the hypervisor.

Before setting up the partition with `WHvSetupPartition`, the partition object should be created with [`WHvCreatePartition`](WHvCreatePartition.md) and the initial properties of the partition object configured with [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md). After setting up the partition, the partition object can be passed to the other Windows Hypervisor Platform APIs. Starting in Insider Preview Builds (19H2), the following properties can be modified after the [`WHvSetupPartition`](WHvSetupPartition.md) function:
`WHvPartitionPropertyCodeExtendedVmExits`
`WHvPartitionPropertyCodeExceptionExitBitmap`
`WHvPartitionPropertyCodeX64MsrExitBitmap`
`WHvPartitionPropertyCodeCpuidExitList`

> [!NOTE]
> On Arm64, the partition's interrupt controller must be configured before `WHvSetupPartition` is called. See [Arm64 interrupt controller configuration](#arm64-interrupt-controller-configuration).

## Arm64 interrupt controller configuration

On Arm64, the partition's interrupt controller must be configured before `WHvSetupPartition` is called.

Before calling `WHvSetupPartition`, set the `WHvPartitionPropertyCodeArm64IcParameters` property with [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md), using a `WHV_ARM64_IC_PARAMETERS` value whose `EmulationMode` field is `WHvArm64IcEmulationModeGicV3`. If the interrupt controller is left unconfigured, `WHvSetupPartition` fails and returns `WHV_E_INVALID_PARTITION_CONFIG`. For the GICv3 parameters that must be supplied, see [`WHV_ARM64_IC_PARAMETERS`](WHvPartitionPropertyDataTypes.md).

On Arm64, some registers must also be set on each virtual processor before it can be run; in particular, the GIC redistributor base address (`WHvArm64RegisterGicrBaseGpa`). See [`WHvSetVirtualProcessorRegisters`](WHvSetVirtualProcessorRegisters.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreatePartition`](WHvCreatePartition.md)
- [`WHvDeletePartition`](WHvDeletePartition.md)
- [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
