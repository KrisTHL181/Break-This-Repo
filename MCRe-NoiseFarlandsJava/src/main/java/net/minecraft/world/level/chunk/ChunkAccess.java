package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ChunkAccess implements LightChunk, StructureAccess, BiomeManager.NoiseBiomeSource, WindowedChunk {
    public static final int NO_FILLED_SECTION = -1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ChunkPos> EMPTY_REFERENCE_SET = java.util.Collections.emptySet();
    protected final @Nullable ShortList[] postProcessing;
    private volatile boolean unsaved;
    private volatile boolean isLightCorrect;
    protected final ChunkPos chunkPos;
    private long inhabitedTime;
    @Deprecated
    private @Nullable BiomeGenerationSettings carverBiomeSettings;
    protected @Nullable NoiseChunk noiseChunk;
    protected final UpgradeData upgradeData;
    protected final @Nullable BlendingData blendingData;
    protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
    protected ChunkSkyLightSources skyLightSources;
    private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
    private final Map<Structure, Set<ChunkPos>> structuresRefences = Maps.newHashMap();
    protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
    protected final Map<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap<>();
    protected final LevelHeightAccessor levelHeightAccessor;
    protected final LevelChunkSection[] sections;

    // ──────── 🔧 MCRe：窗口化区块（P1，参考 inf_farlands） ────────
    /** 无限 Y 的 section 仓库（键 = 绝对 sectionY），超高世界超出窗口的 section 存放于此 */
    protected final java.util.concurrent.ConcurrentHashMap<Integer, LevelChunkSection> allSections = new java.util.concurrent.ConcurrentHashMap<>();
    /** 窗口视图数组（对外暴露的固定窗口，默认 WINDOW_SECTIONS=34 内） */
    private volatile LevelChunkSection[] windowSections = new LevelChunkSection[0];
    /** 窗口底部 sectionY */
    private int windowMinY;
    /** 🔧 MCRe P4b：窗口化生成高度域缓存（getHeightAccessorForGeneration 用） */
    private volatile LevelHeightAccessor windowedHeightAccessor;
    /** PalettedContainerFactory，用于懒创建缺失 section（等价 inf_farlands 的 biomeRegistry） */
    private final PalettedContainerFactory containerFactory;
    /** 🔧 MCRe P5：最近一次网络包携带的 sectionY 范围（discardOutsideHoldBoundary 用），MIN_VALUE=尚未收到 */
    private volatile int lastPacketMinY = Integer.MIN_VALUE;
    private volatile int lastPacketMaxY = Integer.MIN_VALUE;

    /** 🔧 MCRe：外部访问 containerFactory（LevelChunk 网络读入等用） */
    public PalettedContainerFactory getContainerFactory() {
        return this.containerFactory;
    }

    public ChunkAccess(
        final ChunkPos chunkPos,
        final UpgradeData upgradeData,
        final LevelHeightAccessor levelHeightAccessor,
        final PalettedContainerFactory containerFactory,
        final long inhabitedTime,
        final LevelChunkSection @Nullable [] sections,
        final @Nullable BlendingData blendingData
    ) {
        this.chunkPos = chunkPos;
        this.upgradeData = upgradeData;
        this.levelHeightAccessor = levelHeightAccessor;
        this.containerFactory = containerFactory;
        // 🔧 MCRe：窗口鉗制，防止超高世界（±21.47億）分配 1.34億 section 數組 OOM
        this.sections = new LevelChunkSection[levelHeightAccessor.getSectionsCount()];
        this.inhabitedTime = inhabitedTime;
        this.postProcessing = new ShortList[levelHeightAccessor.getSectionsCount()];
        this.blendingData = blendingData;
        this.skyLightSources = new ChunkSkyLightSources(levelHeightAccessor);
        // 🔧 MCRe：传入的 sections 全部转入 allSections 无限仓库（键 = 绝对 sectionY）
        if (sections != null) {
            if (this.sections.length == sections.length) {
                System.arraycopy(sections, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sections.length, this.sections.length);
            }
            for (int i = 0; i < sections.length; i++) {
                if (sections[i] != null) {
                    this.allSections.put(levelHeightAccessor.getSectionYFromSectionIndex(i), sections[i]);
                }
            }
        }

        replaceMissingSections(containerFactory, this.sections);
        this.buildDefaultWindow();
    }

    private static void replaceMissingSections(final PalettedContainerFactory containerFactory, final LevelChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                sections[i] = new LevelChunkSection(containerFactory);
            }
        }
    }

    public GameEventListenerRegistry getListenerRegistry(final int section) {
        return GameEventListenerRegistry.NOOP;
    }

    public @Nullable BlockState setBlockState(final BlockPos pos, final BlockState state) {
        return this.setBlockState(pos, state, 3);
    }

    public abstract @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags);

    public abstract void setBlockEntity(BlockEntity blockEntity);

    public abstract void addEntity(Entity entity);

    public int getHighestFilledSectionIndex() {
        LevelChunkSection[] sections = this.getSections();

        for (int sectionIndex = sections.length - 1; sectionIndex >= 0; sectionIndex--) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.hasOnlyAir()) {
                return sectionIndex;
            }
        }

        return -1;
    }

    @Deprecated(forRemoval = true)
    public int getHighestSectionPosition() {
        int sectionIndex = this.getHighestFilledSectionIndex();
        return sectionIndex == -1 ? this.getMinY() : SectionPos.sectionToBlockCoord(this.getSectionYFromSectionIndex(sectionIndex));
    }

    public Set<BlockPos> getBlockEntitiesPos() {
        Set<BlockPos> result = Sets.newHashSet(this.pendingBlockEntities.keySet());
        result.addAll(this.blockEntities.keySet());
        return result;
    }

    public LevelChunkSection[] getSections() {
        // 🔧 MCRe：返回窗口视图（与 sections 全量一致——超高世界 sections 已钳制到窗口大小）
        return this.sections;
    }

    public LevelChunkSection getSection(final int sectionIndex) {
        LevelChunkSection[] arr = this.getSections();
        // 🔧 MCRe：窗口索引 → 绝对 sectionY，优先从 allSections 取（懒创建）
        int sectionY = this.getSectionYFromSectionIndex(sectionIndex);
        LevelChunkSection s = this.allSections.get(sectionY);
        if (s == null) {
            s = new LevelChunkSection(this.containerFactory);
            this.allSections.put(sectionY, s);
            if (sectionIndex >= 0 && sectionIndex < arr.length) {
                arr[sectionIndex] = s;
            }
        }
        return s;
    }

    /** 🔧 MCRe：存入指定 sectionY 的 section（无限仓库 + 窗口同步） */
    public void setSectionAt(final int sectionY, final LevelChunkSection section) {
        this.allSections.put(sectionY, section);
        int idx = this.getSectionIndexFromSectionY(sectionY);
        if (idx >= 0 && idx < this.sections.length) {
            this.sections[idx] = section;
        }
    }

    /** 🔧 MCRe：按绝对 sectionY 取 section（仅返回已存在的，不懒创建） */
    public @Nullable LevelChunkSection getSectionAt(final int sectionY) {
        return this.allSections.get(sectionY);
    }

    /** 🔧 MCRe P5：最近一次网络包携带的 sectionY 最小值（MIN_VALUE=尚未收到任何包） */
    public int lastPacketMinY() {
        return this.lastPacketMinY;
    }

    /** 🔧 MCRe P5：最近一次网络包携带的 sectionY 最大值 */
    public int lastPacketMaxY() {
        return this.lastPacketMaxY;
    }

    /** 🔧 MCRe P5：网络包接收方调用，记录本次包覆盖的 sectionY 范围（discardOutsideHoldBoundary 用） */
    public void setLastPacketRange(final int minSectionY, final int maxSectionY) {
        if (minSectionY > maxSectionY) {
            return;
        }
        int oldMin = this.lastPacketMinY;
        int oldMax = this.lastPacketMaxY;
        // 取并集——分片到达/光照包可能比方块包范围更大
        int newMin = oldMin == Integer.MIN_VALUE ? minSectionY : Math.min(oldMin, minSectionY);
        int newMax = oldMax == Integer.MIN_VALUE ? maxSectionY : Math.max(oldMax, maxSectionY);
        this.lastPacketMinY = newMin;
        this.lastPacketMaxY = newMax;
    }

    /** 🔧 MCRe：当前窗口底部 sectionY */
    public int getWindowMinY() {
        return this.windowMinY;
    }

    /** 🔧 MCRe：当前窗口顶部 sectionY */
    public int getWindowMaxY() {
        return this.windowMinY + this.windowSections.length - 1;
    }

    /** 🔧 MCRe：窗口基准索引——sectionY → 窗口相对索引（超出窗口允许负/超大，由 getSection 经 allSections 兜底） */
    @Override
    public int getSectionIndexFromSectionY(final int sectionY) {
        return sectionY - this.windowMinY;
    }

    /** 🔧 MCRe：窗口基准逆映射——窗口相对索引 → sectionY */
    @Override
    public int getSectionYFromSectionIndex(final int sectionIndex) {
        return this.windowMinY + sectionIndex;
    }

    /** 🔧 MCRe：WindowedChunk —— 窗口相对索引 → 绝对 sectionY */
    @Override
    public int windowSectionYFromIndex(final int index) {
        return this.windowMinY + index;
    }

    /** 🔧 MCRe：WindowedChunk —— 绝对 sectionY → 窗口相对索引 */
    @Override
    public int windowSectionIndexFromY(final int sectionY) {
        return sectionY - this.windowMinY;
    }

    /** 🔧 MCRe：WindowedChunk —— 无限 Y 的 section 仓库 */
    @Override
    public Map<Integer, LevelChunkSection> windowedAllSections() {
        return this.allSections;
    }

    /** 🔧 MCRe：WindowedChunk —— 区块真实高度访问器（维度范围） */
    @Override
    public LevelHeightAccessor levelHeightAccessor() {
        return this.levelHeightAccessor;
    }

    /** 🔧 MCRe：重建窗口视图为 [sectionYMin, sectionYMax] */
    public void buildWindow(final int sectionYMin, final int sectionYMax) {
        if (sectionYMin > sectionYMax) {
            return;
        }
        int count = sectionYMax - sectionYMin + 1;
        this.windowMinY = sectionYMin;
        LevelChunkSection[] win = new LevelChunkSection[count];
        for (int sy = sectionYMin; sy <= sectionYMax; sy++) {
            int idx = sy - sectionYMin;
            win[idx] = this.allSections.computeIfAbsent(sy, k -> new LevelChunkSection(this.containerFactory));
        }
        // 同步到 sections 数组（保持 getSections() 读数一致）
        if (this.sections.length >= count) {
            for (int i = 0; i < count; i++) {
                this.sections[i] = win[i];
            }
        }
        this.windowSections = win;
    }

    /** 🔧 MCRe P5：滑动窗口——以 centerSectionY 为中心重建窗口（对齐 inf_farlands 的 34-section 窗口）。
     *  <p>早退：相机 sectionY 已被当前窗口完全覆盖（[center-17, center+16] ⊆ [windowMinY, windowMaxY]）→ 直接返回，避免每帧重建数组。
     *  <p>不等式：centerSectionY - windowMinY ∈ [WINDOW_HALF_BELOW, WINDOW_HALF_BELOW+WINDOW_HALF_ABOVE] = [17, 33] 时命中。 */
    public void moveWindowTo(final int centerSectionY) {
        int halfBelow = net.minecraft.world.level.chunk.WindowedChunk.WINDOW_HALF_BELOW;
        int halfAbove = net.minecraft.world.level.chunk.WindowedChunk.WINDOW_HALF_ABOVE;
        if (this.windowSections.length > 0) {
            int offset = centerSectionY - this.windowMinY;
            if (offset >= halfBelow && offset <= halfBelow + halfAbove) {
                // 当前窗口已完全覆盖玩家可探索区，无需滑动
                return;
            }
        }
        this.buildWindow(centerSectionY - halfBelow, centerSectionY + halfAbove);
    }

    /** 🔧 MCRe：确保 sectionY 在窗口内（窗口外则滑窗） */
    public void ensureWindowContains(final int sectionY) {
        if (sectionY < this.windowMinY || sectionY > this.getWindowMaxY()) {
            this.moveWindowTo(sectionY);
        }
    }

    private void buildDefaultWindow() {
        int minSectionY = this.levelHeightAccessor.getMinSectionY();
        int count = this.levelHeightAccessor.getSectionsCount();
        // 🔧 MCRe P4b：超高世界（getMinSectionY = -1.34亿）窗口若锚定世界底部，
        // 玩家活动区（y≈0）完全不在窗口内，方块/光照/序列化全错位。
        // 正常高度世界保持原版语义（minSectionY..minSectionY+count-1），
        // 超高世界锚定到玩家默认活动区（0 中心，-17..+16）。
        // 玩家可探索超过窗口范围的部分由 moveWindowTo 滑动跟随（渲染层接线）。
        if (minSectionY < -512 || minSectionY + count > 0x7FFF) {
            int center = 0;
            this.buildWindow(center - net.minecraft.world.level.chunk.WindowedChunk.WINDOW_HALF_BELOW, center + net.minecraft.world.level.chunk.WindowedChunk.WINDOW_HALF_ABOVE);
        } else {
            this.buildWindow(minSectionY, minSectionY + count - 1);
        }
    }

    public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public void setHeightmap(final Heightmap.Types key, final long[] data) {
        this.getOrCreateHeightmapUnprimed(key).setRawData(this, key, data);
    }

    public Heightmap getOrCreateHeightmapUnprimed(final Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, k -> new Heightmap(this, k));
    }

    public boolean hasPrimedHeightmap(final Heightmap.Types type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHeight(final Heightmap.Types type, final int x, final int z) {
        Heightmap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
                LOGGER.error("Unprimed heightmap: {} {} {}", type, x, z);
            }

            Heightmap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public ChunkPos getPos() {
        return this.chunkPos;
    }

    @Override
    public @Nullable StructureStart getStartForStructure(final Structure structure) {
        return this.structureStarts.get(structure);
    }

    @Override
    public void setStartForStructure(final Structure structure, final StructureStart structureStart) {
        this.structureStarts.put(structure, structureStart);
        this.markUnsaved();
    }

    public Map<Structure, StructureStart> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    public void setAllStarts(final Map<Structure, StructureStart> starts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(starts);
        this.markUnsaved();
    }

    @Override
    public Set<ChunkPos> getReferencesForStructure(final Structure structure) {
        return this.structuresRefences.getOrDefault(structure, EMPTY_REFERENCE_SET);
    }

    @Override
    public void addReferenceForStructure(final Structure structure, final ChunkPos reference) {
        this.structuresRefences.computeIfAbsent(structure, k -> new java.util.HashSet<>()).add(reference);
        this.markUnsaved();
    }

    @Override
    public Map<Structure, Set<ChunkPos>> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(final Map<Structure, Set<ChunkPos>> data) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(data);
        this.markUnsaved();
    }

    public boolean isYSpaceEmpty(int yStartInclusive, int yEndInclusive) {
        if (yStartInclusive < this.getMinY()) {
            yStartInclusive = this.getMinY();
        }

        if (yEndInclusive > this.getMaxY()) {
            yEndInclusive = this.getMaxY();
        }

        for (int y = yStartInclusive; y <= yEndInclusive; y += 16) {
            if (!this.getSection(this.getSectionIndex(y)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public void markUnsaved() {
        this.unsaved = true;
    }

    public boolean tryMarkSaved() {
        if (this.unsaved) {
            this.unsaved = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public abstract ChunkStatus getPersistedStatus();

    public ChunkStatus getHighestGeneratedStatus() {
        ChunkStatus status = this.getPersistedStatus();
        BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            ChunkStatus targetStatus = belowZeroRetrogen.targetStatus();
            return ChunkStatus.max(targetStatus, status);
        } else {
            return status;
        }
    }

    public abstract void removeBlockEntity(BlockPos pos);

    public void markPosForPostProcessing(final BlockPos blockPos) {
        LOGGER.warn("Trying to mark a block for post processing @ {}, but this operation is not supported.", blockPos);
    }

    public @Nullable ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(final ShortList packedOffsets, final int sectionIndex) {
        getOrCreateOffsetList(this.getPostProcessing(), sectionIndex).addAll(packedOffsets);
    }

    public void setBlockEntityNbt(final CompoundTag entityTag) {
        BlockPos posFromTag = BlockEntity.getPosFromTag(this.chunkPos, entityTag);
        if (!this.blockEntities.containsKey(posFromTag)) {
            this.pendingBlockEntities.put(posFromTag, entityTag);
        }
    }

    public @Nullable CompoundTag getBlockEntityNbt(final BlockPos blockPos) {
        return this.pendingBlockEntities.get(blockPos);
    }

    public abstract @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos, HolderLookup.Provider registryAccess);

    @Override
    public final void findBlockLightSources(final BiConsumer<BlockPos, BlockState> consumer) {
        this.findBlocks(state -> state.getLightEmission() != 0, consumer);
    }

    public void findBlocks(final Predicate<BlockState> predicate, final BiConsumer<BlockPos, BlockState> consumer) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int sectionY = this.getMinSectionY(); sectionY <= this.getMaxSectionY(); sectionY++) {
            LevelChunkSection section = this.getSection(this.getSectionIndexFromSectionY(sectionY));
            if (section.maybeHas(predicate)) {
                BlockPos origin = SectionPos.of(this.chunkPos, sectionY).origin();

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState state = section.getBlockState(x, y, z);
                            if (predicate.test(state)) {
                                consumer.accept(mutablePos.setWithOffset(origin, x, y, z), state);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract TickContainerAccess<Block> getBlockTicks();

    public abstract TickContainerAccess<Fluid> getFluidTicks();

    public void collectBiomesInPalette(final Set<Holder<Biome>> output) {
        for (LevelChunkSection section : this.sections) {
            section.getBiomes().forEachInPalette(output::add);
        }
    }

    public boolean canBeSerialized() {
        return true;
    }

    public abstract ChunkAccess.PackedTicks getTicksForSerialization(long currentTick);

    public UpgradeData getUpgradeData() {
        return this.upgradeData;
    }

    public boolean isOldNoiseGeneration() {
        return this.blendingData != null;
    }

    public @Nullable BlendingData getBlendingData() {
        return this.blendingData;
    }

    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    public void incrementInhabitedTime(final long inhabitedTimeDelta) {
        this.inhabitedTime += inhabitedTimeDelta;
    }

    public void setInhabitedTime(final long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public static ShortList getOrCreateOffsetList(final @Nullable ShortList[] list, final int sectionIndex) {
        ShortList result = list[sectionIndex];
        if (result == null) {
            result = new ShortArrayList();
            list[sectionIndex] = result;
        }

        return result;
    }

    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    public void setLightCorrect(final boolean isLightCorrect) {
        this.isLightCorrect = isLightCorrect;
        this.markUnsaved();
    }

    @Override
    public int getMinY() {
        return this.levelHeightAccessor.getMinY();
    }

    @Override
    public int getHeight() {
        return this.levelHeightAccessor.getHeight();
    }

    public NoiseChunk getOrCreateNoiseChunk(final Function<ChunkAccess, NoiseChunk> factory) {
        if (this.noiseChunk == null) {
            this.noiseChunk = factory.apply(this);
        }

        return this.noiseChunk;
    }

    @Deprecated
    public BiomeGenerationSettings carverBiome(final Supplier<BiomeGenerationSettings> source) {
        if (this.carverBiomeSettings == null) {
            this.carverBiomeSettings = source.get();
        }

        return this.carverBiomeSettings;
    }

    @Override
    public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
        try {
            // 🔧 MCRe P4b+：超高世界 dimensionType.height=2147000000，世界域 getMinY/getHeight 会让
            // clamp 后的 quartY 仍是世界域值，转 sectionIndex 后远超 sections.length=34 而越界。
            // 改用窗口化高度域（与 getHeightAccessorForGeneration / NoiseChunk / carve 一致），
            // quartY 钳制到 [windowMinY, windowMaxY] 范围内，sectionIndex 落在 [0, WINDOW_SECTIONS) 内。
            // 同时 this.sections[sectionIndex] 改走 this.getSection(sectionIndex) 懒创建（与
            // LevelChunk.getBlockState/getFluidState 同思路），杜绝任何 sectionIndex 越界。
            LevelHeightAccessor ha = this.getHeightAccessorForGeneration();
            int quartMinY = QuartPos.fromBlock(ha.getMinY());
            int quartMaxY = quartMinY + QuartPos.fromBlock(ha.getHeight()) - 1;
            int clampedQuartY = Mth.clamp(quartY, quartMinY, quartMaxY);
            int sectionIndex = this.getSectionIndex(QuartPos.toBlock(clampedQuartY));
            return this.getSection(sectionIndex).getNoiseBiome(quartX & 3, clampedQuartY & 3, quartZ & 3);
        } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Getting biome");
            CrashReportCategory category = report.addCategory("Biome being got");
            category.setDetail("Location", () -> CrashReportCategory.formatLocation(this, quartX, quartY, quartZ));
            throw new ReportedException(report);
        }
    }

    public void fillBiomesFromNoise(final BiomeResolver biomeResolver, final Climate.Sampler sampler) {
        ChunkPos pos = this.getPos();
        int quartMinX = QuartPos.fromBlock((int)pos.getMinBlockX());
        int quartMinZ = QuartPos.fromBlock((int)pos.getMinBlockZ());
        LevelHeightAccessor heightAccessor = this.getHeightAccessorForGeneration();

        for (int sectionY = heightAccessor.getMinSectionY(); sectionY <= heightAccessor.getMaxSectionY(); sectionY++) {
            LevelChunkSection section = this.getSection(this.getSectionIndexFromSectionY(sectionY));
            int quartMinY = QuartPos.fromSection(sectionY);
            section.fillBiomesFromNoise(biomeResolver, sampler, quartMinX, quartMinY, quartMinZ);
        }
    }

    public boolean hasAnyStructureReferences() {
        return !this.getAllReferences().isEmpty();
    }

    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return null;
    }

    public boolean isUpgrading() {
        return this.getBelowZeroRetrogen() != null;
    }

    public LevelHeightAccessor getHeightAccessorForGeneration() {
        // 🔧 MCRe P4b：生成链必须用「窗口化高度域」而不是世界域——
        // 超高世界 getMinY()=-21.47亿/height=42.9亿，NoiseChunk 按全高算 cellCountY
        // （42.9亿/8=5.36亿）分配插值数组直接 OOM（内存可达 8GB+）。
        // 窗口域 minY=windowMinY*16, height=窗口section数*16(=34*16=544) → cellCountY=68，恒定小内存。
        if (this.windowedHeightAccessor == null) {
            this.windowedHeightAccessor = new LevelHeightAccessor() {
                @Override
                public int getHeight() {
                    return ChunkAccess.this.windowSections.length * 16;
                }

                @Override
                public int getMinY() {
                    return ChunkAccess.this.windowMinY * 16;
                }

                @Override
                public int getMinSectionY() {
                    return ChunkAccess.this.getWindowMinY();
                }

                @Override
                public int getMaxSectionY() {
                    return ChunkAccess.this.getWindowMaxY();
                }
            };
        }
        return this.windowedHeightAccessor;
    }

    public void initializeLightSources() {
        this.skyLightSources.fillFrom(this);
    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return this.skyLightSources;
    }

    public static ProblemReporter.PathElement problemPath(final ChunkPos pos) {
        return new ChunkAccess.ChunkPathElement(pos);
    }

    public ProblemReporter.PathElement problemPath() {
        return problemPath(this.getPos());
    }

    private record ChunkPathElement(ChunkPos pos) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "chunk@" + this.pos;
        }
    }

    public record PackedTicks(List<SavedTick<Block>> blocks, List<SavedTick<Fluid>> fluids) {
    }
}