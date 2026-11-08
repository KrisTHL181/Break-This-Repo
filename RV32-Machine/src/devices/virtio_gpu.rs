// virtio-gpu device implementation based on QEMU's virtio-gpu.c
// References: Virtio Spec v1.2, QEMU hw/display/virtio-gpu.c

// --- Virtio MMIO Transport Registers (offset 0x000 - 0x0FF) ---
const VIRTIO_MMIO_MAGIC: usize = 0x000;
const VIRTIO_MMIO_VERSION: usize = 0x004;
const VIRTIO_MMIO_DEVICE_ID: usize = 0x008;
const VIRTIO_MMIO_VENDOR_ID: usize = 0x00C;
const VIRTIO_MMIO_DEVICE_FEATURES: usize = 0x010;
const VIRTIO_MMIO_DEVICE_FEATURES_SEL: usize = 0x014;
const VIRTIO_MMIO_DRIVER_FEATURES: usize = 0x020;
const VIRTIO_MMIO_DRIVER_FEATURES_SEL: usize = 0x024;
const VIRTIO_MMIO_QUEUE_SEL: usize = 0x030;
const VIRTIO_MMIO_QUEUE_NUM_MAX: usize = 0x034;
const VIRTIO_MMIO_QUEUE_NUM: usize = 0x038;
const VIRTIO_MMIO_QUEUE_READY: usize = 0x044;
const VIRTIO_MMIO_QUEUE_NOTIFY: usize = 0x050;
const VIRTIO_MMIO_INTERRUPT_STATUS: usize = 0x060;
const VIRTIO_MMIO_INTERRUPT_ACK: usize = 0x064;
const VIRTIO_MMIO_STATUS: usize = 0x070;
const VIRTIO_MMIO_QUEUE_DESC_LOW: usize = 0x080;
const VIRTIO_MMIO_QUEUE_DESC_HIGH: usize = 0x084;
const VIRTIO_MMIO_QUEUE_DRIVER_LOW: usize = 0x090;
const VIRTIO_MMIO_QUEUE_DRIVER_HIGH: usize = 0x094;
const VIRTIO_MMIO_QUEUE_DEVICE_LOW: usize = 0x0A0;
const VIRTIO_MMIO_QUEUE_DEVICE_HIGH: usize = 0x0A4;
const VIRTIO_MMIO_CONFIG_GENERATION: usize = 0x0FC;

// --- Virtio GPU Device-Specific Config (offset 0x100 - 0x11F) ---
const VIRTIO_GPU_EVENTS_READ: usize = 0x100;
const VIRTIO_GPU_EVENTS_CLEAR: usize = 0x104;
const VIRTIO_GPU_NUM_SCANOUTS: usize = 0x110;
const VIRTIO_GPU_NUM_CAPSETS: usize = 0x114;

// --- Framebuffer (offset 0xA0000, same as legacy VGA for firmware compat) ---
const VIRTIO_GPU_FB_START: usize = 0xA0000;

// --- Display Parameters ---
pub const VIRTIO_GPU_WIDTH: usize = 640;
pub const VIRTIO_GPU_HEIGHT: usize = 480;
const VIRTIO_GPU_FB_PIXELS: usize = VIRTIO_GPU_WIDTH * VIRTIO_GPU_HEIGHT;
const VIRTIO_GPU_FB_SIZE: usize = VIRTIO_GPU_FB_PIXELS * 4;
const VIRTIO_GPU_FB_END: usize = VIRTIO_GPU_FB_START + VIRTIO_GPU_FB_SIZE - 1;

// --- Virtio GPU Feature Bits ---
const VIRTIO_GPU_F_EDID: u32 = 1 << 1;

