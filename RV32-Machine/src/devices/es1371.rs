const ES1371_REG_CONTROL: usize = 0x00;
const ES1371_REG_STATUS: usize = 0x04;
const ES1371_REG_UART_DATA: usize = 0x08;
const ES1371_REG_UART_STATUS: usize = 0x09;
const ES1371_REG_MEMPAGE: usize = 0x0C;
const ES1371_REG_CODEC: usize = 0x10;
const ES1371_REG_CODEC_READ: usize = 0x14;
const ES1371_REG_SERIAL_INT: usize = 0x20;

const ES1371_REG_DAC1_COUNT: usize = 0x24;
const ES1371_REG_DAC2_COUNT: usize = 0x28;
const ES1371_REG_ADC_COUNT: usize = 0x2C;
const ES1371_REG_DAC1_FRAME: usize = 0x30;
const ES1371_REG_DAC2_FRAME: usize = 0x34;
const ES1371_REG_ADC_FRAME: usize = 0x38;

const ES1371_STAT_DAC1_IRQ: u32 = 1 << 0;
const ES1371_STAT_DAC2_IRQ: u32 = 1 << 1;
const ES1371_STAT_ADC_IRQ: u32 = 1 << 2;

const ES1371_CTRL_DAC1_EN: u32 = 1 << 0;
const ES1371_CTRL_DAC2_EN: u32 = 1 << 1;
const ES1371_CTRL_ADC_EN: u32 = 1 << 2;

pub struct Es1371 {
    pub control: u32,
    pub status: u32,
    pub mempage: u32,
    pub codec: u32,
    pub serial_int: u32,

    pub dac1_count: u32,
    pub dac2_count: u32,
    pub adc_count: u32,
    pub dac1_frame: u32,
    pub dac2_frame: u32,
    pub adc_frame: u32,

    pub dac1_buffer: Vec<u16>,
    pub dac2_buffer: Vec<u16>,
    pub adc_buffer: Vec<u16>,

    pub dac1_pos: usize,
    pub dac2_pos: usize,
    pub adc_pos: usize,

    pub audio_output: Vec<f32>,
    irq_pending: bool,
}

impl Es1371 {
    pub fn new() -> Self {
        Self {
            control: 0,
            status: 0x0000_0007,
            mempage: 0,
            codec: 0,
            serial_int: 0,

            dac1_count: 0,
            dac2_count: 0,
            adc_count: 0,
            dac1_frame: 0,
            dac2_frame: 0,
            adc_frame: 0,

            dac1_buffer: vec![0u16; 0x10000],
            dac2_buffer: vec![0u16; 0x10000],
            adc_buffer: vec![0u16; 0x10000],

            dac1_pos: 0,
            dac2_pos: 0,
            adc_pos: 0,

            audio_output: Vec::new(),
            irq_pending: false,
        }
    }

    pub fn read(&mut self, offset: usize) -> u32 {
        match offset {
            ES1371_REG_CONTROL => self.control,
            ES1371_REG_STATUS => {
                let s = self.status;
                self.status &= !(ES1371_STAT_DAC1_IRQ | ES1371_STAT_DAC2_IRQ | ES1371_STAT_ADC_IRQ);
                self.irq_pending = false;
                s
            }
            ES1371_REG_MEMPAGE => self.mempage,
            ES1371_REG_CODEC => self.codec,
            ES1371_REG_CODEC_READ => 0x7FFF,
            ES1371_REG_SERIAL_INT => self.serial_int,
            ES1371_REG_DAC1_COUNT => self.dac1_count,
            ES1371_REG_DAC2_COUNT => self.dac2_count,
            ES1371_REG_ADC_COUNT => self.adc_count,
            ES1371_REG_DAC1_FRAME => self.dac1_frame,
            ES1371_REG_DAC2_FRAME => self.dac2_frame,
            ES1371_REG_ADC_FRAME => self.adc_frame,
            _ => 0,
        }
    }

