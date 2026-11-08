---
title: Register Intercept Exit
description: Learn about context data for an Arm64 exit caused by an intercepted register access.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Register Intercept Exit
Context data for an Arm64 exit caused by an intercepted register access.

> [!NOTE]
> This exit reason and its context structure apply to Arm64 partitions only.

## Syntax
```C
//
// Context data for an exit caused by an intercepted register access
// (WHvMessageTypeRegisterIntercept)
//

//
// Define register intercept message structure.
//
typedef union WHV_REGISTER_ACCESS_INFO
{
    WHV_REGISTER_VALUE SourceValue;
    WHV_REGISTER_NAME DestinationRegister;
} WHV_REGISTER_ACCESS_INFO;

typedef struct WHV_REGISTER_CONTEXT
{
    WHV_INTERCEPT_MESSAGE_HEADER Header;
    struct
    {
        UINT8 IsMemoryOp:1;
        UINT8 Reserved:7;
    };
    UINT8 Reserved8;
    UINT16 Reserved16;
    WHV_REGISTER_NAME RegisterName;
    WHV_REGISTER_ACCESS_INFO AccessInfo;
} WHV_REGISTER_CONTEXT;
```

## Remarks

Information about an exit caused by the virtual processor accessing an intercepted system register is provided in the `WHV_REGISTER_CONTEXT` structure. The exit is reported with the `WHvMessageTypeRegisterIntercept` exit reason.

The exit is generated when the guest writes one of the synthetic guest-crash registers — `WHvRegisterGuestCrashP0` through `WHvRegisterGuestCrashP4`, or `WHvRegisterGuestCrashCtl` — which a guest can use to report a crash to the virtualization stack.

The `Header` member is a `WHV_INTERCEPT_MESSAGE_HEADER`, which reports the program counter (`Pc`) and saved processor state (`Cpsr`) at the time of the access, along with the access direction in `WHV_INTERCEPT_MESSAGE_HEADER.InterceptAccessType` (a `WHV_MEMORY_ACCESS_TYPE` value).

The `RegisterName` member is the `WHV_REGISTER_NAME` of the intercepted register. The `AccessInfo` member carries the value for a register write and identifies the target for a register read:

- `AccessInfo.SourceValue` is a `WHV_REGISTER_VALUE` that carries the value written for a register write.
- `AccessInfo.DestinationRegister` is a `WHV_REGISTER_NAME` that identifies the target register for a register read.

The `IsMemoryOp` bit indicates whether the intercepted access is a memory operation.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
