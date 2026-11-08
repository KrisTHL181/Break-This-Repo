---
title: Memory Data Types
description: Learn about the memory data types used to map, advise, and access the guest physical address space of a partition.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Memory Data Types

The memory data types define the flags, structures, and enumerations used to map, advise, and access the guest physical address space of a partition.

## Syntax
```C
// Flags used by WHvMapGpaRange/WHvMapGpaRange2
typedef enum WHV_MAP_GPA_RANGE_FLAGS
{
    WHvMapGpaRangeFlagNone             = 0x00000000,
    WHvMapGpaRangeFlagRead             = 0x00000001,
    WHvMapGpaRangeFlagWrite            = 0x00000002,
    WHvMapGpaRangeFlagExecute          = 0x00000004,
    WHvMapGpaRangeFlagTrackDirtyPages  = 0x00000008,

} WHV_MAP_GPA_RANGE_FLAGS;

// Enables bitwise operators on the WHV_MAP_GPA_RANGE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_MAP_GPA_RANGE_FLAGS);

//
// Structure describing a memory range entry
//
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

//
// Maximum data size used by WHvReadGpaRange and WHvWriteGpaRange
//
#define WHV_READ_WRITE_GPA_RANGE_MAX_SIZE 16

typedef enum WHV_CACHE_TYPE {
    WHvCacheTypeUncached         = 0,
    WHvCacheTypeWriteCombining   = 1,
    WHvCacheTypeWriteThrough     = 4,

#if defined(_AMD64_)
    WHvCacheTypeWriteProtected   = 5,
#endif

    WHvCacheTypeWriteBack        = 6
} WHV_CACHE_TYPE;

typedef union WHV_INPUT_VTL
{
    UINT8 AsUINT8;
    struct
    {
        UINT8 TargetVtl    : 4;
        UINT8 UseTargetVtl : 1;
        UINT8 Reserved     : 3;
    };
} WHV_INPUT_VTL;

//
// Control flags used by WHvReadGpaRange and WHvWriteGpaRange
//
typedef union WHV_ACCESS_GPA_CONTROLS
{
    UINT64 AsUINT64;
    struct
    {
        //
        // Cache type for access
        //
        WHV_CACHE_TYPE CacheType;

        //
        // VTL whose GPA is to be accessed
        //
        WHV_INPUT_VTL InputVtl;
        UINT8 Reserved;
        UINT16 Reserved1;
    };
} WHV_ACCESS_GPA_CONTROLS;
```

## Members

`WHV_MAP_GPA_RANGE_FLAGS`

Specifies the access permissions and tracking options for a guest physical address mapping created by [`WHvMapGpaRange`](WHvMapGpaRange.md) or [`WHvMapGpaRange2`](WHvMapGpaRange2.md). `WHvMapGpaRangeFlagRead`, `WHvMapGpaRangeFlagWrite`, and `WHvMapGpaRangeFlagExecute` grant the corresponding access to the mapped range, and `WHvMapGpaRangeFlagTrackDirtyPages` enables dirty-page tracking for the range.

`WHV_MEMORY_RANGE_ENTRY`

Describes a single guest physical address range. `GuestAddress` is the starting guest physical address, and `SizeInBytes` is the length of the range in bytes. Arrays of this structure identify the ranges that [`WHvAdviseGpaRange`](WHvAdviseGpaRange.md) operates on.

`WHV_ADVISE_GPA_RANGE_CODE`

Identifies the advice operation passed to [`WHvAdviseGpaRange`](WHvAdviseGpaRange.md). `WHvAdviseGpaRangeCodePopulate` commits backing memory for the ranges, `WHvAdviseGpaRangeCodePin` pins the backing memory so that it remains resident, and `WHvAdviseGpaRangeCodeUnpin` releases a previously established pin.

`WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS`

Specifies options for the populate advice. `Prefetch` requests that the pages be brought in proactively, and `AvoidHardFaults` requests that the operation avoid taking hard page faults. The remaining bits are reserved and must be zero.

`WHV_MEMORY_ACCESS_TYPE`

Identifies the type of access — read, write, or execute — for which a range is populated.

`WHV_ADVISE_GPA_RANGE_POPULATE`

Provides the parameters for the `WHvAdviseGpaRangeCodePopulate` operation. `Flags` is a `WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS` value, and `AccessType` selects the `WHV_MEMORY_ACCESS_TYPE` used for the populate operation.

`WHV_ADVISE_GPA_RANGE`

The advice buffer passed to [`WHvAdviseGpaRange`](WHvAdviseGpaRange.md). The `Populate` member supplies the parameters when the advice code is `WHvAdviseGpaRangeCodePopulate`.

`WHV_READ_WRITE_GPA_RANGE_MAX_SIZE`

Defines the maximum number of bytes (16) that a single call to [`WHvReadGpaRange`](WHvReadGpaRange.md) or [`WHvWriteGpaRange`](WHvWriteGpaRange.md) can transfer.

`WHV_CACHE_TYPE`

Identifies the cache type used for a guest physical access. `WHvCacheTypeWriteProtected` is defined only on x64.

`WHV_INPUT_VTL`

Identifies a target Virtual Trust Level (VTL). `TargetVtl` is the VTL number, and `UseTargetVtl` indicates whether the access uses `TargetVtl`.

`WHV_ACCESS_GPA_CONTROLS`

Specifies the controls for a guest physical access performed by [`WHvReadGpaRange`](WHvReadGpaRange.md) or [`WHvWriteGpaRange`](WHvWriteGpaRange.md). `CacheType` is the `WHV_CACHE_TYPE` to use for the access, and `InputVtl` is the `WHV_INPUT_VTL` whose guest physical address is accessed.

## Remarks

These types support three related families of operations on the guest physical address space of a partition: mapping host memory into the partition with [`WHvMapGpaRange`](WHvMapGpaRange.md) and [`WHvMapGpaRange2`](WHvMapGpaRange2.md), advising the hypervisor about memory usage with [`WHvAdviseGpaRange`](WHvAdviseGpaRange.md), and performing small, virtual-processor-context reads and writes with [`WHvReadGpaRange`](WHvReadGpaRange.md) and [`WHvWriteGpaRange`](WHvWriteGpaRange.md).

`WHV_ACCESS_GPA_CONTROLS` overlays `WHV_CACHE_TYPE` and `WHV_INPUT_VTL` onto a 64-bit value, so the read and write functions can specify both the cache type and the target VTL for an access in a single argument.

## See also

- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvMapGpaRange2`](WHvMapGpaRange2.md)
- [`WHvAdviseGpaRange`](WHvAdviseGpaRange.md)
- [`WHvReadGpaRange`](WHvReadGpaRange.md)
- [`WHvWriteGpaRange`](WHvWriteGpaRange.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
