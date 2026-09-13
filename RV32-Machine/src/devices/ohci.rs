const OHCI_HC_REVISION: usize = 0x00;
const OHCI_HC_CONTROL: usize = 0x04;
const OHCI_HC_CMD_STATUS: usize = 0x08;
const OHCI_HC_INT_STATUS: usize = 0x0C;
const OHCI_HC_INT_ENABLE: usize = 0x10;
const OHCI_HC_INT_DISABLE: usize = 0x14;
const OHCI_HC_HCCA: usize = 0x18;
const OHCI_HC_PERIOD_CURRENT_ED: usize = 0x1C;
const OHCI_HC_CONTROL_HEAD_ED: usize = 0x20;
const OHCI_HC_CONTROL_CURRENT_ED: usize = 0x24;
const OHCI_HC_BULK_HEAD_ED: usize = 0x28;
const OHCI_HC_BULK_CURRENT_ED: usize = 0x2C;
const OHCI_HC_DONE_HEAD: usize = 0x30;
const OHCI_HC_FM_INTERVAL: usize = 0x34;
const OHCI_HC_FM_REMAINING: usize = 0x38;
const OHCI_HC_FM_NUMBER: usize = 0x3C;
const OHCI_HC_PERIODIC_START: usize = 0x40;
const OHCI_HC_LS_THRESHOLD: usize = 0x44;
const OHCI_HC_RH_DESCRIPTOR_A: usize = 0x48;
const OHCI_HC_RH_DESCRIPTOR_B: usize = 0x4C;
const OHCI_HC_RH_STATUS: usize = 0x50;
const OHCI_HC_RH_PORT_STATUS1: usize = 0x54;
const OHCI_HC_RH_PORT_STATUS2: usize = 0x58;
const OHCI_HC_RH_PORT_STATUS3: usize = 0x5C;
const OHCI_HC_RH_PORT_STATUS4: usize = 0x60;

const OHCI_CTRL_CBSR: u32 = 0x0000_0003;
const OHCI_CTRL_PLE: u32 = 0x0000_0004;
const OHCI_CTRL_IE: u32 = 0x0000_0008;
const OHCI_CTRL_CLE: u32 = 0x0000_0010;
const OHCI_CTRL_BLE: u32 = 0x0000_0020;
const OHCI_CTRL_HCFS_MASK: u32 = 0x0000_00C0;
const OHCI_CTRL_HCFS_RESET: u32 = 0x0000_0000;
const OHCI_CTRL_HCFS_RESUME: u32 = 0x0000_0040;
const OHCI_CTRL_HCFS_OPER: u32 = 0x0000_0080;
const OHCI_CTRL_HCFS_SUSPEND: u32 = 0x0000_00C0;
const OHCI_CTRL_IR: u32 = 0x0000_0100;
const OHCI_CTRL_RWC: u32 = 0x0000_0200;
const OHCI_CTRL_RWE: u32 = 0x0000_0400;

const OHCI_CMD_HCR: u32 = 0x0000_0001;
const OHCI_CMD_CLF: u32 = 0x0000_0002;
const OHCI_CMD_BLF: u32 = 0x0000_0004;
const OHCI_CMD_OCR: u32 = 0x0000_0008;

const OHCI_INT_SO: u32 = 0x0000_0001;
const OHCI_INT_WDH: u32 = 0x0000_0002;
const OHCI_INT_SF: u32 = 0x0000_0004;
const OHCI_INT_RD: u32 = 0x0000_0008;
const OHCI_INT_UE: u32 = 0x0000_0010;
const OHCI_INT_FNO: u32 = 0x0000_0020;
const OHCI_INT_RHSC: u32 = 0x0000_0040;
const OHCI_INT_OC: u32 = 0x4000_0000;
const OHCI_INT_MIE: u32 = 0x8000_0000;

