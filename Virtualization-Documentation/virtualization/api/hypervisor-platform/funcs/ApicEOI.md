---
title: APIC EOI Exit
description: Learn about context data for an exit caused by an APIC EOI of a level-triggered interrupt.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# APIC EOI Exit
Context data for an exit caused by an APIC EOI of a level-triggered interrupt.

> [!NOTE]
> This exit reason and its context structure apply to x64 partitions only.

## Syntax
```C
//
// Context data for an exit caused by an APIC EOI of a level-triggered
// interrupt (WHvRunVpExitReasonX64ApicEoi)
//
typedef struct WHV_X64_APIC_EOI_CONTEXT
{
    UINT32 InterruptVector;
} WHV_X64_APIC_EOI_CONTEXT;
```

## Remarks

Information about an exit caused by an APIC EOI of a level-triggered interrupt is provided in the `WHV_X64_APIC_EOI_CONTEXT` structure.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
