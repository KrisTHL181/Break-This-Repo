mod cpu;
mod bus;
mod devices;
mod gui;
mod firmware;

use std::sync::Arc;
use parking_lot::Mutex;
use log::info;

use bus::Bus;
use cpu::Cpu;
use devices::plic::Plic;
use devices::dtb;

const RAM_SIZE: usize = 128 * 1024 * 1024;

const RAM_BASE: u32 = 0x8000_0000;
const BOOT_ROM_BASE: u32 = 0x0000_0000;
const BOOT_ROM_SIZE: u32 = 0x0001_0000;
const PLIC_BASE: u32 = 0x0C00_0000;
const UART_BASE: u32 = 0x1000_0000;
const VGA_BASE: u32 = 0x2000_0000;
const AUDIO_BASE: u32 = 0x3000_0000;
const USB_BASE: u32 = 0x4000_0000;
const SCSI_BASE: u32 = 0x5000_0000;
const SATA_BASE: u32 = 0x6000_0000;

const PERIPHERAL_SIZE: u32 = 0x1000;

const KERNEL_LOAD_ADDR: u32 = 0x8000_0000;
const DTB_LOAD_ADDR: u32 = 0x8100_0000;
const INITRD_LOAD_ADDR: u32 = 0x8200_0000;

#[cfg(target_os = "windows")]
extern "system" {
    fn AllocConsole() -> i32;
    fn FreeConsole() -> i32;
    fn SetConsoleTitleW(title: *const u16) -> i32;
    fn SetStdHandle(nStdHandle: u32, hHandle: isize) -> i32;
    fn CreateFileW(
        lpFileName: *const u16,
        dwDesiredAccess: u32,
        dwShareMode: u32,
        lpSecurityAttributes: *const u8,
        dwCreationDisposition: u32,
        dwFlagsAndAttributes: u32,
        hTemplateFile: isize,
    ) -> isize;
}

#[cfg(target_os = "windows")]
const STD_OUTPUT_HANDLE: u32 = 0xFFFFFFF5u32; // -11
#[cfg(target_os = "windows")]
const STD_ERROR_HANDLE: u32 = 0xFFFFFFF4u32; // -12
#[cfg(target_os = "windows")]
const GENERIC_WRITE: u32 = 0x40000000;
#[cfg(target_os = "windows")]
const GENERIC_READ: u32 = 0x80000000;
#[cfg(target_os = "windows")]
const FILE_SHARE_WRITE: u32 = 0x00000002;
#[cfg(target_os = "windows")]
const OPEN_EXISTING: u32 = 3;

fn main() {
    #[cfg(target_os = "windows")]
    unsafe {
        use std::os::windows::ffi::OsStrExt;
        FreeConsole();
        AllocConsole();

        let title: Vec<u16> = std::ffi::OsStr::new("UART Console - RV32-Machine")
            .encode_wide()
            .chain(std::iter::once(0))
            .collect();
        SetConsoleTitleW(title.as_ptr());

        // Reopen stdout/stderr to point to the new console
        let conout: Vec<u16> = "CONOUT$\0".encode_utf16().collect();
        let h = CreateFileW(
            conout.as_ptr(),
            GENERIC_WRITE | GENERIC_READ,
            FILE_SHARE_WRITE,
            std::ptr::null(),
            OPEN_EXISTING,
            0,
            0,
        );
        if h != -1 {
            SetStdHandle(STD_OUTPUT_HANDLE, h);
            SetStdHandle(STD_ERROR_HANDLE, h);
        }
    }

    env_logger::init();
    info!("UART Console enabled. Starting RV32-Machine Simulator");

    let ram = Arc::new(Mutex::new(vec![0u8; RAM_SIZE]));
    let plic = Arc::new(Mutex::new(Plic::new()));
    let bus = Arc::new(Mutex::new(Bus::new(ram.clone(), plic.clone())));

    let kernel_path = std::path::Path::new("assets/rootfs/kernel");
    let initrd_path = std::path::Path::new("assets/rootfs/builtin.cpio");
    let kernel_loaded = kernel_path.exists();

    if kernel_loaded {
        info!("Loading Linux kernel from {:?}", kernel_path);
        let kernel_data = std::fs::read(kernel_path).expect("Failed to read kernel image");
        let mut ram_guard = ram.lock();
        let copy_len = kernel_data.len().min(RAM_SIZE);
        let kernel_offset = (KERNEL_LOAD_ADDR - RAM_BASE) as usize;
        ram_guard[kernel_offset..kernel_offset + copy_len].copy_from_slice(&kernel_data[..copy_len]);
        info!("Kernel loaded: {} bytes at {:#010X}", copy_len, KERNEL_LOAD_ADDR);
    }

    let mut initrd_start: u32 = 0;
    let mut initrd_size: u32 = 0;

    if initrd_path.exists() {
        info!("Loading initramfs from {:?}", initrd_path);
        let initrd_data = std::fs::read(initrd_path).expect("Failed to read initramfs");
        let mut ram_guard = ram.lock();
        let initrd_offset = (INITRD_LOAD_ADDR - RAM_BASE) as usize;
        let copy_len = initrd_data.len().min(RAM_SIZE - initrd_offset);
        ram_guard[initrd_offset..initrd_offset + copy_len].copy_from_slice(&initrd_data[..copy_len]);
        initrd_start = INITRD_LOAD_ADDR;
        initrd_size = copy_len as u32;
        info!("Initramfs loaded: {} bytes at {:#010X}", copy_len, INITRD_LOAD_ADDR);
    }

    if kernel_loaded {
        info!("Generating device tree blob");
        let dtb_data = dtb::build_dtb(
            RAM_BASE,
            RAM_SIZE as u32,
            UART_BASE,
            VGA_BASE,
            PLIC_BASE,
            initrd_start,
            initrd_size,
        );
        let mut ram_guard = ram.lock();
        let dtb_offset = (DTB_LOAD_ADDR - RAM_BASE) as usize;
        let copy_len = dtb_data.len().min(RAM_SIZE - dtb_offset);
        ram_guard[dtb_offset..dtb_offset + copy_len].copy_from_slice(&dtb_data[..copy_len]);
        info!("DTB loaded: {} bytes at {:#010X}", copy_len, DTB_LOAD_ADDR);
    }

    let cpu = Cpu::new(bus.clone(), plic.clone());

    let native_options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([1024.0, 768.0]),
        ..Default::default()
    };

    let app = gui::SimulatorApp::new(cpu, bus, ram, plic, kernel_loaded);

    eframe::run_native(
        "RV32-Machine Simulator",
        native_options,
        Box::new(|_cc| Ok(Box::new(app))),
    )
    .expect("Failed to run GUI");
}