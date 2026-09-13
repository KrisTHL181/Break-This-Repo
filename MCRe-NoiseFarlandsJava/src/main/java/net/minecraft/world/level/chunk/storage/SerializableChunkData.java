package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record SerializableChunkData(
    PalettedContainerFactory containerFactory,
    ChunkPos chunkPos,
    int minSectionY,
    long lastUpdateTime,
    long inhabitedTime,
    ChunkStatus chunkStatus,
    BlendingData.@Nullable Packed blendingData,
    @Nullable BelowZeroRetrogen belowZeroRetrogen,
    UpgradeData upgradeData,
    long @Nullable [] carvingMask,
    Map<Heightmap.Types, long[]> heightmaps,
    ChunkAccess.PackedTicks packedTicks,
    @Nullable ShortList[] postProcessingSections,
    boolean lightCorrect,
    List<SerializableChunkData.SectionData> sectionData,
    List<CompoundTag> entities,
    List<CompoundTag> blockEntities,
    CompoundTag structureData
) {
    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";
    public static final String X_POS_TAG = "xPos";
    public static final String Z_POS_TAG = "zPos";
    public static final String HEIGHTMAPS_TAG = "Heightmaps";
    public static final String IS_LIGHT_ON_TAG = "isLightOn";
    public static final String SECTIONS_TAG = "sections";
    public static final String BLOCK_LIGHT_TAG = "BlockLight";
    public static final String SKY_LIGHT_TAG = "SkyLight";

    public static SerializableChunkData parse(
        final LevelHeightAccessor levelHeight, final PalettedContainerFactory containerFactory, final CompoundTag chunkData
    ) {
        if (chunkData.getString("Status").isEmpty()) {
            return null;
        }

        ChunkPos chunkPos = new ChunkPos(chunkData.getIntOr("xPos", 0), chunkData.getIntOr("zPos", 0));
        long lastUpdateTime = chunkData.getLongOr("LastUpdate", 0L);
        long inhabitedTime = chunkData.getLongOr("InhabitedTime", 0L);
        ChunkStatus status = chunkData.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
        UpgradeData upgradeData = chunkData.getCompound("UpgradeData").map(tag -> new UpgradeData(tag, levelHeight)).orElse(UpgradeData.EMPTY);
        boolean lightCorrect = chunkData.getBooleanOr("isLightOn", false);
        BlendingData.Packed blendingData = chunkData.read("blending_data", BlendingData.Packed.CODEC).orElse(null);
        BelowZeroRetrogen belowZeroRetrogen = chunkData.read("below_zero_retrogen", BelowZeroRetrogen.CODEC).orElse(null);
        long[] carvingMask = chunkData.getLongArray("carving_mask").orElse(null);
        Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);
        chunkData.getCompound("Heightmaps").ifPresent(heightmapsTag -> {
            for (Heightmap.Types type : status.heightmapsAfter()) {
                heightmapsTag.getLongArray(type.getSerializationKey()).ifPresent(longs -> heightmaps.put(type, longs));
            }
        });
        List<SavedTick<Block>> blockTicks = SavedTick.filterTickListForChunk(chunkData.read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), chunkPos);
        List<SavedTick<Fluid>> fluidTicks = SavedTick.filterTickListForChunk(chunkData.read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), chunkPos);
        ChunkAccess.PackedTicks packedTicks = new ChunkAccess.PackedTicks(blockTicks, fluidTicks);
        ListTag postProcessTags = chunkData.getListOrEmpty("PostProcessing");
        ShortList[] postProcessingSections = new ShortList[postProcessTags.size()];

        for (int sectionIndex = 0; sectionIndex < postProcessTags.size(); sectionIndex++) {
            ListTag offsetsTag = postProcessTags.getList(sectionIndex).orElse(null);
            if (offsetsTag != null && !offsetsTag.isEmpty()) {
                ShortList packedOffsets = new ShortArrayList(offsetsTag.size());

                for (int i = 0; i < offsetsTag.size(); i++) {
                    packedOffsets.add(offsetsTag.getShortOr(i, (short)0));
                }

                postProcessingSections[sectionIndex] = packedOffsets;
            }
        }

        List<CompoundTag> entities = chunkData.getList("entities").stream().flatMap(ListTag::compoundStream).toList();
        List<CompoundTag> blockEntities = chunkData.getList("block_entities").stream().flatMap(ListTag::compoundStream).toList();
        CompoundTag structureData = chunkData.getCompoundOrEmpty("structures");
        ListTag sectionTags = chunkData.getListOrEmpty("sections");
        List<SerializableChunkData.SectionData> sectionData = new ArrayList<>(sectionTags.size());
        Codec<PalettedContainerRO<Holder<Biome>>> biomesCodec = containerFactory.biomeContainerCodec();
        Codec<PalettedContainer<BlockState>> blockStatesCodec = containerFactory.blockStatesContainerCodec();

        for (int i = 0; i < sectionTags.size(); i++) {
            Optional<CompoundTag> maybeSectionTag = sectionTags.getCompound(i);
            if (!maybeSectionTag.isEmpty()) {
                CompoundTag sectionTag = maybeSectionTag.get();
                int y = sectionTag.getByteOr("Y", (byte)0);
                LevelChunkSection section;
                if (y >= levelHeight.getMinSectionY() && y <= levelHeight.getMaxSectionY()) {
                    PalettedContainer<BlockState> blocks = sectionTag.getCompound("block_states")
                        .map(
                            container -> blockStatesCodec.parse(NbtOps.INSTANCE, container)
                                .promotePartial(msg -> logErrors(chunkPos, y, msg))
                                .getOrThrow(SerializableChunkData.ChunkReadException::new)
                        )
                        .orElseGet(containerFactory::createForBlockStates);
                    PalettedContainerRO<Holder<Biome>> biomes = sectionTag.getCompound("biomes")
                        .map(
                            container -> biomesCodec.parse(NbtOps.INSTANCE, container)
                                .promotePartial(msg -> logErrors(chunkPos, y, msg))
                                .getOrThrow(SerializableChunkData.ChunkReadException::new)
                        )
                        .orElseGet(containerFactory::createForBiomes);
                    section = new LevelChunkSection(blocks, biomes);
                } else {
                    section = null;
                }

                DataLayer blockLight = sectionTag.getByteArray("BlockLight").map(DataLayer::new).orElse(null);
                DataLayer skyLight = sectionTag.getByteArray("SkyLight").map(DataLayer::new).orElse(null);
                sectionData.add(new SerializableChunkData.SectionData(y, section, blockLight, skyLight));
            }
        }

        return new SerializableChunkData(
            containerFactory,
            chunkPos,
            levelHeight.getMinSectionY(),
            lastUpdateTime,
            inhabitedTime,
            status,
            blendingData,
            belowZeroRetrogen,
            upgradeData,
            carvingMask,
            heightmaps,
            packedTicks,
            postProcessingSections,
            lightCorrect,
            sectionData,
            entities,
            blockEntities,
            structureData
        );
    }

    public ProtoChunk read(final ServerLevel level, final PoiManager poiManager, final RegionStorageInfo regionInfo, final ChunkPos pos) {
        if (!Objects.equals(pos, this.chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, this.chunkPos);
            level.getServer().reportMisplacedChunk(this.chunkPos, pos, regionInfo);
        }

        int sectionCount = level.getSectionsCount();
        LevelChunkSection[] sections = new LevelChunkSection[sectionCount];
        boolean skyLight = level.dimensionType().hasSkyLight();
        ChunkSource chunkSource = level.getChunkSource();
        LevelLightEngine lightEngine = chunkSource.getLightEngine();
        PalettedContainerFactory containerFactory = level.palettedContainerFactory();
        boolean loadedAnyLight = false;

        for (SerializableChunkData.SectionData section : this.sectionData) {
            SectionPos sectionPos = SectionPos.of(pos, section.y);
            if (section.chunkSection != null) {
                sections[level.getSectionIndexFromSectionY(section.y)] = section.chunkSection;
                poiManager.checkConsistencyWithBlocks(sectionPos, section.chunkSection);
            }

            boolean hasBlockLight = section.blockLight != null;
            boolean hasSkyLight = skyLight && section.skyLight != null;
            if (hasBlockLight || hasSkyLight) {
                if (!loadedAnyLight) {
                    lightEngine.retainData(pos, true);
                    loadedAnyLight = true;
                }

                if (hasBlockLight) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, section.blockLight);
                }

                if (hasSkyLight) {
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, section.skyLight);
                }
            }
        }

        ChunkType chunkType = this.chunkStatus.getChunkType();
        ChunkAccess chunk;
        if (chunkType == ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> blockTicks = new LevelChunkTicks<>(this.packedTicks.blocks());
            LevelChunkTicks<Fluid> fluidTicks = new LevelChunkTicks<>(this.packedTicks.fluids());
            chunk = new LevelChunk(
                level.getLevel(),
                pos,
                this.upgradeData,
                blockTicks,
                fluidTicks,
                this.inhabitedTime,
                sections,
                postLoadChunk(level, this.entities, this.blockEntities),
                BlendingData.unpack(this.blendingData)
            );
        } else {
            ProtoChunkTicks<Block> blockTicks = ProtoChunkTicks.load(this.packedTicks.blocks());
            ProtoChunkTicks<Fluid> fluidTicks = ProtoChunkTicks.load(this.packedTicks.fluids());
            ProtoChunk protoChunk = new ProtoChunk(
                pos, this.upgradeData, sections, blockTicks, fluidTicks, level, containerFactory, BlendingData.unpack(this.blendingData)
            );
            chunk = protoChunk;
            chunk.setInhabitedTime(this.inhabitedTime);
            if (this.belowZeroRetrogen != null) {
                protoChunk.setBelowZeroRetrogen(this.belowZeroRetrogen);
            }

            protoChunk.setPersistedStatus(this.chunkStatus);
            if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                protoChunk.setLightEngine(lightEngine);
            }
        }

        chunk.setLightCorrect(this.lightCorrect);

        // 🔧 MCRe：窗口化——sectionData 自带绝对 sectionY，读回后回注 allSections，
        // 保证超高世界（超出窗口的 section）也能从存档还原，不受窗口数组钳制
        if (chunk instanceof net.minecraft.world.level.chunk.WindowedChunk windowed) {
            for (SerializableChunkData.SectionData sectionData : this.sectionData) {
                if (sectionData.chunkSection != null) {
                    chunk.setSectionAt(sectionData.y, sectionData.chunkSection);
                }
            }
        }

        EnumSet<Heightmap.Types> toPrime = EnumSet.noneOf(Heightmap.Types.class);

        for (Heightmap.Types type : chunk.getPersistedStatus().heightmapsAfter()) {
            long[] heightmap = this.heightmaps.get(type);
            if (heightmap != null) {
                chunk.setHeightmap(type, heightmap);
            } else {
                toPrime.add(type);
            }
        }

        Heightmap.primeHeightmaps(chunk, toPrime);
        chunk.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(level), this.structureData, level.getSeed()));
        chunk.setAllReferences(unpackStructureReferences(level.registryAccess(), pos, this.structureData));

        for (int sectionIndex = 0; sectionIndex < this.postProcessingSections.length; sectionIndex++) {
            ShortList postProcessingSection = this.postProcessingSections[sectionIndex];
            if (postProcessingSection != null) {
                chunk.addPackedPostProcess(postProcessingSection, sectionIndex);
            }
        }

        if (chunkType == ChunkType.LEVELCHUNK) {
            return new ImposterProtoChunk((LevelChunk)chunk, false);
        }

        ProtoChunk protoChunk = (ProtoChunk)chunk;

        for (CompoundTag entity : this.entities) {
            protoChunk.addEntity(entity);
        }

        for (CompoundTag blockEntity : this.blockEntities) {
            protoChunk.setBlockEntityNbt(blockEntity);
        }

        if (this.carvingMask != null) {
            protoChunk.setCarvingMask(new CarvingMask(this.carvingMask, chunk.getMinY()));
        }

        return protoChunk;
    }

    private static void logErrors(final ChunkPos pos, final int sectionY, final String message) {
        LOGGER.error("Recoverable errors when loading section [{}, {}, {}]: {}", pos.x(), sectionY, pos.z(), message);
    }

    public static SerializableChunkData copyOf(final ServerLevel level, final ChunkAccess chunk) {
        if (!chunk.canBeSerialized()) {
            throw new IllegalArgumentException("Chunk can't be serialized: " + chunk);
        }

        ChunkPos pos = chunk.getPos();
        List<SerializableChunkData.SectionData> sectionData = new ArrayList<>();
        LevelChunkSection[] chunkSections = chunk.getSections();
        LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();

        // 🔧 MCRe：窗口化——遍历 allSections 无限仓库（绝对 sectionY），
        // 保证超高世界（超出窗口）的 section 也能写入存档，不再受窗口数组钳制
        if (chunk instanceof net.minecraft.world.level.chunk.WindowedChunk windowedChunk) {
            java.util.List<java.util.Map.Entry<Integer, LevelChunkSection>> all =
                new java.util.ArrayList<>(windowedChunk.windowedAllSections().entrySet());
            all.sort(java.util.Comparator.comparingInt(java.util.Map.Entry::getKey));
            for (java.util.Map.Entry<Integer, LevelChunkSection> e : all) {
                int sectionY = e.getKey();
                LevelChunkSection section = e.getValue();
                DataLayer sourceBlockLight = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(pos, sectionY));
                DataLayer sourceSkyLight = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(pos, sectionY));
                DataLayer blockLight = sourceBlockLight != null && !sourceBlockLight.isEmpty() ? sourceBlockLight.copy() : null;
                DataLayer skyLight = sourceSkyLight != null && !sourceSkyLight.isEmpty() ? sourceSkyLight.copy() : null;
                if (section != null && !section.hasOnlyAir() || blockLight != null || skyLight != null) {
                    LevelChunkSection sectionCopy = section != null ? section.copy() : null;
                    sectionData.add(new SerializableChunkData.SectionData(sectionY, sectionCopy, blockLight, skyLight));
                }
            }
        } else {
            for (int sectionY = lightEngine.getMinLightSection(); sectionY < lightEngine.getMaxLightSection(); sectionY++) {
                int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                boolean hasSection = sectionIndex >= 0 && sectionIndex < chunkSections.length;
                DataLayer sourceBlockLight = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(pos, sectionY));
                DataLayer sourceSkyLight = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(pos, sectionY));
                DataLayer blockLight = sourceBlockLight != null && !sourceBlockLight.isEmpty() ? sourceBlockLight.copy() : null;
                DataLayer skyLight = sourceSkyLight != null && !sourceSkyLight.isEmpty() ? sourceSkyLight.copy() : null;
                if (hasSection || blockLight != null || skyLight != null) {
                    LevelChunkSection section = hasSection ? chunkSections[sectionIndex].copy() : null;
                    sectionData.add(new SerializableChunkData.SectionData(sectionY, section, blockLight, skyLight));
                }
            }
        }

        List<CompoundTag> blockEntities = new ArrayList<>(chunk.getBlockEntitiesPos().size());

        for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag blockEntityTag = chunk.getBlockEntityNbtForSaving(blockPos, level.registryAccess());
            if (blockEntityTag != null) {
                blockEntities.add(blockEntityTag);
            }
        }

        List<CompoundTag> entities = new ArrayList<>();
        long[] carvingMask = null;
        if (chunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            entities.addAll(protoChunk.getEntities());
            CarvingMask existingMask = protoChunk.getCarvingMask();
            if (existingMask != null) {
                carvingMask = existingMask.toArray();
            }
        }

        Map<Heightmap.Types, long[]> heightmaps = new EnumMap<>(Heightmap.Types.class);

        for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                long[] data = entry.getValue().getRawData();
                heightmaps.put(entry.getKey(), (long[])data.clone());
            }
        }

        ChunkAccess.PackedTicks ticksForSerialization = chunk.getTicksForSerialization(level.getGameTime());
        ShortList[] postProcessingSections = Arrays.stream(chunk.getPostProcessing())
            .map(shorts -> shorts != null && !shorts.isEmpty() ? new ShortArrayList(shorts) : null)
            .toArray(ShortList[]::new);
        CompoundTag structureData = packStructureData(StructurePieceSerializationContext.fromLevel(level), pos, chunk.getAllStarts(), chunk.getAllReferences());
        return new SerializableChunkData(
            level.palettedContainerFactory(),
            pos,
            chunk.getMinSectionY(),
            level.getGameTime(),
            chunk.getInhabitedTime(),
            chunk.getPersistedStatus(),
            Optionull.map(chunk.getBlendingData(), BlendingData::pack),
            chunk.getBelowZeroRetrogen(),
            chunk.getUpgradeData().copy(),
            carvingMask,
            heightmaps,
            ticksForSerialization,
            postProcessingSections,
            chunk.isLightCorrect(),
            sectionData,
            entities,
            blockEntities,
            structureData
        );
    }

    public CompoundTag write() {
        CompoundTag tag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        tag.putInt("xPos", (int)this.chunkPos.x());
        tag.putInt("yPos", this.minSectionY);
        tag.putInt("zPos", (int)this.chunkPos.z());
        tag.putLong("LastUpdate", this.lastUpdateTime);
        tag.putLong("InhabitedTime", this.inhabitedTime);
        tag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(this.chunkStatus).toString());
        tag.storeNullable("blending_data", BlendingData.Packed.CODEC, this.blendingData);
        tag.storeNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, this.belowZeroRetrogen);
        if (!this.upgradeData.isEmpty()) {
            tag.put("UpgradeData", this.upgradeData.write());
        }

        ListTag sectionTags = new ListTag();
        Codec<PalettedContainer<BlockState>> blockStatesCodec = this.containerFactory.blockStatesContainerCodec();
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = this.containerFactory.biomeContainerCodec();

        for (SerializableChunkData.SectionData section : this.sectionData) {
            CompoundTag sectionTag = new CompoundTag();
            LevelChunkSection chunkSection = section.chunkSection;
            if (chunkSection != null) {
                sectionTag.store("block_states", blockStatesCodec, chunkSection.getStates());
                sectionTag.store("biomes", biomeCodec, chunkSection.getBiomes());
            }

            if (section.blockLight != null) {
                sectionTag.putByteArray("BlockLight", section.blockLight.getData());
            }

            if (section.skyLight != null) {
                sectionTag.putByteArray("SkyLight", section.skyLight.getData());
            }

            if (!sectionTag.isEmpty()) {
                sectionTag.putByte("Y", (byte)section.y);
                sectionTags.add(sectionTag);
            }
        }

        tag.put("sections", sectionTags);
        if (this.lightCorrect) {
            tag.putBoolean("isLightOn", true);
        }

        ListTag blockEntityTags = new ListTag();
        blockEntityTags.addAll(this.blockEntities);
        tag.put("block_entities", blockEntityTags);
        if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
            ListTag entityTags = new ListTag();
            entityTags.addAll(this.entities);
            tag.put("entities", entityTags);
            if (this.carvingMask != null) {
                tag.putLongArray("carving_mask", this.carvingMask);
            }
        }

        saveTicks(tag, this.packedTicks);
        tag.put("PostProcessing", packOffsets(this.postProcessingSections));
        CompoundTag heightmapsTag = new CompoundTag();
        this.heightmaps.forEach((type, data) -> heightmapsTag.put(type.getSerializationKey(), new LongArrayTag(data)));
        tag.put("Heightmaps", heightmapsTag);
        tag.put("structures", this.structureData);
        return tag;
    }

    private static void saveTicks(final CompoundTag levelData, final ChunkAccess.PackedTicks ticksForSerialization) {
        levelData.store("block_ticks", BLOCK_TICKS_CODEC, ticksForSerialization.blocks());
        levelData.store("fluid_ticks", FLUID_TICKS_CODEC, ticksForSerialization.fluids());
    }

    public static ChunkStatus getChunkStatusFromTag(final @Nullable CompoundTag tag) {
        return tag != null ? tag.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY) : ChunkStatus.EMPTY;
    }

    private static LevelChunk.@Nullable PostLoadProcessor postLoadChunk(
        final ServerLevel level, final List<CompoundTag> entities, final List<CompoundTag> blockEntities
    ) {
        return entities.isEmpty() && blockEntities.isEmpty()
            ? null
            : levelChunk -> {
                if (!entities.isEmpty()) {
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(levelChunk.problemPath(), LOGGER)) {
                        level.addLegacyChunkEntities(
                            EntityType.loadEntitiesRecursive(TagValueInput.create(reporter, level.registryAccess(), entities), level, EntitySpawnReason.LOAD)
                        );
                    }
                }

                for (CompoundTag entityTag : blockEntities) {
                    boolean keepPacked = entityTag.getBooleanOr("keepPacked", false);
                    if (keepPacked) {
                        levelChunk.setBlockEntityNbt(entityTag);
                    } else {
                        BlockPos pos = BlockEntity.getPosFromTag(levelChunk.getPos(), entityTag);
                        BlockEntity blockEntity = BlockEntity.loadStatic(pos, levelChunk.getBlockState(pos), entityTag, level.registryAccess());
                        if (blockEntity != null) {
                            levelChunk.setBlockEntity(blockEntity);
                        }
                    }
                }
            };
    }

    private static CompoundTag packStructureData(
        final StructurePieceSerializationContext context,
        final ChunkPos pos,
        final Map<Structure, StructureStart> starts,
        final Map<Structure, Set<ChunkPos>> references
    ) {
        CompoundTag outTag = new CompoundTag();
        CompoundTag startsTag = new CompoundTag();
        Registry<Structure> structuresRegistry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        for (Entry<Structure, StructureStart> entry : starts.entrySet()) {
            Identifier key = structuresRegistry.getKey(entry.getKey());
            startsTag.put(key.toString(), entry.getValue().createTag(context, pos));
        }

        outTag.put("starts", startsTag);
        CompoundTag referencesTag = new CompoundTag();

        for (Entry<Structure, Set<ChunkPos>> entry : references.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Identifier key = structuresRegistry.getKey(entry.getKey());
                referencesTag.putLongArray(key.toString(), packChunkPosArray(entry.getValue()));
            }
        }

        outTag.put("References", referencesTag);
        return outTag;
    }

    private static Map<Structure, StructureStart> unpackStructureStart(final StructurePieceSerializationContext context, final CompoundTag tag, final long seed) {
        Map<Structure, StructureStart> outmap = Maps.newHashMap();
        Registry<Structure> structuresRegistry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CompoundTag startsTag = tag.getCompoundOrEmpty("starts");

        for (String key : startsTag.keySet()) {
            Identifier id = Identifier.tryParse(key);
            Structure startFeature = structuresRegistry.getValue(id);
            if (startFeature == null) {
                LOGGER.error("Unknown structure start: {}", id);
            } else {
                StructureStart start = StructureStart.loadStaticStart(context, startsTag.getCompoundOrEmpty(key), seed);
                if (start != null) {
                    outmap.put(startFeature, start);
                }
            }
        }

        return outmap;
    }

    private static Map<Structure, Set<ChunkPos>> unpackStructureReferences(final RegistryAccess registryAccess, final ChunkPos pos, final CompoundTag tag) {
        Map<Structure, Set<ChunkPos>> outmap = Maps.newHashMap();
        Registry<Structure> structuresRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);
        CompoundTag referencesTag = tag.getCompoundOrEmpty("References");
        referencesTag.forEach((key, entry) -> {
            Identifier structureId = Identifier.tryParse(key);
            Structure structureType = structuresRegistry.getValue(structureId);
            if (structureType == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", structureId, pos);
            } else {
                Optional<long[]> longArray = entry.asLongArray();
                if (!longArray.isEmpty()) {
                    Set<ChunkPos> refs = new java.util.HashSet<>();
                    long[] arr = longArray.get();
                    for (int i = 0; i + 1 < arr.length; i += 2) {
                        ChunkPos refPos = new ChunkPos(arr[i], arr[i + 1]);
                        if (refPos.getChessboardDistance(pos) > 8) {
                            LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", structureId, refPos, pos);
                        } else {
                            refs.add(refPos);
                        }
                    }
                    if (!refs.isEmpty()) {
                        outmap.put(structureType, refs);
                    }
                }
            }
        });
        return outmap;
    }

    /** 结构引用序列化：ChunkPos 存为 [x0, z0, x1, z1, ...] long 数组（替代原 ChunkPos.pack 打包） */
    private static long[] packChunkPosArray(final Set<ChunkPos> positions) {
        long[] arr = new long[positions.size() * 2];
        int i = 0;
        for (ChunkPos pos : positions) {
            arr[i++] = pos.x();
            arr[i++] = pos.z();
        }
        return arr;
    }

    private static ListTag packOffsets(final @Nullable ShortList[] sections) {
        ListTag listTag = new ListTag();

        for (ShortList offsetList : sections) {
            ListTag offsetsTag = new ListTag();
            if (offsetList != null) {
                for (int i = 0; i < offsetList.size(); i++) {
                    offsetsTag.add(ShortTag.valueOf(offsetList.getShort(i)));
                }
            }

            listTag.add(offsetsTag);
        }

        return listTag;
    }

    public static class ChunkReadException extends NbtException {
        public ChunkReadException(final String message) {
            super(message);
        }
    }

    public record SectionData(int y, @Nullable LevelChunkSection chunkSection, @Nullable DataLayer blockLight, @Nullable DataLayer skyLight) {
    }
}