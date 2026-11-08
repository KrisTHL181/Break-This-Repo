---
title: Partition Property Data Types
description: Learn about the partition property data types used to configure and query the properties of a partition.
author: Juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# Partition Property Data Types

The partition property data types define the property codes and value structures used to configure and query the properties of a partition.

## Syntax
```C
typedef enum WHV_PARTITION_PROPERTY_CODE
{
    WHvPartitionPropertyCodeExtendedVmExits         = 0x00000001,
#if defined(_AMD64_)
    WHvPartitionPropertyCodeExceptionExitBitmap     = 0x00000002,
#endif
    WHvPartitionPropertyCodeSeparateSecurityDomain  = 0x00000003,
    WHvPartitionPropertyCodeNestedVirtualization    = 0x00000004,
#if defined(_AMD64_)
    WHvPartitionPropertyCodeX64MsrExitBitmap        = 0x00000005,
#endif
    WHvPartitionPropertyCodePrimaryNumaNode         = 0x00000006,
    WHvPartitionPropertyCodeCpuReserve              = 0x00000007,
    WHvPartitionPropertyCodeCpuCap                  = 0x00000008,
    WHvPartitionPropertyCodeCpuWeight               = 0x00000009,
    WHvPartitionPropertyCodeCpuGroupId              = 0x0000000a,
    WHvPartitionPropertyCodeProcessorFrequencyCap   = 0x0000000b,
    WHvPartitionPropertyCodeAllowDeviceAssignment   = 0x0000000c,
    WHvPartitionPropertyCodeDisableSmt              = 0x0000000d,

    WHvPartitionPropertyCodeProcessorFeatures               = 0x00001001,
    WHvPartitionPropertyCodeProcessorClFlushSize            = 0x00001002,
#if defined(_AMD64_)
    WHvPartitionPropertyCodeCpuidExitList                   = 0x00001003,
    WHvPartitionPropertyCodeCpuidResultList                 = 0x00001004,
    WHvPartitionPropertyCodeLocalApicEmulationMode          = 0x00001005,
    WHvPartitionPropertyCodeProcessorXsaveFeatures          = 0x00001006,
#endif
    WHvPartitionPropertyCodeProcessorClockFrequency         = 0x00001007,
#if defined(_AMD64_)
    WHvPartitionPropertyCodeInterruptClockFrequency         = 0x00001008,
    WHvPartitionPropertyCodeApicRemoteReadSupport           = 0x00001009,
#endif
    WHvPartitionPropertyCodeProcessorFeaturesBanks          = 0x0000100A,
    WHvPartitionPropertyCodeReferenceTime                   = 0x0000100B,
    WHvPartitionPropertyCodeSyntheticProcessorFeaturesBanks = 0x0000100C,
#if defined(_AMD64_)
    WHvPartitionPropertyCodeCpuidResultList2                = 0x0000100D,
    WHvPartitionPropertyCodeProcessorPerfmonFeatures        = 0x0000100E,
    WHvPartitionPropertyCodeMsrActionList                   = 0x0000100F,
    WHvPartitionPropertyCodeUnimplementedMsrAction          = 0x00001010,
#endif
    WHvPartitionPropertyCodePhysicalAddressWidth            = 0x00001011,
#if defined(_ARM64_)
    WHvPartitionPropertyCodeArm64IcParameters               = 0x00001012,
#endif
    WHvPartitionPropertyCodeProcessorCount                  = 0x00001fff,
} WHV_PARTITION_PROPERTY_CODE;

#if defined(_AMD64_)

//
// WHvPartitionPropertyCodeCpuidResultList input buffer list element.
//
typedef struct WHV_X64_CPUID_RESULT
{
    UINT32 Function;
    UINT32 Reserved[3];
    UINT32 Eax;
    UINT32 Ebx;
    UINT32 Ecx;
    UINT32 Edx;
} WHV_X64_CPUID_RESULT;

//
// WHvPartitionPropertyCodeExceptionBitmap enumeration values.
//
typedef enum WHV_EXCEPTION_TYPE
{
    WHvX64ExceptionTypeDivideErrorFault = 0x0,
    WHvX64ExceptionTypeDebugTrapOrFault = 0x1,
    WHvX64ExceptionTypeBreakpointTrap = 0x3,
    WHvX64ExceptionTypeOverflowTrap = 0x4,
    WHvX64ExceptionTypeBoundRangeFault = 0x5,
    WHvX64ExceptionTypeInvalidOpcodeFault = 0x6,
    WHvX64ExceptionTypeDeviceNotAvailableFault = 0x7,
    WHvX64ExceptionTypeDoubleFaultAbort = 0x8,
    WHvX64ExceptionTypeInvalidTaskStateSegmentFault = 0x0A,
    WHvX64ExceptionTypeSegmentNotPresentFault = 0x0B,
    WHvX64ExceptionTypeStackFault = 0x0C,
    WHvX64ExceptionTypeGeneralProtectionFault = 0x0D,
    WHvX64ExceptionTypePageFault = 0x0E,
    WHvX64ExceptionTypeFloatingPointErrorFault = 0x10,
    WHvX64ExceptionTypeAlignmentCheckFault = 0x11,
    WHvX64ExceptionTypeMachineCheckAbort = 0x12,
    WHvX64ExceptionTypeSimdFloatingPointFault = 0x13,
} WHV_EXCEPTION_TYPE;

typedef enum WHV_X64_LOCAL_APIC_EMULATION_MODE
{
    WHvX64LocalApicEmulationModeNone,
    WHvX64LocalApicEmulationModeXApic,
    WHvX64LocalApicEmulationModeX2Apic
} WHV_X64_LOCAL_APIC_EMULATION_MODE;

//
// Return value for WHvCapabilityCodeX64MsrExits and input buffer for
// WHvPartitionPropertyCodeX64MsrcExits
//
typedef union WHV_X64_MSR_EXIT_BITMAP
{
    UINT64 AsUINT64;
    struct
    {
        UINT64 UnhandledMsrs:1;
        UINT64 TscMsrWrite:1;
        UINT64 TscMsrRead:1;
        UINT64 ApicBaseMsrWrite:1;
        UINT64 MiscEnableMsrRead:1;
        UINT64 McUpdatePatchLevelMsrRead:1;
        UINT64 Reserved:58;
    };

} WHV_X64_MSR_EXIT_BITMAP;

#elif defined(_ARM64_)

//
// WHvPartitionPropertyCodeArm64IcParameters enumeration values.
//

typedef enum WHV_ARM64_IC_EMULATION_MODE
{
    WHvArm64IcEmulationModeNone = 0,
    WHvArm64IcEmulationModeGicV3
} WHV_ARM64_IC_EMULATION_MODE;

typedef UINT32 WHV_ARM64_INTERRUPT_VECTOR;

typedef struct WHV_ARM64_IC_GIC_V3_PARAMETERS
{
    WHV_GUEST_PHYSICAL_ADDRESS GicdBaseAddress;
    WHV_GUEST_PHYSICAL_ADDRESS GitsTranslaterBaseAddress;
    UINT32 Reserved;
    UINT32 GicLpiIntIdBits;
    WHV_ARM64_INTERRUPT_VECTOR GicPpiOverflowInterruptFromCntv;
    WHV_ARM64_INTERRUPT_VECTOR GicPpiPerformanceMonitorsInterrupt;
    UINT32 Reserved1[6];
} WHV_ARM64_IC_GIC_V3_PARAMETERS;

typedef struct WHV_ARM64_IC_PARAMETERS
{
    WHV_ARM64_IC_EMULATION_MODE EmulationMode;
    UINT32 Reserved;
    union
    {
        WHV_ARM64_IC_GIC_V3_PARAMETERS GicV3Parameters;
    };

} WHV_ARM64_IC_PARAMETERS;

#endif

//
// WHvGetPartitionProperty output buffer / WHvSetPartitionProperty input buffer
//
typedef union WHV_PARTITION_PROPERTY
{
    WHV_EXTENDED_VM_EXITS ExtendedVmExits;
    WHV_PROCESSOR_FEATURES ProcessorFeatures;
    WHV_SYNTHETIC_PROCESSOR_FEATURES_BANKS SyntheticProcessorFeaturesBanks;
    UINT8 ProcessorClFlushSize;
    UINT32 ProcessorCount;
    BOOL SeparateSecurityDomain;
    BOOL NestedVirtualization;
    UINT64 ProcessorClockFrequency;
    WHV_PROCESSOR_FEATURES_BANKS ProcessorFeaturesBanks;
    UINT64 ReferenceTime;
    USHORT PrimaryNumaNode;
    UINT32 CpuReserve;
    UINT32 CpuCap;
    UINT32 CpuWeight;
    UINT64 CpuGroupId;
    UINT32 ProcessorFrequencyCap;
    BOOL AllowDeviceAssignment;
    BOOL DisableSmt;
    UINT32 PhysicalAddressWidth;
#if defined(_AMD64_)
    WHV_PROCESSOR_XSAVE_FEATURES ProcessorXsaveFeatures;
    UINT32 CpuidExitList[1];
    UINT64 ExceptionExitBitmap;
    BOOL ApicRemoteRead;
    WHV_X64_MSR_EXIT_BITMAP X64MsrExitBitmap;
    WHV_PROCESSOR_PERFMON_FEATURES ProcessorPerfmonFeatures;
    UINT64 InterruptClockFrequency;
    WHV_X64_CPUID_RESULT CpuidResultList[1];
    WHV_X64_CPUID_RESULT2 CpuidResultList2[1];
    WHV_MSR_ACTION_ENTRY MsrActionList[1];
    WHV_MSR_ACTION UnimplementedMsrAction;
    WHV_X64_LOCAL_APIC_EMULATION_MODE LocalApicEmulationMode;
#elif defined(_ARM64_)
    WHV_ARM64_IC_PARAMETERS Arm64IcParameters;
#endif
} WHV_PARTITION_PROPERTY;
```


