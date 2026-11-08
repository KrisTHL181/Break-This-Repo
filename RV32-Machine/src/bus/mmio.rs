use std::sync::Arc;
use parking_lot::Mutex;

use crate::devices::plic::Plic;
use crate::devices::uart16550::Uart16550;
use crate::devices::virtio_gpu::VirtioGpu;
use crate::devices::es1371::Es1371;
use crate::devices::ohci::OhciController;
use crate::devices::scsi53c895a::Scsi53c895a;
use crate::devices::sata::SataController;
use crate::{
    RAM_BASE, RAM_SIZE, BOOT_ROM_BASE, BOOT_ROM_SIZE,
    PLIC_BASE, PERIPHERAL_SIZE,
    UART_BASE, VGA_BASE, AUDIO_BASE, USB_BASE,
    SCSI_BASE, SATA_BASE,
};

pub struct Bus {
    ram: Arc<Mutex<Vec<u8>>>,
    boot_rom: Vec<u8>,
    plic: Arc<Mutex<Plic>>,
    pub uart: Uart16550,
    pub vga: VirtioGpu,
    pub audio: Es1371,
    pub usb: OhciController,
    pub scsi: Scsi53c895a,
    pub sata: SataController,
    pub boot_status: u32,

    reservation_addr: Option<u32>,
    reservation_valid: bool,
}

impl Bus {
    pub fn new(ram: Arc<Mutex<Vec<u8>>>, plic: Arc<Mutex<Plic>>) -> Self {
        let mut boot_rom = vec![0u8; BOOT_ROM_SIZE as usize];
        Self::init_boot_rom(&mut boot_rom);

        Self {
            ram,
            boot_rom,
            plic,
            uart: Uart16550::new(),
            vga: VirtioGpu::new(),
            audio: Es1371::new(),
            usb: OhciController::new(),
            scsi: Scsi53c895a::new(),
            sata: SataController::new(),
            boot_status: 0,
            reservation_addr: None,
            reservation_valid: false,
        }
    }

    fn init_boot_rom(rom: &mut [u8]) {
        let (fw, _, _) = crate::firmware::build_firmware();
        let len = fw.len().min(rom.len());
        rom[..len].copy_from_slice(&fw[..len]);
    }

    pub fn read8(&mut self, addr: u32) -> Result<u32, String> {
        const BOOT_ROM_LIMIT: u32 = BOOT_ROM_BASE + BOOT_ROM_SIZE;

        match addr {
            RAM_BASE..=0xFFFF_FFFF => {
                let offset = (addr - RAM_BASE) as usize;
                if offset < RAM_SIZE {
                    Ok(self.ram.lock()[offset] as u32)
                } else {
                    Err(format!("RAM read out of bounds: 0x{:08X}", addr))
                }
            }
            // Allow one-word past the nominal ROM size to read as zero to
            // avoid hard crashes when firmware/patched jumps land exactly at
            // the ROM size boundary (temporary mitigation while tracing).
            BOOT_ROM_BASE..=BOOT_ROM_LIMIT => {
                let offset = (addr - BOOT_ROM_BASE) as usize;
                if offset < self.boot_rom.len() {
                    Ok(self.boot_rom[offset] as u32)
                } else {
                    Ok(0)
                }
            }
            PLIC_BASE..=0x0FFF_FFFF => {
                self.plic.lock().read(addr)
            }
            UART_BASE..=0x1FFF_FFFF => {
                let offset = (addr - UART_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    Ok(self.uart.read(offset) as u32)
                } else {
                    Ok(0)
                }
            }
            VGA_BASE..=0x2FFF_FFFF => {
                let offset = (addr - VGA_BASE) as usize;
                Ok(self.vga.read(offset))
            }
            AUDIO_BASE..=0x3FFF_FFFF => {
                let offset = (addr - AUDIO_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    Ok(self.audio.read(offset) as u32)
                } else {
                    Ok(0)
                }
            }
            USB_BASE..=0x4FFF_FFFF => {
                let offset = (addr - USB_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    Ok(self.usb.read(offset) as u32)
                } else {
                    Ok(0)
                }
            }
            SCSI_BASE..=0x5FFF_FFFF => {
                let offset = (addr - SCSI_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    Ok(self.scsi.read(offset))
                } else {
                    Ok(0)
                }
            }
            SATA_BASE..=0x6FFF_FFFF => {
                let offset = (addr - SATA_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    Ok(self.sata.read(offset))
                } else {
                    Ok(0)
                }
            }
            _ => Err(format!("Unmapped read at 0x{:08X}", addr)),
        }
    }

