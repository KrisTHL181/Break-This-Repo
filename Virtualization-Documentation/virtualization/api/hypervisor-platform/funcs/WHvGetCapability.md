---
title: WHvGetCapability
description: Learn about the WHvGetCapability function that queries the capabilities of the API implementation, the hypervisor, and the system's processor.
author: Juarezhm
ms.author: hajuarez
ms.date: 5/31/2019
ms.topic: reference
---

# WHvGetCapability

Queries the capabilities of the API implementation, the hypervisor, and the system's processor.

## Syntax
```C
//
// Platform capabilities
//
typedef enum WHV_CAPABILITY_CODE
{
    // Capabilities of the API implementation
    WHvCapabilityCodeHypervisorPresent                = 0x00000000,
    WHvCapabilityCodeFeatures                         = 0x00000001,
    WHvCapabilityCodeExtendedVmExits                  = 0x00000002,
#if defined(_AMD64_)
    WHvCapabilityCodeExceptionExitBitmap              = 0x00000003,
    WHvCapabilityCodeX64MsrExitBitmap                 = 0x00000004,
#endif
    WHvCapabilityCodeGpaRangePopulateFlags            = 0x00000005,
    WHvCapabilityCodeSchedulerFeatures                = 0x00000006,

    // Capabilities of the system's processor
    WHvCapabilityCodeProcessorVendor                 = 0x00001000,
    WHvCapabilityCodeProcessorFeatures               = 0x00001001,
    WHvCapabilityCodeProcessorClFlushSize            = 0x00001002,
#if defined(_AMD64_)
    WHvCapabilityCodeProcessorXsaveFeatures          = 0x00001003,
#endif
    WHvCapabilityCodeProcessorClockFrequency         = 0x00001004,
#if defined(_AMD64_)
    WHvCapabilityCodeInterruptClockFrequency         = 0x00001005,
#endif
    WHvCapabilityCodeProcessorFeaturesBanks          = 0x00001006,
    WHvCapabilityCodeProcessorFrequencyCap           = 0x00001007,
    WHvCapabilityCodeSyntheticProcessorFeaturesBanks = 0x00001008,
#if defined(_AMD64_)
    WHvCapabilityCodeProcessorPerfmonFeatures        = 0x00001009,
#endif
    WHvCapabilityCodePhysicalAddressWidth            = 0x0000100A,
#if defined(_AMD64_)
    // Nested-virtualization related capabilities
    WHvCapabilityCodeVmxBasic                        = 0x00002000,
    WHvCapabilityCodeVmxPinbasedCtls                 = 0x00002001,
    WHvCapabilityCodeVmxProcbasedCtls                = 0x00002002,
    WHvCapabilityCodeVmxExitCtls                     = 0x00002003,
    WHvCapabilityCodeVmxEntryCtls                    = 0x00002004,
    WHvCapabilityCodeVmxMisc                         = 0x00002005,
    WHvCapabilityCodeVmxCr0Fixed0                    = 0x00002006,
    WHvCapabilityCodeVmxCr0Fixed1                    = 0x00002007,
    WHvCapabilityCodeVmxCr4Fixed0                    = 0x00002008,
    WHvCapabilityCodeVmxCr4Fixed1                    = 0x00002009,
    WHvCapabilityCodeVmxVmcsEnum                     = 0x0000200A,
    WHvCapabilityCodeVmxProcbasedCtls2               = 0x0000200B,
    WHvCapabilityCodeVmxEptVpidCap                   = 0x0000200C,
    WHvCapabilityCodeVmxTruePinbasedCtls             = 0x0000200D,
    WHvCapabilityCodeVmxTrueProcbasedCtls            = 0x0000200E,
    WHvCapabilityCodeVmxTrueExitCtls                 = 0x0000200F,
    WHvCapabilityCodeVmxTrueEntryCtls                = 0x00002010,
#elif defined (_ARM64_)
    WHvCapabilityCodeGicLpiIntIdBits                 = 0x00002011,
    WHvCapabilityCodeMaxSveVectorLength              = 0x00002012,
#endif
} WHV_CAPABILITY_CODE;

//
// Return values for WHvCapabilityCodeFeatures
//
typedef union WHV_CAPABILITY_FEATURES
{
    struct
    {
        UINT64 PartialUnmap : 1;
#if defined(_AMD64_)
        UINT64 LocalApicEmulation : 1;
        UINT64 Xsave : 1;
#else
        UINT64 ReservedArm0 : 2;
#endif
        UINT64 DirtyPageTracking : 1;
        UINT64 SpeculationControl : 1;
#if defined(_AMD64_)
        UINT64 ApicRemoteRead : 1;
#else
        UINT64 ReservedArm1 : 1;
#endif
        UINT64 IdleSuspend : 1;
        UINT64 VirtualPciDeviceSupport : 1;
        UINT64 IommuSupport : 1;
        UINT64 VpHotAddRemove : 1;
        UINT64 DeviceAccessTracking : 1;
#if defined(_AMD64_)
        UINT64 ReservedX640 : 1;
#else
        UINT64 Arm64Support : 1;
#endif
        UINT64 Reserved : 52;
    };

    UINT64 AsUINT64;
} WHV_CAPABILITY_FEATURES;

//
// Return values for WHvCapabilityCodeExtendedVmExits and input buffer for
// WHvPartitionPropertyCodeExtendedVmExits
//
typedef union WHV_EXTENDED_VM_EXITS
{
    struct
    {
#if defined(_AMD64_)
        UINT64 X64CpuidExit               : 1; // WHvRunVpExitReasonX64CPUID supported
        UINT64 X64MsrExit                 : 1; // WHvRunVpExitX64ReasonMSRAccess supported
        UINT64 ExceptionExit              : 1; // WHvRunVpExitReasonException supported
        UINT64 X64RdtscExit               : 1; // WHvRunVpExitReasonX64Rdtsc supported
        UINT64 X64ApicSmiExitTrap         : 1; // WHvRunVpExitReasonX64ApicSmiTrap supported
#else
        UINT64 ReservedArm0               : 5;
#endif
        UINT64 HypercallExit              : 1; // WHvRunVpExitReasonHypercall supported
#if defined(_AMD64_)
        UINT64 X64ApicInitSipiExitTrap    : 1; // WHvRunVpExitReasonX64ApicInitSipiTrap supported
        UINT64 X64ApicWriteLint0ExitTrap  : 1; // WHvRunVpExitReasonX64ApicWriteTrap supported
        UINT64 X64ApicWriteLint1ExitTrap  : 1; // WHvRunVpExitReasonX64ApicWriteTrap supported
        UINT64 X64ApicWriteSvrExitTrap    : 1; // WHvRunVpExitReasonX64ApicWriteTrap supported
#else
        UINT64 ReservedArm1               : 4;
#endif
        UINT64 UnknownSynicConnection     : 1; // WHvRunVpExitReasonHypercall supported for unknown synic connection to HvSignalEvent
        UINT64 RetargetUnknownVpciDevice  : 1; // WHvRunVpExitReasonHypercall supported for unknown device to HvRetargetDeviceInterrupt
#if defined(_AMD64_)
        UINT64 X64ApicWriteLdrExitTrap    : 1; // WHvRunVpExitReasonX64ApicWriteTrap supported
        UINT64 X64ApicWriteDfrExitTrap    : 1; // WHvRunVpExitReasonX64ApicWriteTrap supported
#else
        UINT64 ReservedArm2               : 2;
#endif
        UINT64 GpaAccessFaultExit         : 1; // WHvRunVpExitReasonMemoryAccess supported for second-level page faults
        UINT64 Reserved                   : 49;
    };

    UINT64 AsUINT64;
} WHV_EXTENDED_VM_EXITS;

//
// Return values for WHvCapabilityCodeProcessorVendor
//
typedef enum WHV_PROCESSOR_VENDOR
{
    WHvProcessorVendorAmd   = 0x0000,
    WHvProcessorVendorIntel = 0x0001,
    WHvProcessorVendorHygon = 0x0002,

    WHvProcessorVendorArm   = 0x0010,

} WHV_PROCESSOR_VENDOR;

//
// Return values for WHvCapabilityCodeProcessorFeatures
//
#if defined(_AMD64_)

typedef union WHV_X64_PROCESSOR_FEATURES
{
    struct
    {
        UINT64 Sse3Support : 1;
        UINT64 LahfSahfSupport : 1;
        UINT64 Ssse3Support : 1;
        UINT64 Sse4_1Support : 1;
        UINT64 Sse4_2Support : 1;
        UINT64 Sse4aSupport : 1;
        UINT64 XopSupport : 1;
        UINT64 PopCntSupport : 1;
        UINT64 Cmpxchg16bSupport : 1;
        UINT64 Altmovcr8Support : 1;
        UINT64 LzcntSupport : 1;
        UINT64 MisAlignSseSupport : 1;
        UINT64 MmxExtSupport : 1;
        UINT64 Amd3DNowSupport : 1;
        UINT64 ExtendedAmd3DNowSupport : 1;
        UINT64 Page1GbSupport : 1;
        UINT64 AesSupport : 1;
        UINT64 PclmulqdqSupport : 1;
        UINT64 PcidSupport : 1;
        UINT64 Fma4Support : 1;
        UINT64 F16CSupport : 1;
        UINT64 RdRandSupport : 1;
        UINT64 RdWrFsGsSupport : 1;
        UINT64 SmepSupport : 1;
        UINT64 EnhancedFastStringSupport : 1;
        UINT64 Bmi1Support : 1;
        UINT64 Bmi2Support : 1;
        UINT64 Reserved1 : 2;
        UINT64 MovbeSupport : 1;
        UINT64 Npiep1Support : 1;
        UINT64 DepX87FPUSaveSupport : 1;
        UINT64 RdSeedSupport : 1;
        UINT64 AdxSupport : 1;
        UINT64 IntelPrefetchSupport : 1;
        UINT64 SmapSupport : 1;
        UINT64 HleSupport : 1;
        UINT64 RtmSupport : 1;
        UINT64 RdtscpSupport : 1;
        UINT64 ClflushoptSupport : 1;
        UINT64 ClwbSupport : 1;
        UINT64 ShaSupport : 1;
        UINT64 X87PointersSavedSupport : 1;
        UINT64 InvpcidSupport : 1;
        UINT64 IbrsSupport : 1;
        UINT64 StibpSupport : 1;
        UINT64 IbpbSupport : 1;
        UINT64 UnrestrictedGuestSupport : 1;
        UINT64 SsbdSupport : 1;
        UINT64 FastShortRepMovSupport : 1;
        UINT64 Reserved3 : 1;
        UINT64 RdclNo : 1;
        UINT64 IbrsAllSupport : 1;
        UINT64 Reserved4 : 1;
        UINT64 SsbNo : 1;
        UINT64 RsbANo : 1;
        UINT64 Reserved5 : 1;
        UINT64 RdPidSupport : 1;
        UINT64 UmipSupport : 1;
        UINT64 MdsNoSupport : 1;
        UINT64 MdClearSupport : 1;
        UINT64 TaaNoSupport : 1;
        UINT64 TsxCtrlSupport : 1;
        UINT64 Reserved6 : 1;
    };

    UINT64 AsUINT64;
} WHV_X64_PROCESSOR_FEATURES, WHV_PROCESSOR_FEATURES;

typedef union WHV_PROCESSOR_XSAVE_FEATURES
{
    struct
    {
        UINT64 XsaveSupport : 1;
        UINT64 XsaveoptSupport : 1;
        UINT64 AvxSupport : 1;
        UINT64 Avx2Support : 1;
        UINT64 FmaSupport : 1;
        UINT64 MpxSupport : 1;
        UINT64 Avx512Support : 1;
        UINT64 Avx512DQSupport : 1;
        UINT64 Avx512CDSupport : 1;
        UINT64 Avx512BWSupport : 1;
        UINT64 Avx512VLSupport : 1;
        UINT64 XsaveCompSupport : 1;
        UINT64 XsaveSupervisorSupport : 1;
        UINT64 Xcr1Support : 1;
        UINT64 Avx512BitalgSupport : 1;
        UINT64 Avx512IfmaSupport : 1;
        UINT64 Avx512VBmiSupport : 1;
        UINT64 Avx512VBmi2Support : 1;
        UINT64 Avx512VnniSupport : 1;
        UINT64 GfniSupport : 1;
        UINT64 VaesSupport : 1;
        UINT64 Avx512VPopcntdqSupport : 1;
        UINT64 VpclmulqdqSupport : 1;
        UINT64 Avx512Bf16Support : 1;
        UINT64 Avx512Vp2IntersectSupport : 1;
        UINT64 Avx512Fp16Support : 1;
        UINT64 XfdSupport : 1;
        UINT64 AmxTileSupport : 1;
        UINT64 AmxBf16Support : 1;
        UINT64 AmxInt8Support : 1;
        UINT64 AvxVnniSupport : 1;
        UINT64 AvxIfmaSupport : 1;
        UINT64 AvxNeConvertSupport : 1;
        UINT64 AvxVnniInt8Support : 1;
        UINT64 AvxVnniInt16Support : 1;
        UINT64 Avx10_1_256Support : 1;
        UINT64 Avx10_1_512Support : 1;
        UINT64 AmxFp16Support : 1;
        UINT64 Reserved : 26;
    };

    UINT64 AsUINT64;
} WHV_PROCESSOR_XSAVE_FEATURES, *PWHV_PROCESSOR_XSAVE_FEATURES;

#elif defined(_ARM64_)

typedef union WHV_ARM64_PROCESSOR_FEATURES
{
    struct
    {
        //
        // Bits for features reported by ID_AA64MMFR0_EL1
        //

        UINT64 Asid16:1;                    // 16 bits ASID.
        UINT64 TGran16:1;                   // Indicated support for 16KB memory translation granule.
        UINT64 TGran64:1;                   // Indicated support for 64KB memory translation granule.

        //
        // Bits for features reported by ID_AA64MMFR1_EL1
        //

        UINT64 Haf:1;                       // Hardware updates to Access flag.
        UINT64 Hdbs:1;                      // Hardware updates to Dirty state.
        UINT64 Pan:1;                       // Privileged access never (ARMv8.1).
        UINT64 AtS1E1:1;                    // AT S1E1RP and AT S1E1WP supported.

        //
        // Bits for features reported by ID_AA64MMFR2_EL1
        //

        UINT64 Uao:1;                       // PSTATE override of Unprivileged Load/Store (ARMv8.2)

        //
        // Bits for features reported by ID_AA64PFR0_EL1
        //

        UINT64 El0Aarch32:1;                // If Aarch32 is supported at El0.
        UINT64 Fp:1;                        // Floating point support.
        UINT64 FpHp:1;                      // Floating point half-precision support.
        UINT64 AdvSimd:1;                   // AdvSIMD is implemented.
        UINT64 AdvSimdHp:1;                 // AdvSIMD with half precision floating point support.
        UINT64 GicV3V4:1;                   // System register interface to versions 3 and 4 of the GIC CPU interface is implemented.
        UINT64 GicV41:1;                    // System register interface to version 4.1 of the GIC CPU interface is implemented.
        UINT64 Ras:1;                       // Ras

        //
        // Bits for features reported by ID_AA64DFR0_EL1
        //

        UINT64 PmuV3:1;                     // PMUv3 implemented.
        UINT64 PmuV3ArmV81:1;               // PMUv3 for Armv8.1 implemented.
        UINT64 PmuV3ArmV84:1;               // PMUv3 for Armv8.4 implemented.
        UINT64 PmuV3ArmV85:1;               // PMUv3 for Armv8.5 implemented.

        //
        // Bits for features reported by ID_AA64ISAR0_EL1
        //

        UINT64 Aes:1;                      // AES(AESE, AESD, AESMC, AESIMC) instructions.
        UINT64 PolyMul:1;                  // Polynomial multiply instructions PMULL/PMULL2
        UINT64 Sha1:1;                     // Sha1 instructions implemented.
        UINT64 Sha256:1;                   // Sha256 instructions implemented.
        UINT64 Sha512:1;                   // Sha512 instructions implemented.
        UINT64 Crc32:1;                    // CRC instructions implemented.
        UINT64 Atomic:1;                   // Atomic instructions implemented.
        UINT64 Rdm:1;                      // SQRDMLAH, SQRDMLSH instructions implemented.
        UINT64 Sha3:1;                     // Sha3 instructions implemented.
        UINT64 Sm3:1;                      // SM3 instructions implemented.
        UINT64 Sm4:1;                      // SM4 instructions implemented.
        UINT64 Dp:1;                       // UDOT and SDOT Dot product instructions implemented.
        UINT64 Fhm:1;                      // FMLAL and FMLSL instructions implemented.


        //
        // Bits for features reported by ID_AA64ISAR1_EL1
        //

        UINT64 DcCvap:1;                   // Clean data cache by address to point of persistence.
        UINT64 DcCvadp:1;                  // Clean data cache by address to point of deep persistence.

        //
        // Pointer authentication using QARMA algo.
        //

        UINT64 ApaBase:1;                  // HaveEnhancedPAC, HaveEnhancedPAC2 return false
        UINT64 ApaEp:1;                    // HaveEnhancedPAC -> true, HaveEnhancedPAC2 -> false.
        UINT64 ApaEp2:1;                   // HaveEnhancedPAC -> false, HaveEnhancedPAC2 -> true, HaveFPAC -> false, HaveFPACCombined -> false.
        UINT64 ApaEp2Fp:1;                 // HaveEnhancedPAC -> false, HaveEnahancedPAC2 -> true, HaveFPAC -> true, HaveFPACCombined -> false.
        UINT64 ApaEp2Fpc:1;                // HaveEnhancedPAC -> false, HaveEnahancedPAC2 -> true, HaveFPAC -> true, HaveFPACCombined -> true.

        UINT64 Jscvt:1;                    // FJCVTZS instruction implemented. Support for JS conversion from double to integer.
        UINT64 Fcma:1;                     // Complex number instructions(FCMLA and FCADD) instructions are implemented.
        UINT64 RcpcV83:1;                  // ARMv8.3-RCPC. LDAPR* instructions.
        UINT64 RcpcV84:1;                  // ARMv8.4-RCPC. LDAPUR* and STLUR* instructions.
        UINT64 Gpa:1;                      // Generic code authentication using QARMA algo.

        //
        // Features reported by CTR_EL0
        //

        UINT64 L1ipPipt:1;                 // If L1 Instruction cache is PIPT.

        //
        // Features reported by DCZID_EL0
        //

        UINT64 DzPermitted:1;       // Data zero instructions permitted.
        UINT64 Ssbs:1;              // FEAT_SSBS (Speculative Store Bypass Safe): ID_AA64PFR1_EL1.SSBS >= 0b0001
        UINT64 SsbsRw:1;            // FEAT_SSBS2 (Speculative Store Bypass Safe):ID_AA64PFR1_EL1.SSBS >= 0b0010
        UINT64 Reserved49:1;        // Unused
        UINT64 Reserved50:1;        // Unused
        UINT64 Reserved51:1;        // Unused
        UINT64 Reserved52:1;        // Unused
        UINT64 Csv2:1;              // FEAT_CSV2 (Cache Speculation Variant 2): ID_AA64PFR0_EL1.CSV2 >= 0b0001
        UINT64 Csv3:1;              // FEAT_CSV3 (Cache Speculation Variant 3): ID_AA64PFR0_EL1.CSV3 >= 0b0001
        UINT64 Sb:1;                // FEAT_SB (Speculation Barrier): ID_AA64ISAR1_EL1.SB >= 0b0001
        UINT64 Idc:1;               // CTR_EL0.IDC == 0b1
        UINT64 Dic:1;               // CTR_EL0.DIC == 0b1
        UINT64 TlbiOs:1;            // FEAT_TLBIOS (TLB invalidate instructions in Outer Shareable domain): ID_AA64ISAR0_EL1.TLB >= 0b0001
        UINT64 TlbiOsRange:1;       // FEAT_TLBIRANGE (TLB invalidate range instructions): ID_AA64ISAR0_EL1.TLB >= 0b0010
        UINT64 FlagsM:1;            // ID_AA64ISAR0_EL1.TS >= 0b0001
        UINT64 FlagsM2:1;           // ID_AA64ISAR0_EL1.TS >= 0b0010
        UINT64 Bf16:1;              // FEAT_BF16 (AArch64 BFloat16 instructions): ID_AA64ISAR1_EL1.BF16 >= 0b0001
        UINT64 Ebf16:1;             // FEAT_EBF16 (AArch64 Extended BFloat16 instructions): ID_AA64ISAR1_EL1.BF16 >= 0b0010
    };

    UINT64 AsUINT64;
} WHV_ARM64_PROCESSOR_FEATURES, WHV_PROCESSOR_FEATURES;

#endif

//
// WHvGetCapability output buffer
//
typedef union WHV_CAPABILITY
{
    BOOL HypervisorPresent;
    WHV_CAPABILITY_FEATURES Features;
    WHV_EXTENDED_VM_EXITS ExtendedVmExits;
    WHV_PROCESSOR_VENDOR ProcessorVendor;
    WHV_PROCESSOR_FEATURES ProcessorFeatures;
    WHV_SYNTHETIC_PROCESSOR_FEATURES_BANKS SyntheticProcessorFeaturesBanks;
    UINT8 ProcessorClFlushSize;
    UINT64 ProcessorClockFrequency;
    WHV_PROCESSOR_FEATURES_BANKS ProcessorFeaturesBanks;
    WHV_ADVISE_GPA_RANGE_POPULATE_FLAGS GpaRangePopulateFlags;
    WHV_CAPABILITY_PROCESSOR_FREQUENCY_CAP ProcessorFrequencyCap;
    WHV_SCHEDULER_FEATURES SchedulerFeatures;
    UINT32 PhysicalAddressWidth;
    UINT64 NestedFeatureRegister;
#if defined(_AMD64_)
    WHV_PROCESSOR_XSAVE_FEATURES ProcessorXsaveFeatures;
    UINT64 InterruptClockFrequency;
    WHV_PROCESSOR_PERFMON_FEATURES ProcessorPerfmonFeatures;
    WHV_X64_MSR_EXIT_BITMAP X64MsrExitBitmap;
    UINT64 ExceptionExitBitmap;
#elif defined (_ARM64_)
    UINT32 GicLpiIntIdBits;
    UINT32 MaxSveVectorLength;
#endif
} WHV_CAPABILITY;

HRESULT
WINAPI
WHvGetCapability(
    _In_ WHV_CAPABILITY_CODE CapabilityCode,
    _Out_writes_bytes_to_(CapabilityBufferSizeInBytes, *WrittenSizeInBytes) VOID* CapabilityBuffer,
    _In_ UINT32 CapabilityBufferSizeInBytes,
    _Out_opt_ UINT32* WrittenSizeInBytes
    );
```

