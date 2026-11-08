---
title: WHvMapGpaRange2
description: Learn about the WHvMapGpaRange2 function that maps a range of the guest physical address space of a partition to memory in a specified host process.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvMapGpaRange2

Maps a range of the guest physical address space of a partition to memory in a specified host process.

## Syntax
```C
// Guest physical address
typedef UINT64 WHV_GUEST_PHYSICAL_ADDRESS;

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

HRESULT
WINAPI
WHvMapGpaRange2(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ HANDLE Process,
    _In_ VOID* SourceAddress,
    _In_ WHV_GUEST_PHYSICAL_ADDRESS GuestAddress,
    _In_ UINT64 SizeInBytes,
    _In_ WHV_MAP_GPA_RANGE_FLAGS Flags
    );
```

### Parameters

`Partition`

Handle to the partition object.

`Process`

Handle to the host process whose address space contains the memory region identified by `SourceAddress`. The handle must grant `PROCESS_VM_READ`, `PROCESS_VM_WRITE`, and `PROCESS_VM_OPERATION` access to that process.

`SourceAddress`

Specifies the page-aligned address, within the address space of the process identified by `Process`, of the memory region that is the source of the mapping.

`GuestAddress`

Specifies the destination address in the VM's physical address space.

`SizeInBytes`

Specifies the number of bytes that are to be mapped.

`Flags`

Specifies the access flags for the mapping.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the source region or the guest physical address range is not page-aligned, has a size of zero, or describes a range that overflows, the function returns `E_INVALIDARG`. The function also returns `E_INVALIDARG` if `Flags` specifies a value or combination of access permissions that cannot be applied to the page.

## Remarks

The `WHvMapGpaRange2` function is the extended form of [`WHvMapGpaRange`](WHvMapGpaRange.md). Whereas `WHvMapGpaRange` always uses the address space of the calling process as the source of the mapping, `WHvMapGpaRange2` accepts an explicit `Process` handle and uses the address space of that process. This lets a virtualization stack back a partition's guest physical memory with memory owned by a different host process.

Creating a mapping for a range in the GPA space of a partition sets a region in the source process as the backing memory for that range. The operation replaces any previous mappings for the specified GPA pages.

To remove a mapping created with `WHvMapGpaRange2`, call [`WHvUnmapGpaRange`](WHvUnmapGpaRange.md).

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
- [`WHvUnmapGpaRange`](WHvUnmapGpaRange.md)
- [`WHvTranslateGva`](WHvTranslateGva.md)
- [`WHvQueryGpaRangeDirtyBitmap`](WHvQueryGpaRangeDirtyBitmap.md)
- [Memory Data Types](WHvMemoryDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
