package com.mojang.blaze3d.buffers;

import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length) {
    public GpuBufferSlice slice(final long offset, final long length) {
        if (offset >= 0L && length >= 0L && offset + length <= this.length) {
            return new GpuBufferSlice(this.buffer, this.offset + offset, length);
        } else {
            throw new IllegalArgumentException(
                "Offset of "
                    + offset
                    + " and length "
                    + length
                    + " would put new slice outside existing slice's range (of "
                    + this.offset
                    + ","
                    + this.length
                    + ")"
            );
        }
    }

    public GpuBufferSlice.MappedView map(final boolean read, final boolean write) {
        return this.buffer.map(this.offset, this.length, read, write);
    }

    @OnlyIn(Dist.CLIENT)
    public record MappedView(GpuBufferSlice slice, ByteBuffer data, Runnable onClose) implements AutoCloseable {
        @Override
        public void close() {
            this.onClose.run();
        }
    }
}