// --- Virtio GPU Command Types ---
const VIRTIO_GPU_CMD_GET_DISPLAY_INFO: u32 = 0x0100;
const VIRTIO_GPU_CMD_RESOURCE_CREATE_2D: u32 = 0x0101;
const VIRTIO_GPU_CMD_RESOURCE_UNREF: u32 = 0x0102;
const VIRTIO_GPU_CMD_SET_SCANOUT: u32 = 0x0103;
const VIRTIO_GPU_CMD_RESOURCE_FLUSH: u32 = 0x0104;
const VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D: u32 = 0x0105;
const VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING: u32 = 0x0106;
const VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING: u32 = 0x0107;

// --- Virtio GPU Response Types ---
const VIRTIO_GPU_RESP_OK_NODATA: u32 = 0x1100;
const VIRTIO_GPU_RESP_OK_DISPLAY_INFO: u32 = 0x1101;

// --- Virtio GPU Error Codes ---
const VIRTIO_GPU_RESP_ERR_UNSPEC: u32 = 0x1200;
const VIRTIO_GPU_RESP_ERR_INVALID_PARAMETER: u32 = 0x1201;
const VIRTIO_GPU_RESP_ERR_INVALID_RESOURCE_ID: u32 = 0x1202;

// --- Virtio GPU Pixel Formats ---
const VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM: u32 = 2;

// --- Virtio Queue constants ---
const VIRTQ_DESC_F_NEXT: u16 = 1;
const VIRTQ_DESC_F_WRITE: u16 = 2;
const MAX_QUEUE_SIZE: u32 = 64;

// --- Virtio Status bits ---
pub const VIRTIO_STATUS_ACKNOWLEDGE: u32 = 1;
pub const VIRTIO_STATUS_DRIVER: u32 = 2;
pub const VIRTIO_STATUS_DRIVER_OK: u32 = 4;
pub const VIRTIO_STATUS_FEATURES_OK: u32 = 8;
pub const VIRTIO_STATUS_DEVICE_NEEDS_RESET: u32 = 64;
pub const VIRTIO_STATUS_FAILED: u32 = 128;

// --- Virtio Descriptor (16 bytes) ---
#[derive(Clone, Copy, Default)]
struct VirtqDesc {
    addr: u64,
    len: u32,
    flags: u16,
    next: u16,
}

// --- Virtio Available Ring ---
#[derive(Clone)]
struct VirtqAvail {
    flags: u16,
    idx: u16,
    ring: Vec<u16>,
    used_event: u16,
}

// --- Virtio Used Ring Element (8 bytes) ---
#[derive(Clone, Copy, Default)]
struct VirtqUsedElem {
    id: u32,
    len: u32,
}

// --- Virtio Used Ring ---
#[derive(Clone)]
struct VirtqUsed {
    flags: u16,
    idx: u16,
    ring: Vec<VirtqUsedElem>,
    avail_event: u16,
}

// --- Virtio Queue ---
#[derive(Clone)]
struct VirtQueue {
    desc: u64,
    driver: u64,
    device: u64,
    ready: bool,
    size: u32,
    last_avail_idx: u16,
}

impl VirtQueue {
    fn new() -> Self {
        Self {
            desc: 0,
            driver: 0,
            device: 0,
            ready: false,
            size: 0,
            last_avail_idx: 0,
        }
    }
}

// --- GPU Control Header (24 bytes) ---
#[repr(C)]
#[derive(Clone, Copy, Default)]
struct VirtioGpuCtrlHdr {
    hdr_type: u32,
    flags: u32,
    fence_id: u64,
    ctx_id: u32,
    padding: u32,
}

// --- GPU Rectangle (16 bytes) ---
#[repr(C)]
#[derive(Clone, Copy, Default)]
struct VirtioGpuRect {
    x: u32,
    y: u32,
    width: u32,
    height: u32,
}

// --- Display Info Response (per-scanout, 24 bytes) ---
#[repr(C)]
#[derive(Clone, Copy, Default)]
struct VirtioGpuDisplayOne {
    r: VirtioGpuRect,
    enabled: u32,
    flags: u32,
}

// --- GPU Resource ---
#[derive(Clone)]
struct GpuResource {
    resource_id: u32,
    width: u32,
    height: u32,
    format: u32,
    scanout_id: u32,
}

