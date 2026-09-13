package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ProtoChunk extends ChunkAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile @Nullable LevelLightEngine lightEngine;
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final List<CompoundTag> entities = Lists.newArrayList();
    private @Nullable CarvingMask carvingMask;
    private @Nullable BelowZeroRetrogen belowZeroRetrogen;
    private final ProtoChunkTicks<Block> blockTicks;
    private final ProtoChunkTicks<Fluid> fluidTicks;

    public ProtoChunk(
        final ChunkPos chunkPos,
        final UpgradeData upgradeData,
        final LevelHeightAccessor levelHeightAccessor,
        final PalettedContainerFactory containerFactory,
        final @Nullable BlendingData blendingData
    ) {
        this(chunkPos, upgradeData, null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), levelHeightAccessor, containerFactory, blendingData);
    }

    public ProtoChunk(
        final ChunkPos chunkPos,
        final UpgradeData upgradeData,
        final LevelChunkSection @Nullable [] sections,
        final ProtoChunkTicks<Block> blockTicks,
        final ProtoChunkTicks<Fluid> fluidTicks,
        final LevelHeightAccessor levelHeightAccessor,
        final PalettedContainerFactory containerFactory,
        final @Nullable BlendingData blendingData
    ) {
        super(chunkPos, upgradeData, levelHeightAccessor, containerFactory, 0L, sections, blendingData);
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(final long currentTick) {
        return new ChunkAccess.PackedTicks(this.blockTicks.pack(currentTick), this.fluidTicks.pack(currentTick));
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        int y = pos.getY();
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        LevelChunkSection section = this.getSection(this.getSectionIndex(y));
        return section.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        int y = pos.getY();
        if (this.isOutsideBuildHeight(y)) {
            return Fluids.EMPTY.defaultFluidState();
        }

        LevelChunkSection section = this.getSection(this.getSectionIndex(y));
        return section.hasOnlyAir() ? Fluids.EMPTY.defaultFluidState() : section.getFluidState(pos.getX() & 15, y & 15, pos.getZ() & 15);
    }

    @Override
    public @Nullable BlockState setBlockState(final BlockPos pos, final BlockState state, final @Block.UpdateFlags int flags) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }

        int sectionIndex = this.getSectionIndex(y);
        LevelChunkSection section = this.getSection(sectionIndex);
        boolean wasEmpty = section.hasOnlyAir();
        if (wasEmpty && state.is(Blocks.AIR)) {
            return state;
        }

        int localX = SectionPos.sectionRelative(x);
        int localY = SectionPos.sectionRelative(y);
        int localZ = SectionPos.sectionRelative(z);
        BlockState oldState = section.setBlockState(localX, localY, localZ, state);
        if (this.status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
            boolean isEmpty = section.hasOnlyAir();
            if (isEmpty != wasEmpty) {
                this.lightEngine.updateSectionStatus(pos, isEmpty);
            }

            if (LightEngine.hasDifferentLightProperties(oldState, state)) {
                this.skyLightSources.update(this, localX, y, localZ);
                this.lightEngine.checkBlock(pos);
            }
        }

        EnumSet<Heightmap.Types> heightmapsAfter = this.getPersistedStatus().heightmapsAfter();
        EnumSet<Heightmap.Types> toPrime = null;

        for (Heightmap.Types type : heightmapsAfter) {
            Heightmap heightmap = this.heightmaps.get(type);
            if (heightmap == null) {
                if (toPrime == null) {
                    toPrime = EnumSet.noneOf(Heightmap.Types.class);
                }

                toPrime.add(type);
            }
        }

        if (toPrime != null) {
            Heightmap.primeHeightmaps(this, toPrime);
        }

        for (Heightmap.Types type : heightmapsAfter) {
            this.heightmaps.get(type).update(localX, y, localZ, state);
        }

        return oldState;
    }

    @Override
    public void setBlockEntity(final BlockEntity blockEntity) {
        this.pendingBlockEntities.remove(blockEntity.getBlockPos());
        this.blockEntities.put(blockEntity.getBlockPos(), blockEntity);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
        return this.blockEntities.get(pos);
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addEntity(final CompoundTag tag) {
        this.entities.add(tag);
    }

    @Override
    public void addEntity(final Entity entity) {
        if (!entity.isPassenger()) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                entity.save(output);
                this.addEntity(output.buildResult());
            }
        }
    }

    @Override
    public void setStartForStructure(final Structure structure, final StructureStart structureStart) {
        BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null && structureStart.isValid()) {
            BoundingBox boundingBox = structureStart.getBoundingBox();
            LevelHeightAccessor heightAccessor = this.getHeightAccessorForGeneration();
            if (boundingBox.minY() < heightAccessor.getMinY() || boundingBox.maxY() > heightAccessor.getMaxY()) {
                return;
            }
        }

        super.setStartForStructure(structure, structureStart);
    }

    public List<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.status;
    }

    public void setPersistedStatus(final ChunkStatus status) {
        this.status = status;
        if (this.belowZeroRetrogen != null && status.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
            this.setBelowZeroRetrogen(null);
        }

        this.markUnsaved();
    }

    @Override
    public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
        if (this.getHighestGeneratedStatus().isOrAfter(ChunkStatus.BIOMES)) {
            return super.getNoiseBiome(quartX, quartY, quartZ);
        } else {
            throw new IllegalStateException("Asking for biomes before we have biomes");
        }
    }

    public static short packOffsetCoordinates(final BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int dx = x & 15;
        int dy = y & 15;
        int dz = z & 15;
        return (short)(dx | dy << 4 | dz << 8);
    }

    public static BlockPos unpackOffsetCoordinates(final short packedData, final int sectionY, final ChunkPos chunkPos) {
        int posX = SectionPos.sectionToBlockCoord((int)chunkPos.x(), packedData & 15);
        int posY = SectionPos.sectionToBlockCoord(sectionY, packedData >>> 4 & 15);
        int posZ = SectionPos.sectionToBlockCoord((int)chunkPos.z(), packedData >>> 8 & 15);
        return new BlockPos(posX, posY, posZ);
    }

    @Override
    public void markPosForPostProcessing(final BlockPos blockPos) {
        if (this.isInsideBuildHeight(blockPos)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(blockPos.getY())).add(packOffsetCoordinates(blockPos));
        }
    }

    @Override
    public void addPackedPostProcess(final ShortList packedOffsets, final int sectionIndex) {
        ChunkAccess.getOrCreateOffsetList(this.postProcessing, sectionIndex).addAll(packedOffsets);
    }

    public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.pendingBlockEntities);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(final BlockPos blockPos, final HolderLookup.Provider registryAccess) {
        BlockEntity blockEntity = this.getBlockEntity(blockPos);
        return blockEntity != null ? blockEntity.saveWithFullMetadata(registryAccess) : this.pendingBlockEntities.get(blockPos);
    }

    @Override
    public void removeBlockEntity(final BlockPos pos) {
        this.blockEntities.remove(pos);
        this.pendingBlockEntities.remove(pos);
    }

    public @Nullable CarvingMask getCarvingMask() {
        return this.carvingMask;
    }

    public CarvingMask getOrCreateCarvingMask() {
        if (this.carvingMask == null) {
            // 🔧 MCRe P4b+：超高世界维度 height 可达 21.47 亿（so_high_overworld.json: height=2147000000），
            // 原版 `new BitSet(256 * height)` 在 int32 下溢出成 -123,813,888 → NegativeArraySizeException。
            // 改用窗口化高度域（与 applyCarvers 传的 CarvingContext 完全一致，都走 getHeightAccessorForGeneration），
            // 高度=544, minY=windowMinY*16 → 256*544=139264 完全在 int 范围。
            // carve 阶段是同步的，windowMinY 此时稳定，windowSections 已 buildDefaultWindow 初始化。
            LevelHeightAccessor carvingHa = this.getHeightAccessorForGeneration();
            this.carvingMask = new CarvingMask(carvingHa.getHeight(), carvingHa.getMinY());
        }

        return this.carvingMask;
    }

    public void setCarvingMask(final CarvingMask data) {
        this.carvingMask = data;
    }

    public void setLightEngine(final LevelLightEngine lightEngine) {
        this.lightEngine = lightEngine;
    }

    public void setBelowZeroRetrogen(final @Nullable BelowZeroRetrogen belowZeroRetrogen) {
        this.belowZeroRetrogen = belowZeroRetrogen;
    }

    @Override
    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.belowZeroRetrogen;
    }

    private static <T> LevelChunkTicks<T> unpackTicks(final ProtoChunkTicks<T> ticks) {
        return new LevelChunkTicks<>(ticks.scheduledTicks());
    }

    public LevelChunkTicks<Block> unpackBlockTicks() {
        return unpackTicks(this.blockTicks);
    }

    public LevelChunkTicks<Fluid> unpackFluidTicks() {
        return unpackTicks(this.fluidTicks);
    }

    @Override
    public LevelHeightAccessor getHeightAccessorForGeneration() {
        if (this.isUpgrading()) {
            return BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR;
        }
        // 🔧 MCRe P4b：走 ChunkAccess 的窗口化生成高度域（超高世界 OOM 修复）
        return super.getHeightAccessorForGeneration();
    }
}