    pub fn read16(&mut self, addr: u32) -> Result<u32, String> {
        let lo = self.read8(addr)?;
        let hi = self.read8(addr + 1)?;
        Ok(lo | (hi << 8))
    }

    pub fn read32(&mut self, addr: u32) -> Result<u32, String> {
        let lo = self.read16(addr)?;
        let hi = self.read16(addr + 2)?;
        Ok(lo | (hi << 16))
    }

    pub fn write8(&mut self, addr: u32, value: u8) -> Result<(), String> {
        match addr {
            RAM_BASE..=0xFFFF_FFFF => {
                let offset = (addr - RAM_BASE) as usize;
                if offset < RAM_SIZE {
                    self.ram.lock()[offset] = value;
                    Ok(())
                } else {
                    Err(format!("RAM write out of bounds: 0x{:08X}", addr))
                }
            }
            BOOT_ROM_BASE..=0x0000_FFFF => {
                Ok(())
            }
            PLIC_BASE..=0x0FFF_FFFF => {
                self.plic.lock().write(addr, value as u32);
                Ok(())
            }
            UART_BASE..=0x1FFF_FFFF => {
                let offset = (addr - UART_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    self.uart.write(offset, value);
                    Ok(())
                } else {
                    Ok(())
                }
            }
            VGA_BASE..=0x2FFF_FFFF => {
                let offset = (addr - VGA_BASE) as usize;
                self.vga.write(offset, value as u32);
                if self.vga.has_pending_notify() {
                    let mut ram = self.ram.lock();
                    let ram_slice = &mut ram[..];
                    self.vga.process_pending_notify(ram_slice);
                }
                Ok(())
            }
            AUDIO_BASE..=0x3FFF_FFFF => {
                let offset = (addr - AUDIO_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    self.audio.write(offset, value);
                    Ok(())
                } else {
                    Ok(())
                }
            }
            USB_BASE..=0x4FFF_FFFF => {
                let offset = (addr - USB_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    self.usb.write(offset, value as u32);
                    Ok(())
                } else {
                    Ok(())
                }
            }
            SCSI_BASE..=0x5FFF_FFFF => {
                let offset = (addr - SCSI_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    self.scsi.write(offset, value as u32);
                    Ok(())
                } else {
                    Ok(())
                }
            }
            SATA_BASE..=0x6FFF_FFFF => {
                let offset = (addr - SATA_BASE) as usize;
                if offset < PERIPHERAL_SIZE as usize {
                    self.sata.write(offset, value as u32);
                    Ok(())
                } else {
                    Ok(())
                }
            }
            _ => Err(format!("Unmapped write at 0x{:08X}", addr)),
        }
    }

    pub fn write16(&mut self, addr: u32, value: u16) -> Result<(), String> {
        self.write8(addr, value as u8)?;
        self.write8(addr + 1, (value >> 8) as u8)?;
        Ok(())
    }

    pub fn write32(&mut self, addr: u32, value: u32) -> Result<(), String> {
        const BOOT_CMD_OFFSET: u32 = 0x0000_0F00;
        const BOOT_STATUS_OFFSET: u32 = 0x0000_0F04;

        // Intercept simple boot commands: write port index to SATA/SCSI/USB BOOT_CMD
        if addr == SATA_BASE + BOOT_CMD_OFFSET {
            let port = value as usize;
            if port < self.sata.ports.len() && self.sata.ports[port].disk_inserted {
                let img = &self.sata.ports[port].disk_image;
                let copy_len = img.len().min(512);
                if copy_len > 0 {
                    let mut ram = self.ram.lock();
                    ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                }
                self.sata.ports[port].boot_status = 1;
            } else {
                if port < self.sata.ports.len() {
                    self.sata.ports[port].boot_status = 0;
                }
            }
            return Ok(());
        }

        if addr == SCSI_BASE + BOOT_CMD_OFFSET {
            let _port = value as usize; // scsi likely single controller
            if self.scsi.disk_inserted {
                let img = &self.scsi.disk_image;
                let copy_len = img.len().min(512);
                if copy_len > 0 {
                    let mut ram = self.ram.lock();
                    ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                }
                self.scsi.irq_pending = false;
                // indicate success via bus boot_status
                self.boot_status = 1;
            } else {
                self.boot_status = 0;
            }
            return Ok(());
        }

        if addr == USB_BASE + BOOT_CMD_OFFSET {
            let _port = value as usize;
            if let Some(img) = self.usb.get_disk_image() {
                let copy_len = img.len().min(512);
                if copy_len > 0 {
                    let mut ram = self.ram.lock();
                    ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                }
                self.boot_status = 1;
            } else {
                self.boot_status = 0;
            }
            return Ok(());
        }

        self.write16(addr, value as u16)?;
        self.write16(addr + 2, (value >> 16) as u16)?;
        Ok(())
    }

