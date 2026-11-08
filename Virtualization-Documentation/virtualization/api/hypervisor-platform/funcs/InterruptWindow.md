---
title: Interrupt Window Exit
description: Learn about context data for an exit that occurs when the interruptibility state of a virtual processor allows delivery of an interrupt.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# Interrupt Window Exit
Context data for an exit that occurs when the interruptibility state of a virtual processor allows delivery of an interrupt.

> [!NOTE]
> This exit reason and its context structure apply to x64 partitions only.

## Syntax
```C
//
// Context data for an exit that occurs when the interruptibility state of the virtual processor allows delivery of an interrupt
// (WHvRunVpExitReasonX64InterruptWindow)
//
typedef enum WHV_X64_PENDING_INTERRUPTION_TYPE
{
    WHvX64PendingInterrupt           = 0,
    WHvX64PendingNmi                 = 2,
    WHvX64PendingException           = 3
} WHV_X64_PENDING_INTERRUPTION_TYPE, *PWHV_X64_PENDING_INTERRUPTION_TYPE;

typedef struct WHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT
{
    WHV_X64_PENDING_INTERRUPTION_TYPE DeliverableType;
} WHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT, *PWHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT;
```

## Remarks

Information about an exit that occurs when the interruptibility state of the virtual processor would allow delivery of a given interrupt is provided in the `WHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT` structure.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
