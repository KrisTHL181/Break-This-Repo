package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientChunkCache extends ChunkSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LevelChunk emptyChunk;
    private final LevelLightEngine lightEngine;
    private volatile ClientChunkCache.Storage storage;
    private final ClientLevel level;

    public ClientChunkCache(final ClientLevel level, final int serverChunkRadius) {
        this.level = level;
        this.emptyChunk = new EmptyLevelChunk(level, new ChunkPos(0, 0), level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
        this.lightEngine = new LevelLightEngine(this, true, level.dimensionType().hasSkyLight());
        this.storage = new ClientChunkCache.Storage(calculateStorageRange(serverChunkRadius));
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    private static boolean isValidChunk(final @Nullable LevelChunk chunk, final int x, final int z) {
        if (chunk == null) {
            return false;
        }

        ChunkPos pos = chunk.getPos();
        return pos.x() == x && pos.z() == z;
    }

    public void drop(final ChunkPos pos) {
        if (this.storage.inRange((int)pos.x(), (int)pos.z())) {
            int index = this.storage.getIndex((int)pos.x(), (int)pos.z());
            LevelChunk currentChunk = this.storage.getChunk(index);
            if (isValidChunk(currentChunk, (int)pos.x(), (int)pos.z())) {
                this.storage.drop(index, currentChunk);
            }
        }
    }

    public @Nullable LevelChunk getChunk(final int x, final int z, final ChunkStatus targetStatus, final boolean loadOrGenerate) {
        if (this.storage.inRange(x, z)) {
            LevelChunk chunk = this.storage.getChunk(this.storage.getIndex(x, z));
            if (isValidChunk(chunk, x, z)) {
                return chunk;
            }
        }

        return loadOrGenerate ? this.emptyChunk : null;
    }

    @Override
    public BlockGetter getLevel() {
        return this.level;
    }

    public void replaceBiomes(final int chunkX, final int chunkZ, final FriendlyByteBuf readBuffer) {
        if (!this.storage.inRange(chunkX, chunkZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", chunkX, chunkZ);
        } else {
            int index = this.storage.getIndex(chunkX, chunkZ);
            LevelChunk chunk = this.storage.chunks.get(index);
            if (!isValidChunk(chunk, chunkX, chunkZ)) {
                LOGGER.warn("Ignoring chunk since it's not present: {}, {}", chunkX, chunkZ);
            } else {
                chunk.replaceBiomes(readBuffer);
            }
        }
    }

    public @Nullable LevelChunk replaceWithPacketData(
        final int chunkX,
        final int chunkZ,
        final FriendlyByteBuf readBuffer,
        final Map<Heightmap.Types, long[]> heightmaps,
        final Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities
    ) {
        if (!this.storage.inRange(chunkX, chunkZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", chunkX, chunkZ);
            return null;
        }

        int index = this.storage.getIndex(chunkX, chunkZ);
        LevelChunk chunk = this.storage.chunks.get(index);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        if (!isValidChunk(chunk, chunkX, chunkZ)) {
            chunk = new LevelChunk(this.level, pos);
            chunk.replaceWithPacketData(readBuffer, heightmaps, blockEntities);
            this.storage.replace(index, chunk);
        } else {
            chunk.replaceWithPacketData(readBuffer, heightmaps, blockEntities);
            this.storage.refreshEmptySections(chunk);
        }

        this.level.onChunkLoaded(pos);
        return chunk;
    }

    @Override
    public void tick(final BooleanSupplier haveTime, final boolean tickChunks) {
    }

    public void updateViewCenter(final int x, final int z) {
        this.storage.viewCenterX = x;
        this.storage.viewCenterZ = z;
    }

    public void updateViewRadius(final int viewRange) {
        int chunkRadius = this.storage.chunkRadius;
        int newChunkRadius = calculateStorageRange(viewRange);
        if (chunkRadius != newChunkRadius) {
            ClientChunkCache.Storage newStorage = new ClientChunkCache.Storage(newChunkRadius);
            newStorage.viewCenterX = this.storage.viewCenterX;
            newStorage.viewCenterZ = this.storage.viewCenterZ;

            for (int i = 0; i < this.storage.chunks.length(); i++) {
                LevelChunk chunk = this.storage.chunks.get(i);
                if (chunk != null) {
                    ChunkPos pos = chunk.getPos();
                    if (newStorage.inRange((int)pos.x(), (int)pos.z())) {
                        newStorage.replace(newStorage.getIndex((int)pos.x(), (int)pos.z()), chunk);
                    }
                }
            }

            this.storage = newStorage;
        }
    }

    private static int calculateStorageRange(final int viewRange) {
        return Math.max(2, viewRange) + 3;
    }

    @Override
    public String gatherStats() {
        return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.storage.chunkCount;
    }

    @Override
    public void onLightUpdate(final LightLayer layer, final SectionPos pos) {
        Minecraft.getInstance().levelExtractor.setSectionDirty(pos.x(), pos.y(), pos.z());
    }

    public Set<SectionPos> addedEmptySections() {
        return this.storage.addedEmptySections[this.storage.updatingSetsIndex];
    }

    public Set<SectionPos> removedEmptySections() {
        return this.storage.removedEmptySections[this.storage.updatingSetsIndex];
    }

    public Set<ChunkPos> addedLoadedChunks() {
        return this.storage.addedLoadedChunks[this.storage.updatingSetsIndex];
    }

    public Set<ChunkPos> removedLoadedChunks() {
        return this.storage.removedLoadedChunks[this.storage.updatingSetsIndex];
    }

    public void flipUpdateTrackingSets() {
        this.storage.updatingSetsIndex = (this.storage.updatingSetsIndex + 1) % 2;
        this.storage.addedEmptySections[this.storage.updatingSetsIndex].clear();
        this.storage.removedEmptySections[this.storage.updatingSetsIndex].clear();
        this.storage.addedLoadedChunks[this.storage.updatingSetsIndex].clear();
        this.storage.removedLoadedChunks[this.storage.updatingSetsIndex].clear();
    }

    @Override
    public void onSectionEmptinessChanged(final int sectionX, final int sectionY, final int sectionZ, final boolean empty) {
        this.storage.onSectionEmptinessChanged(sectionX, sectionY, sectionZ, empty);
    }

    @OnlyIn(Dist.CLIENT)
    private final class Storage {
        private static final int UPDATE_TRACKING_BUFFERS = 2;
        private final AtomicReferenceArray<@Nullable LevelChunk> chunks;
        private final Set<SectionPos>[] addedEmptySections = new Set[2];
        private final Set<SectionPos>[] removedEmptySections = new Set[2];
        private final Set<ChunkPos>[] addedLoadedChunks = new Set[2];
        private final Set<ChunkPos>[] removedLoadedChunks = new Set[2];
        private int updatingSetsIndex;
        private final int chunkRadius;
        private final int viewRange;
        private volatile int viewCenterX;
        private volatile int viewCenterZ;
        private int chunkCount;

        private Storage(final int chunkRadius) {
            this.chunkRadius = chunkRadius;
            this.viewRange = chunkRadius * 2 + 1;
            this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);

            for (int i = 0; i < 2; i++) {
                this.addedEmptySections[i] = new HashSet<>();
                this.removedEmptySections[i] = new HashSet<>();
                this.addedLoadedChunks[i] = new HashSet<>();
                this.removedLoadedChunks[i] = new HashSet<>();
            }
        }

        private int getIndex(final int chunkX, final int chunkZ) {
            return Math.floorMod(chunkZ, this.viewRange) * this.viewRange + Math.floorMod(chunkX, this.viewRange);
        }

        private void replace(final int index, final @Nullable LevelChunk newChunk) {
            LevelChunk removedChunk = this.chunks.getAndSet(index, newChunk);
            if (removedChunk != null) {
                this.chunkCount--;
                this.onChunkRemoved(removedChunk);
                ClientChunkCache.this.level.unload(removedChunk);
            }

            if (newChunk != null) {
                this.chunkCount++;
                this.onChunkAdded(newChunk);
            }
        }

        private void drop(final int index, final LevelChunk oldChunk) {
            if (this.chunks.compareAndSet(index, oldChunk, null)) {
                this.chunkCount--;
                this.onChunkRemoved(oldChunk);
            }

            ClientChunkCache.this.level.unload(oldChunk);
        }

        public void onSectionEmptinessChanged(final int sectionX, final int sectionY, final int sectionZ, final boolean empty) {
            if (this.inRange(sectionX, sectionZ)) {
                SectionPos sectionNode = SectionPos.of(sectionX, sectionY, sectionZ);
                if (empty) {
                    this.addedEmptySections[this.updatingSetsIndex].add(sectionNode);
                } else {
                    this.removedEmptySections[this.updatingSetsIndex].add(sectionNode);
                }
            }
        }

        private void onChunkRemoved(final LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            this.removedLoadedChunks[this.updatingSetsIndex].add(chunkPos);
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                this.removedEmptySections[this.updatingSetsIndex]
                    .add(SectionPos.of((int)chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), (int)chunkPos.z()));
            }
        }

        private void onChunkAdded(final LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            this.addedLoadedChunks[this.updatingSetsIndex].add(chunkPos);
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                LevelChunkSection section = sections[sectionIndex];
                if (section.hasOnlyAir()) {
                    this.addedEmptySections[this.updatingSetsIndex]
                        .add(SectionPos.of((int)chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), (int)chunkPos.z()));
                }
            }
        }

        private void refreshEmptySections(final LevelChunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                LevelChunkSection section = sections[sectionIndex];
                SectionPos sectionNode = SectionPos.of((int)chunkPos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), (int)chunkPos.z());
                if (section.hasOnlyAir()) {
                    this.addedEmptySections[this.updatingSetsIndex].add(sectionNode);
                } else {
                    this.removedEmptySections[this.updatingSetsIndex].add(sectionNode);
                }
            }
        }

        private boolean inRange(final int chunkX, final int chunkZ) {
            return Math.abs(chunkX - this.viewCenterX) <= this.chunkRadius && Math.abs(chunkZ - this.viewCenterZ) <= this.chunkRadius;
        }

        public @Nullable LevelChunk getChunk(final int index) {
            return this.chunks.get(index);
        }

        private void dumpChunks(final String file) {
            try (FileOutputStream stream = new FileOutputStream(file)) {
                int chunkRadius = ClientChunkCache.this.storage.chunkRadius;

                for (int z = this.viewCenterZ - chunkRadius; z <= this.viewCenterZ + chunkRadius; z++) {
                    for (int x = this.viewCenterX - chunkRadius; x <= this.viewCenterX + chunkRadius; x++) {
                        LevelChunk chunk = ClientChunkCache.this.storage.chunks.get(ClientChunkCache.this.storage.getIndex(x, z));
                        if (chunk != null) {
                            ChunkPos pos = chunk.getPos();
                            stream.write((pos.x() + "\t" + pos.z() + "\t" + chunk.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (IOException e) {
                ClientChunkCache.LOGGER.error("Failed to dump chunks to file {}", file, e);
            }
        }
    }
}