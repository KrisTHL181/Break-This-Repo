package net.minecraft.world.level.lighting;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * LightEngine — 光照引擎基类（MCRe NoiseFarlands 对象化版）
 * blockNode(long 打包) → BlockPos 对象；sectionNode(long 打包) → SectionPos 对象。
 * BFS 队列拆为节点队列 + 数据队列两个 ArrayDeque。QueueEntry 的位打包（光照等级/方向）保留。
 */
public abstract class LightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements LayerLightEventListener {
    public static final int MAX_LEVEL = 15;
    protected static final int MIN_OPACITY = 1;
    protected static final long PULL_LIGHT_IN_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(1);
    private static final int MIN_QUEUE_SIZE = 512;
    protected static final Direction[] PROPAGATION_DIRECTIONS = Direction.values();
    protected final LightChunkGetter chunkSource;
    protected final S storage;
    private final Set<BlockPos> blockNodesToCheck = new HashSet<>();
    private final ArrayDeque<BlockPos> decreaseQueueNodes = new ArrayDeque<>();
    private final ArrayDeque<Long> decreaseQueueData = new ArrayDeque<>();
    private final ArrayDeque<BlockPos> increaseQueueNodes = new ArrayDeque<>();
    private final ArrayDeque<Long> increaseQueueData = new ArrayDeque<>();
    private static final int CACHE_SIZE = 2;
    private final ChunkPos[] lastChunkPos = new ChunkPos[2];
    private final LightChunk[] lastChunk = new LightChunk[2];

    protected LightEngine(final LightChunkGetter chunkSource, final S storage) {
        this.chunkSource = chunkSource;
        this.storage = storage;
        this.clearChunkCache();
    }

    public static boolean hasDifferentLightProperties(final BlockState oldState, final BlockState newState) {
        return newState == oldState
            ? false
            : newState.getLightDampening() != oldState.getLightDampening()
                || newState.getLightEmission() != oldState.getLightEmission()
                || newState.useShapeForLightOcclusion()
                || oldState.useShapeForLightOcclusion();
    }

    public static int getLightDampeningInto(final BlockState fromState, final BlockState toState, final Direction direction, final int simpleOpacity) {
        boolean fromEmpty = isEmptyShape(fromState);
        boolean toEmpty = isEmptyShape(toState);
        if (fromEmpty && toEmpty) {
            return simpleOpacity;
        }

        VoxelShape fromShape = fromEmpty ? Shapes.empty() : fromState.getOcclusionShape();
        VoxelShape toShape = toEmpty ? Shapes.empty() : toState.getOcclusionShape();
        return Shapes.mergedFaceOccludes(fromShape, toShape, direction) ? 16 : simpleOpacity;
    }

    public static VoxelShape getOcclusionShape(final BlockState state, final Direction direction) {
        return isEmptyShape(state) ? Shapes.empty() : state.getFaceOcclusionShape(direction);
    }

    protected static boolean isEmptyShape(final BlockState state) {
        return !state.canOcclude() || !state.useShapeForLightOcclusion();
    }

    protected BlockState getState(final BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        LightChunk chunk = this.getChunk(chunkX, chunkZ);
        return chunk == null ? Blocks.BEDROCK.defaultBlockState() : chunk.getBlockState(pos);
    }

    protected int getOpacity(final BlockState state) {
        return Math.max(1, state.getLightDampening());
    }

    protected boolean shapeOccludes(final BlockState fromState, final BlockState toState, final Direction direction) {
        VoxelShape fromShape = getOcclusionShape(fromState, direction);
        VoxelShape toShape = getOcclusionShape(toState, direction.getOpposite());
        return Shapes.faceShapeOccludes(fromShape, toShape);
    }

    protected @Nullable LightChunk getChunk(final int chunkX, final int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);

        for (int i = 0; i < 2; i++) {
            if (pos.equals(this.lastChunkPos[i])) {
                return this.lastChunk[i];
            }
        }

