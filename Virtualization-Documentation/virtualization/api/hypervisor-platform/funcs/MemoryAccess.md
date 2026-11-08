---
title: Memory Access Exit
description: Learn about context data for a VM exit caused by a memory access.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# Memory Access Exit
Context data for a VM exit caused by a memory access.

## Syntax
```C
//
// Context data for a VM exit caused by a memory access
// (WHvRunVpExitReasonMemoryAccess on x64; WHvRunVpExitReasonUnmappedGpa or
// WHvRunVpExitReasonGpaIntercept on Arm64)
//
typedef enum WHV_MEMORY_ACCESS_TYPE
{
    WHvMemoryAccessRead    = 0,
    WHvMemoryAccessWrite   = 1,
    WHvMemoryAccessExecute = 2
} WHV_MEMORY_ACCESS_TYPE;

#if defined(_AMD64_)

typedef union WHV_MEMORY_ACCESS_INFO
{
    struct {
        UINT32 AccessType  : 2;  // WHV_MEMORY_ACCESS_TYPE
        UINT32 GpaUnmapped : 1;
        UINT32 GvaValid    : 1;
        UINT32 Reserved    : 28;
    };

    UINT32 AsUINT32;
} WHV_MEMORY_ACCESS_INFO;

typedef struct WHV_MEMORY_ACCESS_CONTEXT
{
    // Context of the virtual processor
    UINT8 InstructionByteCount;
    UINT8 Reserved[3];
    UINT8 InstructionBytes[16];

    // Memory access info
    WHV_MEMORY_ACCESS_INFO AccessInfo;
    WHV_GUEST_PHYSICAL_ADDRESS Gpa;
    WHV_GUEST_VIRTUAL_ADDRESS Gva;

} WHV_MEMORY_ACCESS_CONTEXT;

#elif defined(_ARM64_)

typedef union WHV_MEMORY_ACCESS_INFO
{
    UINT8 AsUINT8;
    struct
    {
        UINT8 GvaValid:1;
        UINT8 GvaGpaValid:1;
        UINT8 HypercallOutputPending:1;
        UINT8 Reserved:5;
    };
} WHV_MEMORY_ACCESS_INFO;

typedef struct WHV_MEMORY_ACCESS_CONTEXT
{
    WHV_INTERCEPT_MESSAGE_HEADER Header;
    UINT32 Reserved0;
    UINT8 InstructionByteCount;
    WHV_MEMORY_ACCESS_INFO AccessInfo;
    UINT16 Reserved1;
    UINT8 InstructionBytes[4];
    UINT32 Reserved2;
    UINT64 Gva;
    UINT64 Gpa;
    UINT64 Syndrome;
} WHV_MEMORY_ACCESS_CONTEXT;

#endif // _ARCH_
```

## Remarks

Information about exits caused by the virtual processor accessing a memory location that is not mapped or not accessible is provided by the `WHV_MEMORY_ACCESS_CONTEXT` structure.

A common use case for memory access exits is the emulation of MMIO device operations, where unmapped regions of the partition's GPA space are used for the MMIO space of an emulated device and accesses to this region are forwarded to the device emulation logic in the virtualization stack.

The `WHV_MEMORY_ACCESS_CONTEXT` structure is defined differently for x64 (`_AMD64_`) and Arm64 (`_ARM64_`) partitions:

- On x64, the access surfaces with the `WHvRunVpExitReasonMemoryAccess` exit reason. The context reports the access type and address validity through `WHV_MEMORY_ACCESS_INFO.AccessType`, `GpaUnmapped`, and `GvaValid`, and carries the captured instruction bytes for emulation. The structure is 40 bytes.
- On Arm64, the access surfaces with one of two exit reasons: `WHvRunVpExitReasonUnmappedGpa` when the accessed guest physical address is not mapped, or `WHvRunVpExitReasonGpaIntercept` when the address is mapped but configured to intercept. The context begins with a `WHV_INTERCEPT_MESSAGE_HEADER` (which reports the faulting `Pc` and `Cpsr`), and the `Syndrome` member carries the Arm64 Exception Syndrome Register (ESR) value that describes the faulting access. The structure is 64 bytes.

> [!NOTE]
> On Arm64, the access type is reported by the `WHV_INTERCEPT_MESSAGE_HEADER.InterceptAccessType` field (a `WHV_MEMORY_ACCESS_TYPE` value) rather than by an `AccessType` bitfield in `WHV_MEMORY_ACCESS_INFO`.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
