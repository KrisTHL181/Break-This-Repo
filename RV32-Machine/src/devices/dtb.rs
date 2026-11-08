const FDT_BEGIN_NODE: u32 = 0x00000001;
const FDT_END_NODE: u32 = 0x00000002;
const FDT_PROP: u32 = 0x00000003;
const FDT_END: u32 = 0x00000009;

struct FdtBuilder {
    data: Vec<u8>,
    reserve_entries: Vec<(u64, u64)>,
}

impl FdtBuilder {
    fn new() -> Self {
        FdtBuilder {
            data: Vec::new(),
            reserve_entries: Vec::new(),
        }
    }

    fn begin_node(&mut self, name: &str) {
        self.align(4);
        self.data.extend_from_slice(&FDT_BEGIN_NODE.to_be_bytes());
        self.data.extend_from_slice(name.as_bytes());
        self.data.push(0);
        self.align(4);
    }

    fn end_node(&mut self) {
        self.data.extend_from_slice(&FDT_END_NODE.to_be_bytes());
    }

    fn property_str(&mut self, name: &str, value: &str) {
        let mut val_bytes = value.as_bytes().to_vec();
        val_bytes.push(0);
        self.property_raw(name, &val_bytes);
    }

    fn property_u32(&mut self, name: &str, value: u32) {
        self.property_raw(name, &value.to_be_bytes());
    }

    fn property_u64(&mut self, name: &str, value: u64) {
        self.property_raw(name, &value.to_be_bytes());
    }

    fn property_u32_arr(&mut self, name: &str, values: &[u32]) {
        let mut bytes = Vec::with_capacity(values.len() * 4);
        for v in values {
            bytes.extend_from_slice(&v.to_be_bytes());
        }
        self.property_raw(name, &bytes);
    }

    fn property_u64_arr(&mut self, name: &str, values: &[u64]) {
        let mut bytes = Vec::with_capacity(values.len() * 8);
        for v in values {
            bytes.extend_from_slice(&v.to_be_bytes());
        }
        self.property_raw(name, &bytes);
    }

    fn property_raw(&mut self, name: &str, value: &[u8]) {
        self.data.extend_from_slice(&FDT_PROP.to_be_bytes());
        let len = value.len() as u32;
        self.data.extend_from_slice(&len.to_be_bytes());
        let nameoff_pos = self.data.len();
        self.data.extend_from_slice(&0u32.to_be_bytes());
        self.data.extend_from_slice(name.as_bytes());
        self.data.push(0);
        self.align(4);
        let nameoff = (self.data.len() - 4 - nameoff_pos) as u32;
        self.data[nameoff_pos..nameoff_pos + 4].copy_from_slice(&nameoff.to_be_bytes());
        self.data.extend_from_slice(value);
        self.align(4);
    }

    fn align(&mut self, boundary: usize) {
        let pad = (boundary - (self.data.len() % boundary)) % boundary;
        for _ in 0..pad {
            self.data.push(0);
        }
    }

    fn finish(mut self) -> Vec<u8> {
        self.data.extend_from_slice(&FDT_END.to_be_bytes());

        let totalsize = (40 + 8 * self.reserve_entries.len() + self.data.len()) as u32;
        let off_dt_struct = (40 + 8 * self.reserve_entries.len()) as u32;
        let off_dt_strings = off_dt_struct + self.data.len() as u32;
        let off_mem_rsvmap = 40u32;
        let version = 17u32;
        let last_comp_version = 16u32;
        let boot_cpuid_phys = 0u32;
        let size_dt_strings = 0u32;
        let size_dt_struct = self.data.len() as u32;

        let mut result = Vec::new();
        result.extend_from_slice(&0xd00dfeedu32.to_be_bytes());
        result.extend_from_slice(&totalsize.to_be_bytes());
        result.extend_from_slice(&off_dt_struct.to_be_bytes());
        result.extend_from_slice(&off_dt_strings.to_be_bytes());
        result.extend_from_slice(&off_mem_rsvmap.to_be_bytes());
        result.extend_from_slice(&version.to_be_bytes());
        result.extend_from_slice(&last_comp_version.to_be_bytes());
        result.extend_from_slice(&boot_cpuid_phys.to_be_bytes());
        result.extend_from_slice(&size_dt_strings.to_be_bytes());
        result.extend_from_slice(&size_dt_struct.to_be_bytes());

        for (addr, size) in &self.reserve_entries {
            result.extend_from_slice(&addr.to_be_bytes());
            result.extend_from_slice(&size.to_be_bytes());
        }
        result.extend_from_slice(&0u64.to_be_bytes());
        result.extend_from_slice(&0u64.to_be_bytes());

        result.extend_from_slice(&self.data);
        result
    }
}

