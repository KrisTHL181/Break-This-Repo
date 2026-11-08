---
title: Exit Context Data Types
description: Learn about the data types that describe the context of a virtual processor exit.
author: Juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# Exit Context Data Types

The exit context data types describe the context of a virtual processor exit. They define the exit reasons and the context structures that record why a virtual processor exited and the state of the processor at the time of the exit.

## Syntax

```C
//
// Reason for a VM exit
//

#if defined(_AMD64_)

typedef enum WHV_RUN_VP_EXIT_REASON
{
    WHvRunVpExitReasonNone                   = 0x00000000,

    // Standard exits caused by operations of the virtual processor
    WHvRunVpExitReasonMemoryAccess           = 0x00000001,
    WHvRunVpExitReasonX64IoPortAccess        = 0x00000002,
    WHvRunVpExitReasonUnrecoverableException = 0x00000004,
    WHvRunVpExitReasonInvalidVpRegisterValue = 0x00000005,
    WHvRunVpExitReasonUnsupportedFeature     = 0x00000006,
    WHvRunVpExitReasonX64InterruptWindow     = 0x00000007,
    WHvRunVpExitReasonX64Halt                = 0x00000008,
    WHvRunVpExitReasonX64ApicEoi             = 0x00000009,
    WHvRunVpExitReasonSynicSintDeliverable   = 0x0000000A,

    // Additional exits that can be configured through partition properties
    WHvRunVpExitReasonX64MsrAccess           = 0x00001000,
    WHvRunVpExitReasonX64Cpuid               = 0x00001001,
    WHvRunVpExitReasonException              = 0x00001002,
    WHvRunVpExitReasonX64Rdtsc               = 0x00001003,
    WHvRunVpExitReasonX64ApicSmiTrap         = 0x00001004,
    WHvRunVpExitReasonHypercall              = 0x00001005,
    WHvRunVpExitReasonX64ApicInitSipiTrap    = 0x00001006,
    WHvRunVpExitReasonX64ApicWriteTrap       = 0x00001007,

    // Exits caused by the host
    WHvRunVpExitReasonCanceled               = 0x00002001,
} WHV_RUN_VP_EXIT_REASON;

#elif defined(_ARM64_)

typedef enum WHV_RUN_VP_EXIT_REASON
{
    WHvRunVpExitReasonNone                   = 0x00000000,

    // Standard exits caused by operations of the virtual processor
    WHvRunVpExitReasonUnmappedGpa            = 0x80000000,
    WHvRunVpExitReasonGpaIntercept           = 0x80000001,
    WHvRunVpExitReasonUnrecoverableException = 0x80000021,
    WHvRunVpExitReasonInvalidVpRegisterValue = 0x80000020,
    WHvRunVpExitReasonUnsupportedFeature     = 0x80000022,
    WHvRunVpExitReasonSynicSintDeliverable   = 0x80000062,
    WHvMessageTypeRegisterIntercept          = 0x80010006,
    WHvRunVpExitReasonArm64Reset             = 0x8001000c,

    // Additional exits that can be configured through partition properties
    WHvRunVpExitReasonHypercall              = 0x80000050,

    WHvRunVpExitReasonCanceled              = 0xFFFFFFFF
} WHV_RUN_VP_EXIT_REASON;

#endif // _ARCH_

#if defined(_AMD64_)

//
// Execution state of the virtual processor
//
typedef union WHV_X64_VP_EXECUTION_STATE
{
    struct
    {
        UINT16 Cpl : 2;
        UINT16 Cr0Pe : 1;
        UINT16 Cr0Am : 1;
        UINT16 EferLma : 1;
        UINT16 DebugActive : 1;
        UINT16 InterruptionPending : 1;
        UINT16 Reserved0 : 5;
        UINT16 InterruptShadow : 1;
        UINT16 Reserved1 : 3;
    };

    UINT16 AsUINT16;
} WHV_X64_VP_EXECUTION_STATE;

//
// Execution context of a virtual processor at the time of an exit
//
typedef struct WHV_X64_VP_EXIT_CONTEXT
{
    WHV_X64_VP_EXECUTION_STATE ExecutionState;
    UINT8 InstructionLength : 4;
    UINT8 Cr8 : 4;
    UINT8 Reserved;
    UINT32 Reserved2;
    WHV_X64_SEGMENT_REGISTER Cs;
    UINT64 Rip;
    UINT64 Rflags;
} WHV_X64_VP_EXIT_CONTEXT, WHV_VP_EXIT_CONTEXT;

#elif defined(_ARM64_)

//
// Define virtual processor execution state bitfield.
//

typedef union WHV_VP_EXECUTION_STATE
{
    UINT16 AsUINT16;
    struct
    {
        UINT16 Cpl:2;
        UINT16 DebugActive:1;
        UINT16 InterruptionPending:1;
        UINT16 Vtl:4;
        UINT16 VirtualizationFaultActive:1;
        UINT16 Reserved:7;
    };
} WHV_VP_EXECUTION_STATE;

//
// Define intercept message header structure.
//

typedef struct WHV_INTERCEPT_MESSAGE_HEADER
{
    UINT32 VpIndex;
    UINT8 InstructionLength;
    UINT8 InterceptAccessType; // WHV_MEMORY_ACCESS_TYPE
    WHV_VP_EXECUTION_STATE ExecutionState;
    UINT64 Pc;
    UINT64 Cpsr;
} WHV_INTERCEPT_MESSAGE_HEADER;

#endif // _ARCH_

// WHvRunVirtualProcessor output buffer
typedef struct WHV_RUN_VP_EXIT_CONTEXT
{
    WHV_RUN_VP_EXIT_REASON ExitReason;
    UINT32 Reserved;

#if defined(_AMD64_)
    WHV_VP_EXIT_CONTEXT VpContext;
#elif defined(_ARM64_)
    UINT64 Reserved1;
#endif

    union
    {
        WHV_MEMORY_ACCESS_CONTEXT MemoryAccess;
        WHV_RUN_VP_CANCELED_CONTEXT CancelReason;
        WHV_HYPERCALL_CONTEXT Hypercall;
        WHV_SYNIC_SINT_DELIVERABLE_CONTEXT SynicSintDeliverable;
#if defined(_AMD64_)
        WHV_X64_IO_PORT_ACCESS_CONTEXT IoPortAccess;
        WHV_X64_MSR_ACCESS_CONTEXT MsrAccess;
        WHV_X64_CPUID_ACCESS_CONTEXT CpuidAccess;
        WHV_VP_EXCEPTION_CONTEXT VpException;
        WHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT InterruptWindow;
        WHV_X64_UNSUPPORTED_FEATURE_CONTEXT UnsupportedFeature;
        WHV_X64_APIC_EOI_CONTEXT ApicEoi;
        WHV_X64_RDTSC_CONTEXT ReadTsc;
        WHV_X64_APIC_SMI_CONTEXT ApicSmi;
        WHV_X64_APIC_INIT_SIPI_CONTEXT ApicInitSipi;
        WHV_X64_APIC_WRITE_CONTEXT ApicWrite;
        UINT64 AsUINT64[22];
#elif defined(_ARM64_)
        WHV_UNRECOVERABLE_EXCEPTION_CONTEXT UnrecoverableException;
        WHV_INVALID_VP_REGISTER_CONTEXT InvalidVpRegister;
        WHV_REGISTER_CONTEXT Register;
        WHV_ARM64_RESET_CONTEXT Arm64Reset;
        UINT64 AsUINT64[32];
#endif
    };
} WHV_RUN_VP_EXIT_CONTEXT;
```

## Remarks

`WHvRunVirtualProcessor` writes a `WHV_RUN_VP_EXIT_CONTEXT` structure on every exit. The `ExitReason` member is a `WHV_RUN_VP_EXIT_REASON` value that identifies why the virtual processor stopped running, and the trailing union carries the context structure that corresponds to that reason.

The set of exit reasons and the layout of the exit context are architecture-specific.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Memory Access](MemoryAccess.md)
- [I/O Port Access](IOPortAccess.md)
- [MSR Access](MSRAccess.md)
- [CPUID Access](CPUIDAccess.md)
- [Virtual Processor Exception](VirtualProcessorException.md)
- [Interrupt Window](InterruptWindow.md)
- [Unsupported Feature](UnsupportableFeature.md)
- [Execution Canceled](ExecutionCancelled.md)
- [RDTSC(P)](Rdtsc.md)
- [APIC EOI](ApicEOI.md)
- [Register Intercept](RegisterIntercept.md)
- [Arm64 Reset](Arm64Reset.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)