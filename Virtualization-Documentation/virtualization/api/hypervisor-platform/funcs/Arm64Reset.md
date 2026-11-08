---
title: Arm64 Reset Exit
description: Learn about context data for an Arm64 exit caused by a virtual processor reset request.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# Arm64 Reset Exit
Context data for an Arm64 exit caused by a virtual processor reset request.

> [!NOTE]
> This exit reason and its context structure apply to Arm64 partitions only.

## Syntax
```C
//
// Context data for an exit caused by a reset request
// (WHvRunVpExitReasonArm64Reset)
//
typedef enum WHV_ARM64_RESET_TYPE
{
    WHvArm64ResetTypePowerOff = 0,
    WHvArm64ResetTypeReboot
} WHV_ARM64_RESET_TYPE;

typedef struct WHV_ARM64_RESET_CONTEXT
{
    WHV_INTERCEPT_MESSAGE_HEADER Header;
    WHV_ARM64_RESET_TYPE ResetType;
    UINT32 Reserved;
} WHV_ARM64_RESET_CONTEXT;
```

## Remarks

Information about an exit caused by the guest requesting a reset is provided in the `WHV_ARM64_RESET_CONTEXT` structure. The exit is reported with the `WHvRunVpExitReasonArm64Reset` exit reason (`0x8001000c`).

The `Header` member is a `WHV_INTERCEPT_MESSAGE_HEADER`, which reports the program counter (`Pc`) and saved processor state (`Cpsr`) at the time of the request.

The `ResetType` member is a `WHV_ARM64_RESET_TYPE` value that indicates the kind of reset the guest requested:

- `WHvArm64ResetTypePowerOff` — the guest requested a power off.
- `WHvArm64ResetTypeReboot` — the guest requested a reboot.

The virtualization stack is responsible for carrying out the requested action, for example by tearing down or reinitializing the partition.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
