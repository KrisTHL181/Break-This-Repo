use std::sync::Arc;
use std::fs;
use parking_lot::Mutex;
use egui::{Color32, ColorImage, TextureOptions, TextureId};
use egui::epaint::ImageDelta;

use crate::bus::Bus;
use crate::cpu::Cpu;
use crate::devices::plic::Plic;
use crate::devices::virtio_gpu::{VIRTIO_GPU_WIDTH, VIRTIO_GPU_HEIGHT};
use crate::gui::uart_console::UartConsole;

const CYCLES_PER_FRAME: usize = 100_000;

pub struct SimulatorApp {
    cpu: Cpu,
    bus: Arc<Mutex<Bus>>,
    ram: Arc<Mutex<Vec<u8>>>,
    plic: Arc<Mutex<Plic>>,

    vga_texture: Option<TextureId>,
    vga_image: ColorImage,

    // UI state for inserting disk images
    sata_insert_paths: [String; 4],
    scsi_insert_path: String,
    usb_insert_path: String,
    // Boot order: indices into device list
    boot_order: [usize; 3],

    running: bool,
    breakpoints: Vec<u32>,
    show_registers: bool,
    show_memory: bool,
    show_disassembly: bool,
    show_devices: bool,

    memory_view_addr: u32,
    disasm_addr: u32,

    status_message: String,
    frame_count: u64,
    ips: f64,
    uart_console: UartConsole,
    kernel_loaded: bool,
}

impl SimulatorApp {
    pub fn new(
        cpu: Cpu,
        bus: Arc<Mutex<Bus>>,
        ram: Arc<Mutex<Vec<u8>>>,
        plic: Arc<Mutex<Plic>>,
        kernel_loaded: bool,
    ) -> Self {
        Self {
            cpu,
            bus,
            ram,
            plic,
            vga_texture: None,
            vga_image: ColorImage::new(
                [VIRTIO_GPU_WIDTH, VIRTIO_GPU_HEIGHT],
                Color32::BLACK,
            ),
            running: false,
            breakpoints: Vec::new(),
            show_registers: true,
            show_memory: true,
            show_disassembly: false,
            show_devices: true,
            memory_view_addr: 0x8000_0000,
            disasm_addr: 0x8000_0000,
            status_message: "Ready".to_string(),
            frame_count: 0,
            ips: 0.0,
            sata_insert_paths: [String::new(), String::new(), String::new(), String::new()],
            scsi_insert_path: String::new(),
            usb_insert_path: String::new(),
            boot_order: [0, 1, 2],
            uart_console: UartConsole::new(),
            kernel_loaded,
        }
    }
}

impl eframe::App for SimulatorApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        self.update_vga_texture(ctx);

        self.handle_keyboard_input(ctx);

        if self.running {
            self.execute_cycles();
        }

        self.render_menu_bar(ctx);
        self.render_status_bar(ctx);

        egui::CentralPanel::default().show(ctx, |ui| {
            self.render_main_area(ui);
        });

        self.render_windows(ctx);

        if self.running {
            ctx.request_repaint();
        }
    }
}

impl SimulatorApp {
    fn handle_keyboard_input(&mut self, ctx: &egui::Context) {
        let input = ctx.input(|i| {
            let mut keys = Vec::new();
            for event in &i.events {
                if let egui::Event::Key {
                    key,
                    pressed: true,
                    ..
                } = event
                {
                    keys.push(*key);
                }
            }
            keys
        });

        if !input.is_empty() {
            let mut bus = self.bus.lock();
            let uart = bus.uart_mut();
            for key in &input {
                if let Some(byte) = key_to_byte(*key) {
                    uart.receive_byte(byte);
                }
            }
        }
    }

    fn execute_cycles(&mut self) {
        let cycles = CYCLES_PER_FRAME;
        match self.cpu.run_cycles(cycles) {
            Ok(executed) => {
                self.ips = executed as f64;
                if self.cpu.halted {
                    self.running = false;
                    self.status_message = "Halted (EBREAK)".to_string();
                } else {
                    self.status_message = format!("Running... {} insns/frame", executed);
                }
            }
            Err(e) => {
                self.running = false;
                self.status_message = format!("Error: {}", e);
            }
        }

        {
            let mut bus = self.bus.lock();
            bus.update_plic_interrupts();

            let uart = bus.uart_mut();
            let tx_data = uart.tx_drain();
            if !tx_data.is_empty() {
                self.uart_console.write_bytes(&tx_data);
            }

            while let Some(byte) = self.uart_console.read_byte() {
                uart.receive_byte(byte);
            }

            let vga_dirty = bus.vga.dirty;
            let fb_snapshot = if vga_dirty {
                bus.vga.framebuffer.clone()
            } else {
                Vec::new()
            };
            bus.vga.dirty = false;

            let audio = bus.audio_mut();
            audio.tick();
            let _samples = audio.output_samples();

            let usb = bus.usb_mut();
            usb.tick();

            drop(bus);

            if vga_dirty {
                self.update_framebuffer(&fb_snapshot);
            }
        }

        self.frame_count += 1;
    }

