# RV32-Machine

A GUI-based **RV32IMAC** (RISC-V 32-bit) machine simulator written in Rust, featuring a rich set of peripherals and the ability to boot a Linux kernel with initramfs.

## Features

### CPU

- **RV32IMAC** instruction set support (Integer, Multiply, Atomic, Compressed)
- Full CSR (Control and Status Register) implementation for Machine and Supervisor privilege modes
- Trap/exception handling (illegal instruction, ecall, timer interrupt, external interrupt)
- Compressed instruction (RVC) decoding
- Cycle and instruction counters

### Memory Map

| Region | Base Address | Size |
|--------|-------------|------|
| Boot ROM | `0x0000_0000` | 64 KB |
| PLIC | `0x0C00_0000` | — |
| UART | `0x1000_0000` | 4 KB |
| VGA (VirtIO GPU) | `0x2000_0000` | 4 KB |
| Audio (ES1371) | `0x3000_0000` | 4 KB |
| USB (OHCI) | `0x4000_0000` | 4 KB |
| SCSI (53c895a) | `0x5000_0000` | 4 KB |
| SATA (AHCI) | `0x6000_0000` | 4 KB |
| RAM | `0x8000_0000` | 128 MB |

### Peripherals

- **UART 16550** — Serial port with TX/RX buffers and interrupt support
- **VirtIO GPU** — MMIO-based virtio-gpu device with 640×480 framebuffer (VirtIO Spec v1.2)
- **VGA Controller** — S3 Trio64V+ compatible VGA with CRTC/Sequencer/GDC/ATC registers
- **ES1371** — Ensoniq AudioPCI sound device with DAC/ADC channels
- **OHCI** — Open Host Controller Interface for USB 1.1
- **PLIC** — Platform-Level Interrupt Controller with priority, pending, and enable registers
- **LSI 53c895a** — SCSI controller (LSI Logic/Symbios)
- **AHCI SATA** — Advanced Host Controller Interface with up to 4 ports

### GUI

Built with [egui](https://github.com/emilk/egui) / [eframe](https://github.com/emilk/egui/tree/master/crates/eframe):

- Real-time VGA framebuffer display
- UART console with keyboard input
- Register viewer (x0–x31 + PC + CSRs)
- Memory viewer (hex dump)
- Disassembly view
- Device status panel
- Breakpoint support
- Run/pause/step/reset controls
- Disk image insertion (SATA, SCSI, USB)

### Firmware

Custom boot ROM (assembled at compile time) that:

1. Initializes UART and prints a boot banner
2. Sets up the stack pointer
3. Jumps to the Linux kernel entry point at `0x8000_0000`

### Linux Boot Support

The simulator can boot a real Linux kernel with an initramfs:

- **Kernel** loaded at `0x8000_0000`
- **Device Tree Blob** generated at runtime and loaded at `0x8100_0000`
- **Initramfs** (cpio archive) loaded at `0x8200_0000`

## Project Structure

```
RV32-Machine/
├── Cargo.toml
├── assets/
│   └── rootfs/
│       ├── kernel            # Linux kernel image
│       └── builtin.cpio      # Initramfs (cpio archive)
├── roms/
│   ├── S3VGA.bin             # S3 VGA BIOS ROM
│   └── s3_trio64v_plus.bin   # S3 Trio64V+ BIOS ROM
└── src/
    ├── main.rs               # Entry point, RAM/DTB loading, GUI launch
    ├── bus/
    │   ├── mod.rs
    │   └── mmio.rs           # MMIO bus with address decoding
    ├── cpu/
    │   ├── mod.rs
    │   ├── rv32imac.rs       # RV32IMAC CPU core (fetch/decode/execute)
    │   └── csr.rs            # CSR file (mstatus, misa, mtvec, etc.)
    ├── devices/
    │   ├── mod.rs
    │   ├── uart16550.rs      # UART 16550
    │   ├── virtio_gpu.rs     # VirtIO GPU
    │   ├── vga.rs            # VGA controller
    │   ├── es1371.rs         # ES1371 audio
    │   ├── ohci.rs           # OHCI USB
    │   ├── plic.rs           # PLIC
    │   ├── scsi53c895a.rs    # LSI SCSI
    │   ├── sata.rs           # AHCI SATA
    │   └── dtb.rs            # FDT/DTB builder
    ├── firmware/
    │   └── mod.rs            # Boot ROM generator (emits RISC-V machine code)
    ├── gui/
    │   ├── mod.rs
    │   ├── app.rs            # Main simulator GUI application
    │   └── uart_console.rs   # UART console (stdin/stdout bridge)
    └── sample/
        └── virtio-gpu.c      # QEMU virtio-gpu reference (for development)
```

## Getting Started

### Prerequisites

- [Rust](https://www.rust-lang.org/tools/install) (stable toolchain)

### Build & Run

```bash
cargo run
```

This will compile and launch the simulator GUI. If a Linux kernel image is present at `assets/rootfs/kernel`, it will be loaded automatically into RAM.

### Build Only

```bash
cargo build
```

### Release Build

```bash
cargo build --release
```

## Usage

1. **Launch** the simulator — the GUI window opens with a VGA display and control panels
2. **Click "Run"** to start CPU execution (or use **Step** for single-step debugging)
3. **UART Console** — a separate console window is allocated on Windows for UART I/O; type input to send bytes to the emulated UART
4. **Breakpoints** — set breakpoints at specific PC addresses to pause execution
5. **Memory/Registers** — inspect memory and CPU state in real time
6. **Disk Images** — insert disk image paths for SATA, SCSI, or USB devices via the GUI

## Dependencies

| Crate | Purpose |
|-------|---------|
| `eframe` / `egui` | GUI framework |
| `wgpu` | GPU-accelerated rendering backend |
| `winit` | Window creation and event handling |
| `cpal` | Cross-platform audio output |
| `ringbuf` | Lock-free SPSC ring buffer (audio) |
| `parking_lot` | High-performance synchronization primitives |
| `bytemuck` | Safe byte casting for GPU textures |
| `thiserror` | Derive macro for error types |
| `log` / `env_logger` | Logging |

## License

This project is provided as-is for educational and research purposes.
