const SATA_CAP: usize = 0x00;
const SATA_GHC: usize = 0x04;
const SATA_IS: usize = 0x08;
const SATA_PI: usize = 0x0C;
const SATA_VS: usize = 0x10;
const SATA_CCC_CTL: usize = 0x14;
const SATA_CCC_PORTS: usize = 0x18;
const SATA_EM_LOC: usize = 0x1C;
const SATA_EM_CTL: usize = 0x20;
const SATA_CAP2: usize = 0x24;
const SATA_BOHC: usize = 0x28;

const SATA_PORT_OFFSET: usize = 0x100;
const SATA_PORT_STRIDE: usize = 0x80;

const PXCLB: usize = 0x00;
const PXCLBU: usize = 0x04;
const PXFB: usize = 0x08;
const PXFBU: usize = 0x0C;
const PXIS: usize = 0x10;
const PXIE: usize = 0x14;
const PXCMD: usize = 0x18;
const PXTFD: usize = 0x20;
const PXSIG: usize = 0x24;
const PXSSTS: usize = 0x28;
const PXSCTL: usize = 0x2C;
const PXSERR: usize = 0x30;
const PXSACT: usize = 0x34;
const PXCI: usize = 0x38;
const PXSNTF: usize = 0x3C;
const PXFBS: usize = 0x40;

const MAX_PORTS: usize = 4;

const GHC_AE: u32 = 1 << 31;
const GHC_IE: u32 = 1 << 1;
const GHC_HR: u32 = 1;

const PXCMD_ST: u32 = 1;
const PXCMD_SUD: u32 = 1 << 1;
const PXCMD_POD: u32 = 1 << 2;
const PXCMD_CLO: u32 = 1 << 3;
const PXCMD_FRE: u32 = 1 << 4;
const PXCMD_FR: u32 = 1 << 14;
const PXCMD_CR: u32 = 1 << 15;

const PXSSTS_DET_PRESENT: u32 = 0x3;
const PXSSTS_IPM_ACTIVE: u32 = 0x100;
const PXSSTS_SPD_GEN1: u32 = 0x010;

const PXIS_DHRS: u32 = 1;
const PXIS_PSS: u32 = 1 << 1;
const PXIS_DSS: u32 = 1 << 2;
const PXIS_SDBS: u32 = 1 << 3;
const PXIS_UFS: u32 = 1 << 4;

const PXTFD_STS_BSY: u8 = 0x80;
const PXTFD_STS_DRDY: u8 = 0x40;
const PXTFD_STS_DRQ: u8 = 0x08;
const PXTFD_STS_ERR: u8 = 0x01;

pub struct SataPort {
    pub clb: u32,
    pub clbu: u32,
    pub fb: u32,
    pub fbu: u32,
    pub is: u32,
    pub ie: u32,
    pub cmd: u32,
    pub tfd: u32,
    pub sig: u32,
    pub ssts: u32,
    pub sctl: u32,
    pub serr: u32,
    pub sact: u32,
    pub ci: u32,
    pub sntf: u32,
    pub fbs: u32,

    pub disk_image: Vec<u8>,
    pub disk_inserted: bool,
    pub sector_count: u64,
    pub boot_status: u32,
}

impl SataPort {
    fn new() -> Self {
        Self {
            clb: 0,
            clbu: 0,
            fb: 0,
            fbu: 0,
            is: 0,
            ie: 0,
            cmd: 0,
            tfd: 0x50,
            sig: 0x0000_0101,
            ssts: 0,
            sctl: 0,
            serr: 0,
            sact: 0,
            ci: 0,
            sntf: 0,
            fbs: 0,
            disk_image: Vec::new(),
            disk_inserted: false,
            sector_count: 0,
            boot_status: 0,
        }
    }

    fn read(&self, offset: usize) -> u32 {
        match offset {
            PXCLB => self.clb,
            PXCLBU => self.clbu,
            PXFB => self.fb,
            PXFBU => self.fbu,
            PXIS => self.is,
            PXIE => self.ie,
            PXCMD => self.cmd,
            PXTFD => self.tfd,
            PXSIG => self.sig,
            PXSSTS => self.ssts,
            PXSCTL => self.sctl,
            PXSERR => self.serr,
            PXSACT => self.sact,
            PXCI => self.ci,
            PXSNTF => self.sntf,
            PXFBS => self.fbs,
            _ => 0,
        }
    }

