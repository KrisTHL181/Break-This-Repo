const VGA_CRTC_INDEX: usize = 0x3D4;
const VGA_CRTC_DATA: usize = 0x3D5;
const VGA_IS1: usize = 0x3DA;
const VGA_MISC_OUTPUT: usize = 0x3C2;
const VGA_SEQ_INDEX: usize = 0x3C4;
const VGA_GDC_INDEX: usize = 0x3CE;
const VGA_ATC_INDEX: usize = 0x3C0;

const VGA_FB_START: usize = 0xA0000;

pub const VGA_WIDTH: usize = 640;
pub const VGA_HEIGHT: usize = 480;
const VGA_FRAMEBUFFER_SIZE: usize = VGA_WIDTH * VGA_HEIGHT * 4;
const VGA_FB_END: usize = VGA_FB_START + VGA_FRAMEBUFFER_SIZE - 1;
const VGA_ROM_START: usize = 0xC0000;
const VGA_ROM_SIZE: usize = 0x8000;
const VGA_ROM_END: usize = VGA_ROM_START + VGA_ROM_SIZE - 1;

pub struct VgaController {
    crtc_regs: [u8; 0x19],
    pub crtc_index: u8,
    seq_regs: [u8; 0x05],
    seq_index: u8,
    gdc_regs: [u8; 0x09],
    gdc_index: u8,
    atc_regs: [u8; 0x15],
    atc_index: u8,
    atc_flipflop: bool,

    pub misc_output: u8,
    is1: u8,

    pub framebuffer: Vec<u32>,
    pub dirty: bool,
    pub rom: Vec<u8>,

    irq_pending: bool,
}

impl VgaController {
    pub fn new() -> Self {
        let mut crtc = [0u8; 0x19];
        crtc[0x00] = 0x5F;
        crtc[0x01] = 0x4F;
        crtc[0x02] = 0x50;
        crtc[0x03] = 0x82;
        crtc[0x04] = 0x54;
        crtc[0x05] = 0x80;
        crtc[0x06] = 0x0B;
        crtc[0x07] = 0x3E;
        crtc[0x08] = 0x00;
        crtc[0x09] = 0x40;
        crtc[0x10] = 0xEA;
        crtc[0x11] = 0x8C;
        crtc[0x12] = 0xDF;
        crtc[0x13] = 0x28;
        crtc[0x14] = 0x00;
        crtc[0x15] = 0xE7;
        crtc[0x16] = 0x04;
        crtc[0x17] = 0xE3;
        crtc[0x18] = 0xFF;

        let mut seq = [0u8; 0x05];
        seq[0] = 0x03;
        seq[1] = 0x01;
        seq[2] = 0x0F;
        seq[3] = 0x00;
        seq[4] = 0x0E;

        let mut gdc = [0u8; 0x09];
        gdc[0] = 0x00;
        gdc[1] = 0x00;
        gdc[2] = 0x00;
        gdc[3] = 0x00;
        gdc[4] = 0x00;
        gdc[5] = 0x40;
        gdc[6] = 0x05;
        gdc[7] = 0x0F;
        gdc[8] = 0xFF;

        let mut atc = [0u8; 0x15];
        for i in 0..0x10 {
            atc[i] = i as u8;
        }
        atc[0x10] = 0x41;
        atc[0x11] = 0x00;
        atc[0x12] = 0x0F;
        atc[0x13] = 0x00;
        atc[0x14] = 0x00;

        let mut controller = Self {
            crtc_regs: crtc,
            crtc_index: 0,
            seq_regs: seq,
            seq_index: 0,
            gdc_regs: gdc,
            gdc_index: 0,
            atc_regs: atc,
            atc_index: 0,
            atc_flipflop: false,
            misc_output: 0xE3,
            is1: 0x00,
            framebuffer: vec![0xFF000000u32; VGA_FRAMEBUFFER_SIZE],
            dirty: false,
            rom: Vec::new(),
            irq_pending: false,
        };

        // Debug: print current working directory and whether candidate dirs exist
        match std::env::current_dir() {
            Ok(p) => eprintln!("VGA: CWD={:?}", p),
            Err(e) => eprintln!("VGA: CWD get error: {}", e),
        }
        eprintln!("VGA: roms exists: {}", std::path::Path::new("roms").exists());
        eprintln!("VGA: src/roms exists: {}", std::path::Path::new("src/roms").exists());

        // Try to load an S3 Trio64V+ ROM from a few likely places to improve
        // initial palette / font / pixels. Prefer an explicit file if present,
        // otherwise scan `roms/` and `src/roms/` for the first .rom/.bin file.
        let explicit_candidates = [
            "src/roms/S3VGA.bin",
            "roms/s3_trio64v_plus.rom",
            "roms/s3_trio64v_plus.bin",
            "roms/s3_trio64v.rom",
            "roms/s3trio64.rom",
        ];

        fn try_load(path: &str) -> Option<Vec<u8>> {
            match std::fs::read(path) {
                Ok(data) if !data.is_empty() => Some(data),
                Ok(_) => {
                    eprintln!("VGA: candidate ROM {} found but empty", path);
                    None
                }
                Err(e) => {
                    eprintln!("VGA: candidate ROM {} read error: {}", path, e);
                    None
                }
            }
        }

        // Try explicit candidates first
        let mut rom_data: Option<Vec<u8>> = None;
        for p in explicit_candidates.iter() {
            eprintln!("VGA: trying candidate {}", p);
            if let Some(d) = try_load(p) {
                eprintln!("VGA: loaded ROM {} ({} bytes)", p, d.len());
                rom_data = Some(d);
                break;
            }
        }

        // If none, scan directories roms/ and src/roms/ for any .rom/.bin
        if rom_data.is_none() {
            let scan_dirs = ["roms", "src/roms"];
            'outer: for d in scan_dirs.iter() {
                if let Ok(entries) = std::fs::read_dir(d) {
                    for e in entries.flatten() {
                        if let Ok(fname) = e.file_name().into_string() {
                            let fname_l = fname.to_lowercase();
                            if fname_l.ends_with(".rom") || fname_l.ends_with(".bin") {
                                let path = format!("{}/{}", d, fname);
                                if let Some(ddata) = try_load(&path) {
                                    eprintln!("VGA: loaded ROM {} ({} bytes)", path, ddata.len());
                                    rom_data = Some(ddata);
                                    break 'outer;
                                }
                            }
                        }
                    }
                }
            }
        }

