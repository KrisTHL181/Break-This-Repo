---
title: WHvRequestInterrupt
description: Learn about the WHvRequestInterrupt function that requests an interrupt be delivered to one or more virtual processors in a partition.
author: jstarks
ms.author: jostarks
ms.date: 06/06/2019
ms.topic: reference
---

# WHvRequestInterrupt

Requests an interrupt be delivered to one or more virtual processors in a partition.

## Syntax

```C
#if defined(_AMD64_)

typedef enum WHV_INTERRUPT_TYPE
{
    WHvX64InterruptTypeFixed            = 0,
    WHvX64InterruptTypeLowestPriority   = 1,
    WHvX64InterruptTypeNmi              = 4,
    WHvX64InterruptTypeInit             = 5,
    WHvX64InterruptTypeSipi             = 6,
    WHvX64InterruptTypeLocalInt1        = 9,
} WHV_INTERRUPT_TYPE;

typedef enum WHV_INTERRUPT_DESTINATION_MODE
{
    WHvX64InterruptDestinationModePhysical,
    WHvX64InterruptDestinationModeLogical,
} WHV_INTERRUPT_DESTINATION_MODE;

typedef enum WHV_INTERRUPT_TRIGGER_MODE
{
    WHvX64InterruptTriggerModeEdge,
    WHvX64InterruptTriggerModeLevel,
} WHV_INTERRUPT_TRIGGER_MODE;

typedef struct WHV_INTERRUPT_CONTROL
{
    UINT64 Type : 8;             // WHV_INTERRUPT_TYPE
    UINT64 DestinationMode : 4;  // WHV_INTERRUPT_DESTINATION_MODE
    UINT64 TriggerMode : 4;      // WHV_INTERRUPT_TRIGGER_MODE
    UINT64 TargetVtl : 8;        // WHV_VTL
    UINT64 Reserved : 40;
    UINT32 Destination;
    UINT32 Vector;
} WHV_INTERRUPT_CONTROL;

#elif defined(_ARM64_)

typedef enum WHV_INTERRUPT_TYPE
{
    WHvArm64InterruptTypeFixed             = 0x0000,

    //
    // Maximum (exclusive) value of interrupt type.
    //
    WHvArm64InterruptTypeMaximum           = 0x0008,
} WHV_INTERRUPT_TYPE;

typedef union WHV_INTERRUPT_CONTROL2
{
    UINT64 AsUINT64;
    struct
    {
        WHV_INTERRUPT_TYPE InterruptType;
        UINT32 Reserved1:2;
        UINT32 Asserted:1;
        UINT32 Retarget:1;
        UINT32 Reserved2:28;
    };
} WHV_INTERRUPT_CONTROL2;

typedef struct WHV_INTERRUPT_CONTROL
{
    UINT64                  TargetPartition;
    WHV_INTERRUPT_CONTROL2  InterruptControl;
    UINT64                  DestinationAddress;
    UINT32                  RequestedVector;
    UINT8                   TargetVtl;
    UINT8                   ReservedZ0;
    UINT16                  ReservedZ1;
} WHV_INTERRUPT_CONTROL;

#endif // defined(_ARCH_)

HRESULT
WINAPI
WHvRequestInterrupt(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ const WHV_INTERRUPT_CONTROL* Interrupt,
    _In_ UINT32 InterruptControlSize
    );
```

### Parameters

`Partition`

Specifies the partition to interrupt.

`Interrupt`

Specifies the interrupt's characteristics and destination.

`InterruptControlSize`

Specifies the size of `Interrupt`, in bytes.

## Return Value

If the function succeeds, the return value is `S_OK`.

## Remarks

The `WHvRequestInterrupt` function requests that an interrupt described by the `WHV_INTERRUPT_CONTROL` structure be delivered to the partition.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1809 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvGetVirtualProcessorInterruptControllerState`](WHvGetVirtualProcessorInterruptControllerState.md)
- [`WHvSetVirtualProcessorInterruptControllerState`](WHvSetVirtualProcessorInterruptControllerState.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