// --- Main Device ---
pub struct VirtioGpu {
    device_features_sel: u32,
    driver_features_sel: u32,
    pub device_features: u32,
    pub driver_features: u32,
    queue_sel: u32,
    pub status: u32,
    pub interrupt_status: u32,
    config_generation: u32,
    events_read: u32,
    events_clear: u32,
    pub num_scanouts: u32,
    num_capsets: u32,
    pub framebuffer: Vec<u32>,
    pub dirty: bool,
    queues: [VirtQueue; 2],
    resources: Vec<GpuResource>,
    scanout_resource_id: u32,
    scanout_rect: VirtioGpuRect,
    pending_notify: Option<u16>,
}

impl VirtioGpu {
    pub fn new() -> Self {
        let mut queues: [VirtQueue; 2] = std::array::from_fn(|_| VirtQueue::new());
        queues[0] = VirtQueue::new();
        queues[1] = VirtQueue::new();

        Self {
            device_features_sel: 0,
            driver_features_sel: 0,
            device_features: VIRTIO_GPU_F_EDID,
            driver_features: 0,
            queue_sel: 0,
            status: 0,
            interrupt_status: 0,
            config_generation: 1,
            events_read: 0,
            events_clear: 0,
            num_scanouts: 1,
            num_capsets: 0,
            framebuffer: vec![0xFF000000u32; VIRTIO_GPU_FB_PIXELS],
            dirty: false,
            queues,
            resources: Vec::new(),
            scanout_resource_id: 0,
            scanout_rect: VirtioGpuRect { x: 0, y: 0, width: VIRTIO_GPU_WIDTH as u32, height: VIRTIO_GPU_HEIGHT as u32 },
            pending_notify: None,
        }
    }

    // --- Helper: read a u32 from guest RAM at a given address ---
    fn read_ram_u32(ram: &[u8], addr: u64) -> u32 {
        let a = addr as usize;
        if a + 4 <= ram.len() {
            u32::from_le_bytes([ram[a], ram[a+1], ram[a+2], ram[a+3]])
        } else {
            0
        }
    }

    fn write_ram_u32(ram: &mut [u8], addr: u64, val: u32) {
        let a = addr as usize;
        if a + 4 <= ram.len() {
            ram[a..a+4].copy_from_slice(&val.to_le_bytes());
        }
    }

    fn read_ram_u64(ram: &[u8], addr: u64) -> u64 {
        let lo = Self::read_ram_u32(ram, addr) as u64;
        let hi = Self::read_ram_u32(ram, addr + 4) as u64;
        lo | (hi << 32)
    }

    // --- Process a virtqueue (called when driver notifies) ---
    pub fn process_queue(&mut self, queue_idx: u16, ram: &mut [u8]) {
        if queue_idx >= 2 {
            return;
        }

        if !self.queues[queue_idx as usize].ready {
            return;
        }

        let avail_addr = self.queues[queue_idx as usize].driver;
        let _avail_flags = Self::read_ram_u32(ram, avail_addr) as u16;
        let avail_idx = Self::read_ram_u32(ram, avail_addr + 2) as u16;

        let used_addr = self.queues[queue_idx as usize].device;
        let mut used_idx = Self::read_ram_u32(ram, used_addr + 2) as u16;
        let q_size = self.queues[queue_idx as usize].size;

        let mut last_avail = self.queues[queue_idx as usize].last_avail_idx;

        while last_avail != avail_idx {
            let ring_entry_addr = avail_addr + 4 + (last_avail as u64 % q_size as u64) as u64 * 2;
            let desc_idx = Self::read_ram_u32(ram, ring_entry_addr) as u16;

            if queue_idx == 0 {
                self.process_ctrl_cmd(desc_idx, ram);
            }

            let used_elem_offset = 4 + (used_idx as u64 % q_size as u64) * 8;
            Self::write_ram_u32(ram, used_addr + used_elem_offset, desc_idx as u32);
            Self::write_ram_u32(ram, used_addr + used_elem_offset + 4, 0);
            used_idx = used_idx.wrapping_add(1);
            last_avail = last_avail.wrapping_add(1);
        }

        self.queues[queue_idx as usize].last_avail_idx = last_avail;

        // Update used ring idx
        Self::write_ram_u32(ram, used_addr + 2, used_idx as u32);

        // Trigger interrupt
        self.interrupt_status |= 1;
    }

