---
title: WHvDeleteTrigger
description: Learn about the WHvDeleteTrigger function that deletes a trigger object from a partition.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvDeleteTrigger

Deletes a trigger object from a partition.

## Syntax

```C
typedef PVOID WHV_TRIGGER_HANDLE;

HRESULT
WINAPI
WHvDeleteTrigger(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ WHV_TRIGGER_HANDLE TriggerHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`TriggerHandle`

Handle to the trigger object to delete, as returned by [`WHvCreateTrigger`](WHvCreateTrigger.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

The function fails if `TriggerHandle` does not name a valid trigger in the partition.

## Remarks

The `WHvDeleteTrigger` function removes the association between the trigger's event and its configured action. After the trigger is deleted, signaling the event handle returned by [`WHvCreateTrigger`](WHvCreateTrigger.md) has no effect.

Deleting the trigger does not close or invalidate the event handle. The caller still owns that handle and must release it separately with `CloseHandle`.

It is not necessary to call `WHvDeleteTrigger` before tearing down the partition; triggers are deleted automatically when the partition is deleted with [`WHvDeletePartition`](WHvDeletePartition.md).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateTrigger`](WHvCreateTrigger.md)
- [`WHvUpdateTriggerParameters`](WHvUpdateTriggerParameters.md)
- [Trigger Data Types](WHvTriggerDataTypes.md)
- [`WHvDeletePartition`](WHvDeletePartition.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
