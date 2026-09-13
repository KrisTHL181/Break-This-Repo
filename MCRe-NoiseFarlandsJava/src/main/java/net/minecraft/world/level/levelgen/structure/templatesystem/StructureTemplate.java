package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
    private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(final Level level, final BlockPos position, final Vec3i size, final boolean inludeEntities, final List<Block> ignoreBlocks) {
        if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
            BlockPos corner2 = position.offset(size).offset(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> fullBlockList = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> blockEntitiesList = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> otherBlocksList = Lists.newArrayList();
            BlockPos minCorner = new BlockPos(
                Math.min(position.getX(), corner2.getX()), Math.min(position.getY(), corner2.getY()), Math.min(position.getZ(), corner2.getZ())
            );
            BlockPos maxCorner = new BlockPos(
                Math.max(position.getX(), corner2.getX()), Math.max(position.getY(), corner2.getY()), Math.max(position.getZ(), corner2.getZ())
            );
            this.size = size;

            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                for (BlockPos pos : BlockPos.betweenClosed(minCorner, maxCorner)) {
                    BlockPos relativePos = pos.subtract(minCorner);
                    BlockState blockState = level.getBlockState(pos);
                    if (!ignoreBlocks.stream().anyMatch(blockState::is)) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        StructureTemplate.StructureBlockInfo info;
                        if (blockEntity != null) {
                            TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                            blockEntity.saveWithId(output);
                            info = new StructureTemplate.StructureBlockInfo(relativePos, blockState, output.buildResult());
                        } else {
                            info = new StructureTemplate.StructureBlockInfo(relativePos, blockState, null);
                        }

                        addToLists(info, fullBlockList, blockEntitiesList, otherBlocksList);
                    }
                }

                List<StructureTemplate.StructureBlockInfo> blockInfoList = buildInfoList(fullBlockList, blockEntitiesList, otherBlocksList);
                this.palettes.clear();
                this.palettes.add(new StructureTemplate.Palette(blockInfoList));
                if (inludeEntities) {
                    this.fillEntityList(level, minCorner, maxCorner, reporter);
                } else {
                    this.entityInfoList.clear();
                }
            }
        }
    }

    private static void addToLists(
        final StructureTemplate.StructureBlockInfo info,
        final List<StructureTemplate.StructureBlockInfo> fullBlockList,
        final List<StructureTemplate.StructureBlockInfo> blockEntitiesList,
        final List<StructureTemplate.StructureBlockInfo> otherBlocksList
    ) {
        if (info.nbt != null) {
            blockEntitiesList.add(info);
        } else if (!info.state.getBlock().hasDynamicShape() && info.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            fullBlockList.add(info);
        } else {
            otherBlocksList.add(info);
        }
    }

    private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
        final List<StructureTemplate.StructureBlockInfo> fullBlockList,
        final List<StructureTemplate.StructureBlockInfo> blockEntitiesList,
        final List<StructureTemplate.StructureBlockInfo> otherBlocksList
    ) {
        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(o -> o.pos.getY())
            .thenComparingInt(o -> o.pos.getX())
            .thenComparingInt(o -> o.pos.getZ());
        fullBlockList.sort(comparator);
        otherBlocksList.sort(comparator);
        blockEntitiesList.sort(comparator);
        List<StructureTemplate.StructureBlockInfo> blockInfoList = Lists.newArrayList();
        blockInfoList.addAll(fullBlockList);
        blockInfoList.addAll(otherBlocksList);
        blockInfoList.addAll(blockEntitiesList);
        return blockInfoList;
    }

    private void fillEntityList(final Level level, final BlockPos minCorner, final BlockPos maxCorner, final ProblemReporter reporter) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(minCorner, maxCorner), input -> !(input instanceof Player));
        this.entityInfoList.clear();

        for (Entity entity : entities) {
            Vec3 pos = new Vec3(entity.getX() - minCorner.getX(), entity.getY() - minCorner.getY(), entity.getZ() - minCorner.getZ());
            TagValueOutput output = TagValueOutput.createWithContext(reporter.forChild(entity.problemPath()), entity.registryAccess());
            entity.save(output);
            BlockPos blockPos;
            if (entity instanceof Painting painting) {
                blockPos = painting.getPos().subtract(minCorner);
            } else {
                blockPos = BlockPos.containing(pos);
            }

            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(pos, blockPos, output.buildResult().copy()));
        }
    }

    public List<StructureTemplate.StructureBlockInfo> filterBlocks(final BlockPos position, final StructurePlaceSettings settings, final Block block) {
        return this.filterBlocks(position, settings, block, true);
    }

    public List<StructureTemplate.JigsawBlockInfo> getJigsaws(final BlockPos position, final Rotation rotation) {
        if (this.palettes.isEmpty()) {
            return new ArrayList<>();
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        List<StructureTemplate.JigsawBlockInfo> jigsaws = settings.getRandomPalette(this.palettes, position).jigsaws();
        List<StructureTemplate.JigsawBlockInfo> result = new ArrayList<>(jigsaws.size());

        for (StructureTemplate.JigsawBlockInfo jigsaw : jigsaws) {
            StructureTemplate.StructureBlockInfo blockInfo = jigsaw.info;
            result.add(
                jigsaw.withInfo(
                    new StructureTemplate.StructureBlockInfo(
                        calculateRelativePosition(settings, blockInfo.pos()).offset(position), blockInfo.state.rotate(settings.getRotation()), blockInfo.nbt
                    )
                )
            );
        }

        return result;
    }

    public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(
        final BlockPos position, final StructurePlaceSettings settings, final Block block, final boolean absolute
    ) {
        ObjectArrayList<StructureTemplate.StructureBlockInfo> result = new ObjectArrayList<>();
        BoundingBox boundingBox = settings.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return result;
        }

        for (StructureTemplate.StructureBlockInfo blockInfo : settings.getRandomPalette(this.palettes, position).blocks(block)) {
            BlockPos blockPos = absolute ? calculateRelativePosition(settings, blockInfo.pos).offset(position) : blockInfo.pos;
            if (boundingBox == null || boundingBox.isInside(blockPos)) {
                result.add(new StructureTemplate.StructureBlockInfo(blockPos, blockInfo.state.rotate(settings.getRotation()), blockInfo.nbt));
            }
        }

        return result;
    }

    public BlockPos calculateConnectedPosition(
        final StructurePlaceSettings settings1, final BlockPos connection1, final StructurePlaceSettings settings2, final BlockPos connection2
    ) {
        BlockPos markerPos1 = calculateRelativePosition(settings1, connection1);
        BlockPos markerPos2 = calculateRelativePosition(settings2, connection2);
        return markerPos1.subtract(markerPos2);
    }

    public static BlockPos calculateRelativePosition(final StructurePlaceSettings settings, final BlockPos pos) {
        return transform(pos, settings.getMirror(), settings.getRotation(), settings.getRotationPivot());
    }

    public boolean placeInWorld(
        final ServerLevelAccessor level,
        final BlockPos position,
        final BlockPos referencePos,
        final StructurePlaceSettings settings,
        final RandomSource random,
        final @Block.UpdateFlags int updateMode
    ) {
        if (this.palettes.isEmpty()) {
            return false;
        }

        List<StructureTemplate.StructureBlockInfo> blockInfoList = settings.getRandomPalette(this.palettes, position).blocks();
        if ((!blockInfoList.isEmpty() || !settings.isIgnoreEntities() && !this.entityInfoList.isEmpty())
            && this.size.getX() >= 1
            && this.size.getY() >= 1
            && this.size.getZ() >= 1) {
            BoundingBox boundingBox = settings.getBoundingBox();
            List<BlockPos> toFill = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? blockInfoList.size() : 0);
            List<BlockPos> lockedFluids = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? blockInfoList.size() : 0);
            List<Pair<BlockPos, CompoundTag>> placed = Lists.newArrayListWithCapacity(blockInfoList.size());
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList = processBlockInfos(level, position, referencePos, settings, blockInfoList);

            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                for (StructureTemplate.StructureBlockInfo blockInfo : processedBlockInfoList) {
                    BlockPos blockPos = blockInfo.pos;
                    if (boundingBox == null || boundingBox.isInside(blockPos)) {
                        FluidState previousFluidState = settings.shouldApplyWaterlogging() ? level.getFluidState(blockPos) : null;
                        BlockState state = blockInfo.state.mirror(settings.getMirror()).rotate(settings.getRotation());
                        if (blockInfo.nbt != null) {
                            level.setBlock(blockPos, Blocks.BARRIER.defaultBlockState(), 820);
                        }

                        if (level.setBlock(blockPos, state, updateMode)) {
                            minX = Math.min(minX, blockPos.getX());
                            minY = Math.min(minY, blockPos.getY());
                            minZ = Math.min(minZ, blockPos.getZ());
                            maxX = Math.max(maxX, blockPos.getX());
                            maxY = Math.max(maxY, blockPos.getY());
                            maxZ = Math.max(maxZ, blockPos.getZ());
                            placed.add(Pair.of(blockPos, blockInfo.nbt));
                            if (blockInfo.nbt != null) {
                                BlockEntity blockEntity = level.getBlockEntity(blockPos);
                                if (blockEntity != null) {
                                    if (!SharedConstants.DEBUG_STRUCTURE_EDIT_MODE && blockEntity instanceof RandomizableContainer) {
                                        blockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                                    }

                                    blockEntity.loadWithComponents(
                                        TagValueInput.create(reporter.forChild(blockEntity.problemPath()), level.registryAccess(), blockInfo.nbt)
                                    );
                                }
                            }

                            if (previousFluidState != null) {
                                if (state.getFluidState().isSource()) {
                                    lockedFluids.add(blockPos);
                                } else if (state.getBlock() instanceof LiquidBlockContainer) {
                                    ((LiquidBlockContainer)state.getBlock()).placeLiquid(level, blockPos, state, previousFluidState);
                                    if (!previousFluidState.isSource()) {
                                        toFill.add(blockPos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean filled = true;
                Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while (filled && !toFill.isEmpty()) {
                    filled = false;
                    Iterator<BlockPos> iterator = toFill.iterator();

                    while (iterator.hasNext()) {
                        BlockPos pos = iterator.next();
                        FluidState toPlace = level.getFluidState(pos);

                        for (int i = 0; i < directions.length && !toPlace.isSource(); i++) {
                            BlockPos neighborPos = pos.relative(directions[i]);
                            FluidState neighbor = level.getFluidState(neighborPos);
                            if (neighbor.isSource() && !lockedFluids.contains(neighborPos)) {
                                toPlace = neighbor;
                            }
                        }

                        if (toPlace.isSource()) {
                            BlockState state = level.getBlockState(pos);
                            if (state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
                                liquidBlockContainer.placeLiquid(level, pos, state, toPlace);
                                filled = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (minX <= maxX) {
                    if (!settings.getKnownShape()) {
                        DiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
                        int startX = minX;
                        int startY = minY;
                        int startZ = minZ;

                        for (Pair<BlockPos, CompoundTag> blockInfo : placed) {
                            BlockPos blockPos = blockInfo.getFirst();
                            shape.fill(blockPos.getX() - startX, blockPos.getY() - startY, blockPos.getZ() - startZ);
                        }

                        updateShapeAtEdge(level, updateMode, shape, startX, startY, startZ);
                    }

                    for (Pair<BlockPos, CompoundTag> blockInfo : placed) {
                        BlockPos blockPos = blockInfo.getFirst();
                        if (!settings.getKnownShape()) {
                            BlockState state = level.getBlockState(blockPos);
                            BlockState newState = Block.updateFromNeighbourShapes(state, level, blockPos);
                            if (state != newState) {
                                level.setBlock(blockPos, newState, updateMode & -2 | 16);
                            }

                            level.updateNeighborsAt(blockPos, newState.getBlock());
                        }

                        if (blockInfo.getSecond() != null) {
                            BlockEntity blockEntity = level.getBlockEntity(blockPos);
                            if (blockEntity != null) {
                                blockEntity.setChanged();
                            }
                        }
                    }
                }

                if (!settings.isIgnoreEntities()) {
                    this.placeEntities(
                        level,
                        position,
                        settings.getMirror(),
                        settings.getRotation(),
                        settings.getRotationPivot(),
                        boundingBox,
                        settings.shouldFinalizeEntities(),
                        reporter
                    );
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static void updateShapeAtEdge(final LevelAccessor level, final @Block.UpdateFlags int updateMode, final DiscreteVoxelShape shape, final BlockPos pos) {
        updateShapeAtEdge(level, updateMode, shape, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void updateShapeAtEdge(
        final LevelAccessor level,
        final @Block.UpdateFlags int updateMode,
        final DiscreteVoxelShape shape,
        final int startX,
        final int startY,
        final int startZ
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        shape.forAllFaces((direction, x, y, z) -> {
            pos.set(startX + x, startY + y, startZ + z);
            neighborPos.setWithOffset(pos, direction);
            BlockState state = level.getBlockState(pos);
            BlockState neighborState = level.getBlockState(neighborPos);
            BlockState newState = state.updateShape(level, level, pos, direction, neighborPos, neighborState, level.getRandom());
            if (state != newState) {
                level.setBlock(pos, newState, updateMode & -2);
            }

            BlockState newNeighborState = neighborState.updateShape(level, level, neighborPos, direction.getOpposite(), pos, newState, level.getRandom());
            if (neighborState != newNeighborState) {
                level.setBlock(neighborPos, newNeighborState, updateMode & -2);
            }
        });
    }

    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
        final ServerLevelAccessor level,
        final BlockPos position,
        final BlockPos referencePos,
        final StructurePlaceSettings settings,
        final List<StructureTemplate.StructureBlockInfo> blockInfoList
    ) {
        List<StructureTemplate.StructureBlockInfo> originalBlockInfoList = new ArrayList<>();
        List<StructureTemplate.StructureBlockInfo> processedBlockInfoList = new ArrayList<>();
        boolean processOnlyInCurrentChunk = true;

        for (StructureProcessor processor : settings.getProcessors()) {
            if (processor.evaluatesEntirePieceState()) {
                processOnlyInCurrentChunk = false;
                break;
            }
        }

        BoundingBox chunkBb = settings.getBoundingBox();

        for (StructureTemplate.StructureBlockInfo blockInfo : blockInfoList) {
            BlockPos blockPos = calculateRelativePosition(settings, blockInfo.pos).offset(position);
            if (!processOnlyInCurrentChunk || chunkBb == null || chunkBb.isInside(blockPos)) {
                StructureTemplate.StructureBlockInfo processedBlockInfo = new StructureTemplate.StructureBlockInfo(
                    blockPos, blockInfo.state, blockInfo.nbt != null ? blockInfo.nbt.copy() : null
                );
                Iterator<StructureProcessor> iterator = settings.getProcessors().iterator();

                while (processedBlockInfo != null && iterator.hasNext()) {
                    processedBlockInfo = iterator.next().processBlock(level, position, referencePos, blockInfo.pos, processedBlockInfo, settings);
                }

                if (processedBlockInfo != null) {
                    processedBlockInfoList.add(processedBlockInfo);
                    originalBlockInfoList.add(blockInfo);
                }
            }
        }

        for (StructureProcessor processor : settings.getProcessors()) {
            processedBlockInfoList = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings);
        }

        return processedBlockInfoList;
    }

    private void placeEntities(
        final ServerLevelAccessor level,
        final BlockPos position,
        final Mirror mirror,
        final Rotation rotation,
        final BlockPos pivot,
        final @Nullable BoundingBox boundingBox,
        final boolean finalizeEntities,
        final ProblemReporter problemReporter
    ) {
        for (StructureTemplate.StructureEntityInfo entityInfo : this.entityInfoList) {
            BlockPos blockPos = transform(entityInfo.blockPos, mirror, rotation, pivot).offset(position);
            if (boundingBox == null || boundingBox.isInside(blockPos)) {
                CompoundTag tag = entityInfo.nbt.copy();
                Vec3 relativePos = transform(entityInfo.pos, mirror, rotation, pivot);
                Vec3 pos = relativePos.add(position.getX(), position.getY(), position.getZ());
                ListTag posTag = new ListTag();
                posTag.add(DoubleTag.valueOf(pos.x));
                posTag.add(DoubleTag.valueOf(pos.y));
                posTag.add(DoubleTag.valueOf(pos.z));
                tag.put("Pos", posTag);
                tag.remove("UUID");
                createEntityIgnoreException(problemReporter, level, tag).ifPresent(entity -> {
                    float yRot = entity.rotate(rotation);
                    yRot += entity.mirror(mirror) - entity.getYRot();
                    entity.snapTo(pos.x, pos.y, pos.z, yRot, entity.getXRot());
                    entity.setYBodyRot(yRot);
                    entity.setYHeadRot(yRot);
                    if (finalizeEntities && entity instanceof Mob mob) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.STRUCTURE, null);
                    }

                    level.addFreshEntityWithPassengers(entity);
                });
            }
        }
    }

    private static Optional<Entity> createEntityIgnoreException(final ProblemReporter reporter, final ServerLevelAccessor level, final CompoundTag tag) {
        try {
            return EntityType.create(
                TagValueInput.create(reporter, level.registryAccess(), tag), level.getLevel(), new EntitySpawnRequest(EntitySpawnReason.STRUCTURE, false)
            );
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Vec3i getSize(final Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
            default -> this.size;
        };
    }

    public static BlockPos transform(final BlockPos pos, final Mirror mirror, final Rotation rotation, final BlockPos pivot) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean wasMirrored = true;
        switch (mirror) {
            case LEFT_RIGHT:
                z = -z;
                break;
            case FRONT_BACK:
                x = -x;
                break;
            default:
                wasMirrored = false;
        }

        int pivotX = pivot.getX();
        int pivotZ = pivot.getZ();

        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> new BlockPos(pivotX - pivotZ + z, y, pivotX + pivotZ - x);
            case CLOCKWISE_90 -> new BlockPos(pivotX + pivotZ - z, y, pivotZ - pivotX + x);
            case CLOCKWISE_180 -> new BlockPos(pivotX + pivotX - x, y, pivotZ + pivotZ - z);
            default -> wasMirrored ? new BlockPos(x, y, z) : pos;
        };
    }

    public static Vec3 transform(final Vec3 pos, final Mirror mirror, final Rotation rotation, final BlockPos pivot) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        boolean wasMirrored = true;
        switch (mirror) {
            case LEFT_RIGHT:
                z = 1.0 - z;
                break;
            case FRONT_BACK:
                x = 1.0 - x;
                break;
            default:
                wasMirrored = false;
        }

        int pivotX = pivot.getX();
        int pivotZ = pivot.getZ();

        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> new Vec3(pivotX - pivotZ + z, y, pivotX + pivotZ + 1 - x);
            case CLOCKWISE_90 -> new Vec3(pivotX + pivotZ + 1 - z, y, pivotZ - pivotX + x);
            case CLOCKWISE_180 -> new Vec3(pivotX + pivotX + 1 - x, y, pivotZ + pivotZ + 1 - z);
            default -> wasMirrored ? new Vec3(x, y, z) : pos;
        };
    }

    public BlockPos getZeroPositionWithTransform(final BlockPos zeroPos, final Mirror mirror, final Rotation rotation) {
        return getZeroPositionWithTransform(zeroPos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(final BlockPos zeroPos, final Mirror mirror, final Rotation rotation, int sizeX, int sizeZ) {
        sizeX--;
        sizeZ--;
        int mirrorDeltaX = mirror == Mirror.FRONT_BACK ? sizeX : 0;
        int mirrorDeltaZ = mirror == Mirror.LEFT_RIGHT ? sizeZ : 0;

        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> zeroPos.offset(mirrorDeltaZ, 0, sizeX - mirrorDeltaX);
            case CLOCKWISE_90 -> zeroPos.offset(sizeZ - mirrorDeltaZ, 0, mirrorDeltaX);
            case CLOCKWISE_180 -> zeroPos.offset(sizeX - mirrorDeltaX, 0, sizeZ - mirrorDeltaZ);
            case NONE -> zeroPos.offset(mirrorDeltaX, 0, mirrorDeltaZ);
        };
    }

    public BoundingBox getBoundingBox(final StructurePlaceSettings settings, final BlockPos position) {
        return this.getBoundingBox(position, settings.getRotation(), settings.getRotationPivot(), settings.getMirror());
    }

    public BoundingBox getBoundingBox(final BlockPos position, final Rotation rotation, final BlockPos pivot, final Mirror mirror) {
        return getBoundingBox(position, rotation, pivot, mirror, this.size);
    }

    @VisibleForTesting
    protected static BoundingBox getBoundingBox(final BlockPos position, final Rotation rotation, final BlockPos pivot, final Mirror mirror, final Vec3i size) {
        Vec3i delta = size.offset(-1, -1, -1);
        BlockPos corner1 = transform(BlockPos.ZERO, mirror, rotation, pivot);
        BlockPos corner2 = transform(BlockPos.ZERO.offset(delta), mirror, rotation, pivot);
        return BoundingBox.fromCorners(corner1, corner2).move(position);
    }

    public CompoundTag save(final CompoundTag tag) {
        if (this.palettes.isEmpty()) {
            tag.put("blocks", new ListTag());
            tag.put("palette", new ListTag());
        } else {
            List<StructureTemplate.SimplePalette> palettes = Lists.newArrayList();
            StructureTemplate.SimplePalette mainPalette = new StructureTemplate.SimplePalette();
            palettes.add(mainPalette);

            for (int p = 1; p < this.palettes.size(); p++) {
                palettes.add(new StructureTemplate.SimplePalette());
            }

            ListTag blockList = new ListTag();
            List<StructureTemplate.StructureBlockInfo> mainPaletteBlocks = this.palettes.get(0).blocks();

            for (int i = 0; i < mainPaletteBlocks.size(); i++) {
                StructureTemplate.StructureBlockInfo blockInfo = mainPaletteBlocks.get(i);
                CompoundTag blockTag = new CompoundTag();
                blockTag.put("pos", this.newIntegerList(blockInfo.pos.getX(), blockInfo.pos.getY(), blockInfo.pos.getZ()));
                int id = mainPalette.idFor(blockInfo.state);
                blockTag.putInt("state", id);
                if (blockInfo.nbt != null) {
                    blockTag.put("nbt", blockInfo.nbt);
                }

                blockList.add(blockTag);

                for (int p = 1; p < this.palettes.size(); p++) {
                    StructureTemplate.SimplePalette palette = palettes.get(p);
                    palette.addMapping(this.palettes.get(p).blocks().get(i).state, id);
                }
            }

            tag.put("blocks", blockList);
            if (palettes.size() == 1) {
                ListTag paletteList = new ListTag();

                for (BlockState state : mainPalette) {
                    paletteList.add(NbtUtils.writeBlockState(state));
                }

                tag.put("palette", paletteList);
            } else {
                ListTag paletteListList = new ListTag();

                for (StructureTemplate.SimplePalette palette : palettes) {
                    ListTag paletteList = new ListTag();

                    for (BlockState state : palette) {
                        paletteList.add(NbtUtils.writeBlockState(state));
                    }

                    paletteListList.add(paletteList);
                }

                tag.put("palettes", paletteListList);
            }
        }

        ListTag entityList = new ListTag();

        for (StructureTemplate.StructureEntityInfo entityInfo : this.entityInfoList) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.put("pos", this.newDoubleList(entityInfo.pos.x, entityInfo.pos.y, entityInfo.pos.z));
            entityTag.put("blockPos", this.newIntegerList(entityInfo.blockPos.getX(), entityInfo.blockPos.getY(), entityInfo.blockPos.getZ()));
            if (entityInfo.nbt != null) {
                entityTag.put("nbt", entityInfo.nbt);
            }

            entityList.add(entityTag);
        }

        tag.put("entities", entityList);
        tag.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        return NbtUtils.addCurrentDataVersion(tag);
    }

    public void load(final HolderGetter<Block> blockLookup, final CompoundTag tag) {
        this.palettes.clear();
        this.entityInfoList.clear();
        ListTag sizeTag = tag.getListOrEmpty("size");
        this.size = new Vec3i(sizeTag.getIntOr(0, 0), sizeTag.getIntOr(1, 0), sizeTag.getIntOr(2, 0));
        ListTag blockList = tag.getListOrEmpty("blocks");
        Optional<ListTag> paletteListList = tag.getList("palettes");
        if (paletteListList.isPresent()) {
            for (int p = 0; p < paletteListList.get().size(); p++) {
                this.loadPalette(blockLookup, paletteListList.get().getListOrEmpty(p), blockList);
            }
        } else {
            this.loadPalette(blockLookup, tag.getListOrEmpty("palette"), blockList);
        }

        tag.getListOrEmpty("entities").compoundStream().forEach(entityTag -> {
            ListTag posTag = entityTag.getListOrEmpty("pos");
            Vec3 pos = new Vec3(posTag.getDoubleOr(0, 0.0), posTag.getDoubleOr(1, 0.0), posTag.getDoubleOr(2, 0.0));
            ListTag blockPosTag = entityTag.getListOrEmpty("blockPos");
            BlockPos blockPos = new BlockPos(blockPosTag.getIntOr(0, 0), blockPosTag.getIntOr(1, 0), blockPosTag.getIntOr(2, 0));
            entityTag.getCompound("nbt").ifPresent(nbt -> this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(pos, blockPos, nbt)));
        });
    }

    private void loadPalette(final HolderGetter<Block> blockLookup, final ListTag paletteList, final ListTag blockList) {
        StructureTemplate.SimplePalette palette = new StructureTemplate.SimplePalette();

        for (int i = 0; i < paletteList.size(); i++) {
            palette.addMapping(NbtUtils.readBlockState(blockLookup, paletteList.getCompoundOrEmpty(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> fullBlockList = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> blockEntitiesList = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> otherBlocksList = Lists.newArrayList();
        blockList.compoundStream().forEach(blockTag -> {
            ListTag posTag = blockTag.getListOrEmpty("pos");
            BlockPos pos = new BlockPos(posTag.getIntOr(0, 0), posTag.getIntOr(1, 0), posTag.getIntOr(2, 0));
            BlockState state = palette.stateFor(blockTag.getIntOr("state", 0));
            CompoundTag nbt = blockTag.getCompound("nbt").orElse(null);
            StructureTemplate.StructureBlockInfo info = new StructureTemplate.StructureBlockInfo(pos, state, nbt);
            addToLists(info, fullBlockList, blockEntitiesList, otherBlocksList);
        });
        List<StructureTemplate.StructureBlockInfo> blockInfoList = buildInfoList(fullBlockList, blockEntitiesList, otherBlocksList);
        this.palettes.add(new StructureTemplate.Palette(blockInfoList));
    }

    private ListTag newIntegerList(final int... values) {
        ListTag res = new ListTag();

        for (int value : values) {
            res.add(IntTag.valueOf(value));
        }

        return res;
    }

    private ListTag newDoubleList(final double... values) {
        ListTag res = new ListTag();

        for (double value : values) {
            res.add(DoubleTag.valueOf(value));
        }

        return res;
    }

    public static JigsawBlockEntity.JointType getJointType(final CompoundTag nbt, final BlockState state) {
        return nbt.read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> getDefaultJointType(state));
    }

    public static JigsawBlockEntity.JointType getDefaultJointType(final BlockState state) {
        return JigsawBlock.getFrontFacing(state).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
    }

    public record JigsawBlockInfo(
        StructureTemplate.StructureBlockInfo info,
        JigsawBlockEntity.JointType jointType,
        Identifier name,
        ResourceKey<StructureTemplatePool> pool,
        Identifier target,
        int placementPriority,
        int selectionPriority
    ) {
        public static StructureTemplate.JigsawBlockInfo of(final StructureTemplate.StructureBlockInfo info) {
            CompoundTag nbt = Objects.requireNonNull(info.nbt(), () -> info + " nbt was null");
            return new StructureTemplate.JigsawBlockInfo(
                info,
                StructureTemplate.getJointType(nbt, info.state()),
                nbt.read("name", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
                nbt.read("pool", JigsawBlockEntity.POOL_CODEC).orElse(Pools.EMPTY),
                nbt.read("target", Identifier.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
                nbt.getIntOr("placement_priority", 0),
                nbt.getIntOr("selection_priority", 0)
            );
        }

        @Override
        public String toString() {
            return String.format(
                Locale.ROOT,
                "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
                this.info.pos,
                this.info.state,
                this.name,
                this.pool.identifier(),
                this.target,
                this.placementPriority,
                this.selectionPriority,
                this.info.nbt
            );
        }

        public StructureTemplate.JigsawBlockInfo withInfo(final StructureTemplate.StructureBlockInfo info) {
            return new StructureTemplate.JigsawBlockInfo(
                info, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority
            );
        }
    }

    public static final class Palette {
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();
        private @Nullable List<StructureTemplate.JigsawBlockInfo> cachedJigsaws;

        private Palette(final List<StructureTemplate.StructureBlockInfo> blocks) {
            this.blocks = blocks;
        }

        public List<StructureTemplate.JigsawBlockInfo> jigsaws() {
            if (this.cachedJigsaws == null) {
                this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
            }

            return this.cachedJigsaws;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks(final Block filter) {
            return this.cache.computeIfAbsent(filter, block -> this.blocks.stream().filter(b -> b.state.is(block)).collect(Collectors.toList()));
        }
    }

    private static class SimplePalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper<>(16);
        private int lastId;

        public int idFor(final BlockState state) {
            int id = this.ids.getId(state);
            if (id == -1) {
                id = this.lastId++;
                this.ids.addMapping(state, id);
            }

            return id;
        }

        public @Nullable BlockState stateFor(final int index) {
            BlockState blockState = this.ids.byId(index);
            return blockState == null ? DEFAULT_BLOCK_STATE : blockState;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(final BlockState state, final int id) {
            this.ids.addMapping(state, id);
        }
    }

    public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static class StructureEntityInfo {
        public final Vec3 pos;
        public final BlockPos blockPos;
        public final CompoundTag nbt;

        public StructureEntityInfo(final Vec3 pos, final BlockPos blockPos, final CompoundTag nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }
}