    pub fn set_reservation(&mut self, addr: u32) {
        self.reservation_addr = Some(addr);
        self.reservation_valid = true;
    }

    pub fn check_reservation(&self, addr: u32) -> bool {
        self.reservation_valid && self.reservation_addr == Some(addr)
    }

    pub fn clear_reservation(&mut self) {
        self.reservation_valid = false;
    }

    pub fn uart_mut(&mut self) -> &mut Uart16550 {
        &mut self.uart
    }

    pub fn vga_mut(&mut self) -> &mut VirtioGpu {
        &mut self.vga
    }

    pub fn audio_mut(&mut self) -> &mut Es1371 {
        &mut self.audio
    }

    pub fn usb_mut(&mut self) -> &mut OhciController {
        &mut self.usb
    }

    pub fn update_plic_interrupts(&mut self) {
        let mut plic = self.plic.lock();
        plic.set_interrupt(1, self.uart.irq_pending());
        plic.set_interrupt(2, self.audio.irq_pending());
        plic.set_interrupt(3, self.vga.irq_pending());
        plic.set_interrupt(4, self.usb.irq_pending());
        plic.set_interrupt(5, self.scsi.irq_pending());
        plic.set_interrupt(6, self.sata.irq_pending());
    }

    pub fn scsi_mut(&mut self) -> &mut Scsi53c895a {
        &mut self.scsi
    }

    pub fn sata_mut(&mut self) -> &mut SataController {
        &mut self.sata
    }

    pub fn reset(&mut self) {
        self.uart.reset();
        self.vga.dirty = false;
        self.audio.reset();
        self.boot_status = 0;
        self.reservation_addr = None;
        self.reservation_valid = false;
    }

    pub fn load_binary(&mut self, data: &[u8], addr: u32) -> Result<(), String> {
        let ram_base = RAM_BASE;
        if addr < ram_base {
            return Err(format!("Address 0x{:08X} is below RAM base", addr));
        }
        let offset = (addr - ram_base) as usize;
        let mut ram = self.ram.lock();
        if offset + data.len() > RAM_SIZE {
            return Err("Binary exceeds RAM size".to_string());
        }
        ram[offset..offset + data.len()].copy_from_slice(data);
        Ok(())
    }

    pub fn read_block(&self, addr: u32, buf: &mut [u8]) -> Result<(), String> {
        let ram_base = RAM_BASE;
        if addr < ram_base {
            return Err(format!("Address 0x{:08X} is below RAM base", addr));
        }
        let offset = (addr - ram_base) as usize;
        let ram = self.ram.lock();
        if offset + buf.len() > RAM_SIZE {
            return Err("Read exceeds RAM size".to_string());
        }
        buf.copy_from_slice(&ram[offset..offset + buf.len()]);
        Ok(())
    }

    pub fn write_block(&mut self, addr: u32, data: &[u8]) -> Result<(), String> {
        let ram_base = RAM_BASE;
        if addr < ram_base {
            return Err(format!("Address 0x{:08X} is below RAM base", addr));
        }
        let offset = (addr - ram_base) as usize;
        let mut ram = self.ram.lock();
        if offset + data.len() > RAM_SIZE {
            return Err("Write exceeds RAM size".to_string());
        }
        ram[offset..offset + data.len()].copy_from_slice(data);
        Ok(())
    }
}