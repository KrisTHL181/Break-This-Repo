---
title: WHvSetPartitionProperty
description: Learn about the WHvSetPartitionProperty function that sets the value of a partition property.
author: Juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# WHvSetPartitionProperty

Sets the value of a partition property.

## Syntax
```C
HRESULT
WINAPI
WHvSetPartitionProperty(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_PARTITION_PROPERTY_CODE PropertyCode,
    _In_reads_bytes_(PropertyBufferSizeInBytes) const VOID* PropertyBuffer,
    _In_ UINT32 PropertyBufferSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object. 

`PropertyCode`

Specifies the property that is being set.

`PropertyBuffer`

Specifies the input buffer that provides the property value. 

`PropertyBufferSizeInBytes`

Specifies the size of the input buffer, in bytes. 

## Return Value

If the function succeeds, the return value is `S_OK`. 

The function returns `WHV_E_UNKNOWN_PROPERTY` for attempts to configure a property that is not available on the current system. 

The function returns `E_INVALIDARG` if the property cannot be modified in the current state of the partition, particularly for attempts to set a property prior to the [`WHvSetupPartition`](WHvSetupPartition.md) function. Starting in Insider Preview Builds (19H2), the following properties can be modified after the [`WHvSetupPartition`](WHvSetupPartition.md) function:
`WHvPartitionPropertyCodeExtendedVmExits`
`WHvPartitionPropertyCodeExceptionExitBitmap`
`WHvPartitionPropertyCodeX64MsrExitBitmap`
`WHvPartitionPropertyCodeCpuidExitList`

## Remarks

The `WHvSetPartitionProperty` function sets the value of a partition property.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvGetPartitionProperty`](WHvGetPartitionProperty.md)
- [Partition Property Data Types](WHvPartitionPropertyDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
