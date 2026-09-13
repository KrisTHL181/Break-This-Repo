package net.minecraft.world.level.chunk.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class RegionFileStorage implements AutoCloseable {
    public static final String ANVIL_EXTENSION = ".mca";
    // MCRe：far lands 跨大量 region，缓存加大减少频繁开关 region 文件
    // 注意：缓存过大会让 close() 时逐个 fsync 太多 region（Android 存储上极易触发退出看门狗 15s 超时），
    // 512 与 256 的命中率收益有限，保持 256 与退出安全平衡
    private static final int MAX_CACHE_SIZE = 256;
    // MCRe：access-order（true）= 真 LRU——get 移动条目，remove 最久未用（原版 insertion-order 是 FIFO，命中率差）
    private final LinkedHashMap<ChunkPos, RegionFile> regionCache = new LinkedHashMap<>(16, 0.75F, true);
    private final RegionStorageInfo info;
    private final Path folder;
    private final boolean sync;

    public RegionFileStorage(final RegionStorageInfo info, final Path folder, final boolean sync) {
        this.folder = folder;
        this.sync = sync;
        this.info = info;
    }

    private RegionFile getRegionFile(final ChunkPos pos) throws IOException {
        ChunkPos key = new ChunkPos(pos.getRegionX(), pos.getRegionZ());
        RegionFile region = this.regionCache.get(key);
        if (region != null) {
            return region;
        }

        if (this.regionCache.size() >= MAX_CACHE_SIZE) {
            this.regionCache.remove(this.regionCache.keySet().iterator().next()).close();
        }

        FileUtil.createDirectoriesSafe(this.folder);
        Path file = this.folder.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
        RegionFile newRegion = new RegionFile(this.info, file, this.folder, this.sync);
        this.regionCache.put(key, newRegion);
        return newRegion;
    }

    public @Nullable CompoundTag read(final ChunkPos pos) throws IOException {
        RegionFile region = this.getRegionFile(pos);

        try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos)) {
            return regionChunkInputStream == null ? null : NbtIo.read(regionChunkInputStream);
        }
    }

    public void scanChunk(final ChunkPos pos, final StreamTagVisitor scanner) throws IOException {
        RegionFile region = this.getRegionFile(pos);

        try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos)) {
            if (regionChunkInputStream != null) {
                NbtIo.parse(regionChunkInputStream, scanner, NbtAccounter.unlimitedHeap());
            }
        }
    }

    public void write(final ChunkPos pos, final @Nullable CompoundTag value) throws IOException {
        if (!SharedConstants.DEBUG_DONT_SAVE_WORLD) {
            RegionFile region = this.getRegionFile(pos);
            if (value == null) {
                region.clear(pos);
            } else {
                try (DataOutputStream output = region.getChunkDataOutputStream(pos)) {
                    NbtIo.write(value, output);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        ExceptionCollector<IOException> exception = new ExceptionCollector<>();

        for (RegionFile regionFile : this.regionCache.values()) {
            try {
                regionFile.close();
            } catch (IOException e) {
                exception.add(e);
            }
        }

        exception.throwIfPresent();
    }

    public void flush() throws IOException {
        for (RegionFile regionFile : this.regionCache.values()) {
            regionFile.flush();
        }
    }

    public RegionStorageInfo info() {
        return this.info;
    }
}