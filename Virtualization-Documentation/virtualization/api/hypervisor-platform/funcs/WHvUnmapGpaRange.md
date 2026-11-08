---
title: WHvUnmapGpaRange
description: Learn about the WHvUnmapGpaRange function that unmaps a previously mapped range of the guest physical address space of a partition.
author: juarezhm
ms.author: hajuarez
ms.date: 06/22/2018
ms.topic: reference
---

# WHvUnmapGpaRange

Unmaps a previously mapped range of the guest physical address space of a partition.

## Syntax
```C
HRESULT
WINAPI
WHvUnmapGpaRange(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_GUEST_PHYSICAL_ADDRESS GuestAddress,
    _In_ UINT64 SizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`GuestAddress`

Specifies the start address of the region in the VM's physical address space that is unmapped.

`SizeInBytes`

Specifies the number of bytes that are to be unmapped.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

Unmapping a previously mapped GPA range makes the memory range unavailable to the partition. Any further access by a virtual processor to the range will result in a memory access exit.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvTranslateGva`](WHvTranslateGva.md)
- [`WHvQueryGpaRangeDirtyBitmap`](WHvQueryGpaRangeDirtyBitmap.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
