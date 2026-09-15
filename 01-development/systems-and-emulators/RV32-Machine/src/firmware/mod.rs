pub fn build_firmware() -> (Vec<u8>, usize, usize) {
    let mut fw = Vec::new();

    let sp: u8 = 2;
    let ra: u8 = 1;
    let a0: u8 = 10;
    let a1: u8 = 11;
    let t0: u8 = 5;
    let t1: u8 = 6;
    let t2: u8 = 7;
    let t3: u8 = 28;
    let t4: u8 = 29;
    let t5: u8 = 30;
    let zero: u8 = 0;

    let uart_base: u32 = 0x1000_0000;
    let uart_lsr: u32 = 5;
    let uart_thr: u32 = 0;
    let uart_rbr: u32 = 0;

    let vga_base: u32 = 0x2000_0000;
    let vga_fb: u32 = 0xA0000;

    let ram_start: u32 = 0x8000_0000;

    macro_rules! emit32 {
        ($fw:expr, $w:expr) => { $fw.extend_from_slice(&($w as u32).to_le_bytes()); };
    }

    fn lui(fw: &mut Vec<u8>, rd: u8, imm20: u32) {
        emit32!(fw, 0x37 | ((rd as u32) << 7) | ((imm20 & 0xFFFFF) << 12));
    }

    fn addi(fw: &mut Vec<u8>, rd: u8, rs1: u8, imm12: i32) {
        emit32!(fw, 0x13 | ((rd as u32) << 7) | (0 << 12) | ((rs1 as u32) << 15) | (((imm12 as u32) & 0xFFF) << 20));
    }

    fn andi(fw: &mut Vec<u8>, rd: u8, rs1: u8, imm12: i32) {
        emit32!(fw, 0x13 | ((rd as u32) << 7) | (7 << 12) | ((rs1 as u32) << 15) | (((imm12 as u32) & 0xFFF) << 20));
    }

    fn add(fw: &mut Vec<u8>, rd: u8, rs1: u8, rs2: u8) {
        emit32!(fw, 0x33 | ((rd as u32) << 7) | (0 << 12) | ((rs1 as u32) << 15) | ((rs2 as u32) << 20) | (0 << 25));
    }

    fn lw(fw: &mut Vec<u8>, rd: u8, rs1: u8, imm12: i32) {
        emit32!(fw, 0x03 | ((rd as u32) << 7) | (2 << 12) | ((rs1 as u32) << 15) | (((imm12 as u32) & 0xFFF) << 20));
    }

    fn lb(fw: &mut Vec<u8>, rd: u8, rs1: u8, imm12: i32) {
        emit32!(fw, 0x03 | ((rd as u32) << 7) | (0 << 12) | ((rs1 as u32) << 15) | (((imm12 as u32) & 0xFFF) << 20));
    }

    fn sw(fw: &mut Vec<u8>, rs2: u8, rs1: u8, imm12: i32) {
        let imm = imm12 as u32;
        emit32!(fw,
            0x23
            | (2 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 5) & 0x7F) << 25)
            | ((imm & 0x1F) << 7)
        );
    }

    fn sb(fw: &mut Vec<u8>, rs2: u8, rs1: u8, imm12: i32) {
        let imm = imm12 as u32;
        emit32!(fw,
            0x23
            | (0 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 5) & 0x7F) << 25)
            | ((imm & 0x1F) << 7)
        );
    }

    fn beq(fw: &mut Vec<u8>, rs1: u8, rs2: u8, offset: i32) {
        let imm = offset as u32;
        emit32!(fw,
            0x63
            | (0 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 12) & 0x1) << 31)
            | (((imm >> 5) & 0x3F) << 25)
            | (((imm >> 1) & 0xF) << 8)
            | (((imm >> 11) & 0x1) << 7)
        );
    }

    fn bne(fw: &mut Vec<u8>, rs1: u8, rs2: u8, offset: i32) {
        let imm = offset as u32;
        emit32!(fw,
            0x63
            | (1 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 12) & 0x1) << 31)
            | (((imm >> 5) & 0x3F) << 25)
            | (((imm >> 1) & 0xF) << 8)
            | (((imm >> 11) & 0x1) << 7)
        );
    }

    fn jal(fw: &mut Vec<u8>, rd: u8, offset: i32) {
        let imm = offset as u32;
        emit32!(fw,
            0x6F
            | ((rd as u32) << 7)
            | (((imm >> 20) & 0x1) << 31)
            | (((imm >> 1) & 0x3FF) << 21)
            | (((imm >> 11) & 0x1) << 20)
            | (((imm >> 12) & 0xFF) << 12)
        );
    }

    fn jalr(fw: &mut Vec<u8>, rd: u8, rs1: u8, imm12: i32) {
        emit32!(fw, 0x67 | ((rd as u32) << 7) | (0 << 12) | ((rs1 as u32) << 15) | (((imm12 as u32) & 0xFFF) << 20));
    }

    fn li_upper(fw: &mut Vec<u8>, rd: u8, val: u32) {
        if val & 0xFFF == 0 {
            lui(fw, rd, val >> 12);
        } else {
            let upper = (val + 0x800) >> 12;
            lui(fw, rd, upper);
            let lower = (val as i32 & 0xFFF) - if (val & 0x800) != 0 { 0x1000 } else { 0 };
            if lower != 0 {
                addi(fw, rd, rd, lower);
            }
        }
    }

    fn patch_jal(fw: &mut [u8], pos: usize, target: usize) {
        let offset = target as i32 - pos as i32;
        let imm = offset as u32;
        let existing = u32::from_le_bytes([fw[pos], fw[pos+1], fw[pos+2], fw[pos+3]]);
        let rd = (existing >> 7) & 0x1F;
        let enc = 0x6F_u32
            | (rd << 7)
            | (((imm >> 20) & 0x1) << 31)
            | (((imm >> 1) & 0x3FF) << 21)
            | (((imm >> 11) & 0x1) << 20)
            | (((imm >> 12) & 0xFF) << 12);
        fw[pos..pos + 4].copy_from_slice(&enc.to_le_bytes());
    }

    fn patch_beq(fw: &mut [u8], pos: usize, rs1: u8, rs2: u8, target: usize) {
        let offset = target as i32 - pos as i32;
        let imm = offset as u32;
        let enc = 0x63_u32
            | (0 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 12) & 0x1) << 31)
            | (((imm >> 5) & 0x3F) << 25)
            | (((imm >> 1) & 0xF) << 8)
            | (((imm >> 11) & 0x1) << 7);
        fw[pos..pos + 4].copy_from_slice(&enc.to_le_bytes());
    }

    fn patch_bne(fw: &mut [u8], pos: usize, rs1: u8, rs2: u8, target: usize) {
        let offset = target as i32 - pos as i32;
        let imm = offset as u32;
        let enc = 0x63_u32
            | (1 << 12)
            | ((rs1 as u32) << 15)
            | ((rs2 as u32) << 20)
            | (((imm >> 12) & 0x1) << 31)
            | (((imm >> 5) & 0x3F) << 25)
            | (((imm >> 1) & 0xF) << 8)
            | (((imm >> 11) & 0x1) << 7);
        fw[pos..pos + 4].copy_from_slice(&enc.to_le_bytes());
    }

    li_upper(&mut fw, sp, ram_start + 0x0100_0000);

    let vga_clear_init_patch = fw.len();
    jal(&mut fw, ra, 0);
    let vga_clear_skip_patch = fw.len();
    jal(&mut fw, zero, 0);

    let vga_clear = fw.len();
    li_upper(&mut fw, t0, vga_base + vga_fb);
    li_upper(&mut fw, t1, 640 * 480);
    li_upper(&mut fw, t2, 0xFF000000);

    let vga_clear_loop = fw.len();
    sw(&mut fw, t2, t0, 0);
    addi(&mut fw, t0, t0, 4);
    addi(&mut fw, t1, t1, -1);
    let vga_clear_loop_end = fw.len();
    let vga_clear_bne_off = vga_clear_loop as i32 - vga_clear_loop_end as i32;
    bne(&mut fw, t1, zero, vga_clear_bne_off);
    jalr(&mut fw, zero, ra, 0);

    let after_vga_clear = fw.len();
    patch_jal(&mut fw, vga_clear_init_patch, vga_clear);
    patch_jal(&mut fw, vga_clear_skip_patch, after_vga_clear);

    // Draw banner and subtitle as filled rectangles on VGA framebuffer
    // Parameters (pixels)
    let title_row: u32 = 40;
    let title_col: u32 = 40;
    let title_w: u32 = 560; // pixels
    let title_h: u32 = 48; // pixels

    let sub_row: u32 = 120;
    let sub_col: u32 = 60;
    let sub_w: u32 = 520;
    let sub_h: u32 = 20;

    // Helper: fill rectangle by row-major writes. compute starting address
    // start_addr = vga_base + vga_fb + ((row * 640) + col) * 4
    let title_start_addr = (vga_base + vga_fb) as usize + ((title_row as usize * 640) + title_col as usize) * 4;
    let sub_start_addr = (vga_base + vga_fb) as usize + ((sub_row as usize * 640) + sub_col as usize) * 4;

    // Fill title rectangle with bright color
    li_upper(&mut fw, t0, title_start_addr as u32);
    li_upper(&mut fw, t1, 0x00FFFFFF); // white
    li_upper(&mut fw, t3, title_h as u32);
    let title_row_loop = fw.len();
    li_upper(&mut fw, t4, title_w as u32);
    let title_col_loop = fw.len();
    sw(&mut fw, t1, t0, 0);
    addi(&mut fw, t0, t0, 4);
    addi(&mut fw, t4, t4, -1);
    let title_col_loop_end = fw.len();
    let title_col_beq_off = title_col_loop as i32 - title_col_loop_end as i32;
    bne(&mut fw, t4, zero, title_col_beq_off);
    // end inner loop
    // advance to next row: add (640 - title_w) * 4
    li_upper(&mut fw, t5, ((640 - title_w) * 4) as u32);
    add(&mut fw, t0, t0, t5);
    // Decrement row counter and loop
    addi(&mut fw, t3, t3, -1);
    let title_row_loop_end = fw.len();
    let title_row_beq_off = title_row_loop as i32 - title_row_loop_end as i32;
    bne(&mut fw, t3, zero, title_row_beq_off);

    // Fill subtitle rectangle with gray
    li_upper(&mut fw, t0, sub_start_addr as u32);
    li_upper(&mut fw, t1, 0xFFAAAAAA); // light gray
    li_upper(&mut fw, t3, sub_h as u32);
    let sub_row_loop = fw.len();
    li_upper(&mut fw, t4, sub_w as u32);
    let sub_col_loop = fw.len();
    sw(&mut fw, t1, t0, 0);
    addi(&mut fw, t0, t0, 4);
    addi(&mut fw, t4, t4, -1);
    let sub_col_loop_end = fw.len();
    let sub_col_beq_off = sub_col_loop as i32 - sub_col_loop_end as i32;
    bne(&mut fw, t4, zero, sub_col_beq_off);
    // advance to next row
    li_upper(&mut fw, t5, ((640 - sub_w) * 4) as u32);
    add(&mut fw, t0, t0, t5);
    addi(&mut fw, t3, t3, -1);
    let sub_row_loop_end = fw.len();
    let sub_row_beq_off = sub_row_loop as i32 - sub_row_loop_end as i32;
    bne(&mut fw, t3, zero, sub_row_beq_off);

    // (Previously a per-pixel block renderer was here; removed because it made
    // the firmware image exceed BOOT_ROM_SIZE. The banner and subtitle are
    // already drawn as filled rectangles above.)

    let jump_to_main_patch = fw.len();
    jal(&mut fw, zero, 0);

    let uart_puts = fw.len();
    let uart_puts_loop = fw.len();
    lb(&mut fw, t0, a0, 0);
    let uart_puts_done_patch = fw.len();
    beq(&mut fw, t0, zero, 0);

    let _uart_putc = fw.len();
    li_upper(&mut fw, t1, uart_base + uart_lsr);
    let uart_putc_wait = fw.len();
    lb(&mut fw, t2, t1, 0);
    andi(&mut fw, t2, t2, 0x20);
    let uart_putc_wait_end = fw.len();
    let uart_putc_beq_off = uart_putc_wait as i32 - uart_putc_wait_end as i32;
    beq(&mut fw, t2, zero, uart_putc_beq_off);
    li_upper(&mut fw, t1, uart_base + uart_thr);
    sb(&mut fw, t0, t1, 0);

    addi(&mut fw, a0, a0, 1);
    let uart_puts_loop_end = fw.len();
    let uart_puts_jal_off = uart_puts_loop as i32 - uart_puts_loop_end as i32;
    jal(&mut fw, zero, uart_puts_jal_off);

    let uart_puts_done = fw.len();
    patch_beq(&mut fw, uart_puts_done_patch, t0, zero, uart_puts_done);
    jalr(&mut fw, zero, ra, 0);

    let uart_getc = fw.len();
    li_upper(&mut fw, t0, uart_base + uart_lsr);
    let uart_getc_wait = fw.len();
    lb(&mut fw, t1, t0, 0);
    andi(&mut fw, t1, t1, 0x01);
    let uart_getc_wait_end = fw.len();
    let uart_getc_beq_off = uart_getc_wait as i32 - uart_getc_wait_end as i32;
    beq(&mut fw, t1, zero, uart_getc_beq_off);
    li_upper(&mut fw, t0, uart_base + uart_rbr);
    lb(&mut fw, a0, t0, 0);
    jalr(&mut fw, zero, ra, 0);

    let strings_start = fw.len();
    let banner = b"RV32-Machine BIOS v1.0\r\n";
    let banner2 = b"Press DEL to enter Setup, ENTER to boot\r\n";
    let setup_title = b"=== BIOS SETUP ===\r\n";
    let setup_menu = b"1.Set Time  2.Boot Order  3.Exit\r\n";
    let booting = b"Booting...\r\n";

    fw.extend_from_slice(banner); fw.push(0);
    let banner_off = fw.len() - strings_start;

    fw.extend_from_slice(banner2); fw.push(0);
    let banner2_off = fw.len() - strings_start;

    fw.extend_from_slice(setup_title); fw.push(0);
    let _setup_title_off = fw.len() - strings_start;

    fw.extend_from_slice(setup_menu); fw.push(0);
    let _setup_menu_off = fw.len() - strings_start;

    fw.extend_from_slice(booting); fw.push(0);
    let booting_off = fw.len() - strings_start;

    let _main = fw.len();

    li_upper(&mut fw, a0, strings_start as u32 + banner_off as u32);
    let uart_puts_patch1 = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, uart_puts_patch1, uart_puts);

    li_upper(&mut fw, a0, strings_start as u32 + banner2_off as u32);
    let uart_puts_patch2 = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, uart_puts_patch2, uart_puts);

    let main_loop = fw.len();
    let uart_getc_patch = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, uart_getc_patch, uart_getc);

    addi(&mut fw, t0, a0, 0);
    li_upper(&mut fw, t1, 0x7F);
    let setup_patch = fw.len();
    beq(&mut fw, t0, t1, 0);

    li_upper(&mut fw, t1, 0x0D);
    let boot_patch = fw.len();
    beq(&mut fw, t0, t1, 0);

    let main_loop_end = fw.len();
    let main_loop_jal_off = main_loop as i32 - main_loop_end as i32;
    jal(&mut fw, zero, main_loop_jal_off);

    let setup_menu_start = fw.len();
    patch_beq(&mut fw, setup_patch, t0, t1, setup_menu_start);

    let vga_clear_patch3 = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, vga_clear_patch3, vga_clear);

    // Draw simple Setup panel on VGA (dark background + option bars)
    let panel_row: u32 = 80;
    let panel_col: u32 = 80;
    let panel_w: u32 = 480;
    let panel_h: u32 = 220;
    let panel_start_addr = (vga_base + vga_fb) as usize + ((panel_row as usize * 640) + panel_col as usize) * 4;

    // fill panel background (dark)
    li_upper(&mut fw, t0, panel_start_addr as u32);
    li_upper(&mut fw, t1, 0xFF002244); // dark bluish
    li_upper(&mut fw, t3, panel_h as u32);
    let panel_row_loop = fw.len();
    li_upper(&mut fw, t4, panel_w as u32);
    let panel_col_loop = fw.len();
    sw(&mut fw, t1, t0, 0);
    addi(&mut fw, t0, t0, 4);
    addi(&mut fw, t4, t4, -1);
    let panel_col_loop_end = fw.len();
    let panel_col_beq_off = panel_col_loop as i32 - panel_col_loop_end as i32;
    bne(&mut fw, t4, zero, panel_col_beq_off);
    // advance to next row
    li_upper(&mut fw, t5, ((640 - panel_w) * 4) as u32);
    add(&mut fw, t0, t0, t5);
    addi(&mut fw, t3, t3, -1);
    let panel_row_loop_end = fw.len();
    let panel_row_beq_off = panel_row_loop as i32 - panel_row_loop_end as i32;
    bne(&mut fw, t3, zero, panel_row_beq_off);

    // draw three option bars inside panel for choices 1..3
    let opt_x = panel_col + 20;
    let mut opt_y = panel_row + 30;
    let opt_w: u32 = panel_w - 40;
    let opt_h: u32 = 32;
    for _ in 0..3 {
        let opt_start = (vga_base + vga_fb) as usize + ((opt_y as usize * 640) + opt_x as usize) * 4;
        li_upper(&mut fw, t0, opt_start as u32);
        li_upper(&mut fw, t1, 0xFF66AAFF);
        li_upper(&mut fw, t3, opt_h as u32);
        let orow = fw.len();
        li_upper(&mut fw, t4, opt_w as u32);
        let ocol = fw.len();
        sw(&mut fw, t1, t0, 0);
        addi(&mut fw, t0, t0, 4);
        addi(&mut fw, t4, t4, -1);
        let ocolend = fw.len();
        let ocolbeq = ocol as i32 - ocolend as i32;
        bne(&mut fw, t4, zero, ocolbeq);
        li_upper(&mut fw, t5, ((640 - opt_w) * 4) as u32);
        add(&mut fw, t0, t0, t5);
        addi(&mut fw, t3, t3, -1);
        let orowend = fw.len();
        let orowbeq = orow as i32 - orowend as i32;
        bne(&mut fw, t3, zero, orowbeq);
        opt_y += opt_h + 10;
    }

    let setup_wait = fw.len();
    let uart_getc_patch2 = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, uart_getc_patch2, uart_getc);

    li_upper(&mut fw, t0, b'1' as u32);
    let set_time_patch = fw.len();
    beq(&mut fw, a0, t0, 0);

    li_upper(&mut fw, t0, b'2' as u32);
    let boot_order_patch = fw.len();
    beq(&mut fw, a0, t0, 0);

    li_upper(&mut fw, t0, b'3' as u32);
    let exit_patch = fw.len();
    beq(&mut fw, a0, t0, 0);

    let setup_wait_end = fw.len();
    let setup_wait_jal_off = setup_wait as i32 - setup_wait_end as i32;
    jal(&mut fw, zero, setup_wait_jal_off);

    let set_time = fw.len();
    patch_beq(&mut fw, set_time_patch, a0, t0, set_time);

    li_upper(&mut fw, a0, strings_start as u32 + booting_off as u32);
    let uart_puts_patch5 = fw.len();
    jal(&mut fw, ra, 0);
    patch_jal(&mut fw, uart_puts_patch5, uart_puts);

    let boot_ram = fw.len();
    patch_beq(&mut fw, boot_order_patch, a0, t0, boot_ram);
    patch_beq(&mut fw, exit_patch, a0, t0, boot_ram);
    patch_beq(&mut fw, boot_patch, t0, t1, boot_ram);

    // Boot sequence: try Linux kernel, then SATA, SCSI, USB, fallback to RAM
    const SATA_BASE_F: u32 = 0x6000_0000;
    const SCSI_BASE_F: u32 = 0x5000_0000;
    const USB_BASE_F: u32 = 0x4000_0000;
    const BOOT_CMD_OFFSET_F: u32 = 0x0000_0F00;
    const KERNEL_LOAD_ADDR_F: u32 = 0x8000_0000;
    const DTB_LOAD_ADDR_F: u32 = 0x8100_0000;

    // --- Try Linux kernel (check for RISCV magic at offset 0x20) ---
    li_upper(&mut fw, t2, KERNEL_LOAD_ADDR_F + 0x20);
    lw(&mut fw, t3, t2, 0);
    li_upper(&mut fw, t4, 0x43534952); // "RISC" in LE
    let not_linux_patch = fw.len();
    bne(&mut fw, t3, t4, 0);

    // Linux kernel found: a0=hartid, a1=DTB address
    li_upper(&mut fw, a0, 0);
    li_upper(&mut fw, a1, DTB_LOAD_ADDR_F);
    li_upper(&mut fw, t2, KERNEL_LOAD_ADDR_F);
    jalr(&mut fw, zero, t2, 0);

    // --- Try SATA port 0 ---
    let try_sata = fw.len();
    patch_bne(&mut fw, not_linux_patch, t3, t4, try_sata);

    li_upper(&mut fw, t1, (SATA_BASE_F + BOOT_CMD_OFFSET_F) as u32);
    li_upper(&mut fw, t0, 0); // port 0
    sw(&mut fw, t0, t1, 0);
    // check RAM[0]
    li_upper(&mut fw, t2, ram_start);
    lb(&mut fw, t3, t2, 0);
    let sata_check_patch = fw.len();
    beq(&mut fw, t3, zero, 0);
    // jump to RAM if non-zero
    jalr(&mut fw, zero, t2, 0);
    let try_scsi = fw.len();
    patch_beq(&mut fw, sata_check_patch, t3, zero, try_scsi);

    // --- Try SCSI ---
    li_upper(&mut fw, t1, (SCSI_BASE_F + BOOT_CMD_OFFSET_F) as u32);
    li_upper(&mut fw, t0, 0);
    sw(&mut fw, t0, t1, 0);
    li_upper(&mut fw, t2, ram_start);
    lb(&mut fw, t3, t2, 0);
    let scsi_check_patch = fw.len();
    beq(&mut fw, t3, zero, 0);
    jalr(&mut fw, zero, t2, 0);
    let try_usb = fw.len();
    patch_beq(&mut fw, scsi_check_patch, t3, zero, try_usb);

    // --- Try USB ---
    li_upper(&mut fw, t1, (USB_BASE_F + BOOT_CMD_OFFSET_F) as u32);
    li_upper(&mut fw, t0, 0);
    sw(&mut fw, t0, t1, 0);
    li_upper(&mut fw, t2, ram_start);
    lb(&mut fw, t3, t2, 0);
    let usb_check_patch = fw.len();
    beq(&mut fw, t3, zero, 0);
    jalr(&mut fw, zero, t2, 0);
    let after_boot_try = fw.len();
    patch_beq(&mut fw, usb_check_patch, t3, zero, after_boot_try);

    // Fallback: just jump to RAM start
    li_upper(&mut fw, t0, ram_start);
    jalr(&mut fw, zero, t0, 0);

    patch_jal(&mut fw, jump_to_main_patch, _main);

    (fw, strings_start, _main)
}