pub fn build_dtb(
    memory_base: u32,
    memory_size: u32,
    uart_base: u32,
    virtio_gpu_base: u32,
    plic_base: u32,
    initrd_start: u32,
    initrd_size: u32,
) -> Vec<u8> {
    let mut fdt = FdtBuilder::new();

    fdt.begin_node("");
    fdt.property_u32("#address-cells", 2);
    fdt.property_u32("#size-cells", 2);
    fdt.property_str("compatible", "riscv-virtio");
    fdt.property_str("model", "RV32-Machine");

    fdt.begin_node("chosen");
    fdt.property_str("bootargs", "console=ttyS0 earlycon=sbi root=/dev/ram0 rw");
    fdt.property_str("stdout-path", "/soc/uart@10000000");
    if initrd_size > 0 {
        fdt.property_u64("linux,initrd-start", initrd_start as u64);
        fdt.property_u64("linux,initrd-end", (initrd_start + initrd_size) as u64);
    }
    fdt.end_node();

    fdt.begin_node("memory@80000000");
    fdt.property_str("device_type", "memory");
    fdt.property_u64_arr("reg", &[memory_base as u64, memory_size as u64]);
    fdt.end_node();

    fdt.begin_node("cpus");
    fdt.property_u32("#address-cells", 1);
    fdt.property_u32("#size-cells", 0);
    fdt.property_u32("timebase-frequency", 10000000);

    fdt.begin_node("cpu@0");
    fdt.property_str("device_type", "cpu");
    fdt.property_u32("reg", 0);
    fdt.property_str("compatible", "riscv");
    fdt.property_str("mmu-type", "riscv,sv32");
    fdt.property_u32("clock-frequency", 100000000);
    fdt.property_str("status", "okay");
    fdt.property_str("riscv,isa", "rv32imac");
    fdt.end_node();

    fdt.end_node();

    fdt.begin_node("soc");
    fdt.property_u32("#address-cells", 2);
    fdt.property_u32("#size-cells", 2);
    fdt.property_str("compatible", "simple-bus");
    fdt.property_raw("ranges", &[]);

    fdt.begin_node("uart@10000000");
    fdt.property_str("compatible", "ns16550a");
    fdt.property_u64_arr("reg", &[uart_base as u64, 0x1000]);
    fdt.property_u32("clock-frequency", 1843200);
    fdt.property_u32("interrupt-parent", 1);
    fdt.property_u32_arr("interrupts", &[10]);
    fdt.end_node();

    fdt.begin_node("plic@c000000");
    fdt.property_u32("phandle", 1);
    fdt.property_str("compatible", "riscv,plic0");
    fdt.property_u64_arr("reg", &[plic_base as u64, 0x4000000]);
    fdt.property_u32("interrupt-controller", 0);
    fdt.property_u32("#interrupt-cells", 1);
    fdt.property_u32("riscv,ndev", 31);
    fdt.end_node();

    fdt.begin_node("virtio_gpu@20000000");
    fdt.property_str("compatible", "virtio,mmio");
    fdt.property_u64_arr("reg", &[virtio_gpu_base as u64, 0x1000]);
    fdt.property_u32("interrupt-parent", 1);
    fdt.property_u32_arr("interrupts", &[16]);
    fdt.end_node();

    fdt.end_node();

    fdt.end_node();

    fdt.finish()
}