    fn update_framebuffer(&mut self, fb: &[u32]) {
        for (i, pixel) in fb.iter().enumerate() {
            if i < self.vga_image.pixels.len() {
                let r = ((pixel >> 16) & 0xFF) as u8;
                let g = ((pixel >> 8) & 0xFF) as u8;
                let b = (pixel & 0xFF) as u8;
                let a = 255u8;
                self.vga_image.pixels[i] = Color32::from_rgba_unmultiplied(r, g, b, a);
            }
        }
    }

    fn update_vga_texture(&mut self, ctx: &egui::Context) {
        let tex_id = self.vga_texture.get_or_insert_with(|| {
            ctx.tex_manager().write().alloc(
                "vga-framebuffer".into(),
                egui::ImageData::Color(Arc::new(self.vga_image.clone())),
                TextureOptions::NEAREST,
            )
        });

        ctx.tex_manager().write().set(
            *tex_id,
            ImageDelta::full(
                egui::ImageData::Color(Arc::new(self.vga_image.clone())),
                TextureOptions::NEAREST,
            ),
        );
    }

    fn render_menu_bar(&mut self, ctx: &egui::Context) {
        egui::TopBottomPanel::top("menu_bar").show(ctx, |ui| {
            egui::menu::bar(ui, |ui| {
                ui.menu_button("Machine", |ui| {
                    if ui.button("Reset").clicked() {
                        self.cpu.reset();
                        self.status_message = "CPU Reset".to_string();
                        self.running = false;
                    }
                    if ui.button("Load Binary...").clicked() {
                        self.status_message = "Load binary not yet implemented".to_string();
                        ui.close_menu();
                    }
                    ui.separator();
                    ui.menu_button("Boot From", |ui| {
                        if ui.button("RAM (no change)").clicked() {
                            let ram_base = 0x8000_0000u32;
                            self.cpu.pc = ram_base;
                            self.running = true;
                            self.status_message = "Booting from RAM".to_string();
                            ui.close_menu();
                        }

                        // SATA ports
                        use crate::SATA_BASE;
                        for port in 0..4usize {
                            let label = format!("SATA port {}", port);
                            if ui.button(&label).clicked() {
                                let mut bus = self.bus.lock();
                                // Trigger bus-level boot command
                                let _ = bus.write32(SATA_BASE + 0x0000_0F00, port as u32);
                                if bus.sata.ports[port].boot_status != 0 {
                                    let ram_base = 0x8000_0000u32;
                                    self.cpu.pc = ram_base;
                                    self.running = true;
                                    self.status_message = format!("Booting from SATA port {}", port);
                                } else {
                                    self.status_message = format!("No disk in SATA port {}", port);
                                }
                                ui.close_menu();
                            }
                        }

                        if ui.button("SCSI").clicked() {
                            let bus = self.bus.lock();
                            let mut ram = self.ram.lock();
                            let ram_base = 0x8000_0000u32;
                            if bus.scsi.disk_inserted {
                                let img = &bus.scsi.disk_image;
                                let copy_len = img.len().min(512);
                                if copy_len > 0 {
                                    ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                                }
                                self.cpu.pc = ram_base;
                                self.running = true;
                                self.status_message = "Booting from SCSI".to_string();
                            } else {
                                self.status_message = "No SCSI disk".to_string();
                            }
                            ui.close_menu();
                        }

                        if ui.button("USB").clicked() {
                            let mut bus = self.bus.lock();
                            let mut ram = self.ram.lock();
                            let ram_base = 0x8000_0000u32;
                            // OHCI/USB device implementation may expose disk through a device-specific API;
                            // here we attempt to use a simple usb.disk_image if present (some emulated setups may provide it)
                            let usb = &mut bus.usb;
                            if let Some(img) = usb.get_disk_image() {
                                let copy_len = img.len().min(512);
                                if copy_len > 0 {
                                    ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                                }
                                self.cpu.pc = ram_base;
                                self.running = true;
                                self.status_message = "Booting from USB".to_string();
                            } else {
                                self.status_message = "No USB disk".to_string();
                            }
                            ui.close_menu();
                        }
                    });
                    ui.separator();
                    if ui.button("Quit").clicked() {
                        std::process::exit(0);
                    }
                });

                ui.menu_button("View", |ui| {
                    ui.checkbox(&mut self.show_registers, "Registers");
                    ui.checkbox(&mut self.show_memory, "Memory View");
                    ui.checkbox(&mut self.show_disassembly, "Disassembly");
                    ui.checkbox(&mut self.show_devices, "Devices");
                });

                ui.separator();

                if self.running {
                    if ui.button("⏸ Pause").clicked() {
                        self.running = false;
                        self.status_message = "Paused".to_string();
                    }
                } else {
                    if ui.button("▶ Run").clicked() {
                        self.running = true;
                        self.status_message = "Running...".to_string();
                    }

                    if ui.button("⏭ Step").clicked() {
                        match self.cpu.step() {
                            Ok(()) => {
                                self.status_message = format!("Stepped to 0x{:08X}", self.cpu.pc);
                            }
                            Err(e) => {
                                self.status_message = format!("Step error: {}", e);
                            }
                        }
                        {
                            let mut bus = self.bus.lock();
                            bus.update_plic_interrupts();
                        }
                    }
                }
            });
        });
    }

