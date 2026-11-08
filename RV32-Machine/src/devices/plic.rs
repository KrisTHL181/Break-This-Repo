const PLIC_SOURCE_BASE: u32 = 0x0000;
const PLIC_PRIORITY_BASE: u32 = 0x0000;
const PLIC_PENDING_BASE: u32 = 0x1000;
const PLIC_ENABLE_BASE: u32 = 0x2000;
const PLIC_THRESHOLD: u32 = 0x200000;
const PLIC_CLAIM: u32 = 0x200004;

const MAX_INTERRUPTS: usize = 64;

pub struct Plic {
    pub priorities: [u32; MAX_INTERRUPTS],
    pending: [bool; MAX_INTERRUPTS],
    enabled: [bool; MAX_INTERRUPTS],
    pub threshold: u32,
}

impl Plic {
    pub fn new() -> Self {
        Self {
            priorities: [0u32; MAX_INTERRUPTS],
            pending: [false; MAX_INTERRUPTS],
            enabled: [false; MAX_INTERRUPTS],
            threshold: 0,
        }
    }

    pub fn read(&self, addr: u32) -> Result<u32, String> {
        let offset = addr - 0x0C00_0000;
        match offset {
            PLIC_PRIORITY_BASE..=0xFFF => {
                let idx = (offset / 4) as usize;
                if idx < MAX_INTERRUPTS {
                    Ok(self.priorities[idx])
                } else {
                    Ok(0)
                }
            }
            PLIC_PENDING_BASE..=0x1FFF => {
                let word = ((offset - PLIC_PENDING_BASE) / 4) as usize;
                Ok(self.read_pending_word(word))
            }
            PLIC_ENABLE_BASE..=0x1F_FFFF => {
                let word = ((offset - PLIC_ENABLE_BASE) / 4) as usize;
                Ok(self.read_enable_word(word))
            }
            PLIC_THRESHOLD => Ok(self.threshold),
            PLIC_CLAIM => Ok(self.claim_internal()),
            _ => Ok(0),
        }
    }

    pub fn write(&mut self, addr: u32, value: u32) {
        let offset = addr - 0x0C00_0000;
        match offset {
            PLIC_PRIORITY_BASE..=0xFFF => {
                let idx = (offset / 4) as usize;
                if idx < MAX_INTERRUPTS {
                    self.priorities[idx] = value;
                }
            }
            PLIC_ENABLE_BASE..=0x1F_FFFF => {
                let word = ((offset - PLIC_ENABLE_BASE) / 4) as usize;
                self.write_enable_word(word, value);
            }
            PLIC_THRESHOLD => {
                self.threshold = value;
            }
            PLIC_CLAIM => {
                self.complete(value);
            }
            _ => {}
        }
    }

    pub fn set_interrupt(&mut self, irq: usize, active: bool) {
        if irq < MAX_INTERRUPTS {
            self.pending[irq] = active;
        }
    }

    pub fn claim(&mut self) -> u32 {
        self.claim_internal()
    }

    fn claim_internal(&self) -> u32 {
        let mut best_irq = 0u32;
        let mut best_priority = 0u32;

        for i in 1..MAX_INTERRUPTS {
            if self.pending[i] && self.enabled[i] {
                let prio = self.priorities[i];
                if prio > self.threshold && prio > best_priority {
                    best_priority = prio;
                    best_irq = i as u32;
                }
            }
        }

        best_irq
    }

    fn complete(&mut self, irq: u32) {
        let idx = irq as usize;
        if idx < MAX_INTERRUPTS {
            self.pending[idx] = false;
        }
    }

    fn read_pending_word(&self, word: usize) -> u32 {
        let start = word * 32;
        let mut val = 0u32;
        for i in 0..32 {
            let idx = start + i;
            if idx < MAX_INTERRUPTS && self.pending[idx] {
                val |= 1 << i;
            }
        }
        val
    }

    fn read_enable_word(&self, word: usize) -> u32 {
        let start = word * 32;
        let mut val = 0u32;
        for i in 0..32 {
            let idx = start + i;
            if idx < MAX_INTERRUPTS && self.enabled[idx] {
                val |= 1 << i;
            }
        }
        val
    }

    fn write_enable_word(&mut self, word: usize, value: u32) {
        let start = word * 32;
        for i in 0..32 {
            let idx = start + i;
            if idx < MAX_INTERRUPTS {
                self.enabled[idx] = (value & (1 << i)) != 0;
            }
        }
    }

    pub fn reset(&mut self) {
        self.priorities = [0u32; MAX_INTERRUPTS];
        self.pending = [false; MAX_INTERRUPTS];
        self.enabled = [false; MAX_INTERRUPTS];
        self.threshold = 0;
    }

    pub fn is_enabled(&self, irq: usize) -> bool {
        if irq < MAX_INTERRUPTS {
            self.enabled[irq]
        } else {
            false
        }
    }

    pub fn is_pending(&self, irq: usize) -> bool {
        if irq < MAX_INTERRUPTS {
            self.pending[irq]
        } else {
            false
        }
    }

    pub fn get_priority(&self, irq: usize) -> u32 {
        if irq < MAX_INTERRUPTS {
            self.priorities[irq]
        } else {
            0
        }
    }

    pub fn complete_and_check(&mut self, irq: u32) -> bool {
        let idx = irq as usize;
        if idx < MAX_INTERRUPTS {
            self.pending[idx] = false;
            self.pending[idx] && self.enabled[idx] && self.priorities[idx] > self.threshold
        } else {
            false
        }
    }
}