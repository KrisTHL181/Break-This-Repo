const SCSI_REG_SCNTL0: usize = 0x00;
const SCSI_REG_SCNTL1: usize = 0x01;
const SCSI_REG_SCNTL2: usize = 0x02;
const SCSI_REG_SCNTL3: usize = 0x03;
const SCSI_REG_SCID: usize = 0x04;
const SCSI_REG_SXFER: usize = 0x05;
const SCSI_REG_SDID: usize = 0x06;
const SCSI_REG_GPREG: usize = 0x07;
const SCSI_REG_SFBR: usize = 0x08;
const SCSI_REG_SOCL: usize = 0x09;
const SCSI_REG_SSID: usize = 0x0A;
const SCSI_REG_SBCL: usize = 0x0B;
const SCSI_REG_DSTAT: usize = 0x0C;
const SCSI_REG_SSTAT0: usize = 0x0D;
const SCSI_REG_SSTAT1: usize = 0x0E;
const SCSI_REG_SSTAT2: usize = 0x0F;
const SCSI_REG_DSA: usize = 0x10;
const SCSI_REG_ISTAT: usize = 0x14;
const SCSI_REG_CTEST0: usize = 0x18;
const SCSI_REG_CTEST1: usize = 0x19;
const SCSI_REG_CTEST2: usize = 0x1A;
const SCSI_REG_CTEST3: usize = 0x1B;
const SCSI_REG_TEMP: usize = 0x1C;
const SCSI_REG_DFIFO: usize = 0x20;
const SCSI_REG_CTEST4: usize = 0x21;
const SCSI_REG_CTEST5: usize = 0x22;
const SCSI_REG_CTEST6: usize = 0x23;
const SCSI_REG_DBC: usize = 0x24;
const SCSI_REG_DCMD: usize = 0x27;
const SCSI_REG_DNAD: usize = 0x28;
const SCSI_REG_DSP: usize = 0x2C;
const SCSI_REG_DSPS: usize = 0x30;
const SCSI_REG_SCRATCHA: usize = 0x34;
const SCSI_REG_DMODE: usize = 0x38;
const SCSI_REG_DIEN: usize = 0x39;
const SCSI_REG_DWT: usize = 0x3A;
const SCSI_REG_DCNTL: usize = 0x3B;
const SCSI_REG_SCRATCHB: usize = 0x5C;

const SCSI_DSPS_END: usize = SCSI_REG_DSPS + 3;

const ISTAT_DIP: u8 = 0x04;
const ISTAT_INTF: u8 = 0x08;
const ISTAT_CON: u8 = 0x10;
const ISTAT_SIP: u8 = 0x20;

const DSTAT_DFE: u8 = 0x80;
const DSTAT_MDPE: u8 = 0x40;
const DSTAT_BF: u8 = 0x20;
const DSTAT_ABRT: u8 = 0x10;
const DSTAT_SSI: u8 = 0x08;
const DSTAT_SIR: u8 = 0x04;
const DSTAT_IID: u8 = 0x01;

const DMODE_MAN: u8 = 0x01;
const DCNTL_STD: u8 = 0x10;
const DCNTL_IRQD: u8 = 0x08;
const DCNTL_COM: u8 = 0x04;

pub struct Scsi53c895a {
    regs: [u8; 0x60],
    scripts_ram: Vec<u8>,
    dma_fifo: Vec<u8>,

    pub irq_pending: bool,

    pub id: u8,
    pub disk_image: Vec<u8>,
    pub disk_inserted: bool,
}

impl Scsi53c895a {
    pub fn new() -> Self {
        let mut regs = [0u8; 0x60];
        regs[SCSI_REG_SCID] = 0x07;
        regs[SCSI_REG_SXFER] = 0x00;
        regs[SCSI_REG_CTEST4] = 0x80;
        regs[SCSI_REG_CTEST5] = 0x80;
        regs[SCSI_REG_DCNTL] = 0x04;

        Self {
            regs,
            scripts_ram: vec![0u8; 0x1000],
            dma_fifo: Vec::new(),
            irq_pending: false,
            id: 7,
            disk_image: Vec::new(),
            disk_inserted: false,
        }
    }

    pub fn read(&mut self, offset: usize) -> u32 {
        if offset < 0x60 {
            let val = self.regs[offset];
            if offset == SCSI_REG_ISTAT {
                self.regs[SCSI_REG_ISTAT] &= !ISTAT_INTF;
                self.irq_pending = false;
            }
            val as u32
        } else if offset < 0x60 + 0x1000 {
            let ram_offset = offset - 0x60;
            if ram_offset < self.scripts_ram.len() {
                self.scripts_ram[ram_offset] as u32
            } else {
                0
            }
        } else {
            0
        }
    }

