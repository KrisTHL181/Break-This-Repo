---
title: WHvSetVirtualProcessorXsaveState
description: Learn about the WHvSetVirtualProcessorXsaveState function that sets the XSAVE state of a virtual processor.
author: jstarks
ms.author: jostarks
ms.date: 06/06/2019
ms.topic: reference
---

# WHvSetVirtualProcessorXsaveState

Sets the XSAVE state of a virtual processor.

> [!IMPORTANT]
> `WHvSetVirtualProcessorXsaveState` is deprecated. Use [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md) with the `WHvVirtualProcessorStateTypeXsaveState` state type instead.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
HRESULT
WINAPI
WHvSetVirtualProcessorXsaveState(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_reads_bytes_(BufferSizeInBytes) const VOID* Buffer,
    _In_ UINT32 BufferSizeInBytes
    );
```

### Parameters

`Partition`

Specifies the partition of the virtual processor.

`VpIndex`

Specifies the index of the virtual processor whose XSAVE state should be set.

`Buffer`

Specifies the buffer containing the XSAVE state.

`BufferSizeInBytes`

Specifies the size of the buffer, in bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvSetVirtualProcessorXsaveState` function sets the XSAVE state of the specified virtual processor.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1809 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvGetVirtualProcessorXsaveState`](WHvGetVirtualProcessorXsaveState.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