        if let Some(data) = rom_data {
            eprintln!("VGA: storing ROM as BIOS extension ({} bytes at {:#x})", data.len(), VGA_ROM_START);
            controller.rom = data;
        } else {
            eprintln!("VGA: no S3 ROM found; continuing with default framebuffer");
        }

        controller
    }

    pub fn read(&mut self, offset: usize) -> u32 {
        match offset {
            VGA_CRTC_INDEX => self.crtc_index as u32,
            VGA_CRTC_DATA => {
                let idx = self.crtc_index as usize;
                if idx < self.crtc_regs.len() {
                    self.crtc_regs[idx] as u32
                } else {
                    0
                }
            }
            VGA_SEQ_INDEX => self.seq_index as u32,
            0x3C5 => {
                let idx = self.seq_index as usize;
                if idx < self.seq_regs.len() {
                    self.seq_regs[idx] as u32
                } else {
                    0
                }
            }
            VGA_GDC_INDEX => self.gdc_index as u32,
            0x3CF => {
                let idx = self.gdc_index as usize;
                if idx < self.gdc_regs.len() {
                    self.gdc_regs[idx] as u32
                } else {
                    0
                }
            }
            VGA_ATC_INDEX => {
                if self.atc_flipflop {
                    let idx = self.atc_index as usize;
                    if idx < self.atc_regs.len() {
                        self.atc_regs[idx] as u32
                    } else {
                        0
                    }
                } else {
                    self.atc_index as u32
                }
            }
            VGA_MISC_OUTPUT => self.misc_output as u32,
            VGA_IS1 => {
                self.atc_flipflop = false;
                self.is1 as u32
            }
            VGA_FB_START..=VGA_FB_END => {
                let fb_offset = offset - VGA_FB_START;
                if fb_offset < VGA_FRAMEBUFFER_SIZE {
                    let pixel = self.framebuffer[fb_offset / 4];
                    (pixel >> (8 * (fb_offset % 4))) as u32 & 0xFF
                } else {
                    0
                }
            }
            VGA_ROM_START..=VGA_ROM_END => {
                let rom_offset = offset - VGA_ROM_START;
                if rom_offset < self.rom.len() {
                    self.rom[rom_offset] as u32
                } else {
                    0xFF
                }
            }
            _ => 0,
        }
    }

    pub fn write(&mut self, offset: usize, value: u32) {
        let v = value as u8;
        match offset {
            VGA_CRTC_INDEX => {
                self.crtc_index = v;
            }
            VGA_CRTC_DATA => {
                let idx = self.crtc_index as usize;
                if idx < self.crtc_regs.len() {
                    self.crtc_regs[idx] = v;
                }
            }
            VGA_SEQ_INDEX => {
                self.seq_index = v;
            }
            0x3C5 => {
                let idx = self.seq_index as usize;
                if idx < self.seq_regs.len() {
                    self.seq_regs[idx] = v;
                }
            }
            VGA_GDC_INDEX => {
                self.gdc_index = v;
            }
            0x3CF => {
                let idx = self.gdc_index as usize;
                if idx < self.gdc_regs.len() {
                    self.gdc_regs[idx] = v;
                }
            }
            VGA_ATC_INDEX => {
                if !self.atc_flipflop {
                    self.atc_index = v & 0x1F;
                } else {
                    let idx = self.atc_index as usize;
                    if idx < self.atc_regs.len() {
                        self.atc_regs[idx] = v;
                    }
                }
                self.atc_flipflop = !self.atc_flipflop;
            }
            VGA_MISC_OUTPUT => {
                self.misc_output = v;
            }
            VGA_IS1 => {
                self.atc_flipflop = false;
            }
            VGA_FB_START..=VGA_FB_END => {
                let fb_offset = offset - VGA_FB_START;
                if fb_offset < VGA_FRAMEBUFFER_SIZE {
                    let byte_idx = fb_offset % 4;
                    let pixel_idx = fb_offset / 4;
                    let mut pixel = self.framebuffer[pixel_idx];
                    let shift = 8 * byte_idx;
                    pixel &= !(0xFF << shift);
                    pixel |= (v as u32) << shift;
                    self.framebuffer[pixel_idx] = pixel;
                    self.dirty = true;
                }
            }
            VGA_ROM_START..=VGA_ROM_END => {
            }
            _ => {}
        }
    }

    pub fn irq_pending(&self) -> bool {
        self.irq_pending
    }

    pub fn vsync(&mut self) {
        self.irq_pending = true;
    }

    pub fn clear_vsync(&mut self) {
        self.irq_pending = false;
    }

    pub fn reset(&mut self) {
        self.crtc_index = 0;
        self.seq_index = 0;
        self.gdc_index = 0;
        self.atc_index = 0;
        self.atc_flipflop = false;
        self.misc_output = 0xE3;
        self.is1 = 0x00;
        self.framebuffer.fill(0xFF000000);
        self.dirty = true;
        self.irq_pending = false;
    }

    pub fn get_resolution(&self) -> (usize, usize) {
        let h_total = self.crtc_regs[0x00] as usize + 5;
        let v_total = ((self.crtc_regs[0x06] as usize & 0x01) << 8
            | self.crtc_regs[0x12] as usize)
            + 1;
        (h_total.min(VGA_WIDTH), v_total.min(VGA_HEIGHT))
    }

    pub fn read32(&self, offset: usize) -> u32 {
        if offset >= VGA_FB_START && offset + 3 <= VGA_FB_END {
            let fb_offset = offset - VGA_FB_START;
            let pixel_idx = fb_offset / 4;
            if pixel_idx < self.framebuffer.len() {
                return self.framebuffer[pixel_idx];
            }
        }
        0
    }

    pub fn write32(&mut self, offset: usize, value: u32) {
        if offset >= VGA_FB_START && offset + 3 <= VGA_FB_END {
            let fb_offset = offset - VGA_FB_START;
            let pixel_idx = fb_offset / 4;
            if pixel_idx < self.framebuffer.len() {
                self.framebuffer[pixel_idx] = value;
                self.dirty = true;
            }
        }
    }
}