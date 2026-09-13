const CSR_MVENDORID: u16 = 0xF11;
const CSR_MARCHID: u16 = 0xF12;
const CSR_MIMPID: u16 = 0xF13;
const CSR_MHARTID: u16 = 0xF14;

const CSR_MSTATUS: u16 = 0x300;
const CSR_MISA: u16 = 0x301;
const CSR_MEDELEG: u16 = 0x302;
const CSR_MIDELEG: u16 = 0x303;
const CSR_MIE: u16 = 0x304;
const CSR_MTVEC: u16 = 0x305;
const CSR_MCOUNTEREN: u16 = 0x306;
const CSR_MSCRATCH: u16 = 0x340;
const CSR_MEPC: u16 = 0x341;
const CSR_MCAUSE: u16 = 0x342;
const CSR_MTVAL: u16 = 0x343;
const CSR_MIP: u16 = 0x344;

const CSR_SSTATUS: u16 = 0x100;
const CSR_SIE: u16 = 0x104;
const CSR_STVEC: u16 = 0x105;
const CSR_SSCRATCH: u16 = 0x140;
const CSR_SEPC: u16 = 0x141;
const CSR_SCAUSE: u16 = 0x142;
const CSR_STVAL: u16 = 0x143;
const CSR_SIP: u16 = 0x144;
const CSR_SATP: u16 = 0x180;

const CSR_CYCLE: u16 = 0xC00;
const CSR_CYCLEH: u16 = 0xC80;
const CSR_TIME: u16 = 0xC01;
const CSR_TIMEH: u16 = 0xC81;
const CSR_INSTRET: u16 = 0xC02;
const CSR_INSTRETH: u16 = 0xC82;

const MISA_RV32IMAC: u32 = 0x4000_1105
    | (1 << 0)
    | (1 << 8)
    | (1 << 12)
    | (1 << 0 << 2)
    | (1 << 2 << 2);

const MSTATUS_MPP_MASK: u32 = 0x0000_1800;
const MSTATUS_MPP_M: u32 = 0x0000_1800;
const MSTATUS_MPIE: u32 = 0x0000_0080;
const MSTATUS_MIE: u32 = 0x0000_0008;

const MCAUSE_INTERRUPT: u32 = 0x8000_0000;
const MCAUSE_MACHINE_TIMER: u32 = MCAUSE_INTERRUPT | 7;
const MCAUSE_MACHINE_EXTERNAL: u32 = MCAUSE_INTERRUPT | 11;

const MIP_MTIP: u32 = 1 << 7;
const MIP_MEIP: u32 = 1 << 11;
const MIE_MTIE: u32 = 1 << 7;
const MIE_MEIE: u32 = 1 << 11;

pub struct CsrFile {
    pub mstatus: u32,
    pub misa: u32,
    pub medeleg: u32,
    pub mideleg: u32,
    pub mie: u32,
    pub mtvec: u32,
    pub mcounteren: u32,

    pub mscratch: u32,
    pub mepc: u32,
    pub mcause: u32,
    pub mtval: u32,
    pub mip: u32,

    pub sstatus: u32,
    pub sie: u32,
    pub stvec: u32,
    pub sscratch: u32,
    pub sepc: u32,
    pub scause: u32,
    pub stval: u32,
    pub sip: u32,
    pub satp: u32,

    pub cycle: u64,
    pub time: u64,
    pub instret: u64,
}

impl CsrFile {
    pub fn new() -> Self {
        Self {
            mstatus: MSTATUS_MPP_M,
            misa: MISA_RV32IMAC,
            medeleg: 0,
            mideleg: 0,
            mie: 0,
            mtvec: 0,
            mcounteren: 0,

            mscratch: 0,
            mepc: 0,
            mcause: 0,
            mtval: 0,
            mip: 0,

            sstatus: 0,
            sie: 0,
            stvec: 0,
            sscratch: 0,
            sepc: 0,
            scause: 0,
            stval: 0,
            sip: 0,
            satp: 0,

            cycle: 0,
            time: 0,
            instret: 0,
        }
    }