#[cfg(test)]
mod tests {
    use super::build_firmware;

    fn branch_offset(insn: u32) -> i32 {
        let imm = (((insn >> 31) & 0x1) << 12)
            | (((insn >> 25) & 0x3F) << 5)
            | (((insn >> 8) & 0xF) << 1)
            | (((insn >> 7) & 0x1) << 11);
        let signed = (imm as i32) << 19 >> 19;
        signed
    }

    fn jal_offset(insn: u32) -> i32 {
        let imm = (((insn >> 31) & 0x1) << 20)
            | (((insn >> 21) & 0x3FF) << 1)
            | (((insn >> 20) & 0x1) << 11)
            | (((insn >> 12) & 0xFF) << 12);
        let signed = (imm as i32) << 11 >> 11;
        signed
    }

    #[test]
    fn firmware_branch_and_jump_offsets_stay_in_range() {
        let (fw, strings_start, strings_end) = build_firmware();

        eprintln!("firmware size: {} bytes ({} instructions)", fw.len(), fw.len() / 4);
        eprintln!("string data: {:#x}..{:#x}", strings_start, strings_end);

        for (pc_index, chunk) in fw.chunks_exact(4).enumerate() {
            let pc = (pc_index * 4) as i32;
            if pc as usize >= strings_start && (pc as usize) < strings_end {
                continue;
            }
            let insn = u32::from_le_bytes([chunk[0], chunk[1], chunk[2], chunk[3]]);
            match insn & 0x7F {
                0x63 => {
                    let offset = branch_offset(insn);
                    let target = pc + offset;
                    assert!(target >= 0 && (target as usize) < fw.len(),
                        "bad branch offset at pc {pc:#x}: offset {offset} -> target {target:#x}");
                }
                0x6F => {
                    let offset = jal_offset(insn);
                    let target = pc + offset;
                    assert!(target >= 0 && (target as usize) < fw.len(),
                        "bad jal offset at pc {pc:#x}: offset {offset} -> target {target:#x}");
                }
                _ => {}
            }
        }
    }
}