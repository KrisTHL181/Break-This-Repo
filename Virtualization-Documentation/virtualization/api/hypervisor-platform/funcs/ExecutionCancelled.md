---
title: Execution Canceled Exit
description: Learn about context data for an exit caused by a cancellation from the host.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# Execution Canceled Exit
Context data for an exit caused by a cancellation from the host.

## Syntax
```C
//
// Context data for an exit caused by a cancellation from the host (WHvRunVpExitReasonCanceled)
//
typedef enum WHV_RUN_VP_CANCEL_REASON
{
    WHvRunVpCancelReasonUser = 0 // Execution canceled by WHvCancelRunVirtualProcessor
} WHV_RUN_VP_CANCEL_REASON;

//
// Alias for non-standard capitalization found in earlier versions of the header
//
#define WhvRunVpCancelReasonUser WHvRunVpCancelReasonUser

typedef struct WHV_RUN_VP_CANCELED_CONTEXT
{
    WHV_RUN_VP_CANCEL_REASON CancelReason;
} WHV_RUN_VP_CANCELED_CONTEXT;
```

## Remarks

Information about an exit caused by the host system is provided in the `WHV_RUN_VP_CANCELED_CONTEXT` structure.

Note that the numerical value of WHvRunVpExitReasonCanceled is different between x64 and Arm64.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
