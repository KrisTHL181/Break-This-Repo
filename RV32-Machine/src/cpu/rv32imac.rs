use std::sync::Arc;
use parking_lot::Mutex;
use log::{debug, warn};

use crate::bus::Bus;
use crate::cpu::csr::CsrFile;
use crate::devices::plic::Plic;

const REG_COUNT: usize = 32;

const MCAUSE_INTERRUPT: u32 = 0x8000_0000;
const MCAUSE_MACHINE_TIMER: u32 = MCAUSE_INTERRUPT | 7;
const MCAUSE_MACHINE_EXTERNAL: u32 = MCAUSE_INTERRUPT | 11;
const MCAUSE_ILLEGAL_INSTRUCTION: u32 = 2;
const MCAUSE_ECALL_FROM_MACHINE: u32 = 11;

pub struct Cpu {
    pub regs: [u32; REG_COUNT],
    pub pc: u32,
    pub csr: CsrFile,
    pub bus: Arc<Mutex<Bus>>,
    pub plic: Arc<Mutex<Plic>>,
    pub halted: bool,
    pub running: bool,
    pub cycle_count: u64,
    pub insn_count: u64,
}

impl Cpu {
    pub fn new(bus: Arc<Mutex<Bus>>, plic: Arc<Mutex<Plic>>) -> Self {
        Self {
            regs: [0u32; REG_COUNT],
            pc: 0x0000_0000,
            csr: CsrFile::new(),
            bus,
            plic,
            halted: false,
            running: false,
            cycle_count: 0,
            insn_count: 0,
        }
    }

    pub fn reset(&mut self) {
        self.regs = [0u32; REG_COUNT];
        self.pc = 0x0000_0000;
        self.csr = CsrFile::new();
        self.halted = false;
        self.running = false;
        self.cycle_count = 0;
        self.insn_count = 0;
    }

    pub fn step(&mut self) -> Result<(), String> {
        if self.halted {
            return Ok(());
        }

        self.check_interrupts()?;

        let raw = self.fetch()?;
        let insn = self.decode(raw)?;
        self.execute(insn)?;

        self.regs[0] = 0;
        self.csr.tick();
        self.cycle_count += 1;

        Ok(())
    }

    pub fn run_cycles(&mut self, cycles: usize) -> Result<usize, String> {
        let mut executed = 0;
        self.running = true;
        while executed < cycles && self.running && !self.halted {
            self.step()?;
            executed += 1;
        }
        self.running = false;
        Ok(executed)
    }

    fn check_interrupts(&mut self) -> Result<(), String> {
        if !self.csr.interrupts_enabled() {
            return Ok(());
        }

        if self.csr.pending_timer_interrupt() {
            let trap_pc = self.csr.take_trap(MCAUSE_MACHINE_TIMER, 0, self.pc);
            self.pc = trap_pc;
            return Ok(());
        }

        if self.csr.pending_external_interrupt() {
            let irq = self.plic.lock().claim();
            if irq != 0 {
                let trap_pc = self.csr.take_trap(MCAUSE_MACHINE_EXTERNAL, 0, self.pc);
                self.pc = trap_pc;
            }
            return Ok(());
        }

        Ok(())
    }

    fn fetch(&mut self) -> Result<u32, String> {
        let mut bus = self.bus.lock();
        match bus.read32(self.pc) {
            Ok(data) => Ok(data),
            Err(e) => Err(format!("Fetch error at 0x{:08X}: {}", self.pc, e)),
        }
    }

