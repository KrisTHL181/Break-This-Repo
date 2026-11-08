---
title: WHvCancelRunVirtualProcessor
description: Learn about the WHvCancelRunVirtualProcessor function that cancels the call to run a virtual processor.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# WHvCancelRunVirtualProcessor

Cancels the call to run a virtual processor.

## Syntax

```C
HRESULT
WINAPI
WHvCancelRunVirtualProcessor(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ UINT32 Flags
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor for which the execution should be stopped.

`Flags`

Unused, must be zero.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

Canceling the execution of a virtual processor allows an application to abort the call to run the virtual processor ([`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)) by another thread, and to return the control to that thread. The virtualization stack can use this function to return the control of a virtual processor back to the virtualization stack in case it needs to change the state of a VM or to inject an event into the processor.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