        LightChunk chunk = this.chunkSource.getChunkForLighting(chunkX, chunkZ);

        for (int i = 1; i > 0; i--) {
            this.lastChunkPos[i] = this.lastChunkPos[i - 1];
            this.lastChunk[i] = this.lastChunk[i - 1];
        }

        this.lastChunkPos[0] = pos;
        this.lastChunk[0] = chunk;
        return chunk;
    }

    private void clearChunkCache() {
        Arrays.fill(this.lastChunkPos, null);
        Arrays.fill(this.lastChunk, null);
    }

    @Override
    public void checkBlock(final BlockPos pos) {
        this.blockNodesToCheck.add(pos);
    }

    public void queueSectionData(final SectionPos pos, final @Nullable DataLayer data) {
        this.storage.queueSectionData(pos, data);
    }

    public void retainData(final ChunkPos pos, final boolean retain) {
        this.storage.retainData(SectionPos.of((int)pos.x(), 0, (int)pos.z()), retain);
    }

    @Override
    public void updateSectionStatus(final SectionPos pos, final boolean sectionEmpty) {
        this.storage.updateSectionStatus(pos, sectionEmpty);
    }

    @Override
    public void setLightEnabled(final ChunkPos pos, final boolean enable) {
        this.storage.setLightEnabled(SectionPos.of((int)pos.x(), 0, (int)pos.z()), enable);
    }

    @Override
    public int runLightUpdates() {
        for (BlockPos pos : this.blockNodesToCheck) {
            this.checkNode(pos);
        }

        this.blockNodesToCheck.clear();
        int count = 0;
        count += this.propagateDecreases();
        count += this.propagateIncreases();
        this.clearChunkCache();
        this.storage.markNewInconsistencies(this);
        this.storage.swapSectionMap();
        return count;
    }

    private int propagateIncreases() {
        int count;
        for (count = 0; !this.increaseQueueNodes.isEmpty(); count++) {
            BlockPos fromNode = this.increaseQueueNodes.poll();
            long increaseData = this.increaseQueueData.poll();
            if (!this.storage.storingLightForSection(SectionPos.of(fromNode))) {
                // far lands：任务入队后 section 光照数据已被移除（区块卸载/状态变化），丢弃孤儿任务
                continue;
            }
            int fromLevel = this.storage.getStoredLevel(fromNode);
            int fromTargetLevel = LightEngine.QueueEntry.getFromLevel(increaseData);
            if (LightEngine.QueueEntry.isIncreaseFromEmission(increaseData) && fromLevel < fromTargetLevel) {
                this.storage.setStoredLevel(fromNode, fromTargetLevel);
                fromLevel = fromTargetLevel;
            }

            if (fromLevel == fromTargetLevel) {
                this.propagateIncrease(fromNode, increaseData, fromLevel);
            }
        }

        return count;
    }

    private int propagateDecreases() {
        int count;
        for (count = 0; !this.decreaseQueueNodes.isEmpty(); count++) {
            BlockPos fromNode = this.decreaseQueueNodes.poll();
            long decreaseData = this.decreaseQueueData.poll();
            if (!this.storage.storingLightForSection(SectionPos.of(fromNode))) {
                // far lands：任务入队后 section 光照数据已被移除，丢弃孤儿任务
                continue;
            }
            this.propagateDecrease(fromNode, decreaseData);
        }

        return count;
    }

    protected void enqueueDecrease(final BlockPos fromNode, final long decreaseData) {
        this.decreaseQueueNodes.add(fromNode);
        this.decreaseQueueData.add(decreaseData);
    }

    protected void enqueueIncrease(final BlockPos fromNode, final long increaseData) {
        this.increaseQueueNodes.add(fromNode);
        this.increaseQueueData.add(increaseData);
    }

    @Override
    public boolean hasLightWork() {
        return this.storage.hasInconsistencies()
            || !this.blockNodesToCheck.isEmpty()
            || !this.decreaseQueueNodes.isEmpty()
            || !this.increaseQueueNodes.isEmpty();
    }

    @Override
    public @Nullable DataLayer getDataLayerData(final SectionPos pos) {
        return this.storage.getDataLayerData(pos);
    }

    @Override
    public int getLightValue(final BlockPos pos) {
        return this.storage.getLightValue(pos);
    }

    public String getDebugData(final SectionPos sectionNode) {
        return this.getDebugSectionType(sectionNode).display();
    }

    public LayerLightSectionStorage.SectionType getDebugSectionType(final SectionPos sectionNode) {
        return this.storage.getDebugSectionType(sectionNode);
    }

    protected abstract void checkNode(BlockPos blockNode);

    protected abstract void propagateIncrease(BlockPos fromNode, long increaseData, int fromLevel);

    protected abstract void propagateDecrease(BlockPos fromNode, long decreaseData);

    public static class QueueEntry {
        private static final int FROM_LEVEL_BITS = 4;
        private static final int DIRECTION_BITS = 6;
        private static final long LEVEL_MASK = 15L;
        private static final long DIRECTIONS_MASK = 1008L;
        private static final long FLAG_FROM_EMPTY_SHAPE = 1024L;
        private static final long FLAG_INCREASE_FROM_EMISSION = 2048L;

        public static long decreaseSkipOneDirection(final int oldFromLevel, final Direction skipDirection) {
            long decreaseData = withoutDirection(1008L, skipDirection);
            return withLevel(decreaseData, oldFromLevel);
        }

        public static long decreaseAllDirections(final int oldFromLevel) {
            return withLevel(1008L, oldFromLevel);
        }

        public static long increaseLightFromEmission(final int newFromLevel, final boolean fromEmptyShape) {
            long increaseData = 1008L;
            increaseData |= 2048L;
            if (fromEmptyShape) {
                increaseData |= 1024L;
            }

            return withLevel(increaseData, newFromLevel);
        }

        public static long increaseSkipOneDirection(final int newFromLevel, final boolean fromEmptyShape, final Direction skipDirection) {
            long increaseData = withoutDirection(1008L, skipDirection);
            if (fromEmptyShape) {
                increaseData |= 1024L;
            }

            return withLevel(increaseData, newFromLevel);
        }

        public static long increaseOnlyOneDirection(final int newFromLevel, final boolean fromEmptyShape, final Direction direction) {
            long increaseData = 0L;
            if (fromEmptyShape) {
                increaseData |= 1024L;
            }

            increaseData = withDirection(increaseData, direction);
            return withLevel(increaseData, newFromLevel);
        }

        public static long increaseSkySourceInDirections(final boolean down, final boolean north, final boolean south, final boolean west, final boolean east) {
            long increaseData = withLevel(0L, 15);
            if (down) {
                increaseData = withDirection(increaseData, Direction.DOWN);
            }

            if (north) {
                increaseData = withDirection(increaseData, Direction.NORTH);
            }

            if (south) {
                increaseData = withDirection(increaseData, Direction.SOUTH);
            }

            if (west) {
                increaseData = withDirection(increaseData, Direction.WEST);
            }

            if (east) {
                increaseData = withDirection(increaseData, Direction.EAST);
            }

            return increaseData;
        }

        public static int getFromLevel(final long entry) {
            return (int)(entry & 15L);
        }

        public static boolean isFromEmptyShape(final long entry) {
            return (entry & 1024L) != 0L;
        }

        public static boolean isIncreaseFromEmission(final long entry) {
            return (entry & 2048L) != 0L;
        }

        public static boolean shouldPropagateInDirection(final long entry, final Direction direction) {
            return (entry & 1L << direction.ordinal() + 4) != 0L;
        }

        private static long withLevel(final long entry, final int level) {
            return entry & -16L | level & 15L;
        }

        private static long withDirection(final long entry, final Direction direction) {
            return entry | 1L << direction.ordinal() + 4;
        }

        private static long withoutDirection(final long entry, final Direction direction) {
            return entry & ~(1L << direction.ordinal() + 4);
        }
    }
}