    pub fn read(&self, addr: u16) -> Result<u32, &'static str> {
        match addr {
            CSR_MVENDORID => Ok(0),
            CSR_MARCHID => Ok(0),
            CSR_MIMPID => Ok(0),
            CSR_MHARTID => Ok(0),

            CSR_MSTATUS => Ok(self.mstatus),
            CSR_MISA => Ok(self.misa),
            CSR_MEDELEG => Ok(self.medeleg),
            CSR_MIDELEG => Ok(self.mideleg),
            CSR_MIE => Ok(self.mie),
            CSR_MTVEC => Ok(self.mtvec),
            CSR_MCOUNTEREN => Ok(self.mcounteren),

            CSR_MSCRATCH => Ok(self.mscratch),
            CSR_MEPC => Ok(self.mepc),
            CSR_MCAUSE => Ok(self.mcause),
            CSR_MTVAL => Ok(self.mtval),
            CSR_MIP => Ok(self.mip),

            CSR_SSTATUS => Ok(self.sstatus),
            CSR_SIE => Ok(self.sie),
            CSR_STVEC => Ok(self.stvec),
            CSR_SSCRATCH => Ok(self.sscratch),
            CSR_SEPC => Ok(self.sepc),
            CSR_SCAUSE => Ok(self.scause),
            CSR_STVAL => Ok(self.stval),
            CSR_SIP => Ok(self.sip),
            CSR_SATP => Ok(self.satp),

            CSR_CYCLE => Ok(self.cycle as u32),
            CSR_CYCLEH => Ok((self.cycle >> 32) as u32),
            CSR_TIME => Ok(self.time as u32),
            CSR_TIMEH => Ok((self.time >> 32) as u32),
            CSR_INSTRET => Ok(self.instret as u32),
            CSR_INSTRETH => Ok((self.instret >> 32) as u32),

            _ => Err("Unknown CSR address"),
        }
    }

    pub fn write(&mut self, addr: u16, value: u32) -> Result<(), &'static str> {
        match addr {
            CSR_MSTATUS => {
                self.mstatus = value;
                Ok(())
            }
            CSR_MISA => {
                Ok(())
            }
            CSR_MEDELEG => {
                self.medeleg = value;
                Ok(())
            }
            CSR_MIDELEG => {
                self.mideleg = value;
                Ok(())
            }
            CSR_MIE => {
                self.mie = value;
                Ok(())
            }
            CSR_MTVEC => {
                self.mtvec = value & !0x3;
                Ok(())
            }
            CSR_MCOUNTEREN => {
                self.mcounteren = value;
                Ok(())
            }
            CSR_MSCRATCH => {
                self.mscratch = value;
                Ok(())
            }
            CSR_MEPC => {
                self.mepc = value & !0x3;
                Ok(())
            }
            CSR_MCAUSE => {
                self.mcause = value;
                Ok(())
            }
            CSR_MTVAL => {
                self.mtval = value;
                Ok(())
            }
            CSR_MIP => {
                Ok(())
            }
            CSR_SSTATUS => {
                self.sstatus = value & 0x0000_00DE;
                Ok(())
            }
            CSR_SIE => {
                self.sie = value;
                Ok(())
            }
            CSR_STVEC => {
                self.stvec = value & !0x3;
                Ok(())
            }
            CSR_SSCRATCH => {
                self.sscratch = value;
                Ok(())
            }
            CSR_SEPC => {
                self.sepc = value & !0x3;
                Ok(())
            }
            CSR_SCAUSE => {
                self.scause = value;
                Ok(())
            }
            CSR_STVAL => {
                self.stval = value;
                Ok(())
            }
            CSR_SIP => {
                self.sip = value;
                Ok(())
            }
            CSR_SATP => {
                self.satp = value;
                Ok(())
            }
            CSR_CYCLE | CSR_CYCLEH | CSR_TIME | CSR_TIMEH | CSR_INSTRET | CSR_INSTRETH => {
                Ok(())
            }
            _ => Err("Unknown CSR address"),
        }
    }

    pub fn tick(&mut self) {
        self.cycle = self.cycle.wrapping_add(1);
        self.time = self.time.wrapping_add(1);
    }

    pub fn set_timer_interrupt(&mut self) {
        self.mip |= MIP_MTIP;
    }

    pub fn clear_timer_interrupt(&mut self) {
        self.mip &= !MIP_MTIP;
    }

    pub fn set_external_interrupt(&mut self) {
        self.mip |= MIP_MEIP;
    }

    pub fn clear_external_interrupt(&mut self) {
        self.mip &= !MIP_MEIP;
    }

    pub fn take_trap(&mut self, cause: u32, tval: u32, pc: u32) -> u32 {
        let _prev_mode = (self.mstatus & MSTATUS_MPP_MASK) >> 11;
        self.mstatus &= !MSTATUS_MPP_MASK;
        self.mstatus |= MSTATUS_MPP_M;

        let mpie = (self.mstatus & MSTATUS_MIE) >> 3;
        self.mstatus &= !MSTATUS_MPIE;
        self.mstatus |= mpie << 7;

        self.mstatus &= !MSTATUS_MIE;

        self.mepc = pc;
        self.mcause = cause;
        self.mtval = tval;

        self.mtvec
    }

    pub fn mret(&mut self) -> u32 {
        let mpp = (self.mstatus & MSTATUS_MPP_MASK) >> 11;
        self.mstatus &= !MSTATUS_MPP_MASK;
        self.mstatus |= mpp << 11;

        let mpie = (self.mstatus & MSTATUS_MPIE) >> 7;
        self.mstatus &= !MSTATUS_MPIE;
        self.mstatus |= mpie << 3;

        self.mstatus |= MSTATUS_MPIE;

        self.mepc
    }

    pub fn interrupts_enabled(&self) -> bool {
        (self.mstatus & MSTATUS_MIE) != 0
    }

    pub fn pending_timer_interrupt(&self) -> bool {
        (self.mip & MIP_MTIP) != 0 && (self.mie & MIE_MTIE) != 0
    }

    pub fn pending_external_interrupt(&self) -> bool {
        (self.mip & MIP_MEIP) != 0 && (self.mie & MIE_MEIE) != 0
    }
}