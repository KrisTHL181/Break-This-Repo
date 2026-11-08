---
title: WHvReadGpaRange
description: Learn about the WHvReadGpaRange function that reads up to WHV_READ_WRITE_GPA_RANGE_MAX_SIZE bytes from a partition's guest physical address space.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvReadGpaRange

Reads up to `WHV_READ_WRITE_GPA_RANGE_MAX_SIZE` bytes from a partition's guest physical address space.

## Syntax
```C
// Guest physical address
typedef UINT64 WHV_GUEST_PHYSICAL_ADDRESS;

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

HRESULT
WINAPI
WHvReadGpaRange(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ WHV_GUEST_PHYSICAL_ADDRESS GuestAddress,
    _In_ WHV_ACCESS_GPA_CONTROLS Controls,
    _Out_writes_bytes_(DataSizeInBytes) PVOID Data,
    _In_ UINT32 DataSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor in whose context the access is performed.

`GuestAddress`

Specifies the guest physical address at which the read begins.

`Controls`

Specifies the cache type and target VTL for the access, as a `WHV_ACCESS_GPA_CONTROLS` value.

`Data`

Receives the bytes read from the guest physical address range.

`DataSizeInBytes`

Specifies the number of bytes to read. This value must be greater than zero and no larger than `WHV_READ_WRITE_GPA_RANGE_MAX_SIZE`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function can return the following failure codes:

- `E_INVALIDARG` — `DataSizeInBytes` is greater than `WHV_READ_WRITE_GPA_RANGE_MAX_SIZE`, the `CacheType` in `Controls` is not a valid cache type, the `InputVtl` in `Controls` does not refer to the current VTL, or a reserved field in `Controls` is set.
- `E_ACCESSDENIED` — the guest physical address range is not mapped or the access is otherwise not permitted.

## Remarks

The `WHvReadGpaRange` function reads up to `WHV_READ_WRITE_GPA_RANGE_MAX_SIZE` (16) bytes of guest physical memory in the context of a virtual processor. This limited access size makes the function suitable for emulating instruction operands rather than bulk memory transfer. For larger transfers, back the guest physical memory with a host mapping created by [`WHvMapGpaRange`](WHvMapGpaRange.md) or [`WHvMapGpaRange2`](WHvMapGpaRange2.md) and access it directly.

Because the read is performed in the context of the virtual processor identified by `VpIndex`, it observes the memory access semantics that apply to that processor, including the cache type and VTL specified in `Controls`. If the targeted page is not yet resident, the function makes the page resident and retries the access.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvWriteGpaRange`](WHvWriteGpaRange.md)
- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvMapGpaRange2`](WHvMapGpaRange2.md)
- [`WHvTranslateGva`](WHvTranslateGva.md)
- [Memory Data Types](WHvMemoryDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