    fn write(&mut self, offset: usize, value: u32) {
        match offset {
            PXCLB => self.clb = value,
            PXCLBU => self.clbu = value,
            PXFB => self.fb = value,
            PXFBU => self.fbu = value,
            PXIS => {
                self.is &= !value;
            }
            PXIE => self.ie = value,
            PXCMD => {
                let old = self.cmd;
                self.cmd = value;

                if (value & PXCMD_FRE) != 0 && (old & PXCMD_FRE) == 0 {
                    self.cmd |= PXCMD_FR;
                }
                if (value & PXCMD_FRE) == 0 && (old & PXCMD_FRE) != 0 {
                    self.cmd &= !PXCMD_FR;
                }

                if (value & PXCMD_ST) != 0 && (old & PXCMD_ST) == 0 {
                    self.cmd |= PXCMD_CR;
                    self.ssts = PXSSTS_DET_PRESENT | PXSSTS_IPM_ACTIVE | PXSSTS_SPD_GEN1;
                }
                if (value & PXCMD_ST) == 0 && (old & PXCMD_ST) != 0 {
                    self.cmd &= !PXCMD_CR;
                    self.ssts = 0;
                }

                if (value & PXCMD_CLO) != 0 {
                    self.cmd &= !PXCMD_CLO;
                    self.cmd &= !PXCMD_ST;
                    self.cmd &= !PXCMD_CR;
                }
            }
            PXSCTL => self.sctl = value,
            PXSERR => self.serr &= !value,
            PXCI => {
                self.ci = value;
                if value != 0 && self.disk_inserted {
                    self.process_commands();
                }
            }
            _ => {}
        }
    }