    fn render_status_bar(&self, ctx: &egui::Context) {
        egui::TopBottomPanel::bottom("status_bar").show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.label(format!("PC: 0x{:08X}", self.cpu.pc));
                ui.separator();
                ui.label(format!("Cycles: {}", self.cpu.cycle_count));
                ui.separator();
                ui.label(format!("Insns: {}", self.cpu.insn_count));
                ui.separator();
                ui.label(format!("Frame: {}", self.frame_count));
                ui.separator();
                ui.label(&self.status_message);
            });
        });
    }

    fn render_main_area(&mut self, ui: &mut egui::Ui) {
        if let Some(texture_id) = &self.vga_texture {
            let available = ui.available_size();
            let aspect = VIRTIO_GPU_WIDTH as f32 / VIRTIO_GPU_HEIGHT as f32;
            let (w, h) = if available.x / available.y > aspect {
                (available.y * aspect, available.y)
            } else {
                (available.x, available.x / aspect)
            };

            ui.centered_and_justified(|ui| {
                ui.image(egui::ImageSource::Texture(
                    egui::load::SizedTexture::new(*texture_id, [w, h]),
                ));
            });

            // Overlay: show current CPU PC on top-left of the window so it's
            // always visible while the framebuffer is shown.
            egui::Area::new("pc_overlay".into())
                .fixed_pos(egui::pos2(12.0, 52.0))
                .show(ui.ctx(), |ui| {
                    ui.add_space(2.0);
                    ui.colored_label(egui::Color32::YELLOW, format!("PC: 0x{:08X}", self.cpu.pc));
                });
        } else {
            ui.centered_and_justified(|ui| {
                ui.heading("RV32-Machine Simulator");
            });
        }
    }

    fn render_windows(&mut self, ctx: &egui::Context) {
        if self.show_registers {
            egui::Window::new("Registers")
                .default_open(true)
                .resizable(true)
                .show(ctx, |ui| {
                    self.render_register_panel(ui);
                });
        }

        if self.show_memory {
            egui::Window::new("Memory View")
                .default_open(true)
                .resizable(true)
                .show(ctx, |ui| {
                    self.render_memory_view(ui);
                });
        }

        if self.show_disassembly {
            egui::Window::new("Disassembly")
                .default_open(true)
                .resizable(true)
                .show(ctx, |ui| {
                    self.render_disassembly(ui);
                });
        }

        if self.show_devices {
            egui::Window::new("Devices")
                .default_open(true)
                .resizable(true)
                .show(ctx, |ui| {
                    self.render_device_panel(ui);
                });
        }
    }

    fn render_register_panel(&mut self, ui: &mut egui::Ui) {
        egui::ScrollArea::vertical().show(ui, |ui| {
            egui::Grid::new("regs_grid").striped(true).show(ui, |ui| {
                let reg_names = [
                    "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
                    "s0/fp", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
                    "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
                    "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6",
                ];

                for (i, name) in reg_names.iter().enumerate() {
                    let val = self.cpu.regs[i];
                    let color = if val == 0 {
                        Color32::GRAY
                    } else {
                        Color32::WHITE
                    };
                    ui.colored_label(color, format!("x{:<2} {}", i, name));
                    ui.colored_label(color, format!("0x{:08X}", val));
                    if i % 2 == 1 {
                        ui.end_row();
                    }
                }
            });

            ui.separator();
            ui.label(format!("pc:     0x{:08X}", self.cpu.pc));
            ui.label(format!("mstatus: 0x{:08X}", self.cpu.csr.mstatus));
            ui.label(format!("mtvec:   0x{:08X}", self.cpu.csr.mtvec));
            ui.label(format!("mepc:    0x{:08X}", self.cpu.csr.mepc));
            ui.label(format!("mcause:  0x{:08X}", self.cpu.csr.mcause));
            ui.label(format!("mtval:   0x{:08X}", self.cpu.csr.mtval));
            ui.label(format!("mie:     0x{:08X}", self.cpu.csr.mie));
            ui.label(format!("mip:     0x{:08X}", self.cpu.csr.mip));
        });
    }

    fn render_memory_view(&mut self, ui: &mut egui::Ui) {
        ui.horizontal(|ui| {
            ui.label("Address:");
            let mut addr_str = format!("0x{:08X}", self.memory_view_addr);
            if ui.text_edit_singleline(&mut addr_str).changed() {
                if let Some(addr) = parse_hex(&addr_str) {
                    self.memory_view_addr = addr & !0xF;
                }
            }
            if ui.button("▲").clicked() {
                self.memory_view_addr = self.memory_view_addr.wrapping_sub(0x10);
            }
            if ui.button("▼").clicked() {
                self.memory_view_addr = self.memory_view_addr.wrapping_add(0x10);
            }
        });

        egui::ScrollArea::vertical().show(ui, |ui| {
            let ram = self.ram.lock();
            let base = self.memory_view_addr;
            let ram_base = 0x8000_0000u32;

            for row in 0..16 {
                let addr = base + row * 16;
                let mut line = format!("0x{:08X}: ", addr);

                for col in 0..16 {
                    let byte_addr = addr + col;
                    let byte = if byte_addr >= ram_base {
                        let offset = (byte_addr - ram_base) as usize;
                        if offset < ram.len() {
                            ram[offset]
                        } else {
                            0xFF
                        }
                    } else {
                        0
                    };
                    line.push_str(&format!("{:02X} ", byte));
                }

                line.push_str(" |");
                for col in 0..16 {
                    let byte_addr = addr + col;
                    let byte = if byte_addr >= ram_base {
                        let offset = (byte_addr - ram_base) as usize;
                        if offset < ram.len() {
                            ram[offset]
                        } else {
                            0xFF
                        }
                    } else {
                        0
                    };
                    let ch = if byte.is_ascii_graphic() || byte == b' ' {
                        byte as char
                    } else {
                        '.'
                    };
                    line.push(ch);
                }
                line.push('|');

                ui.monospace(&line);
            }
        });
    }

    fn render_disassembly(&mut self, ui: &mut egui::Ui) {
        ui.horizontal(|ui| {
            ui.label("Address:");
            let mut addr_str = format!("0x{:08X}", self.disasm_addr);
            if ui.text_edit_singleline(&mut addr_str).changed() {
                if let Some(addr) = parse_hex(&addr_str) {
                    self.disasm_addr = addr & !0x3;
                }
            }
        });

        egui::ScrollArea::vertical().show(ui, |ui| {
            let ram = self.ram.lock();
            let base = self.disasm_addr;
            let ram_base = 0x8000_0000u32;

            for i in 0..32 {
                let addr = base + i * 4;
                let is_current = addr == self.cpu.pc;
                let is_breakpoint = self.breakpoints.contains(&addr);

                let instr_bytes = if addr >= ram_base {
                    let offset = (addr - ram_base) as usize;
                    if offset + 3 < ram.len() {
                        u32::from_le_bytes([ram[offset], ram[offset + 1], ram[offset + 2], ram[offset + 3]])
                    } else {
                        0
                    }
                } else {
                    0
                };

                let prefix = if is_current { "→ " } else { "  " };
                let bp = if is_breakpoint { "●" } else { " " };

                let text_color = if is_current {
                    Color32::YELLOW
                } else if is_breakpoint {
                    Color32::RED
                } else {
                    Color32::LIGHT_GRAY
                };

                ui.colored_label(
                    text_color,
                    format!(
                        "{}{}0x{:08X}: {:08X}  {}",
                        prefix,
                        bp,
                        addr,
                        instr_bytes,
                        disasm_simple(instr_bytes),
                    ),
                );
            }
        });
    }

    fn render_device_panel(&mut self, ui: &mut egui::Ui) {
        egui::ScrollArea::vertical().show(ui, |ui| {
            ui.collapsing("NS16550A UART", |ui| {
                let bus = self.bus.lock();
                let uart = &bus.uart;
                ui.label(format!("IER: 0x{:02X}  IIR: 0x{:02X}", uart.ier, uart.iir));
                ui.label(format!("LCR: 0x{:02X}  LSR: 0x{:02X}", uart.lcr, uart.lsr));
                ui.label(format!("MCR: 0x{:02X}  MSR: 0x{:02X}", uart.mcr, uart.msr));
                ui.label(format!("IRQ Pending: {}", uart.irq_pending()));
                ui.label(format!("RX Buffer: {} bytes", uart.rx_buffer.len()));
                ui.label(format!("TX Buffer: {} bytes", uart.tx_buffer.len()));
            });

            ui.collapsing("Boot Order", |ui| {
                ui.label("Select boot device order:");
                let options = ["SATA0", "SCSI", "USB", "RAM"];
                for i in 0..3 {
                    egui::ComboBox::from_label(format!("Slot {}", i + 1))
                        .selected_text(options[self.boot_order[i]])
                        .show_ui(ui, |ui| {
                            for (idx, opt) in options.iter().enumerate() {
                                ui.selectable_value(&mut self.boot_order[i], idx, *opt);
                            }
                        });
                }

                ui.horizontal(|ui| {
                    if ui.button("Apply").clicked() {
                        // write boot order into RAM at offset 0x100
                        let mut ram = self.ram.lock();
                        let base = 0x8000_0000u32;
                        let offset = 0x100usize;
                        for (i, v) in self.boot_order.iter().enumerate() {
                            if offset + i < ram.len() {
                                ram[offset + i] = *v as u8;
                            }
                        }
                        self.status_message = "Boot order applied to RAM".to_string();
                    }

                    if ui.button("Boot Now").clicked() {
                        let mut booted = false;
                        for &dev in &self.boot_order {
                            let mut bus = self.bus.lock();
                            match dev {
                                0 => { // SATA0
                                    use crate::SATA_BASE;
                                    let _ = bus.write32(SATA_BASE + 0x0000_0F00, 0);
                                    if bus.sata.ports[0].boot_status != 0 {
                                        let ram_base = 0x8000_0000u32;
                                        self.cpu.pc = ram_base;
                                        self.running = true;
                                        self.status_message = "Booted from SATA0".to_string();
                                        booted = true;
                                        break;
                                    }
                                }
                                1 => { // SCSI
                                    let mut ram = self.ram.lock();
                                    let ram_base = 0x8000_0000u32;
                                    if bus.scsi.disk_inserted {
                                        let img = &bus.scsi.disk_image;
                                        let copy_len = img.len().min(512);
                                        if copy_len > 0 {
                                            ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                                        }
                                        self.cpu.pc = ram_base;
                                        self.running = true;
                                        self.status_message = "Booted from SCSI".to_string();
                                        booted = true;
                                        break;
                                    }
                                }
                                2 => { // USB
                                    let mut ram = self.ram.lock();
                                    let ram_base = 0x8000_0000u32;
                                    if let Some(img) = bus.usb.get_disk_image() {
                                        let copy_len = img.len().min(512);
                                        if copy_len > 0 {
                                            ram[0..copy_len].copy_from_slice(&img[0..copy_len]);
                                        }
                                        self.cpu.pc = ram_base;
                                        self.running = true;
                                        self.status_message = "Booted from USB".to_string();
                                        booted = true;
                                        break;
                                    }
                                }
                                3 => { // RAM
                                    let ram_base = 0x8000_0000u32;
                                    self.cpu.pc = ram_base;
                                    self.running = true;
                                    self.status_message = "Booted from RAM".to_string();
                                    booted = true;
                                    break;
                                }
                                _ => {}
                            }
                        }
                        if !booted {
                            self.status_message = "Boot failed: no device".to_string();
                        }
                    }
                });
            });

            ui.collapsing("ES1371 Audio", |ui| {
                let bus = self.bus.lock();
                let audio = &bus.audio;
                ui.label(format!("Control: 0x{:08X}", audio.control));
                ui.label(format!("Status:  0x{:08X}", audio.status));
                ui.label(format!("DAC1 Count: {}  Frame: 0x{:08X}", audio.dac1_count, audio.dac1_frame));
                ui.label(format!("DAC2 Count: {}  Frame: 0x{:08X}", audio.dac2_count, audio.dac2_frame));
                ui.label(format!("IRQ Pending: {}", audio.irq_pending()));
            });

            ui.collapsing("Virtio GPU", |ui| {
                let bus = self.bus.lock();
                let vga = &bus.vga;
                ui.label(format!("Resolution: {}x{}", VIRTIO_GPU_WIDTH, VIRTIO_GPU_HEIGHT));
                ui.label(format!("Device ID: 0x{:04X}", 0x10));
                ui.label(format!("Vendor ID: 0x{:04X}", 0x1AF4));
                ui.label(format!("Status: 0x{:08X}", vga.status));
                ui.label(format!("Scanouts: {}", vga.num_scanouts));
            });

            ui.collapsing("USB OHCI Controller", |ui| {
                let bus = self.bus.lock();
                let usb = &bus.usb;
                ui.label(format!("Revision: 0x{:08X}", usb.revision));
                ui.label(format!("Control:  0x{:08X}", usb.control));
                ui.label(format!("Cmd/Status: 0x{:08X}", usb.cmd_status));
                ui.label(format!("Int Status: 0x{:08X}", usb.int_status));
                ui.label(format!("Int Enable: 0x{:08X}", usb.int_enable));
                ui.label(format!("Frame Number: {}", usb.fm_number));
                ui.label(format!("RH Ports: {}", (usb.rh_descriptor_a & 0xFF)));
                for i in 0..4 {
                    ui.label(format!(
                        "  Port {}: 0x{:08X}",
                        i + 1,
                        usb.rh_port_status[i]
                    ));
                }
                ui.label(format!("IRQ Pending: {}", usb.irq_pending()));

                ui.horizontal(|ui| {
                    ui.label("Image:");
                    if ui.text_edit_singleline(&mut self.usb_insert_path).lost_focus() {}
                    if ui.button("Insert").clicked() {
                        let path = self.usb_insert_path.clone();
                        if !path.is_empty() {
                            match fs::read(&path) {
                                Ok(img) => {
                                    let mut bus = self.bus.lock();
                                    bus.usb.insert_disk_image(img);
                                    self.status_message = "Inserted USB image".to_string();
                                }
                                Err(e) => {
                                    self.status_message = format!("Failed to read {}: {}", path, e);
                                }
                            }
                        }
                    }
                    if ui.button("Eject").clicked() {
                        let mut bus = self.bus.lock();
                        bus.usb.eject_disk();
                        self.status_message = "Ejected USB image".to_string();
                    }
                });
            });

            ui.collapsing("SCSI 53c895A", |ui| {
                let bus = self.bus.lock();
                let scsi = &bus.scsi;
                ui.label(format!("SCSI ID: {}", scsi.id));
                ui.label(format!("IRQ Pending: {}", scsi.irq_pending()));
                ui.label(format!("Disk Inserted: {}", scsi.disk_inserted));
                if scsi.disk_inserted {
                    ui.label(format!("Disk Size: {} KB", scsi.disk_image.len() / 1024));
                }

                ui.horizontal(|ui| {
                    ui.label("Image:");
                    if ui.text_edit_singleline(&mut self.scsi_insert_path).lost_focus() {}
                    if ui.button("Insert").clicked() {
                        let path = self.scsi_insert_path.clone();
                        if !path.is_empty() {
                            match fs::read(&path) {
                                Ok(img) => {
                                    let mut bus = self.bus.lock();
                                    bus.scsi.insert_disk_image(img);
                                    self.status_message = format!("Inserted image into SCSI");
                                }
                                Err(e) => {
                                    self.status_message = format!("Failed to read {}: {}", path, e);
                                }
                            }
                        }
                    }
                    if ui.button("Eject").clicked() {
                        let mut bus = self.bus.lock();
                        bus.scsi.eject_disk();
                        self.status_message = "Ejected SCSI".to_string();
                    }
                });
            });

            ui.collapsing("SATA AHCI Controller", |ui| {
                let bus = self.bus.lock();
                let sata = &bus.sata;
                ui.label(format!("CAP: 0x{:08X}", sata.cap));
                ui.label(format!("GHC: 0x{:08X}", sata.ghc));
                ui.label(format!("PI:  0x{:08X}", sata.pi));
                ui.label(format!("IS:  0x{:08X}", sata.is));
                ui.label(format!("VS:  0x{:08X}", sata.vs));
                ui.label(format!("IRQ Pending: {}", sata.irq_pending()));
                ui.separator();
                for (i, port) in sata.ports.iter().enumerate() {
                    if (sata.pi & (1 << i)) != 0 {
                        ui.collapsing(format!("Port {}", i), |ui| {
                            ui.label(format!("CMD:  0x{:08X}", port.cmd));
                            ui.label(format!("SSTS: 0x{:08X}", port.ssts));
                            ui.label(format!("SIG:  0x{:08X}", port.sig));
                            ui.label(format!("TFD:  0x{:02X}", port.tfd));
                            ui.label(format!("IS:   0x{:08X}", port.is));
                            ui.label(format!("Disk Inserted: {}", port.disk_inserted));
                            if port.disk_inserted {
                                ui.label(format!("Sectors: {}", port.sector_count));
                            }

                            ui.horizontal(|ui| {
                                let path = &mut self.sata_insert_paths[i];
                                ui.label("Image:");
                                if ui.text_edit_singleline(path).lost_focus() {}
                                if ui.button("Insert").clicked() {
                                    if !path.is_empty() {
                                        let path_str = path.clone();
                                        match fs::read(&path_str) {
                                            Ok(img) => {
                                                let mut bus = self.bus.lock();
                                                bus.sata.insert_disk_image(i, img);
                                                self.status_message = format!("Inserted image into SATA {}", i);
                                            }
                                            Err(e) => {
                                                self.status_message = format!("Failed to read {}: {}", path_str, e);
                                            }
                                        }
                                    }
                                }
                                if ui.button("Eject").clicked() {
                                    let mut bus = self.bus.lock();
                                    bus.sata.eject_disk(i);
                                    self.status_message = format!("Ejected SATA {}", i);
                                }
                            });
                        });
                    }
                }
            });

            ui.collapsing("PLIC", |ui| {
                let plic = self.plic.lock();
                ui.label(format!("Threshold: {}", plic.threshold));
                ui.label("Interrupts:");
                for i in 1..=6 {
                    ui.label(format!("  IRQ {}: priority={}", i, plic.priorities[i]));
                }
            });
        });
    }
}

