---
title: RDTSC(P) Exit
description: Learn about context data for an exit caused by an rdtsc(p) instruction.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# RDTSC(P) Exit
Context data for an exit caused by an rdtsc(p) instruction.

> [!NOTE]
> This exit reason and its context structure apply to x64 partitions only.

## Syntax
```C
//
// Context data for an exit caused by a rdtsc(p) instruction (WHvRunVpExitReasonX64Rdtsc)
//
typedef union WHV_X64_RDTSC_INFO
{
    struct
    {
        UINT64 IsRdtscp:1;
        UINT64 Reserved:63;
    };

    UINT64 AsUINT64;
} WHV_X64_RDTSC_INFO;

typedef struct WHV_X64_RDTSC_CONTEXT
{
    UINT64 TscAux;
    UINT64 VirtualOffset;
    UINT64 Tsc;
    UINT64 ReferenceTime;
    WHV_X64_RDTSC_INFO RdtscInfo;
} WHV_X64_RDTSC_CONTEXT;
```

## Remarks

Information about an `RDTSC` or `RDTSCP` instruction executed by the virtual processor is provided in the `WHV_X64_RDTSC_CONTEXT` structure.

Exits for the `rdtsc` and `rdtscp` instructions are only generated if they are enabled by setting the `WHV_EXTENDED_VM_EXITS.X64RdtscExit` property for the partition.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