const OHCI_RH_NDP: u32 = 0x0000_00FF;
const OHCI_RH_PSM: u32 = 0x0000_0100;
const OHCI_RH_NPS: u32 = 0x0000_0200;
const OHCI_RH_DT: u32 = 0x0000_0400;
const OHCI_RH_OCPM: u32 = 0x0000_0800;
const OHCI_RH_NOCP: u32 = 0x0000_1000;
const OHCI_RH_POTPGT_MASK: u32 = 0xFF00_0000;

const OHCI_PORT_CCS: u32 = 0x0000_0001;
const OHCI_PORT_PES: u32 = 0x0000_0002;
const OHCI_PORT_PSS: u32 = 0x0000_0004;
const OHCI_PORT_POCI: u32 = 0x0000_0008;
const OHCI_PORT_PRS: u32 = 0x0000_0010;
const OHCI_PORT_PPS: u32 = 0x0000_0100;
const OHCI_PORT_LSDA: u32 = 0x0000_0200;
const OHCI_PORT_CSC: u32 = 0x0001_0000;
const OHCI_PORT_PESC: u32 = 0x0002_0000;
const OHCI_PORT_PSSC: u32 = 0x0004_0000;
const OHCI_PORT_OCIC: u32 = 0x0008_0000;
const OHCI_PORT_PRSC: u32 = 0x0010_0000;

pub struct OhciController {
    pub revision: u32,
    pub control: u32,
    pub cmd_status: u32,
    pub int_status: u32,
    pub int_enable: u32,
    pub hcca: u32,
    pub period_current_ed: u32,
    pub control_head_ed: u32,
    pub control_current_ed: u32,
    pub bulk_head_ed: u32,
    pub bulk_current_ed: u32,
    pub done_head: u32,
    pub fm_interval: u32,
    pub fm_remaining: u32,
    pub fm_number: u32,
    pub periodic_start: u32,
    pub ls_threshold: u32,
    pub rh_descriptor_a: u32,
    pub rh_descriptor_b: u32,
    pub rh_status: u32,
    pub rh_port_status: [u32; 4],

    // Optional emulated USB MSD image
    pub disk_image: Vec<u8>,
    pub disk_inserted: bool,

    irq_pending: bool,
    frame_counter: u32,
}

impl OhciController {
    pub fn new() -> Self {
        Self {
            revision: 0x0000_0110,
            control: 0,
            cmd_status: 0,
            int_status: 0,
            int_enable: 0,
            hcca: 0,
            period_current_ed: 0,
            control_head_ed: 0,
            control_current_ed: 0,
            bulk_head_ed: 0,
            bulk_current_ed: 0,
            done_head: 0,
            fm_interval: 0x0000_2EDF,
            fm_remaining: 0,
            fm_number: 0,
            periodic_start: 0,
            ls_threshold: 0,
            rh_descriptor_a: (4 & OHCI_RH_NDP) | OHCI_RH_NPS | OHCI_RH_NOCP | (10 << 24),
            rh_descriptor_b: 0,
            rh_status: 0,
            rh_port_status: [
                OHCI_PORT_CCS | OHCI_PORT_PPS,
                OHCI_PORT_PPS,
                OHCI_PORT_PPS,
                OHCI_PORT_PPS,
            ],
            disk_image: Vec::new(),
            disk_inserted: false,
            irq_pending: false,
            frame_counter: 0,
        }
    }

    pub fn get_disk_image(&self) -> Option<&Vec<u8>> {
        if self.disk_inserted {
            Some(&self.disk_image)
        } else {
            None
        }
    }

    pub fn insert_disk_image(&mut self, image: Vec<u8>) {
        self.disk_image = image;
        self.disk_inserted = true;
        self.irq_pending = true;
    }

    pub fn eject_disk(&mut self) {
        self.disk_image.clear();
        self.disk_inserted = false;
        self.irq_pending = false;
    }