fn key_to_byte(key: egui::Key) -> Option<u8> {
    use egui::Key;
    match key {
        Key::Enter => Some(b'\r'),
        Key::Backspace => Some(0x7F),
        Key::Delete => Some(0x7F),
        Key::Escape => Some(0x1B),
        Key::Tab => Some(b'\t'),
        Key::Space => Some(b' '),
        Key::ArrowUp => Some(0x1B),
        Key::ArrowDown => Some(0x1B),
        Key::ArrowLeft => Some(0x1B),
        Key::ArrowRight => Some(0x1B),
        Key::A => Some(b'a'),
        Key::B => Some(b'b'),
        Key::C => Some(b'c'),
        Key::D => Some(b'd'),
        Key::E => Some(b'e'),
        Key::F => Some(b'f'),
        Key::G => Some(b'g'),
        Key::H => Some(b'h'),
        Key::I => Some(b'i'),
        Key::J => Some(b'j'),
        Key::K => Some(b'k'),
        Key::L => Some(b'l'),
        Key::M => Some(b'm'),
        Key::N => Some(b'n'),
        Key::O => Some(b'o'),
        Key::P => Some(b'p'),
        Key::Q => Some(b'q'),
        Key::R => Some(b'r'),
        Key::S => Some(b's'),
        Key::T => Some(b't'),
        Key::U => Some(b'u'),
        Key::V => Some(b'v'),
        Key::W => Some(b'w'),
        Key::X => Some(b'x'),
        Key::Y => Some(b'y'),
        Key::Z => Some(b'z'),
        Key::Num0 => Some(b'0'),
        Key::Num1 => Some(b'1'),
        Key::Num2 => Some(b'2'),
        Key::Num3 => Some(b'3'),
        Key::Num4 => Some(b'4'),
        Key::Num5 => Some(b'5'),
        Key::Num6 => Some(b'6'),
        Key::Num7 => Some(b'7'),
        Key::Num8 => Some(b'8'),
        Key::Num9 => Some(b'9'),
        Key::Minus => Some(b'-'),
        Key::Equals => Some(b'='),
        Key::Backslash => Some(b'\\'),
        Key::Colon => Some(b':'),
        Key::Comma => Some(b','),
        Key::Period => Some(b'.'),
        Key::Slash => Some(b'/'),
        _ => None,
    }
}

