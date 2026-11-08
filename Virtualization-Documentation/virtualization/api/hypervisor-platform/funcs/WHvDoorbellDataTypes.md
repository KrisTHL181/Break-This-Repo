---
title: Doorbell Data Types
description: Learn about the doorbell data types used to describe the guest writes that signal a partition doorbell event.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Doorbell Data Types

The doorbell data types describe the guest physical write that signals a registered partition doorbell event.

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
```

## Members

`GuestAddress`

Specifies the guest physical address that the guest writes to in order to signal the doorbell event.

`Value`

Specifies the value that the guest write must store to signal the event when `MatchOnValue` is set. When `MatchOnValue` is clear, this field must be zero.

`Length`

Specifies the size, in bytes, of the guest write when `MatchOnLength` is set. The only valid lengths are 1, 2, 4, and 8. When `MatchOnLength` is clear, this field must be zero.

`MatchOnValue`

Specifies whether the value of the write is constrained. When set, only a write of `Value` signals the event; when clear, a write of any value signals the event. `MatchOnValue` can be set only when `MatchOnLength` is also set.

`MatchOnLength`

Specifies whether the length of the write is constrained. When set, only a write of `Length` bytes signals the event; when clear, a write of any length signals the event.

`Reserved`

Reserved for future use. This field must be zero.

## Remarks

`WHV_DOORBELL_MATCH_DATA` defines the criteria a guest physical write must satisfy to signal a doorbell event registered with [`WHvRegisterPartitionDoorbellEvent`](WHvRegisterPartitionDoorbellEvent.md). The `MatchOnValue` and `MatchOnLength` flags select one of three match modes:

- **Any write.** `MatchOnValue` and `MatchOnLength` are both clear (and `Value` and `Length` are zero). Any write to `GuestAddress` signals the event.
- **Size only.** `MatchOnLength` is set with a `Length` of 1, 2, 4, or 8, and `MatchOnValue` is clear. Only a write of that size signals the event.
- **Value and size.** Both `MatchOnLength` and `MatchOnValue` are set. Only a write of `Length` bytes storing `Value` signals the event.

The same match data is used to unregister the event with [`WHvUnregisterPartitionDoorbellEvent`](WHvUnregisterPartitionDoorbellEvent.md), so the data supplied to unregister must exactly match the data supplied to register.

A doorbell is the lightweight, write-to-memory variant of an event notification: when the guest stores to the configured `GuestAddress`, the hypervisor traps the write and signals the registered event without exiting the virtual processor. The hypervisor recognizes only certain store instructions for the triggering write. Writes that use an unrecognized instruction, or writes that do not satisfy the configured match mode, cause the virtual processor to exit with `WHvRunVpExitReasonMemoryAccess` instead.

## See also

- [`WHvRegisterPartitionDoorbellEvent`](WHvRegisterPartitionDoorbellEvent.md)
- [`WHvUnregisterPartitionDoorbellEvent`](WHvUnregisterPartitionDoorbellEvent.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