    pub fn write(&mut self, offset: usize, value: u32) {
        let v = value as u8;
        if offset < 0x60 {
            match offset {
                SCSI_REG_ISTAT => {
                    self.regs[SCSI_REG_ISTAT] &= !(v & 0x0F);
                    if (v & ISTAT_SIP) != 0 {
                        self.regs[SCSI_REG_ISTAT] |= ISTAT_SIP;
                    }
                }
                SCSI_REG_DCNTL => {
                    self.regs[SCSI_REG_DCNTL] = v & 0x1C;
                }
                SCSI_REG_DSP..=SCSI_DSPS_END => {
                    self.regs[offset] = v;
                    if offset == SCSI_REG_DSP + 3 {
                        self.regs[SCSI_REG_DSTAT] &= !(DSTAT_DFE | DSTAT_MDPE | DSTAT_BF | DSTAT_ABRT);
                        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
                        self.irq_pending = true;
                    }
                }
                SCSI_REG_DMODE => {
                    self.regs[SCSI_REG_DMODE] = v & DMODE_MAN;
                }
                SCSI_REG_DIEN => {
                    self.regs[SCSI_REG_DIEN] = v;
                }
                SCSI_REG_DCMD => {
                    self.regs[SCSI_REG_DCMD] = v;
                    self.handle_dma_command();
                }
                _ => {
                    self.regs[offset] = v;
                }
            }
        } else if offset < 0x60 + 0x1000 {
            let ram_offset = offset - 0x60;
            if ram_offset < self.scripts_ram.len() {
                self.scripts_ram[ram_offset] = v;
            }
        }
    }

    fn handle_dma_command(&mut self) {
        let dcmd = self.regs[SCSI_REG_DCMD];
        let dbc = self.read_reg32(SCSI_REG_DBC) as usize;
        let dnad = self.read_reg32(SCSI_REG_DNAD) as usize;

        match dcmd & 0x03 {
            0x01 => {
                self.dma_fifo.resize(dbc, 0);
                self.regs[SCSI_REG_DSTAT] |= DSTAT_DFE;
                self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
                self.irq_pending = true;
            }
            0x02 => {
                self.dma_fifo.clear();
                self.regs[SCSI_REG_DSTAT] |= DSTAT_DFE;
                self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
                self.irq_pending = true;
            }
            _ => {}
        }

        if (dcmd & 0x03) == 0x01 && self.disk_inserted && dbc >= 6 {
            self.handle_scsi_command(dbc, dnad);
        }
    }

    fn handle_scsi_command(&mut self, _dbc: usize, _dnad: usize) {
        if self.dma_fifo.len() < 6 {
            return;
        }

        let cdb = &self.dma_fifo[..];
        let opcode = cdb[0];

        match opcode {
            0x00 => self.scsi_test_unit_ready(),
            0x03 => self.scsi_request_sense(),
            0x12 => self.scsi_inquiry(),
            0x25 => {
                if cdb.len() >= 10 {
                    let lba = ((cdb[2] as u64) << 24) | ((cdb[3] as u64) << 16)
                        | ((cdb[4] as u64) << 8) | (cdb[5] as u64);
                    let blocks = ((cdb[7] as u32) << 8) | (cdb[8] as u32);
                    self.scsi_read_capacity10(lba, blocks);
                }
            }
            0x28 => {
                if cdb.len() >= 10 {
                    let lba = ((cdb[2] as u64) << 24) | ((cdb[3] as u64) << 16)
                        | ((cdb[4] as u64) << 8) | (cdb[5] as u64);
                    let blocks = ((cdb[7] as u32) << 8) | (cdb[8] as u32);
                    self.scsi_read10(lba, blocks);
                }
            }
            0x2A => {
                if cdb.len() >= 10 {
                    let lba = ((cdb[2] as u64) << 24) | ((cdb[3] as u64) << 16)
                        | ((cdb[4] as u64) << 8) | (cdb[5] as u64);
                    let blocks = ((cdb[7] as u32) << 8) | (cdb[8] as u32);
                    self.scsi_write10(lba, blocks);
                }
            }
            0x1A => self.scsi_mode_sense6(),
            0x5A => self.scsi_mode_sense10(),
            _ => {}
        }
    }

    fn scsi_test_unit_ready(&mut self) {
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_request_sense(&mut self) {
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_inquiry(&mut self) {
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_read_capacity10(&mut self, _lba: u64, _blocks: u32) {
        if !self.disk_inserted {
            return;
        }
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_read10(&mut self, lba: u64, blocks: u32) {
        if !self.disk_inserted || blocks == 0 {
            return;
        }
        let byte_offset = lba * 512;
        let byte_count = blocks as u64 * 512;
        if (byte_offset as usize) + (byte_count as usize) <= self.disk_image.len() {
        }
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_write10(&mut self, lba: u64, blocks: u32) {
        if !self.disk_inserted || blocks == 0 {
            return;
        }
        let byte_offset = lba * 512;
        let byte_count = blocks as u64 * 512;
        if (byte_offset as usize) + (byte_count as usize) <= self.disk_image.len() {
        }
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_mode_sense6(&mut self) {
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn scsi_mode_sense10(&mut self) {
        self.regs[SCSI_REG_ISTAT] |= ISTAT_INTF;
        self.irq_pending = true;
    }

    fn read_reg32(&self, offset: usize) -> u32 {
        let mut val = 0u32;
        for i in 0..4 {
            if offset + i < self.regs.len() {
                val |= (self.regs[offset + i] as u32) << (i * 8);
            }
        }
        val
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    pub fn insert_disk_image(&mut self, image: Vec<u8>) {
        self.disk_image = image;
        self.disk_inserted = true;
        self.irq_pending = false;
    }

    pub fn eject_disk(&mut self) {
        self.disk_image.clear();
        self.disk_inserted = false;
        self.irq_pending = false;
    }
}