fn parse_hex(s: &str) -> Option<u32> {
    let s = s.trim().trim_start_matches("0x").trim_start_matches("0X");
    u32::from_str_radix(s, 16).ok()
}

fn disasm_simple(insn: u32) -> String {
    if insn == 0 {
        return "illegal".to_string();
    }

    let opcode = insn & 0x7F;
    let rd = ((insn >> 7) & 0x1F) as usize;
    let rs1 = ((insn >> 15) & 0x1F) as usize;
    let rs2 = ((insn >> 20) & 0x1F) as usize;
    let funct3 = ((insn >> 12) & 0x7) as u8;
    let funct7 = (insn >> 25) & 0x7F;

    let reg_names = [
        "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
        "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
        "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
        "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6",
    ];

    fn imm_i(insn: u32) -> i32 {
        (insn as i32) >> 20
    }

    fn imm_s(insn: u32) -> i32 {
        let imm = ((insn >> 7) & 0x1F) | ((insn >> 20) & 0xFE0);
        ((imm << 20) as i32) >> 20
    }

    fn imm_b(insn: u32) -> i32 {
        let imm = ((insn >> 7) & 0x1E)
            | ((insn >> 20) & 0x7E0)
            | ((insn << 4) & 0x800)
            | ((insn >> 19) & 0x1000);
        ((imm << 19) as i32) >> 19
    }

    fn imm_j(insn: u32) -> i32 {
        let imm = (insn >> 12) & 0xFF
            | (insn >> 20) & 0x7FE
            | (insn >> 9) & 0x800
            | (insn & 0x8000_0000);
        ((imm << 11) as i32) >> 11
    }

    fn imm_u(insn: u32) -> i32 {
        (insn & 0xFFFF_F000) as i32 
    }

    match opcode {
        0x37 => format!("lui     {}, 0x{:X}", reg_names[rd], imm_u(insn)),
        0x17 => format!("auipc   {}, 0x{:X}", reg_names[rd], imm_u(insn)),
        0x6F => format!("jal     {}, {}", reg_names[rd], imm_j(insn)),
        0x67 => format!("jalr    {}, {}({})", reg_names[rd], imm_i(insn), reg_names[rs1]),
        0x63 => {
            let name = match funct3 {
                0x0 => "beq",
                0x1 => "bne",
                0x4 => "blt",
                0x5 => "bge",
                0x6 => "bltu",
                0x7 => "bgeu",
                _ => "???",
            };
            format!("{}      {}, {}, {}", name, reg_names[rs1], reg_names[rs2], imm_b(insn))
        }
        0x03 => {
            let name = match funct3 {
                0x0 => "lb",
                0x1 => "lh",
                0x2 => "lw",
                0x4 => "lbu",
                0x5 => "lhu",
                _ => "???",
            };
            format!("{}      {}, {}({})", name, reg_names[rd], imm_i(insn), reg_names[rs1])
        }
        0x23 => {
            let name = match funct3 {
                0x0 => "sb",
                0x1 => "sh",
                0x2 => "sw",
                _ => "???",
            };
            format!("{}      {}, {}({})", name, reg_names[rs2], imm_s(insn), reg_names[rs1])
        }
        0x13 => {
            match funct3 {
                0x0 => format!("addi    {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                0x1 => format!("slli    {}, {}, {}", reg_names[rd], reg_names[rs1], rs2),
                0x2 => format!("slti    {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                0x3 => format!("sltiu   {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                0x4 => format!("xori    {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                0x5 => {
                    if funct7 == 0x20 {
                        format!("srai    {}, {}, {}", reg_names[rd], reg_names[rs1], rs2)
                    } else {
                        format!("srli    {}, {}, {}", reg_names[rd], reg_names[rs1], rs2)
                    }
                }
                0x6 => format!("ori     {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                0x7 => format!("andi    {}, {}, {}", reg_names[rd], reg_names[rs1], imm_i(insn)),
                _ => format!("???     op13.{}", funct3),
            }
        }
        0x33 => {
            match (funct3, funct7) {
                (0x0, 0x00) => format!("add     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x0, 0x01) => format!("mul     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x0, 0x20) => format!("sub     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x1, 0x00) => format!("sll     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x1, 0x01) => format!("mulh    {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x2, 0x00) => format!("slt     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x3, 0x00) => format!("sltu    {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x3, 0x01) => format!("mulhu   {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x4, 0x00) => format!("xor     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x4, 0x01) => format!("div     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x5, 0x00) => format!("srl     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x5, 0x20) => format!("sra     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x5, 0x01) => format!("divu    {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x6, 0x00) => format!("or      {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x6, 0x01) => format!("rem     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x7, 0x00) => format!("and     {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                (0x7, 0x01) => format!("remu    {}, {}, {}", reg_names[rd], reg_names[rs1], reg_names[rs2]),
                _ => format!("???     rv32.{}", funct3),
            }
        }
        0x73 => {
            let csr_addr = ((insn >> 20) & 0xFFF) as u16;
            match funct3 {
                0x0 => {
                    if rd == 0 && rs1 == 0 && csr_addr == 0x000 {
                        "ecall".to_string()
                    } else if rd == 0 && rs1 == 0 && csr_addr == 0x001 {
                        "ebreak".to_string()
                    } else if rd == 0 && rs1 == 0 && csr_addr == 0x302 {
                        "mret".to_string()
                    } else {
                        "???".to_string()
                    }
                }
                0x1 => format!("csrrw   {}, 0x{:03X}, {}", reg_names[rd], csr_addr, reg_names[rs1]),
                0x2 => format!("csrrs   {}, 0x{:03X}, {}", reg_names[rd], csr_addr, reg_names[rs1]),
                0x3 => format!("csrrc   {}, 0x{:03X}, {}", reg_names[rd], csr_addr, reg_names[rs1]),
                0x5 => format!("csrrwi  {}, 0x{:03X}, {}", reg_names[rd], csr_addr, rs1),
                0x6 => format!("csrrsi  {}, 0x{:03X}, {}", reg_names[rd], csr_addr, rs1),
                0x7 => format!("csrrci  {}, 0x{:03X}, {}", reg_names[rd], csr_addr, rs1),
                _ => "???".to_string(),
            }
        }
        0x2F => {
            let funct5 = (insn >> 27) & 0x1F;
            match funct5 {
                0x02 => format!("lr.w    {}, ({})", reg_names[rd], reg_names[rs1]),
                0x03 => format!("sc.w    {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x01 => format!("amoswap.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x00 => format!("amoadd.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x04 => format!("amoxor.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x0C => format!("amoand.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x08 => format!("amoor.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x10 => format!("amomin.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x14 => format!("amomax.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x18 => format!("amominu.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                0x1C => format!("amomaxu.w {}, {}, ({})", reg_names[rd], reg_names[rs2], reg_names[rs1]),
                _ => "??? amo".to_string(),
            }
        }
        0x0F => "fence".to_string(),
        _ => {
            if (insn & 0x3) != 0x3 {
                format!("c.???   {:04X}", insn & 0xFFFF)
            } else {
                format!("???     0x{:08X}", insn)
            }
        }
    }
}