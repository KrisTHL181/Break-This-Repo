package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class IOWorker implements AutoCloseable, ChunkScanAccess {
    public static final Supplier<CompoundTag> STORE_EMPTY = () -> null;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final PriorityConsecutiveExecutor consecutiveExecutor;
    private final RegionFileStorage storage;
    private final SequencedMap<ChunkPos, IOWorker.PendingStore> pendingWrites = new LinkedHashMap<>();
    private final LinkedHashMap<ChunkPos, CompletableFuture<BitSet>> regionCacheForBlender = new LinkedHashMap<>();
    private static final int REGION_CACHE_SIZE = 1024;

    protected IOWorker(final RegionStorageInfo info, final Path dir, final boolean sync) {
        this.storage = new RegionFileStorage(info, dir, sync);
        this.consecutiveExecutor = new PriorityConsecutiveExecutor(IOWorker.Priority.values().length, Util.ioPool(), "IOWorker-" + info.type());
    }

    public boolean isOldChunkAround(final ChunkPos pos, final int range) {
        ChunkPos from = new ChunkPos(pos.x() - range, pos.z() - range);
        ChunkPos to = new ChunkPos(pos.x() + range, pos.z() + range);

        for (int regionX = (int)from.getRegionX(); regionX <= (int)to.getRegionX(); regionX++) {
            for (int regionZ = (int)from.getRegionZ(); regionZ <= (int)to.getRegionZ(); regionZ++) {
                BitSet data = this.getOrCreateOldDataForRegion(regionX, regionZ).join();
                if (!data.isEmpty()) {
                    ChunkPos minChunkPos = ChunkPos.minFromRegion(regionX, regionZ);
                    int startChunkX = (int)Math.max(from.x() - minChunkPos.x(), 0L);
                    int startChunkZ = (int)Math.max(from.z() - minChunkPos.z(), 0L);
                    int endChunkX = (int)Math.min(to.x() - minChunkPos.x(), 31L);
                    int endChunkZ = (int)Math.min(to.z() - minChunkPos.z(), 31L);

                    for (int x = startChunkX; x <= endChunkX; x++) {
                        for (int z = startChunkZ; z <= endChunkZ; z++) {
                            int chunkIndex = z * 32 + x;
                            if (data.get(chunkIndex)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private CompletableFuture<BitSet> getOrCreateOldDataForRegion(final int regionX, final int regionZ) {
        ChunkPos regionPos = new ChunkPos(regionX, regionZ);
        synchronized (this.regionCacheForBlender) {
            CompletableFuture<BitSet> result = this.regionCacheForBlender.get(regionPos);
            if (result == null) {
                result = this.createOldDataForRegion(regionX, regionZ);
                this.regionCacheForBlender.put(regionPos, result);
                if (this.regionCacheForBlender.size() > 1024) {
                    this.regionCacheForBlender.remove(this.regionCacheForBlender.keySet().iterator().next());
                }
            }

            return result;
        }
    }

    private CompletableFuture<BitSet> createOldDataForRegion(final int regionX, final int regionZ) {
        return CompletableFuture.supplyAsync(
            () -> {
                ChunkPos from = ChunkPos.minFromRegion(regionX, regionZ);
                ChunkPos to = ChunkPos.maxFromRegion(regionX, regionZ);
                BitSet resultSet = new BitSet();
                ChunkPos.rangeClosed(from, to)
                    .forEach(
                        pos -> {
                            CollectFields collectFields = new CollectFields(
                                new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data")
                            );

                            try {
                                this.scanChunk(pos, collectFields).join();
                            } catch (Exception e) {
                                LOGGER.warn("Failed to scan chunk {}", pos, e);
                                return;
                            }

                            if (collectFields.getResult() instanceof CompoundTag chunkTag && this.isOldChunk(chunkTag)) {
                                int chunkIndex = (int)pos.getRegionLocalZ() * 32 + (int)pos.getRegionLocalX();
                                resultSet.set(chunkIndex);
                            }
                        }
                    );
                return resultSet;
            },
            Util.backgroundExecutor()
        );
    }

    private boolean isOldChunk(final CompoundTag tag) {
        return tag.getIntOr("DataVersion", 0) < 4882 ? true : tag.getCompound("blending_data").isPresent();
    }

    public CompletableFuture<Void> store(final ChunkPos pos, final CompoundTag value) {
        return this.store(pos, () -> value);
    }

    public CompletableFuture<Void> store(final ChunkPos pos, final Supplier<CompoundTag> supplier) {
        return this.<CompletableFuture<Void>>submitTask(() -> {
            CompoundTag data = supplier.get();
            IOWorker.PendingStore pendingStore = this.pendingWrites.computeIfAbsent(pos, p -> new IOWorker.PendingStore(data));
            pendingStore.data = data;
            return pendingStore.result;
        }).thenCompose(Function.identity());
    }

    public CompletableFuture<Optional<CompoundTag>> loadAsync(final ChunkPos pos) {
        return this.submitThrowingTask(() -> {
            IOWorker.PendingStore pendingStore = this.pendingWrites.get(pos);
            if (pendingStore != null) {
                return Optional.ofNullable(pendingStore.copyData());
            }

            try {
                CompoundTag data = this.storage.read(pos);
                return Optional.ofNullable(data);
            } catch (Exception e) {
                LOGGER.warn("Failed to read chunk {}", pos, e);
                throw e;
            }
        });
    }

    public CompletableFuture<Void> synchronize(final boolean flush) {
        CompletableFuture<Void> currentWrites = this.<CompletableFuture<Void>>submitTask(
                () -> CompletableFuture.allOf(this.pendingWrites.values().stream().map(store -> store.result).toArray(CompletableFuture[]::new))
            )
            .thenCompose(Function.identity());
        return flush ? currentWrites.thenCompose(ignore -> this.submitThrowingTask(() -> {
            try {
                this.storage.flush();
                return null;
            } catch (Exception e) {
                LOGGER.warn("Failed to synchronize chunks", e);
                throw e;
            }
        })) : currentWrites.thenCompose(ignore -> this.submitTask(() -> null));
    }

    @Override
    public CompletableFuture<Void> scanChunk(final ChunkPos pos, final StreamTagVisitor visitor) {
        return this.submitThrowingTask(() -> {
            try {
                IOWorker.PendingStore pendingStore = this.pendingWrites.get(pos);
                if (pendingStore != null) {
                    if (pendingStore.data != null) {
                        pendingStore.data.acceptAsRoot(visitor);
                    }
                } else {
                    this.storage.scanChunk(pos, visitor);
                }

                return null;
            } catch (Exception e) {
                LOGGER.warn("Failed to bulk scan chunk {}", pos, e);
                throw e;
            }
        });
    }

    private <T> CompletableFuture<T> submitThrowingTask(final IOWorker.ThrowingSupplier<T> task) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), future -> {
            if (!this.shutdownRequested.get()) {
                try {
                    future.complete(task.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }

            this.tellStorePending();
        });
    }

    private <T> CompletableFuture<T> submitTask(final Supplier<T> task) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), future -> {
            if (!this.shutdownRequested.get()) {
                future.complete(task.get());
            }

            this.tellStorePending();
        });
    }

    private void storePendingChunk() {
        // MCRe：批量写盘（一次最多 8 个 pending），减少调度/队列操作开销
        // 原版每次调度只写 1 个区块，保存风暴（far lands 大量卸载）时调度开销大
        int batch = 0;
        Entry<ChunkPos, IOWorker.PendingStore> entry;
        while (batch < 8 && (entry = this.pendingWrites.pollFirstEntry()) != null) {
            this.runStore(entry.getKey(), entry.getValue());
            batch++;
        }
        // 🔧 修复：仅在仍有待写条目时继续调度。
        // 原实现无条件 tellStorePending() 会让 BACKGROUND(1) 队列无限自循环（永远非空），
        // 而 close() 的 waitForShutdown() 使用最低优先级 SHUTDOWN(2)，
        // FixedPriorityQueue.pop() 从高优先级开始轮询 → SHUTDOWN 永远轮不到 → join 永久挂起，
        // 表现为：服务端已保存世界但客户端卡死"保存世界"页面。
        if (!this.pendingWrites.isEmpty()) {
            this.tellStorePending();
        }
    }

    private void tellStorePending() {
        this.consecutiveExecutor.schedule(new StrictQueue.RunnableWithPriority(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
    }

    private void runStore(final ChunkPos pos, final IOWorker.PendingStore write) {
        try {
            this.storage.write(pos, write.data);
            write.result.complete(null);
        } catch (Exception e) {
            LOGGER.error("Failed to store chunk {}", pos, e);
            write.result.completeExceptionally(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.shutdownRequested.compareAndSet(false, true)) {
            this.waitForShutdown();
            this.consecutiveExecutor.close();

            try {
                this.storage.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close storage", e);
            }
        }
    }

    private void waitForShutdown() {
        this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.SHUTDOWN.ordinal(), future -> future.complete(Unit.INSTANCE)).join();
    }

    public RegionStorageInfo storageInfo() {
        return this.storage.info();
    }

    private static class PendingStore {
        private @Nullable CompoundTag data;
        private final CompletableFuture<Void> result = new CompletableFuture<>();

        public PendingStore(final @Nullable CompoundTag data) {
            this.data = data;
        }

        private @Nullable CompoundTag copyData() {
            CompoundTag data = this.data;
            return data == null ? null : data.copy();
        }
    }

    private enum Priority {
        FOREGROUND,
        BACKGROUND,
        SHUTDOWN;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        @Nullable T get() throws Exception;
    }
}