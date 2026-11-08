---
title: WHvGetVirtualProcessorCpuidOutput
description: Learn about the WHvGetVirtualProcessorCpuidOutput function that returns the CPUID result a virtual processor would observe for a given leaf.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvGetVirtualProcessorCpuidOutput

Returns the CPUID result a virtual processor would observe for a given leaf and subleaf.

> [!NOTE]
> This function applies to x64 partitions only.

## Syntax

```C
typedef struct WHV_CPUID_OUTPUT
{
    UINT32 Eax;
    UINT32 Ebx;
    UINT32 Ecx;
    UINT32 Edx;
} WHV_CPUID_OUTPUT;

HRESULT
WINAPI
WHvGetVirtualProcessorCpuidOutput(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ UINT32 Eax,
    _In_ UINT32 Ecx,
    _Out_ WHV_CPUID_OUTPUT* CpuidOutput
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the virtual processor whose CPUID result is queried.

`Eax`

Specifies the CPUID leaf (the value of `EAX` at the time of the instruction).

`Ecx`

Specifies the CPUID subleaf (the value of `ECX` at the time of the instruction).

`CpuidOutput`

Receives the `EAX`, `EBX`, `ECX`, and `EDX` values the virtual processor would observe.

## Return Value

If the function succeeds, the return value is `S_OK`.

If `CpuidOutput` is `NULL`, the return value is `E_POINTER`.

## Remarks

The `WHvGetVirtualProcessorCpuidOutput` function computes the CPUID result that the specified virtual processor would observe if it executed the `CPUID` instruction with the given `Eax` leaf and `Ecx` subleaf. The result reflects the virtual processor's current extended-state configuration and any CPUID result overrides registered for the partition through `WHvPartitionPropertyCodeCpuidResultList`, so it represents the value the guest would actually see rather than the raw host CPUID value.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64 |

## See also

- [`WHvGetVirtualProcessorRegisters`](WHvGetVirtualProcessorRegisters.md)
- [Partition Property Data Types](WHvPartitionPropertyDataTypes.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
