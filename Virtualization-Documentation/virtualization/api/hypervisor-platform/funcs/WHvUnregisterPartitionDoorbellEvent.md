---
title: WHvUnregisterPartitionDoorbellEvent
description: Learn about the WHvUnregisterPartitionDoorbellEvent function that unregisters a previously registered partition doorbell event.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvUnregisterPartitionDoorbellEvent

Unregisters a doorbell event previously registered with [`WHvRegisterPartitionDoorbellEvent`](WHvRegisterPartitionDoorbellEvent.md).

> [!IMPORTANT]
> `WHvUnregisterPartitionDoorbellEvent` is deprecated. Use [`WHvDeleteNotificationPort`](WHvDeleteNotificationPort.md) to remove a notification port of type `WHvNotificationPortTypeDoorbell` instead.

## Syntax
```C
// Guest physical address
typedef UINT64 WHV_GUEST_PHYSICAL_ADDRESS;

typedef struct WHV_DOORBELL_MATCH_DATA
{
    WHV_GUEST_PHYSICAL_ADDRESS GuestAddress;
    UINT64 Value;
    UINT32 Length;
    UINT32 MatchOnValue:1;
    UINT32 MatchOnLength:1;
    UINT32 Reserved:30;
} WHV_DOORBELL_MATCH_DATA;

HRESULT
WINAPI
WHvUnregisterPartitionDoorbellEvent(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_DOORBELL_MATCH_DATA* MatchData
    );
```

### Parameters

`Partition`

Handle to the partition object.

`MatchData`

Specifies the match data of the doorbell event to unregister. The data must exactly match the data supplied to [`WHvRegisterPartitionDoorbellEvent`](WHvRegisterPartitionDoorbellEvent.md) when the event was registered. See [Doorbell Data Types](WHvDoorbellDataTypes.md).

## Return Value

If the function succeeds, the return value is `S_OK`.

If no doorbell event with the specified match data is registered on the partition, the function returns `HRESULT_FROM_WIN32(ERROR_NOT_FOUND)`.

## Remarks

Calling `WHvUnregisterPartitionDoorbellEvent` is not required before deleting the partition: any remaining doorbell events are automatically unregistered when the partition is deleted.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 2004 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvRegisterPartitionDoorbellEvent`](WHvRegisterPartitionDoorbellEvent.md)
- [Doorbell Data Types](WHvDoorbellDataTypes.md)
- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