    fn process_commands(&mut self) {
        if self.clb == 0 || self.fb == 0 {
            self.ci = 0;
            return;
        }

        let issued = self.ci;
        for slot in 0..32 {
            if (issued & (1 << slot)) == 0 {
                continue;
            }

            let cmd_table_addr = self.clb as u64 + (slot as u64) * 0x80;

            let fis_type = self.read_cmd_byte(cmd_table_addr, 0);
            match fis_type {
                0x27 => {
                    let cmd_fis = self.read_cmd_byte(cmd_table_addr, 2);
                    let lba0 = self.read_cmd_byte(cmd_table_addr, 4) as u64;
                    let lba1 = self.read_cmd_byte(cmd_table_addr, 5) as u64;
                    let lba2 = self.read_cmd_byte(cmd_table_addr, 6) as u64;
                    let lba3 = self.read_cmd_byte(cmd_table_addr, 8) as u64;
                    let lba4 = self.read_cmd_byte(cmd_table_addr, 9) as u64;
                    let lba5 = self.read_cmd_byte(cmd_table_addr, 10) as u64;
                    let lba = lba0 | (lba1 << 8) | (lba2 << 16) | (lba3 << 24) | (lba4 << 32) | (lba5 << 40);
                    let sector_count_lo = self.read_cmd_byte(cmd_table_addr, 12) as u32;
                    let sector_count_hi = self.read_cmd_byte(cmd_table_addr, 13) as u32;
                    let sector_count = sector_count_lo | (sector_count_hi << 8);

                    match cmd_fis {
                        0xEC => self.handle_identify_device(),
                        0x25 => self.handle_read_dma_ext(lba, sector_count),
                        0x35 => self.handle_write_dma_ext(lba, sector_count),
                        0x00 => {}
                        _ => {}
                    }
                }
                _ => {}
            }

            self.ci &= !(1 << slot);
        }

        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn read_cmd_byte(&self, _cmd_table_addr: u64, _offset: usize) -> u8 {
        0
    }

    pub fn process_commands_with_ram(&mut self, ram: &[u8]) {
        if self.clb == 0 || self.fb == 0 {
            self.ci = 0;
            return;
        }

        let issued = self.ci;
        for slot in 0..32 {
            if (issued & (1 << slot)) == 0 {
                continue;
            }

            let cmd_table_base = (self.clb as u64 + (slot as u64) * 0x80) as usize;
            if cmd_table_base + 64 > ram.len() {
                self.ci &= !(1 << slot);
                continue;
            }

            let fis_type = ram[cmd_table_base];
            match fis_type {
                0x27 => {
                    let cmd_fis = ram[cmd_table_base + 2];
                    let lba0 = ram[cmd_table_base + 4] as u64;
                    let lba1 = ram[cmd_table_base + 5] as u64;
                    let lba2 = ram[cmd_table_base + 6] as u64;
                    let lba3 = ram[cmd_table_base + 8] as u64;
                    let lba4 = ram[cmd_table_base + 9] as u64;
                    let lba5 = ram[cmd_table_base + 10] as u64;
                    let lba = lba0 | (lba1 << 8) | (lba2 << 16) | (lba3 << 24) | (lba4 << 32) | (lba5 << 40);
                    let sector_count_lo = ram[cmd_table_base + 12] as u32;
                    let sector_count_hi = ram[cmd_table_base + 13] as u32;
                    let sector_count = sector_count_lo | (sector_count_hi << 8);

                    let prdt_offset = cmd_table_base + 0x80;
                    match cmd_fis {
                        0xEC => self.handle_identify_device_with_ram(ram, prdt_offset),
                        0x25 => self.handle_read_dma_ext_with_ram(lba, sector_count, ram, prdt_offset),
                        0x35 => self.handle_write_dma_ext_with_ram(lba, sector_count, ram, prdt_offset),
                        0x00 => {}
                        _ => {}
                    }
                }
                _ => {}
            }

            self.ci &= !(1 << slot);
        }

        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_identify_device_with_ram(&mut self, _ram: &[u8], _prdt_offset: usize) {
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_read_dma_ext_with_ram(&mut self, lba: u64, sector_count: u32, _ram: &[u8], _prdt_offset: usize) {
        if !self.disk_inserted || sector_count == 0 {
            self.tfd |= PXTFD_STS_ERR as u32;
            return;
        }
        let byte_offset = lba * 512;
        let byte_count = sector_count as u64 * 512;
        if byte_offset + byte_count <= self.disk_image.len() as u64 {
        }
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_write_dma_ext_with_ram(&mut self, lba: u64, sector_count: u32, _ram: &[u8], _prdt_offset: usize) {
        if !self.disk_inserted || sector_count == 0 {
            self.tfd |= PXTFD_STS_ERR as u32;
            return;
        }
        let byte_offset = lba * 512;
        let byte_count = sector_count as u64 * 512;
        if (byte_offset as usize) + (byte_count as usize) <= self.disk_image.len() {
        }
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_identify_device(&mut self) {
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_read_dma_ext(&mut self, lba: u64, sector_count: u32) {
        if !self.disk_inserted || sector_count == 0 {
            self.tfd |= PXTFD_STS_ERR as u32;
            return;
        }
        let _byte_offset = lba * 512;
        let _byte_count = sector_count as u64 * 512;
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    fn handle_write_dma_ext(&mut self, lba: u64, sector_count: u32) {
        if !self.disk_inserted || sector_count == 0 {
            self.tfd |= PXTFD_STS_ERR as u32;
            return;
        }
        let _byte_offset = lba * 512;
        let _byte_count = sector_count as u64 * 512;
        self.tfd &= !(PXTFD_STS_BSY as u32 | PXTFD_STS_DRQ as u32);
        self.tfd |= PXTFD_STS_DRDY as u32;
        self.is |= PXIS_DHRS;
    }

    pub fn insert_disk_image(&mut self, image: Vec<u8>) {
        let len = image.len();
        self.sector_count = (len / 512) as u64;
        self.disk_image = image;
        self.disk_inserted = true;
        self.sig = 0x0000_0101;
    }

    pub fn eject_disk(&mut self) {
        self.disk_image.clear();
        self.disk_inserted = false;
        self.sector_count = 0;
        self.sig = 0xFFFF_FFFF;
    }
}

pub struct SataController {
    pub cap: u32,
    pub ghc: u32,
    pub is: u32,
    pub pi: u32,
    pub vs: u32,
    pub ccc_ctl: u32,
    pub ccc_ports: u32,
    pub em_loc: u32,
    pub em_ctl: u32,
    pub cap2: u32,
    pub bohc: u32,

    pub ports: [SataPort; MAX_PORTS],
    pub irq_pending: bool,
}

impl SataController {
    pub fn new() -> Self {
        let ports: [SataPort; MAX_PORTS] = std::array::from_fn(|_| SataPort::new());

        Self {
            cap: (MAX_PORTS as u32) | (0x1F << 8) | (1 << 18) | (1 << 20) | (1 << 26),
            ghc: 0,
            is: 0,
            pi: ((1u32 << MAX_PORTS) - 1),
            vs: 0x0001_0300,
            ccc_ctl: 0,
            ccc_ports: 0,
            em_loc: 0,
            em_ctl: 0,
            cap2: 0,
            bohc: 0,
            ports,
            irq_pending: false,
        }
    }

    pub fn read(&mut self, offset: usize) -> u32 {
        if offset < SATA_PORT_OFFSET {
            match offset {
                SATA_CAP => self.cap,
                SATA_GHC => self.ghc,
                SATA_IS => self.is,
                SATA_PI => self.pi,
                SATA_VS => self.vs,
                SATA_CCC_CTL => self.ccc_ctl,
                SATA_CCC_PORTS => self.ccc_ports,
                SATA_EM_LOC => self.em_loc,
                SATA_EM_CTL => self.em_ctl,
                SATA_CAP2 => self.cap2,
                SATA_BOHC => self.bohc,
                _ => 0,
            }
        } else {
            let port_offset = offset - SATA_PORT_OFFSET;
            let port_idx = port_offset / SATA_PORT_STRIDE;
            let reg_offset = port_offset % SATA_PORT_STRIDE;

            if port_idx < MAX_PORTS {
                self.ports[port_idx].read(reg_offset)
            } else {
                0
            }
        }
    }

    pub fn write(&mut self, offset: usize, value: u32) {
        if offset < SATA_PORT_OFFSET {
            match offset {
                SATA_GHC => {
                    let old = self.ghc;
                    self.ghc = value & (GHC_AE | GHC_IE);
                    if (value & GHC_HR) != 0 {
                        self.perform_reset();
                    }
                    if (value & GHC_AE) != 0 && (old & GHC_AE) == 0 {
                        self.is = 0;
                    }
                }
                SATA_IS => {
                    self.is &= !value;
                    self.update_irq();
                }
                SATA_CCC_CTL => self.ccc_ctl = value,
                SATA_CCC_PORTS => self.ccc_ports = value,
                SATA_EM_LOC => self.em_loc = value,
                SATA_EM_CTL => self.em_ctl = value,
                SATA_BOHC => self.bohc = value,
                _ => {}
            }
        } else {
            let port_offset = offset - SATA_PORT_OFFSET;
            let port_idx = port_offset / SATA_PORT_STRIDE;
            let reg_offset = port_offset % SATA_PORT_STRIDE;

            if port_idx < MAX_PORTS {
                self.ports[port_idx].write(reg_offset, value);
                self.update_irq();
            }
        }
    }

    fn perform_reset(&mut self) {
        for port in self.ports.iter_mut() {
            port.cmd = 0;
            port.ssts = 0;
            port.sig = 0xFFFF_FFFF;
            port.tfd = 0x7F;
            port.serr = 0;
            port.sact = 0;
            port.ci = 0;
        }
        self.ghc = 0;
        self.is = 0;
        self.irq_pending = false;
    }

    fn update_irq(&mut self) {
        self.irq_pending = false;
        for (i, port) in self.ports.iter().enumerate() {
            if (port.is & port.ie) != 0 {
                self.is |= 1 << i;
                self.irq_pending = true;
            }
        }
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    pub fn insert_disk_image(&mut self, port: usize, image: Vec<u8>) {
        if port < MAX_PORTS {
            self.ports[port].insert_disk_image(image);
        }
    }

    pub fn eject_disk(&mut self, port: usize) {
        if port < MAX_PORTS {
            self.ports[port].eject_disk();
        }
    }
}