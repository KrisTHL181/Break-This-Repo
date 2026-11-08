---
title: WHvAdviseGpaRange
description: Learn about the WHvAdviseGpaRange function that provides hints to the hypervisor about the memory backing one or more guest physical address ranges.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvAdviseGpaRange

Provides hints to the hypervisor about the memory backing one or more guest physical address ranges.

## Syntax
```C
// Structure describing a memory range entry
typedef struct WHV_MEMORY_RANGE_ENTRY {
    UINT64 GuestAddress;
    UINT64 SizeInBytes;
} WHV_MEMORY_RANGE_ENTRY;

// WHvAdviseGpaRange types
typedef enum WHV_ADVISE_GPA_RANGE_CODE
{
    WHvAdviseGpaRangeCodePopulate         = 0x00000000,
    WHvAdviseGpaRangeCodePin              = 0x00000001,
    WHvAdviseGpaRangeCodeUnpin            = 0x00000002,
} WHV_ADVISE_GPA_RANGE_CODE;

typedef union WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS
{
    UINT32 AsUINT32;
    struct
    {
        UINT32 Prefetch:1;
        UINT32 AvoidHardFaults:1;
        UINT32 Reserved:30;
    };
} WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS;

typedef enum WHV_MEMORY_ACCESS_TYPE
{
    WHvMemoryAccessRead    = 0,
    WHvMemoryAccessWrite   = 1,
    WHvMemoryAccessExecute = 2
} WHV_MEMORY_ACCESS_TYPE;

//
// Parameters used by WHvAdviseGpaRangeCodePopulate
//
typedef struct WHV_ADVISE_GPA_RANGE_POPULATE
{
    WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS Flags;
    WHV_MEMORY_ACCESS_TYPE AccessType;
} WHV_ADVISE_GPA_RANGE_POPULATE;

//
// WHvAdviseGpaRange buffer
//
typedef union WHV_ADVISE_GPA_RANGE
{
    WHV_ADVISE_GPA_RANGE_POPULATE Populate;
} WHV_ADVISE_GPA_RANGE;

HRESULT
WINAPI
WHvAdviseGpaRange(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_reads_(GpaRangesCount) const WHV_MEMORY_RANGE_ENTRY* GpaRanges,
    _In_ UINT32 GpaRangesCount,
    _In_ WHV_ADVISE_GPA_RANGE_CODE Advice,
    _In_reads_bytes_(AdviceBufferSizeInBytes) const VOID* AdviceBuffer,
    _In_ UINT32 AdviceBufferSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`GpaRanges`

Specifies a pointer to an array of `WHV_MEMORY_RANGE_ENTRY` structures, each of which describes a guest physical address range to which the advice applies. For the `WHvAdviseGpaRangeCodePin` and `WHvAdviseGpaRangeCodeUnpin` advice codes, each range must be page-aligned.

`GpaRangesCount`

Specifies the number of entries in the array pointed to by `GpaRanges`. This value must be greater than zero.

`Advice`

Specifies the advice to apply to the ranges, as a `WHV_ADVISE_GPA_RANGE_CODE` value.

`AdviceBuffer`

Specifies a pointer to a buffer that contains the parameters for the advice. The layout of the buffer is determined by `Advice`. For `WHvAdviseGpaRangeCodePopulate`, the buffer is a `WHV_ADVISE_GPA_RANGE_POPULATE` structure.

`AdviceBufferSizeInBytes`

Specifies the size, in bytes, of the buffer pointed to by `AdviceBuffer`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `Advice` is not a defined `WHV_ADVISE_GPA_RANGE_CODE` value, or if the parameters in the advice buffer are not valid for the specified `Advice`.

## Remarks

The `WHvAdviseGpaRange` function lets the virtualization stack inform the hypervisor about the intended use of the memory backing a set of guest physical address ranges, so that the hypervisor can optimize how that memory is committed and accessed.

The `Advice` parameter selects the operation:

- `WHvAdviseGpaRangeCodePopulate` commits the backing memory for the specified ranges ahead of access by a virtual processor, faulting the pages in according to the `AccessType` in the `WHV_ADVISE_GPA_RANGE_POPULATE` structure. The `Prefetch` flag requests that the pages be brought in proactively, and the `AvoidHardFaults` flag requests that the operation avoid taking hard page faults. Ranges supplied to the populate operation do not need to be page-aligned.
- `WHvAdviseGpaRangeCodePin` pins the backing memory for the specified ranges so that it remains resident.
- `WHvAdviseGpaRangeCodeUnpin` releases a pin previously established with `WHvAdviseGpaRangeCodePin`.

The pin and unpin operations require every range in `GpaRanges` to be page-aligned.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvMapGpaRange2`](WHvMapGpaRange2.md)
- [`WHvReadGpaRange`](WHvReadGpaRange.md)
- [`WHvWriteGpaRange`](WHvWriteGpaRange.md)
- [Memory Data Types](WHvMemoryDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