    pub fn read(&mut self, offset: usize) -> u32 {
        match offset {
            OHCI_HC_REVISION => self.revision,
            OHCI_HC_CONTROL => self.control,
            OHCI_HC_CMD_STATUS => self.cmd_status,
            OHCI_HC_INT_STATUS => self.int_status,
            OHCI_HC_INT_ENABLE => self.int_enable,
            OHCI_HC_HCCA => self.hcca,
            OHCI_HC_PERIOD_CURRENT_ED => self.period_current_ed,
            OHCI_HC_CONTROL_HEAD_ED => self.control_head_ed,
            OHCI_HC_CONTROL_CURRENT_ED => self.control_current_ed,
            OHCI_HC_BULK_HEAD_ED => self.bulk_head_ed,
            OHCI_HC_BULK_CURRENT_ED => self.bulk_current_ed,
            OHCI_HC_DONE_HEAD => self.done_head,
            OHCI_HC_FM_INTERVAL => self.fm_interval,
            OHCI_HC_FM_REMAINING => self.fm_remaining,
            OHCI_HC_FM_NUMBER => self.fm_number,
            OHCI_HC_PERIODIC_START => self.periodic_start,
            OHCI_HC_LS_THRESHOLD => self.ls_threshold,
            OHCI_HC_RH_DESCRIPTOR_A => self.rh_descriptor_a,
            OHCI_HC_RH_DESCRIPTOR_B => self.rh_descriptor_b,
            OHCI_HC_RH_STATUS => self.rh_status,
            OHCI_HC_RH_PORT_STATUS1 => self.rh_port_status[0],
            OHCI_HC_RH_PORT_STATUS2 => self.rh_port_status[1],
            OHCI_HC_RH_PORT_STATUS3 => self.rh_port_status[2],
            OHCI_HC_RH_PORT_STATUS4 => self.rh_port_status[3],
            _ => 0,
        }
    }

    pub fn write(&mut self, offset: usize, value: u32) {
        match offset {
            OHCI_HC_CONTROL => {
                let old_hcfs = self.control & OHCI_CTRL_HCFS_MASK;
                self.control = (value & 0x0000_07FF) | (self.control & !0x0000_07FF);
                let new_hcfs = self.control & OHCI_CTRL_HCFS_MASK;

                if old_hcfs != new_hcfs {
                    match new_hcfs {
                        OHCI_CTRL_HCFS_RESET => {
                            self.cmd_status = 0;
                            self.int_status = 0;
                        }
                        OHCI_CTRL_HCFS_OPER => {
                            self.cmd_status &= !OHCI_CMD_HCR;
                        }
                        _ => {}
                    }
                }
            }
            OHCI_HC_CMD_STATUS => {
                if (value & OHCI_CMD_HCR) != 0 {
                    self.control = (self.control & !OHCI_CTRL_HCFS_MASK) | OHCI_CTRL_HCFS_RESET;
                    self.cmd_status = OHCI_CMD_HCR;
                }
                self.cmd_status = (self.cmd_status & OHCI_CMD_HCR)
                    | (value & !OHCI_CMD_HCR);
            }
            OHCI_HC_INT_STATUS => {
                self.int_status &= !value;
                self.update_irq();
            }
            OHCI_HC_INT_ENABLE => {
                self.int_enable = value;
                self.update_irq();
            }
            OHCI_HC_INT_DISABLE => {
                self.int_enable &= !value;
                self.update_irq();
            }
            OHCI_HC_HCCA => self.hcca = value & !0xFF,
            OHCI_HC_PERIOD_CURRENT_ED => self.period_current_ed = value,
            OHCI_HC_CONTROL_HEAD_ED => self.control_head_ed = value,
            OHCI_HC_BULK_HEAD_ED => self.bulk_head_ed = value,
            OHCI_HC_DONE_HEAD => self.done_head = value,
            OHCI_HC_FM_INTERVAL => self.fm_interval = (value & 0xFFFF) | 0x2EDF,
            OHCI_HC_PERIODIC_START => self.periodic_start = value,
            OHCI_HC_LS_THRESHOLD => self.ls_threshold = value,
            OHCI_HC_RH_DESCRIPTOR_A => {}, // Read-only
            OHCI_HC_RH_DESCRIPTOR_B => {
                self.rh_descriptor_b = value & 0x0000_FFFF;
            }
            OHCI_HC_RH_STATUS => {
                self.rh_status = value;
            }
            OHCI_HC_RH_PORT_STATUS1 => {
                self.write_port_status(0, value);
            }
            OHCI_HC_RH_PORT_STATUS2 => {
                self.write_port_status(1, value);
            }
            OHCI_HC_RH_PORT_STATUS3 => {
                self.write_port_status(2, value);
            }
            OHCI_HC_RH_PORT_STATUS4 => {
                self.write_port_status(3, value);
            }
            _ => {}
        }
    }

