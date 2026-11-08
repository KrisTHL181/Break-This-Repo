---
title: WHvRunVirtualProcessor
description: Learn about the WHvRunVirtualProcessor function that runs a virtual processor in a partition.
author: Juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# WHvRunVirtualProcessor

Runs a virtual processor in a partition, enabling it to execute guest code.

## Syntax

```C
// Exit reasons
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

    // Additional exits that can be configured through partition properties
    WHvRunVpExitReasonX64MsrAccess           = 0x00001000,
    WHvRunVpExitReasonX64Cpuid               = 0x00001001,
    WHvRunVpExitReasonException              = 0x00001002,
    WHvRunVpExitReasonX64Rdtsc               = 0x00001003,

    // Exits caused by the host
    WHvRunVpExitReasonCanceled               = 0x00002001
} WHV_RUN_VP_EXIT_REASON;
 
// WHvRunVirtualProcessor output buffer
typedef struct WHV_RUN_VP_EXIT_CONTEXT
{
    WHV_RUN_VP_EXIT_REASON ExitReason;
    UINT32 Reserved;
    WHV_VP_EXIT_CONTEXT VpContext;

    union
    {
        WHV_MEMORY_ACCESS_CONTEXT MemoryAccess;
        WHV_X64_IO_PORT_ACCESS_CONTEXT IoPortAccess;
        WHV_X64_MSR_ACCESS_CONTEXT MsrAccess;
        WHV_X64_CPUID_ACCESS_CONTEXT CpuidAccess;
        WHV_VP_EXCEPTION_CONTEXT VpException;
        WHV_X64_INTERRUPTION_DELIVERABLE_CONTEXT InterruptWindow;
        WHV_X64_UNSUPPORTED_FEATURE_CONTEXT UnsupportedFeature;
        WHV_RUN_VP_CANCELED_CONTEXT CancelReason;
        WHV_X64_APIC_EOI_CONTEXT ApicEoi;
        WHV_X64_RDTSC_CONTEXT ReadTsc;
    };
} WHV_RUN_VP_EXIT_CONTEXT;
 
HRESULT
WINAPI
WHvRunVirtualProcessor(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _Out_writes_bytes_(ExitContextSizeInBytes) VOID* ExitContext,
    _In_ UINT32 ExitContextSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor that is executed.

`ExitContext`

Specifies the output buffer that receives the context structure providing the information about the reason that caused the `WHvRunVirtualProcessor` function to return.

`ExitContextSizeInBytes`

Specifies the size of the buffer that receives the exit context, in bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

A virtual processor is run (that is, enabled to execute guest code) by calling the `WHvRunVirtualProcessor` function. The call blocks synchronously until either the virtual processor performs an operation that the virtualization stack must handle (for example, accessing memory in the GPA space that is not mapped or not accessible) or the virtualization stack explicitly requests an exit of the function (for example, to inject an interrupt for the virtual processor or to change the state of the VM).

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCancelRunVirtualProcessor`](WHvCancelRunVirtualProcessor.md)
- [Exit Context Data Types](WHvExitContextDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)