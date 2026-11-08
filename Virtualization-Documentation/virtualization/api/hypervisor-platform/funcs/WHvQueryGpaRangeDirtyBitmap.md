---
title: WHvQueryGpaRangeDirtyBitmap
description: Learn about the WHvQueryGpaRangeDirtyBitmap function that queries the dirty page bitmap for a range of guest physical addresses.
author: jstarks
ms.author: jostarks
ms.date: 06/06/2019
ms.topic: reference
---

# WHvQueryGpaRangeDirtyBitmap

Queries the dirty page bitmap for a range of guest physical addresses.

## Syntax

```C
HRESULT
WINAPI
WHvQueryGpaRangeDirtyBitmap(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_GUEST_PHYSICAL_ADDRESS GuestAddress,
    _In_ UINT64 RangeSizeInBytes,
    _Out_writes_bytes_opt_(BitmapSizeInBytes) UINT64* Bitmap,
    _In_ UINT32 BitmapSizeInBytes
    );
```

### Parameters

`Partition`

Specifies the partition to query.

`GuestAddress`

Specifies the guest physical address of the beginning of the range to query. This address must be page-aligned.

`RangeSizeInBytes`

Specifies the size of the range to query, in bytes.

`Bitmap`

If non-NULL, specifies a pointer to the bitmap to write to. The bitmap must be 8-byte aligned.

`BitmapSizeInBytes`

Specifies the size of the bitmap, in bytes. The bitmap must be large enough to include one bit per each page, rounded up to an 8-byte value.

If zero, then `Bitmap` can be NULL. This indicates that the range's dirty state should be cleared without querying the current state.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the specified address range has not been mapped or has not been registered for dirty range tracking, the return value is `WHV_E_GPA_RANGE_NOT_FOUND`.

## Remarks

To use this function for a given address range, that address range must be mapped with [`WHvMapGpaRange`](WHvMapGpaRange.md), specifying the `WHvMapGpaRangeFlagTrackDirtyPages` flag.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1809 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvUnmapGpaRange`](WHvUnmapGpaRange.md)
- [`WHvTranslateGva`](WHvTranslateGva.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