    fn write_port_status(&mut self, port: usize, value: u32) {
        let ps = &mut self.rh_port_status[port];

        if (value & OHCI_PORT_PES) != 0 {
            *ps |= OHCI_PORT_PES;
        }
        if (value & OHCI_PORT_PRS) != 0 {
            *ps |= OHCI_PORT_PRS;
            *ps |= OHCI_PORT_PRSC;
        }
        if (value & OHCI_PORT_PSS) != 0 {
            *ps |= OHCI_PORT_PSS;
        }
        if (value & OHCI_PORT_POCI) != 0 {
            *ps &= !OHCI_PORT_POCI;
        }
        if (value & OHCI_PORT_CSC) != 0 {
            *ps &= !OHCI_PORT_CSC;
        }
        if (value & OHCI_PORT_PESC) != 0 {
            *ps &= !OHCI_PORT_PESC;
        }
        if (value & OHCI_PORT_PSSC) != 0 {
            *ps &= !OHCI_PORT_PSSC;
        }
        if (value & OHCI_PORT_OCIC) != 0 {
            *ps &= !OHCI_PORT_OCIC;
        }
        if (value & OHCI_PORT_PRSC) != 0 {
            *ps &= !OHCI_PORT_PRSC;
        }
    }

    fn update_irq(&mut self) {
        self.irq_pending = (self.int_status & self.int_enable) != 0;
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    pub fn tick(&mut self) {
        self.frame_counter += 1;

        let hcfs = self.control & OHCI_CTRL_HCFS_MASK;
        if hcfs != OHCI_CTRL_HCFS_OPER {
            return;
        }

        if self.frame_counter % 12000 == 0 {
            self.fm_number = self.fm_number.wrapping_add(1);
            self.int_status |= OHCI_INT_SF;
            self.update_irq();
        }

        if self.frame_counter % 12000 == 0 {
            self.int_status |= OHCI_INT_SO;
            self.update_irq();
        }

        if self.frame_counter % 60000 == 0 {
            self.process_done_queue();
        }
    }

    fn process_done_queue(&mut self) {
        if self.control_head_ed != 0 && (self.control & OHCI_CTRL_CLE) != 0 {
            self.process_ed_chain(self.control_head_ed, true);
        }

        if self.bulk_head_ed != 0 && (self.control & OHCI_CTRL_BLE) != 0 {
            self.process_ed_chain(self.bulk_head_ed, false);
        }
    }

    fn process_ed_chain(&mut self, _head_ed: u32, _is_control: bool) {
    }

    pub fn process_with_ram(&mut self, _ram: &[u8]) {
        if (self.control & OHCI_CTRL_HCFS_MASK) != OHCI_CTRL_HCFS_OPER {
            return;
        }

        if (self.control & OHCI_CTRL_CLE) != 0 && self.control_head_ed != 0 {
            self.walk_control_list();
        }

        if (self.control & OHCI_CTRL_BLE) != 0 && self.bulk_head_ed != 0 {
            self.walk_bulk_list();
        }

        if (self.control & OHCI_CTRL_PLE) != 0 && self.period_current_ed != 0 {
            self.walk_periodic_list();
        }
    }

    fn walk_control_list(&mut self) {
    }

    fn walk_bulk_list(&mut self) {
    }

    fn walk_periodic_list(&mut self) {
    }
}