    pub fn write(&mut self, offset: usize, value: u8) {
        let val32 = value as u32;
        match offset {
            ES1371_REG_CONTROL => {
                self.control = val32;
            }
            ES1371_REG_SERIAL_INT => {
                self.serial_int = val32;
            }
            ES1371_REG_CODEC => {
                self.codec = val32;
            }
            ES1371_REG_MEMPAGE => {
                self.mempage = val32;
            }
            ES1371_REG_DAC1_COUNT => {
                self.dac1_count = val32;
                self.dac1_pos = 0;
            }
            ES1371_REG_DAC2_COUNT => {
                self.dac2_count = val32;
                self.dac2_pos = 0;
            }
            ES1371_REG_ADC_COUNT => {
                self.adc_count = val32;
                self.adc_pos = 0;
            }
            ES1371_REG_DAC1_FRAME => {
                self.dac1_frame = val32;
            }
            ES1371_REG_DAC2_FRAME => {
                self.dac2_frame = val32;
            }
            ES1371_REG_ADC_FRAME => {
                self.adc_frame = val32;
            }
            _ => {}
        }
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    pub fn output_samples(&mut self) -> Vec<f32> {
        let out = self.audio_output.clone();
        self.audio_output.clear();
        out
    }

    pub fn tick(&mut self) {
        if (self.control & ES1371_CTRL_DAC1_EN) != 0 && self.dac1_count > 0 {
            if self.dac1_pos < self.dac1_count as usize {
                let sample = self.dac1_buffer[self.dac1_pos] as i16;
                self.audio_output.push(sample as f32 / 32768.0);
                self.dac1_pos += 1;
            }
            if self.dac1_pos >= self.dac1_count as usize {
                self.status |= ES1371_STAT_DAC1_IRQ;
                self.irq_pending = true;
            }
        }

        if (self.control & ES1371_CTRL_DAC2_EN) != 0 && self.dac2_count > 0 {
            if self.dac2_pos < self.dac2_count as usize {
                let sample = self.dac2_buffer[self.dac2_pos] as i16;
                self.audio_output.push(sample as f32 / 32768.0);
                self.dac2_pos += 1;
            }
            if self.dac2_pos >= self.dac2_count as usize {
                self.status |= ES1371_STAT_DAC2_IRQ;
                self.irq_pending = true;
            }
        }

        if (self.control & ES1371_CTRL_ADC_EN) != 0 && self.adc_count > 0 {
            if self.adc_pos < self.adc_count as usize {
                self.adc_pos += 1;
            }
            if self.adc_pos >= self.adc_count as usize {
                self.status |= ES1371_STAT_ADC_IRQ;
                self.irq_pending = true;
            }
        }
    }

    pub fn dma_transfer_from_ram(&mut self, ram: &[u8], channel: u32, addr: u32, count: u32) {
        let ram_offset = addr as usize;
        if ram_offset >= ram.len() {
            return;
        }

        match channel {
            0 => {
                if (self.control & ES1371_CTRL_DAC1_EN) != 0 {
                    let samples = (count as usize / 2).min(self.dac1_buffer.len() - self.dac1_pos);
                    for i in 0..samples {
                        let byte_offset = ram_offset + i * 2;
                        if byte_offset + 1 < ram.len() {
                            let lo = ram[byte_offset] as u16;
                            let hi = ram[byte_offset + 1] as u16;
                            self.dac1_buffer[self.dac1_pos + i] = lo | (hi << 8);
                        }
                    }
                    self.dac1_pos = 0;
                    self.dac1_count = samples as u32;
                }
            }
            1 => {
                if (self.control & ES1371_CTRL_DAC2_EN) != 0 {
                    let samples = (count as usize / 2).min(self.dac2_buffer.len() - self.dac2_pos);
                    for i in 0..samples {
                        let byte_offset = ram_offset + i * 2;
                        if byte_offset + 1 < ram.len() {
                            let lo = ram[byte_offset] as u16;
                            let hi = ram[byte_offset + 1] as u16;
                            self.dac2_buffer[self.dac2_pos + i] = lo | (hi << 8);
                        }
                    }
                    self.dac2_pos = 0;
                    self.dac2_count = samples as u32;
                }
            }
            2 => {
                if (self.control & ES1371_CTRL_ADC_EN) != 0 {
                    let samples = (count as usize / 2).min(self.adc_buffer.len() - self.adc_pos);
                    for i in 0..samples {
                        self.adc_buffer[self.adc_pos + i] = 0;
                    }
                    self.adc_pos = 0;
                    self.adc_count = samples as u32;
                }
            }
            _ => {}
        }
    }

    pub fn dma_transfer_to_ram(&self, ram: &mut [u8], channel: u32, addr: u32, count: u32) {
        let ram_offset = addr as usize;
        if ram_offset >= ram.len() {
            return;
        }

        if channel == 2 && (self.control & ES1371_CTRL_ADC_EN) != 0 {
            let samples = (count as usize / 2).min(self.adc_buffer.len());
            for i in 0..samples {
                let byte_offset = ram_offset + i * 2;
                if byte_offset + 1 < ram.len() {
                    let sample = self.adc_buffer[i];
                    ram[byte_offset] = (sample & 0xFF) as u8;
                    ram[byte_offset + 1] = ((sample >> 8) & 0xFF) as u8;
                }
            }
        }
    }

    pub fn reset(&mut self) {
        self.control = 0;
        self.status = 0x0000_0007;
        self.mempage = 0;
        self.codec = 0;
        self.serial_int = 0;
        self.dac1_count = 0;
        self.dac2_count = 0;
        self.adc_count = 0;
        self.dac1_frame = 0;
        self.dac2_frame = 0;
        self.adc_frame = 0;
        self.dac1_pos = 0;
        self.dac2_pos = 0;
        self.adc_pos = 0;
        self.audio_output.clear();
        self.irq_pending = false;
    }
}