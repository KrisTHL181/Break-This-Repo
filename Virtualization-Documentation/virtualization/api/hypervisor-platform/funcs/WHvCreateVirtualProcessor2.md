---
title: WHvCreateVirtualProcessor2
description: Learn about the WHvCreateVirtualProcessor2 function that creates a new virtual processor in a partition with optional creation-time properties.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvCreateVirtualProcessor2

Creates a new virtual processor in a partition with optional creation-time properties.

## Syntax

```C
typedef enum WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE
{
    WHvVirtualProcessorPropertyCodeNumaNode = 0x00000000,
} WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE;

typedef struct WHV_VIRTUAL_PROCESSOR_PROPERTY
{
    WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE PropertyCode;
    UINT32 Reserved;
    union
    {
        USHORT NumaNode;
        UINT64 Padding;
    };
} WHV_VIRTUAL_PROCESSOR_PROPERTY;

HRESULT
WINAPI
WHvCreateVirtualProcessor2(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_(PropertyCount) const WHV_VIRTUAL_PROCESSOR_PROPERTY* Properties,
    _In_ UINT32 PropertyCount
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the new virtual processor.

`Properties`

Specifies an array of properties to apply to the new virtual processor at creation time. May be `NULL` when `PropertyCount` is zero.

`PropertyCount`

Specifies the number of elements in the `Properties` array. When zero, the virtual processor is created with default properties.

### Properties

Each element of the `Properties` array is a `WHV_VIRTUAL_PROCESSOR_PROPERTY` whose `PropertyCode` member selects the property to apply and whose value member supplies the setting. If the same property code appears more than once, the last value supplied takes effect. The following property codes are defined.

`WHvVirtualProcessorPropertyCodeNumaNode`

Specifies the NUMA node that backs the virtual processor, in the `NumaNode` member. When this property is not supplied, the virtual processor is placed on the ideal NUMA node of the calling thread.

## Return Value

If the function succeeds, the return value is `S_OK`.

If a virtual processor already exists at `VpIndex`, the return value is `WHV_E_VP_ALREADY_EXISTS`. If `VpIndex` is greater than or equal to the partition's processor count, if a property specifies an unknown property code, or if the `NumaNode` value is not a valid NUMA node, the return value is `E_INVALIDARG`.

## Remarks

The `WHvCreateVirtualProcessor2` function creates a new virtual processor in a partition. On x64, the index of the virtual processor is used to set the APIC ID of the processor.
Calling `WHvCreateVirtualProcessor(Partition, VpIndex, 0)` is equivalent to calling `WHvCreateVirtualProcessor2(Partition, VpIndex, NULL, 0)`.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateVirtualProcessor`](WHvCreateVirtualProcessor.md)
- [`WHvDeleteVirtualProcessor`](WHvDeleteVirtualProcessor.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
