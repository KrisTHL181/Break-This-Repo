---
title: Unsupported Feature Exit
description: Learn about context data for an exit caused by an unsupported processor feature.
author: sethmanheim
ms.author: roharwoo
ms.date: 04/20/2022
ms.topic: reference
---

# Unsupported Feature Exit
Context data for an exit caused by an unsupported processor feature.

> [!NOTE]
> The `WHV_X64_UNSUPPORTED_FEATURE_CONTEXT` structure applies to x64 partitions only.

## Syntax
```C
//
// Context data for an exit caused by the use of an unsupported processor feature
// (WHvRunVpExitReasonUnsupportedFeature)
//
typedef enum WHV_X64_UNSUPPORTED_FEATURE_CODE
{
    WHvUnsupportedFeatureIntercept     = 1,
    WHvUnsupportedFeatureTaskSwitchTss = 2
} WHV_X64_UNSUPPORTED_FEATURE_CODE;

typedef struct WHV_X64_UNSUPPORTED_FEATURE_CONTEXT
{
    WHV_X64_UNSUPPORTED_FEATURE_CODE FeatureCode;
    UINT32 Reserved;
    UINT64 FeatureParameter;
} WHV_X64_UNSUPPORTED_FEATURE_CONTEXT;
```

## Remarks
An exit for an unsupported feature occurs when the virtual processor accesses a feature of the architecture that is not properly virtualized by the hypervisor.

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Exit Contexts](WHvExitContextDataTypes.md)