    fn decode(&self, raw: u32) -> Result<Instruction, String> {
        let opcode = raw & 0x7F;

        if (raw & 0x3) != 0x3 {
            return self.decode_compressed((raw & 0xFFFF) as u16);
        }

        match opcode {
            0x37 => Ok(Instruction::Lui {
                rd: ((raw >> 7) & 0x1F) as u8,
                imm: raw & 0xFFFF_F000,
            }),
            0x17 => Ok(Instruction::Auipc {
                rd: ((raw >> 7) & 0x1F) as u8,
                imm: raw & 0xFFFF_F000,
            }),
            0x6F => {
                let imm = ((raw >> 12) & 0xFF) as u32
                    | ((raw >> 20) & 0x7FE) as u32
                    | ((raw >> 9) & 0x800) as u32
                    | ((raw & 0x8000_0000) != 0) as u32 * 0xFFF0_0000;
                Ok(Instruction::Jal {
                    rd: ((raw >> 7) & 0x1F) as u8,
                    imm: sign_extend(imm, 20),
                })
            }
            0x67 => {
                let _funct3 = ((raw >> 12) & 0x7) as u8;
                Ok(Instruction::Jalr {
                    rd: ((raw >> 7) & 0x1F) as u8,
                    rs1: ((raw >> 15) & 0x1F) as u8,
                    imm: sign_extend((raw >> 20) & 0xFFF, 11),
                })
            }
            0x63 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let imm = ((raw >> 7) & 0x1E) as u32
                    | ((raw >> 20) & 0x7E0) as u32
                    | ((raw << 4) & 0x800) as u32
                    | ((raw >> 19) & 0x1000) as u32;
                let imm = sign_extend(imm, 12);
                let rs1 = ((raw >> 15) & 0x1F) as u8;
                let rs2 = ((raw >> 20) & 0x1F) as u8;
                Ok(Instruction::Branch {
                    funct3,
                    rs1,
                    rs2,
                    imm,
                })
            }
            0x03 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                Ok(Instruction::Load {
                    rd: ((raw >> 7) & 0x1F) as u8,
                    rs1: ((raw >> 15) & 0x1F) as u8,
                    imm: sign_extend((raw >> 20) & 0xFFF, 11),
                    funct3,
                })
            }
            0x23 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let imm = ((raw >> 7) & 0x1F) as u32 | ((raw >> 20) & 0xFE0) as u32;
                let imm = sign_extend(imm, 11);
                Ok(Instruction::Store {
                    rs1: ((raw >> 15) & 0x1F) as u8,
                    rs2: ((raw >> 20) & 0x1F) as u8,
                    imm,
                    funct3,
                })
            }
            0x13 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let funct7 = (raw >> 25) & 0x7F;
                let rd = ((raw >> 7) & 0x1F) as u8;
                let rs1 = ((raw >> 15) & 0x1F) as u8;
                let shamt = ((raw >> 20) & 0x1F) as u8;
                let imm = sign_extend((raw >> 20) & 0xFFF, 11);

                match funct3 {
                    0x0 => Ok(Instruction::Addi { rd, rs1, imm }),
                    0x1 => {
                        if funct7 == 0x00 {
                            Ok(Instruction::Slli { rd, rs1, shamt })
                        } else {
                            Err(format!("Invalid SLLI funct7: 0x{:02X}", funct7))
                        }
                    }
                    0x2 => Ok(Instruction::Slti { rd, rs1, imm }),
                    0x3 => Ok(Instruction::Sltiu { rd, rs1, imm }),
                    0x4 => Ok(Instruction::Xori { rd, rs1, imm }),
                    0x5 => {
                        if funct7 == 0x00 {
                            Ok(Instruction::Srli { rd, rs1, shamt })
                        } else if funct7 == 0x20 {
                            Ok(Instruction::Srai { rd, rs1, shamt })
                        } else {
                            Err(format!("Invalid SRLI/SRAI funct7: 0x{:02X}", funct7))
                        }
                    }
                    0x6 => Ok(Instruction::Ori { rd, rs1, imm }),
                    0x7 => Ok(Instruction::Andi { rd, rs1, imm }),
                    _ => Err(format!("Unknown I-type funct3: 0x{:X}", funct3)),
                }
            }
            0x33 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let funct7 = (raw >> 25) & 0x7F;
                let rd = ((raw >> 7) & 0x1F) as u8;
                let rs1 = ((raw >> 15) & 0x1F) as u8;
                let rs2 = ((raw >> 20) & 0x1F) as u8;

                match (funct3, funct7) {
                    (0x0, 0x00) => Ok(Instruction::Add { rd, rs1, rs2 }),
                    (0x0, 0x01) => Ok(Instruction::Mul { rd, rs1, rs2 }),
                    (0x0, 0x20) => Ok(Instruction::Sub { rd, rs1, rs2 }),
                    (0x1, 0x00) => Ok(Instruction::Sll { rd, rs1, rs2 }),
                    (0x1, 0x01) => Ok(Instruction::Mulh { rd, rs1, rs2 }),
                    (0x2, 0x00) => Ok(Instruction::Slt { rd, rs1, rs2 }),
                    (0x2, 0x01) => Ok(Instruction::Mulhsu { rd, rs1, rs2 }),
                    (0x3, 0x00) => Ok(Instruction::Sltu { rd, rs1, rs2 }),
                    (0x3, 0x01) => Ok(Instruction::Mulhu { rd, rs1, rs2 }),
                    (0x4, 0x00) => Ok(Instruction::Xor { rd, rs1, rs2 }),
                    (0x4, 0x01) => Ok(Instruction::Div { rd, rs1, rs2 }),
                    (0x5, 0x00) => Ok(Instruction::Srl { rd, rs1, rs2 }),
                    (0x5, 0x20) => Ok(Instruction::Sra { rd, rs1, rs2 }),
                    (0x5, 0x01) => Ok(Instruction::Divu { rd, rs1, rs2 }),
                    (0x6, 0x00) => Ok(Instruction::Or { rd, rs1, rs2 }),
                    (0x6, 0x01) => Ok(Instruction::Rem { rd, rs1, rs2 }),
                    (0x7, 0x00) => Ok(Instruction::And { rd, rs1, rs2 }),
                    (0x7, 0x01) => Ok(Instruction::Remu { rd, rs1, rs2 }),
                    _ => Err(format!("Unknown R-type: funct3=0x{:X}, funct7=0x{:02X}", funct3, funct7)),
                }
            }
            0x0F => {
                Ok(Instruction::Fence {
                    rd: ((raw >> 7) & 0x1F) as u8,
                    rs1: ((raw >> 15) & 0x1F) as u8,
                })
            }
            0x73 => {
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let rd = ((raw >> 7) & 0x1F) as u8;
                let rs1 = ((raw >> 15) & 0x1F) as u8;
                let csr_addr = ((raw >> 20) & 0xFFF) as u16;
                let uimm = rs1 as u32;

                match funct3 {
                    0x0 => {
                        if rd == 0 && rs1 == 0 && csr_addr == 0x000 {
                            Ok(Instruction::Ecall)
                        } else if rd == 0 && rs1 == 0 && csr_addr == 0x001 {
                            Ok(Instruction::Ebreak)
                        } else if rd == 0 && rs1 == 0 && csr_addr == 0x302 {
                            Ok(Instruction::Mret)
                        } else {
                            Err(format!("Unknown SYSTEM: rd={}, rs1={}, csr=0x{:03X}", rd, rs1, csr_addr))
                        }
                    }
                    0x1 => Ok(Instruction::Csrrw { rd, csr_addr, rs1 }),
                    0x2 => Ok(Instruction::Csrrs { rd, csr_addr, rs1 }),
                    0x3 => Ok(Instruction::Csrrc { rd, csr_addr, rs1 }),
                    0x5 => Ok(Instruction::Csrrwi { rd, csr_addr, uimm }),
                    0x6 => Ok(Instruction::Csrrsi { rd, csr_addr, uimm }),
                    0x7 => Ok(Instruction::Csrrci { rd, csr_addr, uimm }),
                    _ => Err(format!("Unknown SYSTEM funct3: 0x{:X}", funct3)),
                }
            }
            0x2F => {
                let funct5 = (raw >> 27) & 0x1F;
                let aq = ((raw >> 26) & 0x1) != 0;
                let rl = ((raw >> 25) & 0x1) != 0;
                let funct3 = ((raw >> 12) & 0x7) as u8;
                let rd = ((raw >> 7) & 0x1F) as u8;
                let rs1 = ((raw >> 15) & 0x1F) as u8;
                let rs2 = ((raw >> 20) & 0x1F) as u8;

                match funct5 {
                    0x02 => {
                        if funct3 == 0x2 {
                            Ok(Instruction::LrW { rd, rs1 })
                        } else {
                            Err(format!("Unknown LR: funct3=0x{:X}", funct3))
                        }
                    }
                    0x03 => Ok(Instruction::ScW {
                        rd,
                        rs1,
                        rs2,
                        aq,
                        rl,
                    }),
                    0x01 => Ok(Instruction::AmoswapW { rd, rs1, rs2 }),
                    0x00 => Ok(Instruction::AmoaddW { rd, rs1, rs2 }),
                    0x04 => Ok(Instruction::AmoxorW { rd, rs1, rs2 }),
                    0x0C => Ok(Instruction::AmoandW { rd, rs1, rs2 }),
                    0x08 => Ok(Instruction::AmoorW { rd, rs1, rs2 }),
                    0x10 => Ok(Instruction::AmominW { rd, rs1, rs2 }),
                    0x14 => Ok(Instruction::AmomaxW { rd, rs1, rs2 }),
                    0x18 => Ok(Instruction::AmominuW { rd, rs1, rs2 }),
                    0x1C => Ok(Instruction::AmomaxuW { rd, rs1, rs2 }),
                    _ => Err(format!("Unknown AMO funct5: 0x{:02X}", funct5)),
                }
            }
            _ => Err(format!("Unknown opcode: 0x{:02X}", opcode)),
        }
    }

    fn decode_compressed(&self, raw: u16) -> Result<Instruction, String> {
        let opcode = raw & 0x3;
        let funct3 = ((raw >> 13) & 0x7) as u8;
        let funct4 = (raw >> 12) & 0xF;

        match (opcode, funct3) {
            (0x0, 0x0) => {
                Ok(Instruction::CIllegal)
            }
            (0x0, 0x1) | (0x0, 0x2) | (0x0, 0x3) => {
                Err("C.ADDI4SPN/C.FLD/C.LQ/C.LW: not fully implemented".into())
            }
            (0x1, 0x0) => {
                let rd_rs1 = ((raw >> 7) & 0x1F) as u8;
                let imm = sign_extend(
                    (((raw >> 2) & 0x1F) as u32)
                        | (((raw >> 12) & 0x1) as u32) << 5,
                    5,
                );
                Ok(Instruction::CAddi { rd_rs1, imm })
            }
            (0x1, 0x1) => {
                let imm = sign_extend(
                    (((raw >> 2) & 0x1F) as u32)
                        | (((raw >> 12) & 0x1) as u32) << 5
                        | (((raw >> 3) & 0x7) as u32) << 6
                        | (((raw >> 7) & 0x1) as u32) << 9
                        | (((raw >> 8) & 0x3) as u32) << 10
                        | (((raw >> 12) & 0x1) as u32) << 11,
                    11,
                );
                Ok(Instruction::CJal { imm })
            }
            (0x1, 0x2) => {
                let rd = ((raw >> 7) & 0x1F) as u8;
                let imm = sign_extend(
                    (((raw >> 2) & 0x1F) as u32)
                        | (((raw >> 12) & 0x1) as u32) << 5,
                    5,
                );
                Ok(Instruction::CLi { rd, imm })
            }
            (0x1, 0x3) => {
                let rd = ((raw >> 7) & 0x1F) as u8;
                match funct4 {
                    0x0 => {
                        let nzuimm = (((raw >> 2) & 0x1F) as u32)
                            | (((raw >> 12) & 0x1) as u32) << 5;
                        Ok(Instruction::CAddi16sp { nzuimm })
                    }
                    0x2 => {
                        let imm = sign_extend(
                            (((raw >> 2) & 0x1F) as u32)
                                | (((raw >> 12) & 0x1) as u32) << 5,
                            5,
                        );
                        Ok(Instruction::CLui { rd, imm })
                    }
                    0x4 => {
                        let funct2 = (raw >> 10) & 0x3;
                        let rd_rs1 = ((raw >> 7) & 0x7) as u8 + 8;
                        let shamt = (((raw >> 2) & 0x1F) | ((raw >> 12) & 0x1) << 5) as u8;
                        match funct2 {
                            0x0 => Ok(Instruction::CSrli { rd_rs1, shamt }),
                            0x1 => Ok(Instruction::CSrai { rd_rs1, shamt }),
                            0x2 => Ok(Instruction::CAndi { rd_rs1, imm: sign_extend((((raw >> 2) & 0x1F) | ((raw >> 12) & 0x1) << 5) as u32, 5) }),
                            0x3 => {
                                let funct1 = (raw >> 5) & 0x3;
                                let rs2 = ((raw >> 2) & 0x7) as u8 + 8;
                                match funct1 {
                                    0x0 => Ok(Instruction::CSub { rd_rs1, rs2 }),
                                    0x1 => Ok(Instruction::CXor { rd_rs1, rs2 }),
                                    0x2 => Ok(Instruction::COr { rd_rs1, rs2 }),
                                    0x3 => Ok(Instruction::CAnd { rd_rs1, rs2 }),
                                    _ => Err("Invalid C.ALU funct1".into()),
                                }
                            }
                            _ => Err("Invalid C.ALU funct2".into()),
                        }
                    }
                    _ => Err(format!("Unknown C.LUI/C.ADDI16SP funct4: 0x{:X}", funct4)),
                }
            }
            (0x2, 0x0) => {
                let rd_rs1 = ((raw >> 7) & 0x7) as u8 + 8;
                let imm = (((raw >> 5) & 0x1) as u32) << 6
                    | (((raw >> 10) & 0x7) as u32) << 3
                    | (((raw >> 6) & 0x1) as u32) << 2;
                Ok(Instruction::CSw { rd_rs1, imm })
            }
            (0x2, 0x1) | (0x2, 0x2) | (0x2, 0x3) => {
                Err("C.LW/C.FLD/C.LQ: not implemented".into())
            }
            (0x1, 0x4) | (0x1, 0x5) | (0x1, 0x6) | (0x1, 0x7) => {
                let rd_rs1 = ((raw >> 7) & 0x7) as u8 + 8;
                let rs2 = ((raw >> 2) & 0x7) as u8 + 8;
                let funct2 = (raw >> 5) & 0x3;
                let funct6 = (raw >> 10) & 0x3F;
                let funct4 = (raw >> 12) & 0xF;

                match (funct6, funct2) {
                    (0x07, 0x0) => {
                        Ok(Instruction::CMv { rd: rd_rs1, rs2 })
                    }
                    (0x07, 0x1) => {
                        if funct4 == 0x9 {
                            if rs2 == 0 {
                                Ok(Instruction::CEbreak)
                            } else {
                                Ok(Instruction::CAdd { rd: rd_rs1, rs2 })
                            }
                        } else {
                            Err(format!("Invalid C.ADD: funct4=0x{:X}", funct4))
                        }
                    }
                    _ => {
                        let imm = (((raw >> 2) & 0x1F) as u32)
                            | (((raw >> 12) & 0x1) as u32) << 5;
                        Ok(Instruction::CSlli { rd_rs1, shamt: imm as u8 })
                    }
                }
            }
            (0x2, 0x4) | (0x2, 0x5) | (0x2, 0x6) | (0x2, 0x7) => {
                let rd_rs1 = ((raw >> 7) & 0x7) as u8 + 8;
                let imm = (((raw >> 5) & 0x1) as u32) << 6
                    | (((raw >> 10) & 0x7) as u32) << 3
                    | (((raw >> 6) & 0x1) as u32) << 2;
                Ok(Instruction::CLw { rd_rs1, imm })
            }
            _ => Err(format!("Unknown compressed: opcode=0x{:X}, funct3=0x{:X}", opcode, funct3)),
        }
    }

    fn execute(&mut self, insn: Instruction) -> Result<(), String> {
        let next_pc = self.pc.wrapping_add(insn.size());
        match insn {
            Instruction::Lui { rd, imm } => {
                self.regs[rd as usize] = imm;
                self.pc = next_pc;
            }
            Instruction::Auipc { rd, imm } => {
                self.regs[rd as usize] = self.pc.wrapping_add(imm);
                self.pc = next_pc;
            }
            Instruction::Jal { rd, imm } => {
                self.regs[rd as usize] = next_pc;
                self.pc = self.pc.wrapping_add(imm as u32);
            }
            Instruction::Jalr { rd, rs1, imm } => {
                let target = (self.regs[rs1 as usize].wrapping_add(imm as u32)) & !1;
                self.regs[rd as usize] = next_pc;
                self.pc = target;
            }
            Instruction::Branch { funct3, rs1, rs2, imm } => {
                let taken = match funct3 {
                    0x0 => self.regs[rs1 as usize] == self.regs[rs2 as usize],
                    0x1 => self.regs[rs1 as usize] != self.regs[rs2 as usize],
                    0x4 => (self.regs[rs1 as usize] as i32) < (self.regs[rs2 as usize] as i32),
                    0x5 => (self.regs[rs1 as usize] as i32) >= (self.regs[rs2 as usize] as i32),
                    0x6 => self.regs[rs1 as usize] < self.regs[rs2 as usize],
                    0x7 => self.regs[rs1 as usize] >= self.regs[rs2 as usize],
                    _ => return Err(format!("Unknown branch funct3: 0x{:X}", funct3)),
                };
                if taken {
                    self.pc = self.pc.wrapping_add(imm as u32);
                } else {
                    self.pc = next_pc;
                }
            }
            Instruction::Load { rd, rs1, imm, funct3 } => {
                let addr = self.regs[rs1 as usize].wrapping_add(imm as u32);
                let mut bus = self.bus.lock();
                let val = match funct3 {
                    0x0 => bus.read8(addr).map(|v| sign_extend(v as u32, 7) as u32)?,
                    0x1 => bus.read16(addr).map(|v| sign_extend(v as u32, 15) as u32)?,
                    0x2 => bus.read32(addr)?,
                    0x4 => bus.read8(addr).map(|v| v as u32)?,
                    0x5 => bus.read16(addr).map(|v| v as u32)?,
                    _ => return Err(format!("Unknown load funct3: 0x{:X}", funct3)),
                };
                self.regs[rd as usize] = val;
                self.pc = next_pc;
            }
            Instruction::Store { rs1, rs2, imm, funct3 } => {
                let addr = self.regs[rs1 as usize].wrapping_add(imm as u32);
                let val = self.regs[rs2 as usize];
                let mut bus = self.bus.lock();
                match funct3 {
                    0x0 => bus.write8(addr, val as u8)?,
                    0x1 => bus.write16(addr, val as u16)?,
                    0x2 => bus.write32(addr, val)?,
                    _ => return Err(format!("Unknown store funct3: 0x{:X}", funct3)),
                }
                self.pc = next_pc;
            }
            Instruction::Addi { rd, rs1, imm } => {
                self.regs[rd as usize] = self.regs[rs1 as usize].wrapping_add(imm as u32);
                self.pc = next_pc;
            }
            Instruction::Slli { rd, rs1, shamt } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] << (shamt & 0x1F);
                self.pc = next_pc;
            }
            Instruction::Slti { rd, rs1, imm } => {
                self.regs[rd as usize] = if (self.regs[rs1 as usize] as i32) < imm { 1 } else { 0 };
                self.pc = next_pc;
            }
            Instruction::Sltiu { rd, rs1, imm } => {
                self.regs[rd as usize] = if self.regs[rs1 as usize] < imm as u32 { 1 } else { 0 };
                self.pc = next_pc;
            }
            Instruction::Xori { rd, rs1, imm } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] ^ (imm as u32);
                self.pc = next_pc;
            }
            Instruction::Srli { rd, rs1, shamt } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] >> (shamt & 0x1F);
                self.pc = next_pc;
            }
            Instruction::Srai { rd, rs1, shamt } => {
                self.regs[rd as usize] = ((self.regs[rs1 as usize] as i32) >> (shamt & 0x1F)) as u32;
                self.pc = next_pc;
            }
            Instruction::Ori { rd, rs1, imm } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] | (imm as u32);
                self.pc = next_pc;
            }
            Instruction::Andi { rd, rs1, imm } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] & (imm as u32);
                self.pc = next_pc;
            }
            Instruction::Add { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize].wrapping_add(self.regs[rs2 as usize]);
                self.pc = next_pc;
            }
            Instruction::Sub { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize].wrapping_sub(self.regs[rs2 as usize]);
                self.pc = next_pc;
            }
            Instruction::Sll { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] << (self.regs[rs2 as usize] & 0x1F);
                self.pc = next_pc;
            }
            Instruction::Slt { rd, rs1, rs2 } => {
                self.regs[rd as usize] = if (self.regs[rs1 as usize] as i32) < (self.regs[rs2 as usize] as i32) { 1 } else { 0 };
                self.pc = next_pc;
            }
            Instruction::Sltu { rd, rs1, rs2 } => {
                self.regs[rd as usize] = if self.regs[rs1 as usize] < self.regs[rs2 as usize] { 1 } else { 0 };
                self.pc = next_pc;
            }
            Instruction::Xor { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] ^ self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::Srl { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] >> (self.regs[rs2 as usize] & 0x1F);
                self.pc = next_pc;
            }
            Instruction::Sra { rd, rs1, rs2 } => {
                self.regs[rd as usize] = ((self.regs[rs1 as usize] as i32) >> (self.regs[rs2 as usize] & 0x1F)) as u32;
                self.pc = next_pc;
            }
            Instruction::Or { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] | self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::And { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize] & self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::Mul { rd, rs1, rs2 } => {
                self.regs[rd as usize] = self.regs[rs1 as usize].wrapping_mul(self.regs[rs2 as usize]);
                self.pc = next_pc;
            }
            Instruction::Mulh { rd, rs1, rs2 } => {
                let a = self.regs[rs1 as usize] as i32 as i64;
                let b = self.regs[rs2 as usize] as i32 as i64;
                self.regs[rd as usize] = ((a * b) >> 32) as u32;
                self.pc = next_pc;
            }
            Instruction::Mulhsu { rd, rs1, rs2 } => {
                let a = self.regs[rs1 as usize] as i32 as i64;
                let b = self.regs[rs2 as usize] as i64;
                self.regs[rd as usize] = ((a * b) >> 32) as u32;
                self.pc = next_pc;
            }
            Instruction::Mulhu { rd, rs1, rs2 } => {
                let a = self.regs[rs1 as usize] as u64;
                let b = self.regs[rs2 as usize] as u64;
                self.regs[rd as usize] = ((a * b) >> 32) as u32;
                self.pc = next_pc;
            }
            Instruction::Div { rd, rs1, rs2 } => {
                let divisor = self.regs[rs2 as usize] as i32;
                if divisor == 0 {
                    self.regs[rd as usize] = u32::MAX;
                } else {
                    let dividend = self.regs[rs1 as usize] as i32;
                    if dividend == i32::MIN && divisor == -1 {
                        self.regs[rd as usize] = dividend as u32;
                    } else {
                        self.regs[rd as usize] = (dividend / divisor) as u32;
                    }
                }
                self.pc = next_pc;
            }
            Instruction::Divu { rd, rs1, rs2 } => {
                let divisor = self.regs[rs2 as usize];
                if divisor == 0 {
                    self.regs[rd as usize] = u32::MAX;
                } else {
                    self.regs[rd as usize] = self.regs[rs1 as usize] / divisor;
                }
                self.pc = next_pc;
            }
            Instruction::Rem { rd, rs1, rs2 } => {
                let divisor = self.regs[rs2 as usize] as i32;
                if divisor == 0 {
                    self.regs[rd as usize] = self.regs[rs1 as usize];
                } else {
                    let dividend = self.regs[rs1 as usize] as i32;
                    if dividend == i32::MIN && divisor == -1 {
                        self.regs[rd as usize] = 0;
                    } else {
                        self.regs[rd as usize] = (dividend % divisor) as u32;
                    }
                }
                self.pc = next_pc;
            }
            Instruction::Remu { rd, rs1, rs2 } => {
                let divisor = self.regs[rs2 as usize];
                if divisor == 0 {
                    self.regs[rd as usize] = self.regs[rs1 as usize];
                } else {
                    self.regs[rd as usize] = self.regs[rs1 as usize] % divisor;
                }
                self.pc = next_pc;
            }
            Instruction::Fence { .. } => {
                self.pc = next_pc;
            }
            Instruction::Ecall => {
                let trap_pc = self.csr.take_trap(MCAUSE_ECALL_FROM_MACHINE, 0, self.pc);
                self.pc = trap_pc;
            }
            Instruction::Ebreak => {
                debug!("EBREAK at PC=0x{:08X}", self.pc);
                self.halted = true;
                self.pc = next_pc;
            }
            Instruction::Mret => {
                let mepc = self.csr.mret();
                self.pc = mepc;
            }
            Instruction::Csrrw { rd, csr_addr, rs1 } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                self.csr.write(csr_addr, self.regs[rs1 as usize])?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::Csrrs { rd, csr_addr, rs1 } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                if rs1 != 0 {
                    self.csr.write(csr_addr, old | self.regs[rs1 as usize])?;
                }
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::Csrrc { rd, csr_addr, rs1 } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                if rs1 != 0 {
                    self.csr.write(csr_addr, old & !self.regs[rs1 as usize])?;
                }
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::Csrrwi { rd, csr_addr, uimm } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                self.csr.write(csr_addr, uimm)?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::Csrrsi { rd, csr_addr, uimm } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                if uimm != 0 {
                    self.csr.write(csr_addr, old | uimm)?;
                }
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::Csrrci { rd, csr_addr, uimm } => {
                let old = self.csr.read(csr_addr).unwrap_or(0);
                if uimm != 0 {
                    self.csr.write(csr_addr, old & !uimm)?;
                }
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::LrW { rd, rs1 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let val = bus.read32(addr)?;
                self.regs[rd as usize] = val;
                bus.set_reservation(addr);
                self.pc = next_pc;
            }
            Instruction::ScW { rd, rs1, rs2, .. } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                if bus.check_reservation(addr) {
                    bus.write32(addr, self.regs[rs2 as usize])?;
                    self.regs[rd as usize] = 0;
                } else {
                    self.regs[rd as usize] = 1;
                }
                bus.clear_reservation();
                self.pc = next_pc;
            }
            Instruction::AmoswapW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                bus.write32(addr, self.regs[rs2 as usize])?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmoaddW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                bus.write32(addr, old.wrapping_add(self.regs[rs2 as usize]))?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmoxorW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                bus.write32(addr, old ^ self.regs[rs2 as usize])?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmoandW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                bus.write32(addr, old & self.regs[rs2 as usize])?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmoorW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                bus.write32(addr, old | self.regs[rs2 as usize])?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmominW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                let new = std::cmp::min(old as i32, self.regs[rs2 as usize] as i32) as u32;
                bus.write32(addr, new)?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmomaxW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                let new = std::cmp::max(old as i32, self.regs[rs2 as usize] as i32) as u32;
                bus.write32(addr, new)?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmominuW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                let new = std::cmp::min(old, self.regs[rs2 as usize]);
                bus.write32(addr, new)?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::AmomaxuW { rd, rs1, rs2 } => {
                let addr = self.regs[rs1 as usize];
                let mut bus = self.bus.lock();
                let old = bus.read32(addr)?;
                let new = std::cmp::max(old, self.regs[rs2 as usize]);
                bus.write32(addr, new)?;
                self.regs[rd as usize] = old;
                self.pc = next_pc;
            }
            Instruction::CAddi { rd_rs1, imm } => {
                if rd_rs1 != 0 {
                    self.regs[rd_rs1 as usize] = self.regs[rd_rs1 as usize].wrapping_add(imm as u32);
                }
                self.pc = next_pc;
            }
            Instruction::CJal { imm } => {
                self.regs[1] = next_pc;
                self.pc = self.pc.wrapping_add(imm as u32);
            }
            Instruction::CLi { rd, imm } => {
                self.regs[rd as usize] = imm as u32;
                self.pc = next_pc;
            }
            Instruction::CLui { rd, imm } => {
                if rd != 0 {
                    self.regs[rd as usize] = imm as u32;
                }
                self.pc = next_pc;
            }
            Instruction::CAddi16sp { nzuimm } => {
                let imm = sign_extend(nzuimm, 5);
                self.regs[2] = self.regs[2].wrapping_add(imm as u32);
                self.pc = next_pc;
            }
            Instruction::CSrli { rd_rs1, shamt } => {
                self.regs[rd_rs1 as usize] >>= shamt & 0x1F;
                self.pc = next_pc;
            }
            Instruction::CSrai { rd_rs1, shamt } => {
                self.regs[rd_rs1 as usize] = ((self.regs[rd_rs1 as usize] as i32) >> (shamt & 0x1F)) as u32;
                self.pc = next_pc;
            }
            Instruction::CAndi { rd_rs1, imm } => {
                self.regs[rd_rs1 as usize] &= imm as u32;
                self.pc = next_pc;
            }
            Instruction::CSub { rd_rs1, rs2 } => {
                self.regs[rd_rs1 as usize] = self.regs[rd_rs1 as usize].wrapping_sub(self.regs[rs2 as usize]);
                self.pc = next_pc;
            }
            Instruction::CXor { rd_rs1, rs2 } => {
                self.regs[rd_rs1 as usize] ^= self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::COr { rd_rs1, rs2 } => {
                self.regs[rd_rs1 as usize] |= self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::CAnd { rd_rs1, rs2 } => {
                self.regs[rd_rs1 as usize] &= self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::CSw { rd_rs1, imm } => {
                let addr = self.regs[rd_rs1 as usize].wrapping_add(imm as u32);
                let val = self.regs[rd_rs1 as usize];
                let mut bus = self.bus.lock();
                bus.write32(addr, val)?;
                self.pc = next_pc;
            }
            Instruction::CLw { rd_rs1, imm } => {
                let addr = self.regs[rd_rs1 as usize].wrapping_add(imm as u32);
                let mut bus = self.bus.lock();
                let val = bus.read32(addr)?;
                self.regs[rd_rs1 as usize] = val;
                self.pc = next_pc;
            }
            Instruction::CMv { rd, rs2 } => {
                self.regs[rd as usize] = self.regs[rs2 as usize];
                self.pc = next_pc;
            }
            Instruction::CAdd { rd, rs2 } => {
                self.regs[rd as usize] = self.regs[rd as usize].wrapping_add(self.regs[rs2 as usize]);
                self.pc = next_pc;
            }
            Instruction::CSlli { rd_rs1, shamt } => {
                self.regs[rd_rs1 as usize] <<= shamt & 0x1F;
                self.pc = next_pc;
            }
            Instruction::CEbreak => {
                debug!("C.EBREAK at PC=0x{:08X}", self.pc);
                self.halted = true;
                self.pc = next_pc;
            }
            Instruction::CIllegal => {
                warn!("Illegal compressed instruction at PC=0x{:08X}", self.pc);
                let trap_pc = self.csr.take_trap(MCAUSE_ILLEGAL_INSTRUCTION, 0, self.pc);
                self.pc = trap_pc;
            }
        }
        self.insn_count += 1;
        Ok(())
    }
}

