---
title: Virtual Processor Register Names and Values
description: Learn about the data types that define virtual processor register names and values.
author: juarezhm
ms.author: hajuarez
ms.date: 05/31/2019
ms.topic: reference
---

# Virtual Processor Register Names and Values

The virtual processor register data types define the register names and value structures used to get and set the registers of a virtual processor.

## Syntax

```C
//
// Virtual Processor Register Definitions
//
#if defined(_AMD64_)
typedef enum WHV_REGISTER_NAME
{
    // X64 General purpose registers
    WHvX64RegisterRax              = 0x00000000,
    WHvX64RegisterRcx              = 0x00000001,
    WHvX64RegisterRdx              = 0x00000002,
    WHvX64RegisterRbx              = 0x00000003,
    WHvX64RegisterRsp              = 0x00000004,
    WHvX64RegisterRbp              = 0x00000005,
    WHvX64RegisterRsi              = 0x00000006,
    WHvX64RegisterRdi              = 0x00000007,
    WHvX64RegisterR8               = 0x00000008,
    WHvX64RegisterR9               = 0x00000009,
    WHvX64RegisterR10              = 0x0000000A,
    WHvX64RegisterR11              = 0x0000000B,
    WHvX64RegisterR12              = 0x0000000C,
    WHvX64RegisterR13              = 0x0000000D,
    WHvX64RegisterR14              = 0x0000000E,
    WHvX64RegisterR15              = 0x0000000F,
    WHvX64RegisterRip              = 0x00000010,
    WHvX64RegisterRflags           = 0x00000011,

    // X64 Segment registers
    WHvX64RegisterEs               = 0x00000012,
    WHvX64RegisterCs               = 0x00000013,
    WHvX64RegisterSs               = 0x00000014,
    WHvX64RegisterDs               = 0x00000015,
    WHvX64RegisterFs               = 0x00000016,
    WHvX64RegisterGs               = 0x00000017,
    WHvX64RegisterLdtr             = 0x00000018,
    WHvX64RegisterTr               = 0x00000019,

    // X64 Table registers
    WHvX64RegisterIdtr             = 0x0000001A,
    WHvX64RegisterGdtr             = 0x0000001B,

    // X64 Control Registers
    WHvX64RegisterCr0              = 0x0000001C,
    WHvX64RegisterCr2              = 0x0000001D,
    WHvX64RegisterCr3              = 0x0000001E,
    WHvX64RegisterCr4              = 0x0000001F,
    WHvX64RegisterCr8              = 0x00000020,

    // X64 Debug Registers
    WHvX64RegisterDr0              = 0x00000021,
    WHvX64RegisterDr1              = 0x00000022,
    WHvX64RegisterDr2              = 0x00000023,
    WHvX64RegisterDr3              = 0x00000024,
    WHvX64RegisterDr6              = 0x00000025,
    WHvX64RegisterDr7              = 0x00000026,

    // X64 Extended Control Registers
    WHvX64RegisterXCr0             = 0x00000027,

    // X64 Virtual Control Registers
    WHvX64RegisterVirtualCr0       = 0x00000028,
    WHvX64RegisterVirtualCr3       = 0x00000029,
    WHvX64RegisterVirtualCr4       = 0x0000002A,
    WHvX64RegisterVirtualCr8       = 0x0000002B,

    // X64 Floating Point and Vector Registers
    WHvX64RegisterXmm0             = 0x00001000,
    WHvX64RegisterXmm1             = 0x00001001,
    WHvX64RegisterXmm2             = 0x00001002,
    WHvX64RegisterXmm3             = 0x00001003,
    WHvX64RegisterXmm4             = 0x00001004,
    WHvX64RegisterXmm5             = 0x00001005,
    WHvX64RegisterXmm6             = 0x00001006,
    WHvX64RegisterXmm7             = 0x00001007,
    WHvX64RegisterXmm8             = 0x00001008,
    WHvX64RegisterXmm9             = 0x00001009,
    WHvX64RegisterXmm10            = 0x0000100A,
    WHvX64RegisterXmm11            = 0x0000100B,
    WHvX64RegisterXmm12            = 0x0000100C,
    WHvX64RegisterXmm13            = 0x0000100D,
    WHvX64RegisterXmm14            = 0x0000100E,
    WHvX64RegisterXmm15            = 0x0000100F,
    WHvX64RegisterFpMmx0           = 0x00001010,
    WHvX64RegisterFpMmx1           = 0x00001011,
    WHvX64RegisterFpMmx2           = 0x00001012,
    WHvX64RegisterFpMmx3           = 0x00001013,
    WHvX64RegisterFpMmx4           = 0x00001014,
    WHvX64RegisterFpMmx5           = 0x00001015,
    WHvX64RegisterFpMmx6           = 0x00001016,
    WHvX64RegisterFpMmx7           = 0x00001017,
    WHvX64RegisterFpControlStatus  = 0x00001018,
    WHvX64RegisterXmmControlStatus = 0x00001019,

    // X64 MSRs
    WHvX64RegisterTsc              = 0x00002000,
    WHvX64RegisterEfer             = 0x00002001,
    WHvX64RegisterKernelGsBase     = 0x00002002,
    WHvX64RegisterApicBase         = 0x00002003,
    WHvX64RegisterPat              = 0x00002004,
    WHvX64RegisterSysenterCs       = 0x00002005,
    WHvX64RegisterSysenterEip      = 0x00002006,
    WHvX64RegisterSysenterEsp      = 0x00002007,
    WHvX64RegisterStar             = 0x00002008,
    WHvX64RegisterLstar            = 0x00002009,
    WHvX64RegisterCstar            = 0x0000200A,
    WHvX64RegisterSfmask           = 0x0000200B,
    WHvX64RegisterInitialApicId    = 0x0000200C,

    WHvX64RegisterMsrMtrrCap         = 0x0000200D,
    WHvX64RegisterMsrMtrrDefType     = 0x0000200E,

    WHvX64RegisterMsrMtrrPhysBase0   = 0x00002010,
    WHvX64RegisterMsrMtrrPhysBase1   = 0x00002011,
    WHvX64RegisterMsrMtrrPhysBase2   = 0x00002012,
    WHvX64RegisterMsrMtrrPhysBase3   = 0x00002013,
    WHvX64RegisterMsrMtrrPhysBase4   = 0x00002014,
    WHvX64RegisterMsrMtrrPhysBase5   = 0x00002015,
    WHvX64RegisterMsrMtrrPhysBase6   = 0x00002016,
    WHvX64RegisterMsrMtrrPhysBase7   = 0x00002017,
    WHvX64RegisterMsrMtrrPhysBase8   = 0x00002018,
    WHvX64RegisterMsrMtrrPhysBase9   = 0x00002019,
    WHvX64RegisterMsrMtrrPhysBaseA   = 0x0000201A,
    WHvX64RegisterMsrMtrrPhysBaseB   = 0x0000201B,
    WHvX64RegisterMsrMtrrPhysBaseC   = 0x0000201C,
    WHvX64RegisterMsrMtrrPhysBaseD   = 0x0000201D,
    WHvX64RegisterMsrMtrrPhysBaseE   = 0x0000201E,
    WHvX64RegisterMsrMtrrPhysBaseF   = 0x0000201F,

    WHvX64RegisterMsrMtrrPhysMask0   = 0x00002040,
    WHvX64RegisterMsrMtrrPhysMask1   = 0x00002041,
    WHvX64RegisterMsrMtrrPhysMask2   = 0x00002042,
    WHvX64RegisterMsrMtrrPhysMask3   = 0x00002043,
    WHvX64RegisterMsrMtrrPhysMask4   = 0x00002044,
    WHvX64RegisterMsrMtrrPhysMask5   = 0x00002045,
    WHvX64RegisterMsrMtrrPhysMask6   = 0x00002046,
    WHvX64RegisterMsrMtrrPhysMask7   = 0x00002047,
    WHvX64RegisterMsrMtrrPhysMask8   = 0x00002048,
    WHvX64RegisterMsrMtrrPhysMask9   = 0x00002049,
    WHvX64RegisterMsrMtrrPhysMaskA   = 0x0000204A,
    WHvX64RegisterMsrMtrrPhysMaskB   = 0x0000204B,
    WHvX64RegisterMsrMtrrPhysMaskC   = 0x0000204C,
    WHvX64RegisterMsrMtrrPhysMaskD   = 0x0000204D,
    WHvX64RegisterMsrMtrrPhysMaskE   = 0x0000204E,
    WHvX64RegisterMsrMtrrPhysMaskF   = 0x0000204F,

    WHvX64RegisterMsrMtrrFix64k00000 = 0x00002070,
    WHvX64RegisterMsrMtrrFix16k80000 = 0x00002071,
    WHvX64RegisterMsrMtrrFix16kA0000 = 0x00002072,
    WHvX64RegisterMsrMtrrFix4kC0000  = 0x00002073,
    WHvX64RegisterMsrMtrrFix4kC8000  = 0x00002074,
    WHvX64RegisterMsrMtrrFix4kD0000  = 0x00002075,
    WHvX64RegisterMsrMtrrFix4kD8000  = 0x00002076,
    WHvX64RegisterMsrMtrrFix4kE0000  = 0x00002077,
    WHvX64RegisterMsrMtrrFix4kE8000  = 0x00002078,
    WHvX64RegisterMsrMtrrFix4kF0000  = 0x00002079,
    WHvX64RegisterMsrMtrrFix4kF8000  = 0x0000207A,

    WHvX64RegisterTscAux           = 0x0000207B,
    WHvX64RegisterBndcfgs          = 0x0000207C,
    WHvX64RegisterMCount           = 0x0000207E,
    WHvX64RegisterACount           = 0x0000207F,
    WHvX64RegisterSpecCtrl         = 0x00002084,
    WHvX64RegisterPredCmd          = 0x00002085,
    WHvX64RegisterTscVirtualOffset = 0x00002087,
    WHvX64RegisterTsxCtrl          = 0x00002088,
    WHvX64RegisterXss              = 0x0000208B,
    WHvX64RegisterUCet             = 0x0000208C,
    WHvX64RegisterSCet             = 0x0000208D,
    WHvX64RegisterSsp              = 0x0000208E,
    WHvX64RegisterPl0Ssp           = 0x0000208F,
    WHvX64RegisterPl1Ssp           = 0x00002090,
    WHvX64RegisterPl2Ssp           = 0x00002091,
    WHvX64RegisterPl3Ssp           = 0x00002092,
    WHvX64RegisterInterruptSspTableAddr = 0x00002093,
    WHvX64RegisterTscDeadline      = 0x00002095,
    WHvX64RegisterTscAdjust        = 0x00002096,
    WHvX64RegisterUmwaitControl    = 0x00002098,
    WHvX64RegisterXfd              = 0x00002099,
    WHvX64RegisterXfdErr           = 0x0000209A,

    // Feature control and nested capability MSRs
    WHvX64RegisterMsrIa32MiscEnable         = 0x000020A0,
    WHvX64RegisterIa32FeatureControl        = 0x000020A1,
    WHvX64RegisterIa32VmxBasic              = 0x000020A2,
    WHvX64RegisterIa32VmxPinbasedCtls       = 0x000020A3,
    WHvX64RegisterIa32VmxProcbasedCtls      = 0x000020A4,
    WHvX64RegisterIa32VmxExitCtls           = 0x000020A5,
    WHvX64RegisterIa32VmxEntryCtls          = 0x000020A6,
    WHvX64RegisterIa32VmxMisc               = 0x000020A7,
    WHvX64RegisterIa32VmxCr0Fixed0          = 0x000020A8,
    WHvX64RegisterIa32VmxCr0Fixed1          = 0x000020A9,
    WHvX64RegisterIa32VmxCr4Fixed0          = 0x000020AA,
    WHvX64RegisterIa32VmxCr4Fixed1          = 0x000020AB,
    WHvX64RegisterIa32VmxVmcsEnum           = 0x000020AC,
    WHvX64RegisterIa32VmxProcbasedCtls2     = 0x000020AD,
    WHvX64RegisterIa32VmxEptVpidCap         = 0x000020AE,
    WHvX64RegisterIa32VmxTruePinbasedCtls   = 0x000020AF,
    WHvX64RegisterIa32VmxTrueProcbasedCtls  = 0x000020B0,
    WHvX64RegisterIa32VmxTrueExitCtls       = 0x000020B1,
    WHvX64RegisterIa32VmxTrueEntryCtls      = 0x000020B2,
    WHvX64RegisterAmdVmHsavePa              = 0x000020B3,
    WHvX64RegisterAmdVmCr                   = 0x000020B4,

    // APIC state (also accessible via WHv(Get/Set)VirtualProcessorInterruptControllerState)
    WHvX64RegisterApicId           = 0x00003002,
    WHvX64RegisterApicVersion      = 0x00003003,
    // X2APIC state (also accessible via WHv(Get/Set)VirtualProcessorInterruptControllerState)
    WHvX64RegisterApicTpr          = 0x00003008,
    WHvX64RegisterApicPpr          = 0x0000300A,
    WHvX64RegisterApicEoi          = 0x0000300B,
    WHvX64RegisterApicLdr          = 0x0000300D,
    WHvX64RegisterApicSpurious     = 0x0000300F,
    WHvX64RegisterApicIsr0         = 0x00003010,
    WHvX64RegisterApicIsr1         = 0x00003011,
    WHvX64RegisterApicIsr2         = 0x00003012,
    WHvX64RegisterApicIsr3         = 0x00003013,
    WHvX64RegisterApicIsr4         = 0x00003014,
    WHvX64RegisterApicIsr5         = 0x00003015,
    WHvX64RegisterApicIsr6         = 0x00003016,
    WHvX64RegisterApicIsr7         = 0x00003017,
    WHvX64RegisterApicTmr0         = 0x00003018,
    WHvX64RegisterApicTmr1         = 0x00003019,
    WHvX64RegisterApicTmr2         = 0x0000301A,
    WHvX64RegisterApicTmr3         = 0x0000301B,
    WHvX64RegisterApicTmr4         = 0x0000301C,
    WHvX64RegisterApicTmr5         = 0x0000301D,
    WHvX64RegisterApicTmr6         = 0x0000301E,
    WHvX64RegisterApicTmr7         = 0x0000301F,
    WHvX64RegisterApicIrr0         = 0x00003020,
    WHvX64RegisterApicIrr1         = 0x00003021,
    WHvX64RegisterApicIrr2         = 0x00003022,
    WHvX64RegisterApicIrr3         = 0x00003023,
    WHvX64RegisterApicIrr4         = 0x00003024,
    WHvX64RegisterApicIrr5         = 0x00003025,
    WHvX64RegisterApicIrr6         = 0x00003026,
    WHvX64RegisterApicIrr7         = 0x00003027,
    WHvX64RegisterApicEse          = 0x00003028,
    WHvX64RegisterApicIcr          = 0x00003030,
    WHvX64RegisterApicLvtTimer     = 0x00003032,
    WHvX64RegisterApicLvtThermal   = 0x00003033,
    WHvX64RegisterApicLvtPerfmon   = 0x00003034,
    WHvX64RegisterApicLvtLint0     = 0x00003035,
    WHvX64RegisterApicLvtLint1     = 0x00003036,
    WHvX64RegisterApicLvtError     = 0x00003037,
    WHvX64RegisterApicInitCount    = 0x00003038,
    WHvX64RegisterApicCurrentCount = 0x00003039,
    WHvX64RegisterApicDivide       = 0x0000303E,
    WHvX64RegisterApicSelfIpi      = 0x0000303F,

    // Synic registers
    WHvRegisterSint0               = 0x00004000,
    WHvRegisterSint1               = 0x00004001,
    WHvRegisterSint2               = 0x00004002,
    WHvRegisterSint3               = 0x00004003,
    WHvRegisterSint4               = 0x00004004,
    WHvRegisterSint5               = 0x00004005,
    WHvRegisterSint6               = 0x00004006,
    WHvRegisterSint7               = 0x00004007,
    WHvRegisterSint8               = 0x00004008,
    WHvRegisterSint9               = 0x00004009,
    WHvRegisterSint10              = 0x0000400A,
    WHvRegisterSint11              = 0x0000400B,
    WHvRegisterSint12              = 0x0000400C,
    WHvRegisterSint13              = 0x0000400D,
    WHvRegisterSint14              = 0x0000400E,
    WHvRegisterSint15              = 0x0000400F,
    WHvRegisterScontrol            = 0x00004010,
    WHvRegisterSversion            = 0x00004011,
    WHvRegisterSiefp               = 0x00004012,
    WHvRegisterSimp                = 0x00004013,
    WHvRegisterEom                 = 0x00004014,

    // Hypervisor defined registers
    WHvRegisterVpRuntime           = 0x00005000,
    WHvX64RegisterHypercall        = 0x00005001,
    WHvRegisterGuestOsId           = 0x00005002,
    WHvRegisterVpAssistPage        = 0x00005013,
    WHvRegisterReferenceTsc        = 0x00005017,
    WHvRegisterReferenceTscSequence = 0x0000501A,
    WHvX64RegisterNestedGuestState  = 0x00005050,
    WHvX64RegisterNestedCurrentVmGpa = 0x00005051,
    WHvX64RegisterNestedVmxInvEpt   = 0x00005052,
    WHvX64RegisterNestedVmxInvVpid  = 0x00005053,

    // Interrupt / Event Registers
    WHvRegisterPendingInterruption = 0x80000000,
    WHvRegisterInterruptState      = 0x80000001,
    WHvRegisterPendingEvent        = 0x80000002,
    WHvRegisterPendingEvent1       = 0x80000003,
    WHvX64RegisterDeliverabilityNotifications = 0x80000004,
    WHvRegisterDeliverabilityNotifications = 0x80000004,
    WHvRegisterInternalActivityState = 0x80000005,
    WHvX64RegisterPendingDebugException = 0x80000006,
    WHvRegisterPendingEvent2       = 0x80000007,
    WHvRegisterPendingEvent3       = 0x80000008,

} WHV_REGISTER_NAME;
#elif defined (_ARM64_)
typedef enum WHV_REGISTER_NAME
{
    //
    // AArch64 System Register Descriptions: General-purpose registers
    //

    WHvArm64RegisterX0                = 0x00020000,
    WHvArm64RegisterX1                = 0x00020001,
    WHvArm64RegisterX2                = 0x00020002,
    WHvArm64RegisterX3                = 0x00020003,
    WHvArm64RegisterX4                = 0x00020004,
    WHvArm64RegisterX5                = 0x00020005,
    WHvArm64RegisterX6                = 0x00020006,
    WHvArm64RegisterX7                = 0x00020007,
    WHvArm64RegisterX8                = 0x00020008,
    WHvArm64RegisterX9                = 0x00020009,
    WHvArm64RegisterX10               = 0x0002000A,
    WHvArm64RegisterX11               = 0x0002000B,
    WHvArm64RegisterX12               = 0x0002000C,
    WHvArm64RegisterX13               = 0x0002000D,
    WHvArm64RegisterX14               = 0x0002000E,
    WHvArm64RegisterX15               = 0x0002000F,
    WHvArm64RegisterX16               = 0x00020010,
    WHvArm64RegisterX17               = 0x00020011,
    WHvArm64RegisterX18               = 0x00020012,
    WHvArm64RegisterX19               = 0x00020013,
    WHvArm64RegisterX20               = 0x00020014,
    WHvArm64RegisterX21               = 0x00020015,
    WHvArm64RegisterX22               = 0x00020016,
    WHvArm64RegisterX23               = 0x00020017,
    WHvArm64RegisterX24               = 0x00020018,
    WHvArm64RegisterX25               = 0x00020019,
    WHvArm64RegisterX26               = 0x0002001A,
    WHvArm64RegisterX27               = 0x0002001B,
    WHvArm64RegisterX28               = 0x0002001C,
    WHvArm64RegisterFp                = 0x0002001D,
    WHvArm64RegisterLr                = 0x0002001E,
    WHvArm64RegisterPc                = 0x00020022,

    //
    // AArch64 System Register Descriptions: Floating-point registers
    //

    WHvArm64RegisterQ0                = 0x00030000,
    WHvArm64RegisterQ1                = 0x00030001,
    WHvArm64RegisterQ2                = 0x00030002,
    WHvArm64RegisterQ3                = 0x00030003,
    WHvArm64RegisterQ4                = 0x00030004,
    WHvArm64RegisterQ5                = 0x00030005,
    WHvArm64RegisterQ6                = 0x00030006,
    WHvArm64RegisterQ7                = 0x00030007,
    WHvArm64RegisterQ8                = 0x00030008,
    WHvArm64RegisterQ9                = 0x00030009,
    WHvArm64RegisterQ10               = 0x0003000A,
    WHvArm64RegisterQ11               = 0x0003000B,
    WHvArm64RegisterQ12               = 0x0003000C,
    WHvArm64RegisterQ13               = 0x0003000D,
    WHvArm64RegisterQ14               = 0x0003000E,
    WHvArm64RegisterQ15               = 0x0003000F,
    WHvArm64RegisterQ16               = 0x00030010,
    WHvArm64RegisterQ17               = 0x00030011,
    WHvArm64RegisterQ18               = 0x00030012,
    WHvArm64RegisterQ19               = 0x00030013,
    WHvArm64RegisterQ20               = 0x00030014,
    WHvArm64RegisterQ21               = 0x00030015,
    WHvArm64RegisterQ22               = 0x00030016,
    WHvArm64RegisterQ23               = 0x00030017,
    WHvArm64RegisterQ24               = 0x00030018,
    WHvArm64RegisterQ25               = 0x00030019,
    WHvArm64RegisterQ26               = 0x0003001A,
    WHvArm64RegisterQ27               = 0x0003001B,
    WHvArm64RegisterQ28               = 0x0003001C,
    WHvArm64RegisterQ29               = 0x0003001D,
    WHvArm64RegisterQ30               = 0x0003001E,
    WHvArm64RegisterQ31               = 0x0003001F,

    //
    // AArch64 System Register Descriptions: Special-purpose registers
    //

    WHvArm64RegisterPstate            = 0x00020023,
    WHvArm64RegisterElrEl1            = 0x00040015,
    WHvArm64RegisterFpcr              = 0x00040012,
    WHvArm64RegisterFpsr              = 0x00040013,
    WHvArm64RegisterSp                = 0x0002001F,
    WHvArm64RegisterSpEl0             = 0x00020020,
    WHvArm64RegisterSpEl1             = 0x00020021,
    WHvArm64RegisterSpsrEl1           = 0x00040014,

    //
    // AArch64 System Register Descriptions: ID Registers
    //
    // ID registers are exposed as partition wide registers. A partition wide register can be
    // set by the parent using WHV_ANY_VP to change the value read by a guest. ID registers
    // have the following behavior:
    // -Parent write with WHV_ANY_VP - sets an override to replace the default value read by a guest.
    // -Parent read with WHV_ANY_VP - returns the override if one has been set, else the default value.
    // -Guest write\read with WHV_ANY_VP - not allowed.
    // -Parent\guest write\read with VP index - not allowed.
    //

    WHvArm64RegisterIdMidrEl1         = 0x00022000,
    WHvArm64RegisterIdRes01El1        = 0x00022001,
    WHvArm64RegisterIdRes02El1        = 0x00022002,
    WHvArm64RegisterIdRes03El1        = 0x00022003,
    WHvArm64RegisterIdRes04El1        = 0x00022004,
    WHvArm64RegisterIdMpidrEl1        = 0x00022005,
    WHvArm64RegisterIdRevidrEl1       = 0x00022006,
    WHvArm64RegisterIdRes07El1        = 0x00022007,
    WHvArm64RegisterIdPfr0El1         = 0x00022008,
    WHvArm64RegisterIdPfr1El1         = 0x00022009,
    WHvArm64RegisterIdDfr0El1         = 0x0002200A,
    WHvArm64RegisterIdRes13El1        = 0x0002200B,
    WHvArm64RegisterIdMmfr0El1        = 0x0002200C,
    WHvArm64RegisterIdMmfr1El1        = 0x0002200D,
    WHvArm64RegisterIdMmfr2El1        = 0x0002200E,
    WHvArm64RegisterIdMmfr3El1        = 0x0002200F,
    WHvArm64RegisterIdIsar0El1        = 0x00022010,
    WHvArm64RegisterIdIsar1El1        = 0x00022011,
    WHvArm64RegisterIdIsar2El1        = 0x00022012,
    WHvArm64RegisterIdIsar3El1        = 0x00022013,
    WHvArm64RegisterIdIsar4El1        = 0x00022014,
    WHvArm64RegisterIdIsar5El1        = 0x00022015,
    WHvArm64RegisterIdRes26El1        = 0x00022016,
    WHvArm64RegisterIdRes27El1        = 0x00022017,
    WHvArm64RegisterIdMvfr0El1        = 0x00022018,
    WHvArm64RegisterIdMvfr1El1        = 0x00022019,
    WHvArm64RegisterIdMvfr2El1        = 0x0002201A,
    WHvArm64RegisterIdRes33El1        = 0x0002201B,
    WHvArm64RegisterIdPfr2El1         = 0x0002201C,
    WHvArm64RegisterIdRes35El1        = 0x0002201D,
    WHvArm64RegisterIdRes36El1        = 0x0002201E,
    WHvArm64RegisterIdRes37El1        = 0x0002201F,
    WHvArm64RegisterIdAa64Pfr0El1     = 0x00022020,
    WHvArm64RegisterIdAa64Pfr1El1     = 0x00022021,
    WHvArm64RegisterIdAa64Pfr2El1     = 0x00022022,
    WHvArm64RegisterIdRes43El1        = 0x00022023,
    WHvArm64RegisterIdAa64Zfr0El1     = 0x00022024,
    WHvArm64RegisterIdAa64Smfr0El1    = 0x00022025,
    WHvArm64RegisterIdRes46El1        = 0x00022026,
    WHvArm64RegisterIdRes47El1        = 0x00022027,
    WHvArm64RegisterIdAa64Dfr0El1     = 0x00022028,
    WHvArm64RegisterIdAa64Dfr1El1     = 0x00022029,
    WHvArm64RegisterIdRes52El1        = 0x0002202A,
    WHvArm64RegisterIdRes53El1        = 0x0002202B,
    WHvArm64RegisterIdRes54El1        = 0x0002202C,
    WHvArm64RegisterIdRes55El1        = 0x0002202D,
    WHvArm64RegisterIdRes56El1        = 0x0002202E,
    WHvArm64RegisterIdRes57El1        = 0x0002202F,
    WHvArm64RegisterIdAa64Isar0El1    = 0x00022030,
    WHvArm64RegisterIdAa64Isar1El1    = 0x00022031,
    WHvArm64RegisterIdAa64Isar2El1    = 0x00022032,
    WHvArm64RegisterIdRes63El1        = 0x00022033,
    WHvArm64RegisterIdRes64El1        = 0x00022034,
    WHvArm64RegisterIdRes65El1        = 0x00022035,
    WHvArm64RegisterIdRes66El1        = 0x00022036,
    WHvArm64RegisterIdRes67El1        = 0x00022037,
    WHvArm64RegisterIdAa64Mmfr0El1     = 0x00022038,
    WHvArm64RegisterIdAa64Mmfr1El1    = 0x00022039,
    WHvArm64RegisterIdAa64Mmfr2El1    = 0x0002203A,
    WHvArm64RegisterIdAa64Mmfr3El1    = 0x0002203B,
    WHvArm64RegisterIdAa64Mmfr4El1    = 0x0002203C,
    WHvArm64RegisterIdRes75El1        = 0x0002203D,
    WHvArm64RegisterIdRes76El1        = 0x0002203E,
    WHvArm64RegisterIdRes77El1        = 0x0002203F,
    WHvArm64RegisterIdRes80El1        = 0x00022040,
    WHvArm64RegisterIdRes81El1        = 0x00022041,
    WHvArm64RegisterIdRes82El1        = 0x00022042,
    WHvArm64RegisterIdRes83El1        = 0x00022043,
    WHvArm64RegisterIdRes84El1        = 0x00022044,
    WHvArm64RegisterIdRes85El1        = 0x00022045,
    WHvArm64RegisterIdRes86El1        = 0x00022046,
    WHvArm64RegisterIdRes87El1        = 0x00022047,
    WHvArm64RegisterIdRes90El1        = 0x00022048,
    WHvArm64RegisterIdRes91El1        = 0x00022049,
    WHvArm64RegisterIdRes92El1        = 0x0002204A,
    WHvArm64RegisterIdRes93El1        = 0x0002204B,
    WHvArm64RegisterIdRes94El1        = 0x0002204C,
    WHvArm64RegisterIdRes95El1        = 0x0002204D,
    WHvArm64RegisterIdRes96El1        = 0x0002204E,
    WHvArm64RegisterIdRes97El1        = 0x0002204F,
    WHvArm64RegisterIdRes100El1       = 0x00022050,
    WHvArm64RegisterIdRes101El1       = 0x00022051,
    WHvArm64RegisterIdRes102El1       = 0x00022052,
    WHvArm64RegisterIdRes103El1       = 0x00022053,
    WHvArm64RegisterIdRes104El1       = 0x00022054,
    WHvArm64RegisterIdRes105El1       = 0x00022055,
    WHvArm64RegisterIdRes106El1       = 0x00022056,
    WHvArm64RegisterIdRes107El1       = 0x00022057,
    WHvArm64RegisterIdRes110El1       = 0x00022058,
    WHvArm64RegisterIdRes111El1       = 0x00022059,
    WHvArm64RegisterIdRes112El1       = 0x0002205A,
    WHvArm64RegisterIdRes113El1       = 0x0002205B,
    WHvArm64RegisterIdRes114El1       = 0x0002205C,
    WHvArm64RegisterIdRes115El1       = 0x0002205D,
    WHvArm64RegisterIdRes116El1       = 0x0002205E,
    WHvArm64RegisterIdRes117El1       = 0x0002205F,
    WHvArm64RegisterIdRes120El1       = 0x00022060,
    WHvArm64RegisterIdRes121El1       = 0x00022061,
    WHvArm64RegisterIdRes122El1       = 0x00022062,
    WHvArm64RegisterIdRes123El1       = 0x00022063,
    WHvArm64RegisterIdRes124El1       = 0x00022064,
    WHvArm64RegisterIdRes125El1       = 0x00022065,
    WHvArm64RegisterIdRes126El1       = 0x00022066,
    WHvArm64RegisterIdRes127El1       = 0x00022067,
    WHvArm64RegisterIdRes130El1       = 0x00022068,
    WHvArm64RegisterIdRes131El1       = 0x00022069,
    WHvArm64RegisterIdRes132El1       = 0x0002206A,
    WHvArm64RegisterIdRes133El1       = 0x0002206B,
    WHvArm64RegisterIdRes134El1       = 0x0002206C,
    WHvArm64RegisterIdRes135El1       = 0x0002206D,
    WHvArm64RegisterIdRes136El1       = 0x0002206E,
    WHvArm64RegisterIdRes137El1       = 0x0002206F,
    WHvArm64RegisterIdRes140El1       = 0x00022070,
    WHvArm64RegisterIdRes141El1       = 0x00022071,
    WHvArm64RegisterIdRes142El1       = 0x00022072,
    WHvArm64RegisterIdRes143El1       = 0x00022073,
    WHvArm64RegisterIdRes144El1       = 0x00022074,
    WHvArm64RegisterIdRes145El1       = 0x00022075,
    WHvArm64RegisterIdRes146El1       = 0x00022076,
    WHvArm64RegisterIdRes147El1       = 0x00022077,
    WHvArm64RegisterIdRes150El1       = 0x00022078,
    WHvArm64RegisterIdRes151El1       = 0x00022079,
    WHvArm64RegisterIdRes152El1       = 0x0002207A,
    WHvArm64RegisterIdRes153El1       = 0x0002207B,
    WHvArm64RegisterIdRes154El1       = 0x0002207C,
    WHvArm64RegisterIdRes155El1       = 0x0002207D,
    WHvArm64RegisterIdRes156El1       = 0x0002207E,
    WHvArm64RegisterIdRes157El1       = 0x0002207F,

    //
    // AArch64 System Register Descriptions: General system control registers
    //

    WHvArm64RegisterActlrEl1          = 0x00040003,
    WHvArm64RegisterApdAKeyHiEl1      = 0x00040026,
    WHvArm64RegisterApdAKeyLoEl1      = 0x00040027,
    WHvArm64RegisterApdBKeyHiEl1      = 0x00040028,
    WHvArm64RegisterApdBKeyLoEl1      = 0x00040029,
    WHvArm64RegisterApgAKeyHiEl1      = 0x0004002A,
    WHvArm64RegisterApgAKeyLoEl1      = 0x0004002B,
    WHvArm64RegisterApiAKeyHiEl1      = 0x0004002C,
    WHvArm64RegisterApiAKeyLoEl1      = 0x0004002D,
    WHvArm64RegisterApiBKeyHiEl1      = 0x0004002E,
    WHvArm64RegisterApiBKeyLoEl1      = 0x0004002F,
    WHvArm64RegisterContextidrEl1     = 0x0004000D,
    WHvArm64RegisterCpacrEl1          = 0x00040004,
    WHvArm64RegisterCsselrEl1         = 0x00040035,
    WHvArm64RegisterEsrEl1            = 0x00040008,
    WHvArm64RegisterFarEl1            = 0x00040009,
    WHvArm64RegisterMairEl1           = 0x0004000B,
    WHvArm64RegisterMidrEl1           = 0x00040051,
    WHvArm64RegisterMpidrEl1          = 0x00040001,
    WHvArm64RegisterParEl1            = 0x0004000A,
    WHvArm64RegisterSctlrEl1          = 0x00040002,
    WHvArm64RegisterTcrEl1            = 0x00040007,
    WHvArm64RegisterTpidrEl0          = 0x00040011,
    WHvArm64RegisterTpidrEl1          = 0x0004000E,
    WHvArm64RegisterTpidrroEl0        = 0x00040010,
    WHvArm64RegisterTtbr0El1          = 0x00040005,
    WHvArm64RegisterTtbr1El1          = 0x00040006,
    WHvArm64RegisterVbarEl1           = 0x0004000C,
    WHvArm64RegisterZcrEl1            = 0x00040071,

    //
    // AArch64 System Register Descriptions: Debug Registers
    //

    WHvArm64RegisterDbgbcr0El1        = 0x00050000,
    WHvArm64RegisterDbgbcr1El1        = 0x00050001,
    WHvArm64RegisterDbgbcr2El1        = 0x00050002,
    WHvArm64RegisterDbgbcr3El1        = 0x00050003,
    WHvArm64RegisterDbgbcr4El1        = 0x00050004,
    WHvArm64RegisterDbgbcr5El1        = 0x00050005,
    WHvArm64RegisterDbgbcr6El1        = 0x00050006,
    WHvArm64RegisterDbgbcr7El1        = 0x00050007,
    WHvArm64RegisterDbgbcr8El1        = 0x00050008,
    WHvArm64RegisterDbgbcr9El1        = 0x00050009,
    WHvArm64RegisterDbgbcr10El1       = 0x0005000A,
    WHvArm64RegisterDbgbcr11El1       = 0x0005000B,
    WHvArm64RegisterDbgbcr12El1       = 0x0005000C,
    WHvArm64RegisterDbgbcr13El1       = 0x0005000D,
    WHvArm64RegisterDbgbcr14El1       = 0x0005000E,
    WHvArm64RegisterDbgbcr15El1       = 0x0005000F,
    WHvArm64RegisterDbgbvr0El1        = 0x00050020,
    WHvArm64RegisterDbgbvr1El1        = 0x00050021,
    WHvArm64RegisterDbgbvr2El1        = 0x00050022,
    WHvArm64RegisterDbgbvr3El1        = 0x00050023,
    WHvArm64RegisterDbgbvr4El1        = 0x00050024,
    WHvArm64RegisterDbgbvr5El1        = 0x00050025,
    WHvArm64RegisterDbgbvr6El1        = 0x00050026,
    WHvArm64RegisterDbgbvr7El1        = 0x00050027,
    WHvArm64RegisterDbgbvr8El1        = 0x00050028,
    WHvArm64RegisterDbgbvr9El1        = 0x00050029,
    WHvArm64RegisterDbgbvr10El1       = 0x0005002A,
    WHvArm64RegisterDbgbvr11El1       = 0x0005002B,
    WHvArm64RegisterDbgbvr12El1       = 0x0005002C,
    WHvArm64RegisterDbgbvr13El1       = 0x0005002D,
    WHvArm64RegisterDbgbvr14El1       = 0x0005002E,
    WHvArm64RegisterDbgbvr15El1       = 0x0005002F,
    WHvArm64RegisterDbgprcrEl1        = 0x00050045,
    WHvArm64RegisterDbgwcr0El1        = 0x00050010,
    WHvArm64RegisterDbgwcr1El1        = 0x00050011,
    WHvArm64RegisterDbgwcr2El1        = 0x00050012,
    WHvArm64RegisterDbgwcr3El1        = 0x00050013,
    WHvArm64RegisterDbgwcr4El1        = 0x00050014,
    WHvArm64RegisterDbgwcr5El1        = 0x00050015,
    WHvArm64RegisterDbgwcr6El1        = 0x00050016,
    WHvArm64RegisterDbgwcr7El1        = 0x00050017,
    WHvArm64RegisterDbgwcr8El1        = 0x00050018,
    WHvArm64RegisterDbgwcr9El1        = 0x00050019,
    WHvArm64RegisterDbgwcr10El1       = 0x0005001A,
    WHvArm64RegisterDbgwcr11El1       = 0x0005001B,
    WHvArm64RegisterDbgwcr12El1       = 0x0005001C,
    WHvArm64RegisterDbgwcr13El1       = 0x0005001D,
    WHvArm64RegisterDbgwcr14El1       = 0x0005001E,
    WHvArm64RegisterDbgwcr15El1       = 0x0005001F,
    WHvArm64RegisterDbgwvr0El1        = 0x00050030,
    WHvArm64RegisterDbgwvr1El1        = 0x00050031,
    WHvArm64RegisterDbgwvr2El1        = 0x00050032,
    WHvArm64RegisterDbgwvr3El1        = 0x00050033,
    WHvArm64RegisterDbgwvr4El1        = 0x00050034,
    WHvArm64RegisterDbgwvr5El1        = 0x00050035,
    WHvArm64RegisterDbgwvr6El1        = 0x00050036,
    WHvArm64RegisterDbgwvr7El1        = 0x00050037,
    WHvArm64RegisterDbgwvr8El1        = 0x00050038,
    WHvArm64RegisterDbgwvr9El1        = 0x00050039,
    WHvArm64RegisterDbgwvr10El1       = 0x0005003A,
    WHvArm64RegisterDbgwvr11El1       = 0x0005003B,
    WHvArm64RegisterDbgwvr12El1       = 0x0005003C,
    WHvArm64RegisterDbgwvr13El1       = 0x0005003D,
    WHvArm64RegisterDbgwvr14El1       = 0x0005003E,
    WHvArm64RegisterDbgwvr15El1       = 0x0005003F,
    WHvArm64RegisterMdrarEl1          = 0x0005004C,
    WHvArm64RegisterMdscrEl1          = 0x0005004D,
    WHvArm64RegisterOsdlrEl1          = 0x0005004E,
    WHvArm64RegisterOslarEl1          = 0x00050052,
    WHvArm64RegisterOslsrEl1          = 0x00050053,

    //
    // AArch64 System Register Descriptions: Performance Monitors Registers
    //

    WHvArm64RegisterPmccfiltrEl0      = 0x00052000,
    WHvArm64RegisterPmccntrEl0        = 0x00052001,
    WHvArm64RegisterPmceid0El0        = 0x00052002,
    WHvArm64RegisterPmceid1El0        = 0x00052003,
    WHvArm64RegisterPmcntenclrEl0     = 0x00052004,
    WHvArm64RegisterPmcntensetEl0     = 0x00052005,
    WHvArm64RegisterPmcrEl0           = 0x00052006,
    WHvArm64RegisterPmevcntr0El0      = 0x00052007,
    WHvArm64RegisterPmevcntr1El0      = 0x00052008,
    WHvArm64RegisterPmevcntr2El0      = 0x00052009,
    WHvArm64RegisterPmevcntr3El0      = 0x0005200A,
    WHvArm64RegisterPmevcntr4El0      = 0x0005200B,
    WHvArm64RegisterPmevcntr5El0      = 0x0005200C,
    WHvArm64RegisterPmevcntr6El0      = 0x0005200D,
    WHvArm64RegisterPmevcntr7El0      = 0x0005200E,
    WHvArm64RegisterPmevcntr8El0      = 0x0005200F,
    WHvArm64RegisterPmevcntr9El0      = 0x00052010,
    WHvArm64RegisterPmevcntr10El0     = 0x00052011,
    WHvArm64RegisterPmevcntr11El0     = 0x00052012,
    WHvArm64RegisterPmevcntr12El0     = 0x00052013,
    WHvArm64RegisterPmevcntr13El0     = 0x00052014,
    WHvArm64RegisterPmevcntr14El0     = 0x00052015,
    WHvArm64RegisterPmevcntr15El0     = 0x00052016,
    WHvArm64RegisterPmevcntr16El0     = 0x00052017,
    WHvArm64RegisterPmevcntr17El0     = 0x00052018,
    WHvArm64RegisterPmevcntr18El0     = 0x00052019,
    WHvArm64RegisterPmevcntr19El0     = 0x0005201A,
    WHvArm64RegisterPmevcntr20El0     = 0x0005201B,
    WHvArm64RegisterPmevcntr21El0     = 0x0005201C,
    WHvArm64RegisterPmevcntr22El0     = 0x0005201D,
    WHvArm64RegisterPmevcntr23El0     = 0x0005201E,
    WHvArm64RegisterPmevcntr24El0     = 0x0005201F,
    WHvArm64RegisterPmevcntr25El0     = 0x00052020,
    WHvArm64RegisterPmevcntr26El0     = 0x00052021,
    WHvArm64RegisterPmevcntr27El0     = 0x00052022,
    WHvArm64RegisterPmevcntr28El0     = 0x00052023,
    WHvArm64RegisterPmevcntr29El0     = 0x00052024,
    WHvArm64RegisterPmevcntr30El0     = 0x00052025,
    WHvArm64RegisterPmevtyper0El0     = 0x00052026,
    WHvArm64RegisterPmevtyper1El0     = 0x00052027,
    WHvArm64RegisterPmevtyper2El0     = 0x00052028,
    WHvArm64RegisterPmevtyper3El0     = 0x00052029,
    WHvArm64RegisterPmevtyper4El0     = 0x0005202A,
    WHvArm64RegisterPmevtyper5El0     = 0x0005202B,
    WHvArm64RegisterPmevtyper6El0     = 0x0005202C,
    WHvArm64RegisterPmevtyper7El0     = 0x0005202D,
    WHvArm64RegisterPmevtyper8El0     = 0x0005202E,
    WHvArm64RegisterPmevtyper9El0     = 0x0005202F,
    WHvArm64RegisterPmevtyper10El0    = 0x00052030,
    WHvArm64RegisterPmevtyper11El0    = 0x00052031,
    WHvArm64RegisterPmevtyper12El0    = 0x00052032,
    WHvArm64RegisterPmevtyper13El0    = 0x00052033,
    WHvArm64RegisterPmevtyper14El0    = 0x00052034,
    WHvArm64RegisterPmevtyper15El0    = 0x00052035,
    WHvArm64RegisterPmevtyper16El0    = 0x00052036,
    WHvArm64RegisterPmevtyper17El0    = 0x00052037,
    WHvArm64RegisterPmevtyper18El0    = 0x00052038,
    WHvArm64RegisterPmevtyper19El0    = 0x00052039,
    WHvArm64RegisterPmevtyper20El0    = 0x0005203A,
    WHvArm64RegisterPmevtyper21El0    = 0x0005203B,
    WHvArm64RegisterPmevtyper22El0    = 0x0005203C,
    WHvArm64RegisterPmevtyper23El0    = 0x0005203D,
    WHvArm64RegisterPmevtyper24El0    = 0x0005203E,
    WHvArm64RegisterPmevtyper25El0    = 0x0005203F,
    WHvArm64RegisterPmevtyper26El0    = 0x00052040,
    WHvArm64RegisterPmevtyper27El0    = 0x00052041,
    WHvArm64RegisterPmevtyper28El0    = 0x00052042,
    WHvArm64RegisterPmevtyper29El0    = 0x00052043,
    WHvArm64RegisterPmevtyper30El0    = 0x00052044,
    WHvArm64RegisterPmintenclrEl1     = 0x00052045,
    WHvArm64RegisterPmintensetEl1     = 0x00052046,
    WHvArm64RegisterPmovsclrEl0       = 0x00052048,
    WHvArm64RegisterPmovssetEl0       = 0x00052049,
    WHvArm64RegisterPmselrEl0         = 0x0005204A,
    WHvArm64RegisterPmuserenrEl0      = 0x0005204C,

    //
    // AArch64 System Register Descriptions: Generic Timer Registers
    //

    WHvArm64RegisterCntkctlEl1        = 0x00058008,
    WHvArm64RegisterCntvCtlEl0        = 0x0005800E,
    WHvArm64RegisterCntvCvalEl0       = 0x0005800F,
    WHvArm64RegisterCntvctEl0         = 0x00058011,

    //
    // ARM GIC (System Registers): The GIC Redistributor
    //

    WHvArm64RegisterGicrBaseGpa    = 0x00063000,

    // Synic registers
    WHvRegisterSint0               = 0x000A0000,
    WHvRegisterSint1               = 0x000A0001,
    WHvRegisterSint2               = 0x000A0002,
    WHvRegisterSint3               = 0x000A0003,
    WHvRegisterSint4               = 0x000A0004,
    WHvRegisterSint5               = 0x000A0005,
    WHvRegisterSint6               = 0x000A0006,
    WHvRegisterSint7               = 0x000A0007,
    WHvRegisterSint8               = 0x000A0008,
    WHvRegisterSint9               = 0x000A0009,
    WHvRegisterSint10              = 0x000A000A,
    WHvRegisterSint11              = 0x000A000B,
    WHvRegisterSint12              = 0x000A000C,
    WHvRegisterSint13              = 0x000A000D,
    WHvRegisterSint14              = 0x000A000E,
    WHvRegisterSint15              = 0x000A000F,
    WHvRegisterScontrol            = 0x000A0010,
    WHvRegisterSversion            = 0x000A0011,
    WHvRegisterSifp                = 0x000A0012,
    WHvRegisterSipp                = 0x000A0013,
    WHvRegisterEom                 = 0x000A0014,

    // Hypervisor defined registers

    // Hypervisor synthetic CPUID leaves are exposed as partition wide registers. A partition wide
    // register can be set by the parent using WHV_ANY_VP to change the value read by a guest. Hypervisor
    // synthetic CPUID leaf registers have the following behavior:
    // -Parent write with WHV_ANY_VP - sets an override to replace the default value read by a guest.
    // -Parent read with WHV_ANY_VP - returns the override if one has been set, else the default value.
    // -Guest write\read with WHV_ANY_VP - not allowed.
    // -Parent\guest write with VP index - not allowed.
    // -Parent\guest read with VP index - returns the override if one has been set, else the default value.
    //
    // Version
    WHvRegisterHypervisorVersion            = 0x00000100,   // 128-bit result same as CPUID 0x40000002

    // Feature Access
    WHvRegisterPrivilegesAndFeaturesInfo    = 0x00000200,   // 128-bit result same as CPUID 0x40000003
    WHvRegisterFeaturesInfo                 = 0x00000201,   // 128-bit result same as CPUID 0x40000004
    WHvRegisterImplementationLimitsInfo     = 0x00000202,   // 128-bit result same as CPUID 0x40000005
    WHvRegisterHardwareFeaturesInfo         = 0x00000203,   // 128-bit result same as CPUID 0x40000006
    WHvRegisterCpuManagementFeaturesInfo    = 0x00000204,   // 128-bit result same as CPUID 0x40000007

    // Guest Crash Registers
    WHvRegisterGuestCrashP0        = 0x00000210,
    WHvRegisterGuestCrashP1        = 0x00000211,
    WHvRegisterGuestCrashP2        = 0x00000212,
    WHvRegisterGuestCrashP3        = 0x00000213,
    WHvRegisterGuestCrashP4        = 0x00000214,
    WHvRegisterGuestCrashCtl       = 0x00000215,

    WHvRegisterVpRuntime           = 0x00090000,

    WHvRegisterGuestOsId           = 0x00090002,
    WHvRegisterVpAssistPage        = 0x00090013,
    WHvArm64RegisterPartitionInfoPage = 0x00090015,
    WHvRegisterReferenceTsc        = 0x00090017,
    WHvRegisterReferenceTscSequence = 0x0009001A,

    WHvRegisterPendingEvent0       = 0x00010004,
    WHvRegisterPendingEvent1       = 0x00010005,
    WHvRegisterDeliverabilityNotifications = 0x00010006,
    WHvRegisterInternalActivityState = 0x00000004,
    WHvRegisterPendingEvent2       = 0x00010008,
    WHvRegisterPendingEvent3       = 0x00010009,
} WHV_REGISTER_NAME;

#define WHvRegisterSiefp WHvRegisterSifp
#define WHvRegisterSimp WHvRegisterSipp
#define WHvRegisterPendingEvent WHvRegisterPendingEvent0

#endif // _ARCH_

typedef union DECLSPEC_ALIGN(16) WHV_UINT128
{
    struct
    {
        UINT64  Low64;
        UINT64  High64;
    };

    UINT32  Dword[4];
} WHV_UINT128;

typedef union WHV_X64_FP_REGISTER
{
    struct
    {
        UINT64 Mantissa;
        UINT64 BiasedExponent:15;
        UINT64 Sign:1;
        UINT64 Reserved:48;
    };

    WHV_UINT128 AsUINT128;
} WHV_X64_FP_REGISTER;

typedef union WHV_X64_FP_CONTROL_STATUS_REGISTER
{
    struct
    {
        UINT16 FpControl;
        UINT16 FpStatus;
        UINT8  FpTag;
        UINT8  Reserved;
        UINT16 LastFpOp;
        union
        {
            // Long Mode
            UINT64 LastFpRip;

            // 32 Bit Mode
            struct
            {
                UINT32 LastFpEip;
                UINT16 LastFpCs;
                UINT16 Reserved2;
            };
        };
    };

    WHV_UINT128 AsUINT128;
} WHV_X64_FP_CONTROL_STATUS_REGISTER;

typedef union WHV_X64_XMM_CONTROL_STATUS_REGISTER
{
    struct
    {
        union
        {
            // Long Mode
            UINT64 LastFpRdp;

            // 32 Bit Mode
            struct
            {
                UINT32 LastFpDp;
                UINT16 LastFpDs;
                UINT16 Reserved;
            };
        };
        UINT32 XmmStatusControl;
        UINT32 XmmStatusControlMask;
    };

    WHV_UINT128 AsUINT128;
} WHV_X64_XMM_CONTROL_STATUS_REGISTER;

typedef struct WHV_X64_SEGMENT_REGISTER
{
    UINT64 Base;
    UINT32 Limit;
    UINT16 Selector;

    union
    {
        struct
        {
            UINT16 SegmentType:4;
            UINT16 NonSystemSegment:1;
            UINT16 DescriptorPrivilegeLevel:2;
            UINT16 Present:1;
            UINT16 Reserved:4;
            UINT16 Available:1;
            UINT16 Long:1;
            UINT16 Default:1;
            UINT16 Granularity:1;
        };

        UINT16 Attributes;
    };
} WHV_X64_SEGMENT_REGISTER;

typedef struct WHV_X64_TABLE_REGISTER
{
    UINT16     Pad[3];
    UINT16     Limit;
    UINT64     Base;
} WHV_X64_TABLE_REGISTER;

typedef union WHV_X64_INTERRUPT_STATE_REGISTER
{
    struct
    {
        UINT64 InterruptShadow:1;
        UINT64 NmiMasked:1;
        UINT64 Reserved:62;
    };

    UINT64 AsUINT64;
} WHV_X64_INTERRUPT_STATE_REGISTER;

typedef union WHV_X64_PENDING_INTERRUPTION_REGISTER
{
    struct
    {
        UINT32 InterruptionPending:1;
        UINT32 InterruptionType:3;  // WHV_X64_PENDING_INTERRUPTION_TYPE
        UINT32 DeliverErrorCode:1;
        UINT32 InstructionLength:4;
        UINT32 NestedEvent:1;
        UINT32 Reserved:6;
        UINT32 InterruptionVector:16;
        UINT32 ErrorCode;
    };

    UINT64 AsUINT64;
} WHV_X64_PENDING_INTERRUPTION_REGISTER;

typedef union WHV_DELIVERABILITY_NOTIFICATIONS_REGISTER
{
    struct
    {
#if defined(_AMD64_)
        UINT64 NmiNotification:1;
        UINT64 InterruptNotification:1;
        UINT64 InterruptPriority:4;
        UINT64 Reserved:42;
#elif defined(_ARM64_)
        UINT64 Reserved:48;
#endif
        UINT64 Sint:16;
    };

    UINT64 AsUINT64;
} WHV_DELIVERABILITY_NOTIFICATIONS_REGISTER, WHV_X64_DELIVERABILITY_NOTIFICATIONS_REGISTER;

typedef union WHV_INTERNAL_ACTIVITY_REGISTER
{
    struct
    {
        UINT64 StartupSuspend : 1;
        UINT64 HaltSuspend : 1;
        UINT64 IdleSuspend : 1;
        UINT64 Reserved:61;
    };

    UINT64 AsUINT64;
} WHV_INTERNAL_ACTIVITY_REGISTER;

typedef enum WHV_X64_PENDING_EVENT_TYPE
{
    WHvX64PendingEventException = 0,
    WHvX64PendingEventExtInt    = 5,
    WHvX64PendingEventSvmNestedExit = 7,
    WHvX64PendingEventVmxNestedExit = 8,
} WHV_X64_PENDING_EVENT_TYPE;

typedef union WHV_X64_PENDING_EXCEPTION_EVENT
{
    struct
    {
        UINT32 EventPending         : 1;
        UINT32 EventType            : 3; // Must be WHvX64PendingEventException
        UINT32 Reserved0            : 4;

        UINT32 DeliverErrorCode     : 1;
        UINT32 Reserved1            : 7;
        UINT32 Vector               : 16;
        UINT32 ErrorCode;
        UINT64 ExceptionParameter;
    };

    WHV_UINT128 AsUINT128;
} WHV_X64_PENDING_EXCEPTION_EVENT;

typedef union WHV_X64_PENDING_EXT_INT_EVENT
{
    struct
    {
        UINT64 EventPending     : 1;
        UINT64 EventType        : 3; // Must be WHvX64PendingEventExtInt
        UINT64 Reserved0        : 4;
        UINT64 Vector           : 8;
        UINT64 Reserved1        : 48;

        UINT64 Reserved2;
    };

    WHV_UINT128 AsUINT128;
} WHV_X64_PENDING_EXT_INT_EVENT;

//
// Register values
//
typedef union WHV_REGISTER_VALUE
{
    WHV_UINT128 Reg128;
    UINT64 Reg64;
    UINT32 Reg32;
    UINT16 Reg16;
    UINT8 Reg8;
    WHV_INTERNAL_ACTIVITY_REGISTER InternalActivity;
    WHV_DELIVERABILITY_NOTIFICATIONS_REGISTER DeliverabilityNotifications;

#if defined(_AMD64_)
    WHV_X64_FP_REGISTER Fp;
    WHV_X64_FP_CONTROL_STATUS_REGISTER FpControlStatus;
    WHV_X64_XMM_CONTROL_STATUS_REGISTER XmmControlStatus;
    WHV_X64_SEGMENT_REGISTER Segment;
    WHV_X64_TABLE_REGISTER Table;
    WHV_X64_INTERRUPT_STATE_REGISTER InterruptState;
    WHV_X64_PENDING_INTERRUPTION_REGISTER PendingInterruption;
    WHV_X64_PENDING_EXCEPTION_EVENT ExceptionEvent;
    WHV_X64_PENDING_EXT_INT_EVENT ExtIntEvent;
    WHV_X64_PENDING_DEBUG_EXCEPTION PendingDebugException;
    WHV_X64_NESTED_GUEST_STATE NestedState;
    WHV_X64_NESTED_INVEPT_REGISTER InvEpt;
    WHV_X64_NESTED_INVVPID_REGISTER InvVpid;
    WHV_X64_PENDING_SVM_NESTED_EXIT_EVENT0 SvmNestedExit0;
    WHV_X64_PENDING_SVM_NESTED_EXIT_EVENT1 SvmNestedExit1;
    WHV_X64_PENDING_SVM_NESTED_EXIT_EVENT2 SvmNestedExit2;
    WHV_X64_PENDING_SVM_NESTED_EXIT_EVENT3 SvmNestedExit3;
    WHV_X64_PENDING_VMX_NESTED_EXIT_EVENT0 VmxNestedExit0;
    WHV_X64_PENDING_VMX_NESTED_EXIT_EVENT1 VmxNestedExit1;
    WHV_X64_PENDING_VMX_NESTED_EXIT_EVENT2 VmxNestedExit2;
#endif
} WHV_REGISTER_VALUE;

//
// Virtual processor creation properties (WHvCreateVirtualProcessor2)
//
typedef enum WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE
{
    WHvVirtualProcessorPropertyCodeNumaNode = 0x00000000,
} WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE;

typedef struct WHV_VIRTUAL_PROCESSOR_PROPERTY
{
    WHV_VIRTUAL_PROCESSOR_PROPERTY_CODE PropertyCode;
    UINT32 Reserved;
    union
    {
        USHORT NumaNode;
        UINT64 Padding;
    };
} WHV_VIRTUAL_PROCESSOR_PROPERTY;

//
// Virtual processor state categories (WHvGetVirtualProcessorState / WHvSetVirtualProcessorState)
//
#if defined(_AMD64_)

typedef enum WHV_VIRTUAL_PROCESSOR_STATE_TYPE
{
    WHvVirtualProcessorStateTypeSynicMessagePage          = 0x00000000,
    WHvVirtualProcessorStateTypeSynicEventFlagPage        = 0x00000001,
    WHvVirtualProcessorStateTypeSynicTimerState           = 0x00000002,

    WHvVirtualProcessorStateTypeInterruptControllerState2 = 0x00001000,
    WHvVirtualProcessorStateTypeXsaveState                = 0x00001001,
    WHvVirtualProcessorStateTypeNestedState               = 0x00001002,
} WHV_VIRTUAL_PROCESSOR_STATE_TYPE;

#elif defined (_ARM64_)

#define WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN    (1ui32 << 31)
#define WHV_VIRTUAL_PROCESSOR_STATE_TYPE_ANY_VP (1ui32 << 30)

typedef enum WHV_VIRTUAL_PROCESSOR_STATE_TYPE
{
    WHvVirtualProcessorStateTypeInterruptControllerState  = 0x00000000 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,

    WHvVirtualProcessorStateTypeSynicMessagePage          = 0x00000002 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
    WHvVirtualProcessorStateTypeSynicEventFlagPage        = 0x00000003 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
    WHvVirtualProcessorStateTypeSynicTimerState           = 0x00000004,

    WHvVirtualProcessorStateTypeGlobalInterruptState      = 0x00000006 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_ANY_VP,
    WHvVirtualProcessorStateTypeSveState                  = 0x00000007 | WHV_VIRTUAL_PROCESSOR_STATE_TYPE_PFN,
} WHV_VIRTUAL_PROCESSOR_STATE_TYPE;

#endif

//
// Synic event and message parameters (WHvSignalVirtualProcessorSynicEvent / WHvPostVirtualProcessorSynicMessage)
//
typedef UINT8 WHV_VTL;

typedef struct WHV_SYNIC_EVENT_PARAMETERS
{
    UINT32 VpIndex;
    UINT8 TargetSint;
    WHV_VTL TargetVtl;
    UINT16 FlagNumber;
} WHV_SYNIC_EVENT_PARAMETERS;

#define WHV_SYNIC_MESSAGE_SIZE  256

//
// CPUID output (WHvGetVirtualProcessorCpuidOutput) and CPUID result list element
//
typedef struct WHV_CPUID_OUTPUT
{
    UINT32 Eax;
    UINT32 Ebx;
    UINT32 Ecx;
    UINT32 Edx;
} WHV_CPUID_OUTPUT;

typedef enum WHV_X64_CPUID_RESULT2_FLAGS
{
    WHvX64CpuidResult2FlagSubleafSpecific   = 0x00000001,
    WHvX64CpuidResult2FlagVpSpecific        = 0x00000002,
} WHV_X64_CPUID_RESULT2_FLAGS;

// Enables bitwise operators on the WHV_X64_CPUID_RESULT2_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_X64_CPUID_RESULT2_FLAGS);

typedef struct WHV_X64_CPUID_RESULT2
{
    UINT32 Function;
    UINT32 Index;
    UINT32 VpIndex;
    WHV_X64_CPUID_RESULT2_FLAGS Flags;
    WHV_CPUID_OUTPUT Output;
    WHV_CPUID_OUTPUT Mask;
} WHV_X64_CPUID_RESULT2;

//
// Interrupt destination mode (WHvGetInterruptTargetVpSet)
//
typedef enum WHV_INTERRUPT_DESTINATION_MODE
{
    WHvX64InterruptDestinationModePhysical,
    WHvX64InterruptDestinationModeLogical,
} WHV_INTERRUPT_DESTINATION_MODE;
```

## Remarks

These data types define the virtual processor registers and the supporting types used to create virtual processors, save and restore virtual processor state, signal the synthetic interrupt controller (SynIC), and query CPUID and interrupt routing.

## See also

- [`WHvGetVirtualProcessorRegisters`](WHvGetVirtualProcessorRegisters.md)
- [`WHvSetVirtualProcessorRegisters`](WHvSetVirtualProcessorRegisters.md)
- [`WHvCreateVirtualProcessor2`](WHvCreateVirtualProcessor2.md)
- [`WHvGetVirtualProcessorState`](WHvGetVirtualProcessorState.md)
- [`WHvSetVirtualProcessorState`](WHvSetVirtualProcessorState.md)
- [`WHvSignalVirtualProcessorSynicEvent`](WHvSignalVirtualProcessorSynicEvent.md)
- [`WHvPostVirtualProcessorSynicMessage`](WHvPostVirtualProcessorSynicMessage.md)
- [`WHvGetVirtualProcessorCpuidOutput`](WHvGetVirtualProcessorCpuidOutput.md)
- [`WHvGetInterruptTargetVpSet`](WHvGetInterruptTargetVpSet.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)