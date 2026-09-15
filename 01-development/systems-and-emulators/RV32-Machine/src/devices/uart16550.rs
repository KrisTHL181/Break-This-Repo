const RBR: usize = 0x00;
const THR: usize = 0x00;
const IER: usize = 0x01;
const IIR: usize = 0x02;
const FCR: usize = 0x02;
const LCR: usize = 0x03;
const MCR: usize = 0x04;
const LSR: usize = 0x05;
const MSR: usize = 0x06;
const SCR: usize = 0x07;
const DLL: usize = 0x00;
const DLM: usize = 0x01;

const LSR_THRE: u8 = 0x20;
const LSR_TEMT: u8 = 0x40;
const LSR_DR: u8 = 0x01;

const IER_RX: u8 = 0x01;
const IER_TX: u8 = 0x02;

const IIR_NO_INT: u8 = 0x01;
const IIR_THR_EMPTY: u8 = 0x02;
const IIR_RX_AVAIL: u8 = 0x04;

pub struct Uart16550 {
    pub rbr: u8,
    pub thr: u8,
    pub ier: u8,
    pub iir: u8,
    pub fcr: u8,
    pub lcr: u8,
    pub mcr: u8,
    pub lsr: u8,
    pub msr: u8,
    pub scr: u8,
    pub dll: u8,
    pub dlm: u8,

    pub rx_buffer: Vec<u8>,
    pub tx_buffer: Vec<u8>,

    irq_pending: bool,
}

impl Uart16550 {
    pub fn new() -> Self {
        Self {
            rbr: 0,
            thr: 0,
            ier: 0,
            iir: IIR_NO_INT,
            fcr: 0,
            lcr: 0,
            mcr: 0,
            lsr: LSR_THRE | LSR_TEMT,
            msr: 0xB0,
            scr: 0,
            dll: 0,
            dlm: 0,
            rx_buffer: Vec::new(),
            tx_buffer: Vec::new(),
            irq_pending: false,
        }
    }

    pub fn read(&mut self, offset: usize) -> u8 {
        let dlab = (self.lcr & 0x80) != 0;
        match offset {
            RBR | DLL if !dlab => {
                let val = self.rx_buffer.pop().unwrap_or(0);
                self.update_irq();
                val
            }
            DLL if dlab => self.dll,
            IER | DLM if dlab => self.dll,
            IER | DLM if !dlab => self.ier,
            IIR | FCR if offset == IIR => self.iir,
            LCR => self.lcr,
            MCR => self.mcr,
            LSR => self.lsr,
            MSR => self.msr,
            SCR => self.scr,
            _ => 0,
        }
    }

    pub fn write(&mut self, offset: usize, value: u8) {
        let dlab = (self.lcr & 0x80) != 0;
        match offset {
            THR | DLL if !dlab => {
                self.thr = value;
                self.tx_buffer.push(value);
                self.update_irq();
            }
            DLL if dlab => {
                self.dll = value;
            }
            IER | DLM if dlab => {
                self.dlm = value;
            }
            IER | DLM if !dlab => {
                self.ier = value;
                self.update_irq();
            }
            FCR => {
                self.fcr = value;
                if (value & 0x01) != 0 {
                    self.rx_buffer.clear();
                }
            }
            LCR => {
                self.lcr = value;
            }
            MCR => {
                self.mcr = value;
            }
            SCR => {
                self.scr = value;
            }
            _ => {}
        }
    }

    pub fn receive_byte(&mut self, byte: u8) {
        self.rx_buffer.push(byte);
        self.lsr |= LSR_DR;
        self.update_irq();
    }

    pub fn can_send(&self) -> bool {
        (self.lsr & (LSR_THRE | LSR_TEMT)) == (LSR_THRE | LSR_TEMT)
    }

    pub fn tx_drain(&mut self) -> Vec<u8> {
        let data = self.tx_buffer.clone();
        self.tx_buffer.clear();
        self.lsr |= LSR_THRE | LSR_TEMT;
        self.update_irq();
        data
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    fn update_irq(&mut self) {
        self.irq_pending = false;
        self.iir = IIR_NO_INT;

        if (self.ier & IER_RX) != 0 && (self.lsr & LSR_DR) != 0 {
            self.irq_pending = true;
            self.iir = IIR_RX_AVAIL;
        } else if (self.ier & IER_TX) != 0 && (self.lsr & LSR_THRE) != 0 {
            self.irq_pending = true;
            self.iir = IIR_THR_EMPTY;
        }
    }

    pub fn reset(&mut self) {
        self.rbr = 0;
        self.thr = 0;
        self.ier = 0;
        self.iir = IIR_NO_INT;
        self.fcr = 0;
        self.lcr = 0;
        self.mcr = 0;
        self.lsr = LSR_THRE | LSR_TEMT;
        self.msr = 0xB0;
        self.scr = 0;
        self.dll = 0;
        self.dlm = 0;
        self.rx_buffer.clear();
        self.tx_buffer.clear();
        self.irq_pending = false;
    }

    pub fn get_baud_rate(&self) -> u32 {
        let divisor = (self.dlm as u32) << 8 | self.dll as u32;
        if divisor == 0 {
            0
        } else {
            1843200 / (16 * divisor)
        }
    }

    pub fn is_dlab(&self) -> bool {
        (self.lcr & 0x80) != 0
    }

    pub fn rx_buffer_len(&self) -> usize {
        self.rx_buffer.len()
    }

    pub fn tx_buffer_len(&self) -> usize {
        self.tx_buffer.len()
    }
}