---
title: WHvGetPartitionProperty
description: Learn about the WHvGetPartitionProperty function that queries the value of a partition property.
author: Juarezhm
ms.author: hajuarez
ms.date: 07/18/2018
ms.topic: reference
---

# WHvGetPartitionProperty

Queries the value of a partition property.

## Syntax
```C
HRESULT
WINAPI
WHvGetPartitionProperty(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_PARTITION_PROPERTY_CODE PropertyCode,
    _Out_writes_bytes_to_(PropertyBufferSizeInBytes, *WrittenSizeInBytes) VOID* PropertyBuffer,
    _In_ UINT32 PropertyBufferSizeInBytes,
    _Out_opt_ UINT32 *WrittenSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`PropertyCode`

Specifies the property that is queried. `WHvPartitionPropertyCodeCpuidExitList` and `WHvPartitionPropertyCodeCpuidResultList` are not supported.

`PropertyBuffer`

Specifies the output buffer that receives the value of the requested property. 

`PropertyBufferSizeInBytes`

Specifies the size of the output buffer, in bytes. For the currently available set of properties, the buffer should be large enough to hold a 64-bit value.

`WrittenSizeInBytes`

Receives the written size in bytes of the `PropertyBuffer`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `WHV_E_UNKNOWN_PROPERTY` if an unknown `PropertyCode` is requested or `HRESULT_FROM_WIN32(ERROR_NOT_SUPPORTED)` when called with an unsupported property code.

## Remarks

The `WHvGetPartitionProperty` function queries the value of a partition property.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md)
- [Partition Property Data Types](WHvPartitionPropertyDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
