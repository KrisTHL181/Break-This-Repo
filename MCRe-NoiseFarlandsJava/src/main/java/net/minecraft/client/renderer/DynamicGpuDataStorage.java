package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * MCRe NoiseFarlands：GPU 动态数据存储（26.3 MultiDrawIndirect 移植）。
 * 与 DynamicUniformStorage 类似，但支持非 UBO 用途（indirect 命令 buffer / instanced vertex 数据）的精确 blockSize：
 * UBO（usage 128）按 minUniformOffsetAlignment round；其他用途（indirect/vertex）精确 20/16 字节。
 */
@OnlyIn(Dist.CLIENT)
public class DynamicGpuDataStorage<T extends DynamicGpuDataStorage.DynamicGpuData> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
    private final int usage;
    private final int blockSize;
    private MappableRingBuffer ringBuffer;
    private int nextBlock;
    private int capacity;
    private @Nullable T lastData;
    private final String label;

    public DynamicGpuDataStorage(final String label, final int dataSize, final @GpuBuffer.Usage int usage, final int initialCapacity) {
        GpuDevice device = RenderSystem.getDevice();
        this.usage = usage;
        this.blockSize = usage == 128 ? Mth.roundToward(dataSize, device.getDeviceInfo().limits().minUniformOffsetAlignment()) : dataSize;
        this.capacity = Mth.smallestEncompassingPowerOfTwo(initialCapacity);
        this.nextBlock = 0;
        this.ringBuffer = new MappableRingBuffer(() -> label + " x" + this.blockSize, this.usage | 2, this.blockSize * this.capacity);
        this.label = label;
    }

    public void endFrame() {
        this.nextBlock = 0;
        this.lastData = null;
        this.ringBuffer.rotate();
        if (!this.oldBuffers.isEmpty()) {
            for (MappableRingBuffer oldBuffer : this.oldBuffers) {
                oldBuffer.close();
            }

            this.oldBuffers.clear();
        }
    }

    private void resizeBuffers(final int newCapacity) {
        this.capacity = newCapacity;
        this.nextBlock = 0;
        this.lastData = null;
        this.oldBuffers.add(this.ringBuffer);
        this.ringBuffer = new MappableRingBuffer(() -> this.label + " x" + this.blockSize, this.usage | 2, this.blockSize * this.capacity);
    }

    public GpuBufferSlice writeData(final T data) {
        if (this.lastData != null && this.lastData.equals(data)) {
            return this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
        }

        if (this.nextBlock >= this.capacity) {
            int newCapacity = this.capacity * 2;
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int offset = this.nextBlock * this.blockSize;

        try (GpuBufferSlice.MappedView view = this.ringBuffer.currentBuffer().slice(offset, this.blockSize).map(false, true)) {
            data.write(view.data());
        }

        this.nextBlock++;
        this.lastData = data;
        return this.ringBuffer.currentBuffer().slice(offset, this.blockSize);
    }

    public GpuBufferSlice[] writeData(final T[] dataArray) {
        if (dataArray.length == 0) {
            return new GpuBufferSlice[0];
        }

        if (this.nextBlock + dataArray.length > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, dataArray.length));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int firstOffset = this.nextBlock * this.blockSize;
        GpuBufferSlice[] result = new GpuBufferSlice[dataArray.length];

        try (GpuBufferSlice.MappedView view = this.ringBuffer.currentBuffer().slice(firstOffset, dataArray.length * this.blockSize).map(false, true)) {
            ByteBuffer byteBuffer = view.data();

            for (int i = 0; i < dataArray.length; i++) {
                T data = dataArray[i];
                result[i] = this.ringBuffer.currentBuffer().slice(firstOffset + i * this.blockSize, this.blockSize);
                byteBuffer.position(i * this.blockSize);
                data.write(byteBuffer);
            }
        }

        this.nextBlock += dataArray.length;
        this.lastData = dataArray[dataArray.length - 1];
        return result;
    }

    public GpuBufferSlice writeDataBatched(final List<T> dataList) {
        if (dataList.isEmpty()) {
            return new GpuBufferSlice(null, 0L, 0L);
        }

        if (this.nextBlock + dataList.size() > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, dataList.size()));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int firstOffset = this.nextBlock * this.blockSize;
        GpuBufferSlice result = this.ringBuffer.currentBuffer().slice(firstOffset, dataList.size() * this.blockSize);

        try (GpuBufferSlice.MappedView view = result.map(false, true)) {
            ByteBuffer byteBuffer = view.data();

            for (int i = 0; i < dataList.size(); i++) {
                T data = dataList.get(i);
                byteBuffer.position(i * this.blockSize);
                data.write(byteBuffer);
            }
        }

        this.nextBlock += dataList.size();
        this.lastData = dataList.get(dataList.size() - 1);
        return result;
    }

    public GpuBufferSlice[] writeDataBatchedMultiple(final List<List<T>> dataLists) {
        if (dataLists.isEmpty()) {
            return new GpuBufferSlice[0];
        }

        int totalCount = 0;

        for (List<T> data : dataLists) {
            totalCount += data.size();
        }

        if (this.nextBlock + totalCount > this.capacity) {
            int newCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, totalCount));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, newCapacity);
            this.resizeBuffers(newCapacity);
        }

        int offset = this.nextBlock * this.blockSize;
        GpuBufferSlice[] result = new GpuBufferSlice[dataLists.size()];
        int bufferPositionIndex = 0;

        try (GpuBufferSlice.MappedView view = this.ringBuffer.currentBuffer().slice(offset, totalCount * this.blockSize).map(false, true)) {
            ByteBuffer byteBuffer = view.data();
            long currentOffset = offset;

            for (int i = 0; i < dataLists.size(); i++) {
                List<T> dataList = dataLists.get(i);
                // MCRe 修复：每组 slice 偏移必须累加（原版只用了起始 offset，第 2 组起指向错误命令区）
                result[i] = this.ringBuffer.currentBuffer().slice(currentOffset, dataList.size() * this.blockSize);
                currentOffset += dataList.size() * this.blockSize;

                for (T data : dataList) {
                    byteBuffer.position(bufferPositionIndex++ * this.blockSize);
                    data.write(byteBuffer);
                }
            }
        }

        this.nextBlock += totalCount;
        this.lastData = null;
        return result;
    }

    @Override
    public void close() {
        for (MappableRingBuffer oldBuffer : this.oldBuffers) {
            oldBuffer.close();
        }

        this.ringBuffer.close();
    }

    @OnlyIn(Dist.CLIENT)
    public interface DynamicGpuData {
        void write(ByteBuffer byteBuffer);
    }
}