### Parameters

`CapabilityCode`

Specifies the capability that is queried.

`CapabilityBuffer`

Specifies the output buffer that receives the value of the capability:

The `WHvCapabilityCodeHypervisorPresent` capability can be used to determine whether the Windows Hypervisor is running on a host and the functions of the platform APIs can be used to create VM partitions.

The `WHvCapabilityCodeFeatures` capability returns the set of optional platform features that the API implementation supports, as a `WHV_CAPABILITY_FEATURES` value. Each bit indicates whether a particular feature is available; callers should query this capability and check the relevant bit before relying on the corresponding functionality.

For the `WHvCapabilityCodeExtendedVmExits` capability, the buffer contains a bit field that specifies which additional exit reasons are available that can be configured to cause the execution of a virtual processor to be halted (see [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)).

The values returned for the processor properties are based on the capabilities of the physical processor on the system (that is, they are retrieved by querying the corresponding properties of the root partition).

`CapabilityBufferSizeInBytes`

Specifies the size of the output buffer, in bytes. For the currently defined set of capabilities, the output buffer should be large enough to hold a 64-bit value.

`WrittenSizeInBytes`

Receives the written size in bytes of the `CapabilityBuffer`.

## Return Value

If the function succeeds, the return value is `S_OK`.

The function returns `WHV_E_UNKNOWN_CAPABILITY` if an unknown capability is requested. The functionality that corresponds to the requested capability must be treated as being not available on the system.

## Remarks

Platform capabilities are a generic way for callers to query properties and capabilities of the hypervisor, of the API implementation, and of the hardware platform that the application is running on. The platform API uses these capabilities to publish the availability of extended functionality of the API as well as the set of features that the processor on the current system supports. Applications must query the availability of a feature prior to calling the corresponding APIs or allowing a VM to use a processor feature.

To determine whether the current system supports the Windows Hypervisor Platform on Arm64, call `WHvGetCapability` with `WHvCapabilityCodeFeatures` and test the `Arm64Support` bit of the returned `WHV_CAPABILITY_FEATURES` value. The `Arm64Support` bit is defined and set only on Arm64 systems where the platform is available, so a caller should first confirm the hypervisor is present with `WHvCapabilityCodeHypervisorPresent`, then check `Arm64Support` before using the platform APIs.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 1803 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvRunVirtualProcessor`](WHvRunVirtualProcessor.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