fn sign_extend(value: u32, bits: u32) -> i32 {
    let shift = 32 - bits;
    ((value << shift) as i32) >> shift
}

#[derive(Debug, Clone)]
pub enum Instruction {
    Lui { rd: u8, imm: u32 },
    Auipc { rd: u8, imm: u32 },
    Jal { rd: u8, imm: i32 },
    Jalr { rd: u8, rs1: u8, imm: i32 },
    Branch { funct3: u8, rs1: u8, rs2: u8, imm: i32 },
    Load { rd: u8, rs1: u8, imm: i32, funct3: u8 },
    Store { rs1: u8, rs2: u8, imm: i32, funct3: u8 },
    Addi { rd: u8, rs1: u8, imm: i32 },
    Slli { rd: u8, rs1: u8, shamt: u8 },
    Slti { rd: u8, rs1: u8, imm: i32 },
    Sltiu { rd: u8, rs1: u8, imm: i32 },
    Xori { rd: u8, rs1: u8, imm: i32 },
    Srli { rd: u8, rs1: u8, shamt: u8 },
    Srai { rd: u8, rs1: u8, shamt: u8 },
    Ori { rd: u8, rs1: u8, imm: i32 },
    Andi { rd: u8, rs1: u8, imm: i32 },
    Add { rd: u8, rs1: u8, rs2: u8 },
    Sub { rd: u8, rs1: u8, rs2: u8 },
    Sll { rd: u8, rs1: u8, rs2: u8 },
    Slt { rd: u8, rs1: u8, rs2: u8 },
    Sltu { rd: u8, rs1: u8, rs2: u8 },
    Xor { rd: u8, rs1: u8, rs2: u8 },
    Srl { rd: u8, rs1: u8, rs2: u8 },
    Sra { rd: u8, rs1: u8, rs2: u8 },
    Or { rd: u8, rs1: u8, rs2: u8 },
    And { rd: u8, rs1: u8, rs2: u8 },
    Mul { rd: u8, rs1: u8, rs2: u8 },
    Mulh { rd: u8, rs1: u8, rs2: u8 },
    Mulhsu { rd: u8, rs1: u8, rs2: u8 },
    Mulhu { rd: u8, rs1: u8, rs2: u8 },
    Div { rd: u8, rs1: u8, rs2: u8 },
    Divu { rd: u8, rs1: u8, rs2: u8 },
    Rem { rd: u8, rs1: u8, rs2: u8 },
    Remu { rd: u8, rs1: u8, rs2: u8 },
    Fence { rd: u8, rs1: u8 },
    Ecall,
    Ebreak,
    Mret,
    Csrrw { rd: u8, csr_addr: u16, rs1: u8 },
    Csrrs { rd: u8, csr_addr: u16, rs1: u8 },
    Csrrc { rd: u8, csr_addr: u16, rs1: u8 },
    Csrrwi { rd: u8, csr_addr: u16, uimm: u32 },
    Csrrsi { rd: u8, csr_addr: u16, uimm: u32 },
    Csrrci { rd: u8, csr_addr: u16, uimm: u32 },
    LrW { rd: u8, rs1: u8 },
    ScW { rd: u8, rs1: u8, rs2: u8, aq: bool, rl: bool },
    AmoswapW { rd: u8, rs1: u8, rs2: u8 },
    AmoaddW { rd: u8, rs1: u8, rs2: u8 },
    AmoxorW { rd: u8, rs1: u8, rs2: u8 },
    AmoandW { rd: u8, rs1: u8, rs2: u8 },
    AmoorW { rd: u8, rs1: u8, rs2: u8 },
    AmominW { rd: u8, rs1: u8, rs2: u8 },
    AmomaxW { rd: u8, rs1: u8, rs2: u8 },
    AmominuW { rd: u8, rs1: u8, rs2: u8 },
    AmomaxuW { rd: u8, rs1: u8, rs2: u8 },
    CAddi { rd_rs1: u8, imm: i32 },
    CJal { imm: i32 },
    CLi { rd: u8, imm: i32 },
    CLui { rd: u8, imm: i32 },
    CAddi16sp { nzuimm: u32 },
    CSrli { rd_rs1: u8, shamt: u8 },
    CSrai { rd_rs1: u8, shamt: u8 },
    CAndi { rd_rs1: u8, imm: i32 },
    CSub { rd_rs1: u8, rs2: u8 },
    CXor { rd_rs1: u8, rs2: u8 },
    COr { rd_rs1: u8, rs2: u8 },
    CAnd { rd_rs1: u8, rs2: u8 },
    CSw { rd_rs1: u8, imm: u32 },
    CLw { rd_rs1: u8, imm: u32 },
    CMv { rd: u8, rs2: u8 },
    CAdd { rd: u8, rs2: u8 },
    CSlli { rd_rs1: u8, shamt: u8 },
    CEbreak,
    CIllegal,
}

impl Instruction {
    pub fn size(&self) -> u32 {
        match self {
            Instruction::CAddi { .. }
            | Instruction::CJal { .. }
            | Instruction::CLi { .. }
            | Instruction::CLui { .. }
            | Instruction::CAddi16sp { .. }
            | Instruction::CSrli { .. }
            | Instruction::CSrai { .. }
            | Instruction::CAndi { .. }
            | Instruction::CSub { .. }
            | Instruction::CXor { .. }
            | Instruction::COr { .. }
            | Instruction::CAnd { .. }
            | Instruction::CSw { .. }
            | Instruction::CLw { .. }
            | Instruction::CMv { .. }
            | Instruction::CAdd { .. }
            | Instruction::CSlli { .. }
            | Instruction::CEbreak { .. }
            | Instruction::CIllegal { .. } => 2,
            _ => 4,
        }
    }
}