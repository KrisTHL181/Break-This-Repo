---
title: WHvPostVirtualProcessorSynicMessage
description: Learn about the WHvPostVirtualProcessorSynicMessage function that posts a synthetic interrupt controller message to a virtual processor.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvPostVirtualProcessorSynicMessage

Posts a synthetic interrupt controller (SynIC) message to a virtual processor.

## Syntax

```C
#define WHV_SYNIC_MESSAGE_SIZE  256

HRESULT
WINAPI
WHvPostVirtualProcessorSynicMessage(
    _In_ WHV_PARTITION_HANDLE Partition,
    _In_ UINT32 VpIndex,
    _In_ UINT32 SintIndex,
    _In_reads_bytes_(MessageSizeInBytes) const VOID* Message,
    _In_ UINT32 MessageSizeInBytes
    );
```

### Parameters

`Partition`

Handle to the partition object.

`VpIndex`

Specifies the index of the target virtual processor.

`SintIndex`

Specifies the synthetic interrupt source (SINT) on which the message is delivered.

`Message`

Specifies the message payload to post.

`MessageSizeInBytes`

Specifies the size of `Message`, in bytes. Must be greater than zero and no larger than `WHV_SYNIC_MESSAGE_SIZE` (256).

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `E_INVALIDARG` if `MessageSizeInBytes` is greater than `WHV_SYNIC_MESSAGE_SIZE`. If the target virtual processor's SINT message slot is already occupied by an undelivered message, the function returns `HRESULT_FROM_WIN32(ERROR_HV_OBJECT_IN_USE)`.

## Remarks

The `WHvPostVirtualProcessorSynicMessage` function delivers a message to the SynIC message page of the target virtual processor on the specified SINT, as though the message had been sent by the hypervisor. The message is written into the corresponding SINT message slot and the virtual processor is notified through that SINT.

Each SINT has a single message slot. If the previous message on that SINT has not yet been consumed by the guest, the call fails with `ERROR_HV_OBJECT_IN_USE`; the caller should retry after the guest has acknowledged the prior message.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvSignalVirtualProcessorSynicEvent`](WHvSignalVirtualProcessorSynicEvent.md)
- [Virtual Processor Register Names and Values](WHvVirtualProcessorDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