    fn process_ctrl_cmd(&mut self, desc_idx: u16, ram: &mut [u8]) {
        let q = &self.queues[0];
        let mut d = desc_idx;

        // Walk descriptor chain to collect buffers
        let mut out_bufs: Vec<(u64, u32)> = Vec::new();
        let mut in_bufs: Vec<(u64, u32)> = Vec::new();

        loop {
            let desc_addr = q.desc + (d as u64) * 16;
            let addr = Self::read_ram_u64(ram, desc_addr);
            let len = Self::read_ram_u32(ram, desc_addr + 8);
            let flags = Self::read_ram_u32(ram, desc_addr + 12) as u16;
            let next = Self::read_ram_u32(ram, desc_addr + 14) as u16;

            if flags & VIRTQ_DESC_F_WRITE != 0 {
                in_bufs.push((addr, len));
            } else {
                out_bufs.push((addr, len));
            }

            if flags & VIRTQ_DESC_F_NEXT == 0 {
                break;
            }
            d = next;
        }

        // Read command header from first out buffer
        if out_bufs.is_empty() {
            return;
        }

        let (cmd_addr, _cmd_len) = out_bufs[0];
        let cmd_type = Self::read_ram_u32(ram, cmd_addr);

        // All command-specific fields start at offset 24 (after virtio_gpu_ctrl_hdr)
        const HDR_SZ: u64 = 24;

        match cmd_type {
            VIRTIO_GPU_CMD_GET_DISPLAY_INFO => {
                if let Some(&(resp_addr, _resp_len)) = in_bufs.first() {
                    self.write_display_info(ram, resp_addr);
                }
            }
            VIRTIO_GPU_CMD_RESOURCE_CREATE_2D => {
                // struct virtio_gpu_resource_create_2d: hdr + resource_id + format + width + height
                let resource_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ);
                let format = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 4);
                let width = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 8);
                let height = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 12);

                if resource_id == 0 {
                    self.write_error_response(ram, in_bufs, VIRTIO_GPU_RESP_ERR_INVALID_RESOURCE_ID);
                    return;
                }
                if self.resources.iter().any(|r| r.resource_id == resource_id) {
                    self.write_error_response(ram, in_bufs, VIRTIO_GPU_RESP_ERR_INVALID_RESOURCE_ID);
                    return;
                }

                self.resources.push(GpuResource {
                    resource_id,
                    width,
                    height,
                    format,
                    scanout_id: 0,
                });
                self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
            }
            VIRTIO_GPU_CMD_RESOURCE_UNREF => {
                // struct virtio_gpu_resource_unref: hdr + resource_id + padding
                let resource_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ);
                if let Some(pos) = self.resources.iter().position(|r| r.resource_id == resource_id) {
                    if self.scanout_resource_id == resource_id {
                        self.scanout_resource_id = 0;
                    }
                    self.resources.remove(pos);
                }
                self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
            }
            VIRTIO_GPU_CMD_SET_SCANOUT => {
                // struct virtio_gpu_set_scanout: hdr + rect(16) + scanout_id + resource_id
                let r_x = Self::read_ram_u32(ram, cmd_addr + HDR_SZ);
                let r_y = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 4);
                let r_w = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 8);
                let r_h = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 12);
                let scanout_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 16);
                let resource_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 20);

                if scanout_id >= self.num_scanouts {
                    self.write_error_response(ram, in_bufs, VIRTIO_GPU_RESP_ERR_INVALID_PARAMETER);
                    return;
                }

                if resource_id == 0 {
                    self.scanout_resource_id = 0;
                    self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
                    return;
                }

                if let Some(res) = self.resources.iter_mut().find(|r| r.resource_id == resource_id) {
                    res.scanout_id = scanout_id;
                    self.scanout_resource_id = resource_id;
                    self.scanout_rect = VirtioGpuRect {
                        x: r_x,
                        y: r_y,
                        width: r_w,
                        height: r_h,
                    };
                    self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
                } else {
                    self.write_error_response(ram, in_bufs, VIRTIO_GPU_RESP_ERR_INVALID_RESOURCE_ID);
                }
            }
            VIRTIO_GPU_CMD_RESOURCE_FLUSH => {
                // struct virtio_gpu_resource_flush: hdr + rect(16) + resource_id + padding
                let r_x = Self::read_ram_u32(ram, cmd_addr + HDR_SZ);
                let r_y = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 4);
                let r_w = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 8);
                let r_h = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 12);
                let _resource_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 16);

                // Validate bounds against scanout
                if r_x + r_w <= self.scanout_rect.width && r_y + r_h <= self.scanout_rect.height {
                    self.dirty = true;
                }
                self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
            }
            VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D => {
                // struct virtio_gpu_transfer_to_host_2d: hdr + rect(16) + offset(8) + resource_id + padding
                let r_x = Self::read_ram_u32(ram, cmd_addr + HDR_SZ);
                let r_y = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 4);
                let r_w = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 8);
                let r_h = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 12);
                let _offset = Self::read_ram_u64(ram, cmd_addr + HDR_SZ + 16);
                let resource_id = Self::read_ram_u32(ram, cmd_addr + HDR_SZ + 24);

                if self.resources.iter().any(|r| r.resource_id == resource_id) {
                    if r_x + r_w <= self.scanout_rect.width && r_y + r_h <= self.scanout_rect.height {
                        // Data was already written to framebuffer via direct MMIO
                        self.dirty = true;
                    }
                }
                self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
            }
            VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING | VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING => {
                self.write_ctrl_header(ram, in_bufs, VIRTIO_GPU_RESP_OK_NODATA);
            }
            _ => {
                self.write_error_response(ram, in_bufs, VIRTIO_GPU_RESP_ERR_UNSPEC);
            }
        }
    }

    fn write_ctrl_header(&self, ram: &mut [u8], in_bufs: Vec<(u64, u32)>, resp_type: u32) {
        if let Some(&(resp_addr, _resp_len)) = in_bufs.first() {
            Self::write_ram_u32(ram, resp_addr, resp_type);
            Self::write_ram_u32(ram, resp_addr + 4, 0);
            Self::write_ram_u64(ram, resp_addr + 8, 0);
            Self::write_ram_u32(ram, resp_addr + 16, 0);
            Self::write_ram_u32(ram, resp_addr + 20, 0);
        }
    }

    fn write_ram_u64(ram: &mut [u8], addr: u64, val: u64) {
        Self::write_ram_u32(ram, addr, val as u32);
        Self::write_ram_u32(ram, addr + 4, (val >> 32) as u32);
    }

    fn write_error_response(&self, ram: &mut [u8], in_bufs: Vec<(u64, u32)>, err: u32) {
        self.write_ctrl_header(ram, in_bufs, err);
    }

    fn write_display_info(&self, ram: &mut [u8], resp_addr: u64) {
        // struct virtio_gpu_resp_display_info
        // hdr.type = VIRTIO_GPU_RESP_OK_DISPLAY_INFO
        Self::write_ram_u32(ram, resp_addr, VIRTIO_GPU_RESP_OK_DISPLAY_INFO);
        Self::write_ram_u32(ram, resp_addr + 4, 0); // flags
        Self::write_ram_u64(ram, resp_addr + 8, 0); // fence_id
        Self::write_ram_u32(ram, resp_addr + 16, 0); // ctx_id
        Self::write_ram_u32(ram, resp_addr + 20, 0); // padding

        // pmodes[0]
        let pmodes_off = resp_addr + 24;
        Self::write_ram_u32(ram, pmodes_off, 0); // r.x
        Self::write_ram_u32(ram, pmodes_off + 4, 0); // r.y
        Self::write_ram_u32(ram, pmodes_off + 8, VIRTIO_GPU_WIDTH as u32); // r.width
        Self::write_ram_u32(ram, pmodes_off + 12, VIRTIO_GPU_HEIGHT as u32); // r.height
        Self::write_ram_u32(ram, pmodes_off + 16, 1); // enabled
        Self::write_ram_u32(ram, pmodes_off + 20, 0); // flags
    }

    // --- MMIO Read ---
    pub fn read(&mut self, offset: usize) -> u32 {
        match offset {
            VIRTIO_MMIO_MAGIC => 0x74726976,
            VIRTIO_MMIO_VERSION => 0x2,
            VIRTIO_MMIO_DEVICE_ID => 0x10,
            VIRTIO_MMIO_VENDOR_ID => 0x1AF4,
            VIRTIO_MMIO_DEVICE_FEATURES => {
                if self.device_features_sel == 0 {
                    self.device_features as u32
                } else {
                    ((self.device_features as u64) >> 32) as u32
                }
            }
            VIRTIO_MMIO_DEVICE_FEATURES_SEL => self.device_features_sel,
            VIRTIO_MMIO_DRIVER_FEATURES => self.driver_features,
            VIRTIO_MMIO_DRIVER_FEATURES_SEL => self.driver_features_sel,
            VIRTIO_MMIO_QUEUE_SEL => self.queue_sel,
            VIRTIO_MMIO_QUEUE_NUM_MAX => MAX_QUEUE_SIZE,
            VIRTIO_MMIO_QUEUE_NUM => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].size
                } else {
                    0
                }
            }
            VIRTIO_MMIO_QUEUE_READY => {
                if (self.queue_sel as usize) < self.queues.len() {
                    if self.queues[self.queue_sel as usize].ready { 1 } else { 0 }
                } else {
                    0
                }
            }
            VIRTIO_MMIO_QUEUE_NOTIFY => 0,
            VIRTIO_MMIO_INTERRUPT_STATUS => self.interrupt_status,
            VIRTIO_MMIO_INTERRUPT_ACK => 0,
            VIRTIO_MMIO_STATUS => self.status,
            VIRTIO_MMIO_QUEUE_DESC_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].desc as u32
                } else { 0 }
            }
            VIRTIO_MMIO_QUEUE_DESC_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    (self.queues[self.queue_sel as usize].desc >> 32) as u32
                } else { 0 }
            }
            VIRTIO_MMIO_QUEUE_DRIVER_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].driver as u32
                } else { 0 }
            }
            VIRTIO_MMIO_QUEUE_DRIVER_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    (self.queues[self.queue_sel as usize].driver >> 32) as u32
                } else { 0 }
            }
            VIRTIO_MMIO_QUEUE_DEVICE_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].device as u32
                } else { 0 }
            }
            VIRTIO_MMIO_QUEUE_DEVICE_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    (self.queues[self.queue_sel as usize].device >> 32) as u32
                } else { 0 }
            }
            VIRTIO_MMIO_CONFIG_GENERATION => self.config_generation,
            VIRTIO_GPU_EVENTS_READ => self.events_read,
            VIRTIO_GPU_EVENTS_CLEAR => self.events_clear,
            VIRTIO_GPU_NUM_SCANOUTS => self.num_scanouts,
            VIRTIO_GPU_NUM_CAPSETS => self.num_capsets,
            VIRTIO_GPU_FB_START..=VIRTIO_GPU_FB_END => {
                let fb_offset = offset - VIRTIO_GPU_FB_START;
                if fb_offset < VIRTIO_GPU_FB_SIZE {
                    let pixel = self.framebuffer[fb_offset / 4];
                    (pixel >> (8 * (fb_offset % 4))) as u32 & 0xFF
                } else { 0 }
            }
            _ => 0,
        }
    }

    // --- MMIO Write ---
    pub fn write(&mut self, offset: usize, value: u32) {
        match offset {
            VIRTIO_MMIO_DEVICE_FEATURES_SEL => self.device_features_sel = value,
            VIRTIO_MMIO_DRIVER_FEATURES => self.driver_features = value,
            VIRTIO_MMIO_DRIVER_FEATURES_SEL => self.driver_features_sel = value,
            VIRTIO_MMIO_QUEUE_SEL => self.queue_sel = value,
            VIRTIO_MMIO_QUEUE_NUM => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].size = value.min(MAX_QUEUE_SIZE);
                }
            }
            VIRTIO_MMIO_QUEUE_READY => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].ready = value != 0;
                }
            }
            VIRTIO_MMIO_QUEUE_NOTIFY => {
                self.pending_notify = Some(value as u16);
            }
            VIRTIO_MMIO_INTERRUPT_STATUS => self.interrupt_status = value,
            VIRTIO_MMIO_INTERRUPT_ACK => self.interrupt_status &= !value,
            VIRTIO_MMIO_STATUS => self.status = value,
            VIRTIO_MMIO_QUEUE_DESC_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].desc =
                        (self.queues[self.queue_sel as usize].desc & 0xFFFF_FFFF_0000_0000) | value as u64;
                }
            }
            VIRTIO_MMIO_QUEUE_DESC_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].desc =
                        (self.queues[self.queue_sel as usize].desc & 0xFFFF_FFFF) | ((value as u64) << 32);
                }
            }
            VIRTIO_MMIO_QUEUE_DRIVER_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].driver =
                        (self.queues[self.queue_sel as usize].driver & 0xFFFF_FFFF_0000_0000) | value as u64;
                }
            }
            VIRTIO_MMIO_QUEUE_DRIVER_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].driver =
                        (self.queues[self.queue_sel as usize].driver & 0xFFFF_FFFF) | ((value as u64) << 32);
                }
            }
            VIRTIO_MMIO_QUEUE_DEVICE_LOW => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].device =
                        (self.queues[self.queue_sel as usize].device & 0xFFFF_FFFF_0000_0000) | value as u64;
                }
            }
            VIRTIO_MMIO_QUEUE_DEVICE_HIGH => {
                if (self.queue_sel as usize) < self.queues.len() {
                    self.queues[self.queue_sel as usize].device =
                        (self.queues[self.queue_sel as usize].device & 0xFFFF_FFFF) | ((value as u64) << 32);
                }
            }
            VIRTIO_GPU_EVENTS_CLEAR => self.events_clear = value,
            VIRTIO_GPU_FB_START..=VIRTIO_GPU_FB_END => {
                let fb_offset = offset - VIRTIO_GPU_FB_START;
                if fb_offset < VIRTIO_GPU_FB_SIZE {
                    let byte_idx = fb_offset % 4;
                    let pixel_idx = fb_offset / 4;
                    let mut pixel = self.framebuffer[pixel_idx];
                    let shift = 8 * byte_idx;
                    pixel &= !(0xFF << shift);
                    pixel |= (value as u32) << shift;
                    self.framebuffer[pixel_idx] = pixel;
                    self.dirty = true;
                }
            }
            _ => {}
        }
    }

    pub fn process_pending_notify(&mut self, ram: &mut [u8]) {
        if let Some(queue_idx) = self.pending_notify.take() {
            if self.status & VIRTIO_STATUS_DRIVER_OK == 0 {
                return;
            }
            self.process_queue(queue_idx, ram);
        }
    }

    pub fn has_pending_notify(&self) -> bool {
        self.pending_notify.is_some()
    }

    pub fn irq_pending(&self) -> bool {
        self.interrupt_status != 0
    }
}