## Remarks

The `WHvPartitionPropertyCodeExtendedVmExits` property controls the set of additional operations by a virtual processor that should cause the execution of the processor to exit and to return to the caller of the [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md) function.

The processor-related partition properties control the processor features that are made available to the virtual processor of the partition. These properties can only be configured during the initial creation of the partition, prior to calling [`WHvSetupPartition`](WHvSetupPartition.md).

On Arm64, `WHvPartitionPropertyCodeArm64IcParameters` configures the emulated interrupt controller and is required before the partition is set up. Set its `EmulationMode` field to `WHvArm64IcEmulationModeGicV3`; if it is left at `WHvArm64IcEmulationModeNone`, [`WHvSetupPartition`](WHvSetupPartition.md) fails with `WHV_E_INVALID_PARTITION_CONFIG`. When `EmulationMode` is `WHvArm64IcEmulationModeGicV3`, the `GicV3Parameters` member supplies the GICv3 configuration, which the hypervisor validates. Provide valid values for all of the following fields:

- `GicdBaseAddress` — the guest physical address of the GIC distributor.
- `GitsTranslaterBaseAddress` — the guest physical address of the GIC Interrupt Translation Service (ITS) translator registers.
- `GicLpiIntIdBits` — the number of locality-specific peripheral interrupt (LPI) identifier bits. Query the supported value through the `WHvCapabilityCodeGicLpiIntIdBits` capability (see [`WHvGetCapability`](WHvGetCapability.md)).
- `GicPpiOverflowInterruptFromCntv` — the private peripheral interrupt (PPI) used for the virtual timer (CNTV) interrupt.
- `GicPpiPerformanceMonitorsInterrupt` — the PPI used for the performance monitors interrupt.

## See also

- [`WHvGetPartitionProperty`](WHvGetPartitionProperty.md)
- [`WHvSetPartitionProperty`](WHvSetPartitionProperty.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)