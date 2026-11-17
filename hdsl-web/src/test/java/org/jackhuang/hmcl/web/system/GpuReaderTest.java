/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpuReaderTest {

    @Test
    void parsesNvidiaCsv() {
        var gpus = GpuReaders.NvidiaGpuReader.parse(List.of(
                "0, NVIDIA GeForce RTX 4090, 37, 82, 4, 1228, 24564, 54, 28.5"));
        assertEquals(1, gpus.size());
        var gpu = gpus.get(0);
        assertEquals(0, gpu.index());
        assertEquals("NVIDIA GeForce RTX 4090", gpu.name());
        assertEquals("NVIDIA", gpu.vendor());
        assertEquals(0.37, gpu.utilization(), 0.0001);
        assertEquals(0.82, gpu.encoderUtilization(), 0.0001);
        assertEquals(0.04, gpu.decoderUtilization(), 0.0001);
        assertEquals(1228L * 1024 * 1024, gpu.memoryUsedBytes());
        assertEquals(24564L * 1024 * 1024, gpu.memoryTotalBytes());
        assertEquals(54, gpu.temperatureC(), 0.001);
        assertEquals(28.5, gpu.powerW(), 0.001);
    }

    @Test
    void unavailableFieldsBecomeMinusOne() {
        var gpus = GpuReaders.NvidiaGpuReader.parseShort(List.of(
                "0, GPU, [N/A], [N/A], [N/A], [N/A], [N/A]"));
        assertEquals(1, gpus.size());
        assertEquals(-1, gpus.get(0).utilization(), 0.0001);
        assertEquals(-1, gpus.get(0).memoryUsedBytes());
    }
    @Test
    void readsAmdSysfsAndFallsBackToUnknownCards(@TempDir Path dir) throws Exception {
        Path card0 = dir.resolve("card0");
        Path device0 = Files.createDirectories(card0.resolve("device"));
        Files.writeString(device0.resolve("vendor"), "0x1002\n");
        Files.writeString(device0.resolve("gpu_busy_percent"), "50\n");
        Files.writeString(device0.resolve("mem_info_vram_used"), "1024\n");
        Files.writeString(device0.resolve("mem_info_vram_total"), "2048\n");

        Path card1 = dir.resolve("card1");
        Path device1 = Files.createDirectories(card1.resolve("device"));
        // vendor unreadable: still show a card so /dev/dri being mounted is not
        // reported as "no GPU at all".

        var reader = new GpuReaders.LinuxGpuReader(dir);
        var gpus = reader.read();
        assertEquals(2, gpus.size());
        assertEquals("AMD", gpus.get(0).vendor());
        assertEquals(0.5, gpus.get(0).utilization(), 0.0001);
        assertEquals("unknown", gpus.get(1).vendor());
    }

}
