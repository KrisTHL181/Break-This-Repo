---
title: WHvRegisterPartitionDoorbellEvent
description: Learn about the WHvRegisterPartitionDoorbellEvent function that registers an event to be signaled when a guest writes to a specified guest physical address.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvRegisterPartitionDoorbellEvent

Registers an event to be signaled when a guest writes to a specified guest physical address.

> [!IMPORTANT]
> `WHvRegisterPartitionDoorbellEvent` is deprecated. Use [`WHvCreateNotificationPort`](WHvCreateNotificationPort.md) with a notification port of type `WHvNotificationPortTypeDoorbell` instead.

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
WHvRegisterPartitionDoorbellEvent(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_DOORBELL_MATCH_DATA* MatchData,
    _In_ HANDLE EventHandle
    );
```

### Parameters

`Partition`

Handle to the partition object.

`MatchData`

Specifies the guest physical address and the optional value and length constraints that a guest write must satisfy to signal the event. See [Doorbell Data Types](WHvDoorbellDataTypes.md) for the field semantics and the supported match modes.

`EventHandle`

Specifies a handle to the event to signal, as returned by `CreateEvent`. When a matching write occurs, the hypervisor sets the event as though `SetEvent` were called.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if the match data is malformed: when any reserved bit is set; when `MatchOnLength` is set but `Length` is not one of 1, 2, 4, or 8; when `MatchOnLength` is clear but `Length` is nonzero or `MatchOnValue` is set; or when `MatchOnValue` is clear but `Value` is nonzero.

The function returns `ERROR_HV_INVALID_PARAMETER` if the requested registration conflicts with a doorbell already registered at the same `GuestAddress`. Multiple doorbells can be registered at one guest physical address when each matches on a distinct `Value` of the same `Length`. A conflict occurs when an identical doorbell is already registered, when the new registration specifies a different `Length` than an existing doorbell at that address, or when either the new registration or an existing doorbell at that address matches on any value (that is, `MatchOnValue` is clear).

## Remarks

When a virtual processor performs a matching write, the event is signaled and the virtual processor does *not* exit with `WHvRunVpExitReasonMemoryAccess`. The hypervisor only recognizes certain store instructions for the triggering write; if the guest performs the write using an unrecognized instruction, the event is not signaled and the virtual processor exits with `WHvRunVpExitReasonMemoryAccess`. The set of supported instructions may change between Windows releases, so callers should ensure the virtual-processor exit path behaves correctly for the specified guest address.

If the matching guest address falls within a page previously mapped with [`WHvMapGpaRange`](WHvMapGpaRange.md), registering the doorbell effectively unmaps that page for the lifetime of the registration. Non-matching writes to the page cause the virtual processor to exit with `WHvRunVpExitReasonMemoryAccess` as though the page were unmapped. The page reverts to its mapped state when [`WHvUnregisterPartitionDoorbellEvent`](WHvUnregisterPartitionDoorbellEvent.md) is called.

The registered write must not straddle a page boundary; there are no other alignment requirements.

The event is removed by a call to [`WHvUnregisterPartitionDoorbellEvent`](WHvUnregisterPartitionDoorbellEvent.md) with identical match data, and any remaining doorbell events are automatically unregistered when the partition is deleted.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 2004 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvUnregisterPartitionDoorbellEvent`](WHvUnregisterPartitionDoorbellEvent.md)
- [Doorbell Data Types](WHvDoorbellDataTypes.md)
- [`WHvMapGpaRange`](WHvMapGpaRange.md)
- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
