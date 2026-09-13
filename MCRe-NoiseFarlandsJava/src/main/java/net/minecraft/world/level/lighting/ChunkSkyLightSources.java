package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ChunkSkyLightSources — 天空光源最低 Y 记录（MCRe NoiseFarlands 对象化版）
 *
 * <p>🔧 修复：原版用 SimpleBitStorage 按「value - minY」相对值存列最低光源 Y，
 * 超高世界（±21.47 亿）span 达 42.9 亿 → ceillog2 位宽膨胀且相对值溢出 int。
 * 改为 Int2IntOpenHashMap 直接存绝对 sourceY（列号 0..255 为键），
 * 未设置 = minY（无光源），消除高度敏感性。
 */
public class ChunkSkyLightSources {
    private static final int SIZE = 16;
    public static final int NEGATIVE_INFINITY = Integer.MIN_VALUE;
    private final int minY;
    /** 🔧 MCRe：列号 → 绝对 sourceY（替代 BitStorage 相对值，支持任意高度） */
    private final Int2IntOpenHashMap sourceMap = new Int2IntOpenHashMap();
    private final BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos mutablePos2 = new BlockPos.MutableBlockPos();

    public ChunkSkyLightSources(final LevelHeightAccessor level) {
        this.minY = level.getMinY() - 1;
    }

    public void fillFrom(final ChunkAccess chunk) {
        int maxSectionIndex = chunk.getHighestFilledSectionIndex();
        if (maxSectionIndex == -1) {
            this.fill(this.minY);
        } else {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int initialEdgeY = Math.max(this.findLowestSourceY(chunk, maxSectionIndex, x, z), this.minY);
                    this.set(index(x, z), initialEdgeY);
                }
            }
        }
    }

    private int findLowestSourceY(final ChunkAccess chunk, final int topSectionIndex, final int x, final int z) {
        int topY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(topSectionIndex) + 1);
        BlockPos.MutableBlockPos topPos = this.mutablePos1.set(x, topY, z);
        BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset(topPos, Direction.DOWN);
        BlockState topState = Blocks.AIR.defaultBlockState();

        for (int sectionIndex = topSectionIndex; sectionIndex >= 0; sectionIndex--) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section.hasOnlyAir()) {
                topState = Blocks.AIR.defaultBlockState();
                int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                topPos.setY(SectionPos.sectionToBlockCoord(sectionY));
                bottomPos.setY(topPos.getY() - 1);
            } else {
                for (int y = 15; y >= 0; y--) {
                    BlockState bottomState = section.getBlockState(x, y, z);
                    if (isEdgeOccluded(topState, bottomState)) {
                        return topPos.getY();
                    }

                    topState = bottomState;
                    topPos.set(bottomPos);
                    bottomPos.move(Direction.DOWN);
                }
            }
        }

        return this.minY;
    }

    public boolean update(final BlockGetter level, final int x, final int y, final int z) {
        int upperEdgeY = y + 1;
        int index = index(x, z);
        int currentLowestSourceY = this.get(index);
        if (upperEdgeY < currentLowestSourceY) {
            return false;
        }

        BlockPos topPos = this.mutablePos1.set(x, y + 1, z);
        BlockState topState = level.getBlockState(topPos);
        BlockPos middlePos = this.mutablePos2.set(x, y, z);
        BlockState middleState = level.getBlockState(middlePos);
        if (this.updateEdge(level, index, currentLowestSourceY, topPos, topState, middlePos, middleState)) {
            return true;
        }

        BlockPos bottomPos = this.mutablePos1.set(x, y - 1, z);
        BlockState bottomState = level.getBlockState(bottomPos);
        return this.updateEdge(level, index, currentLowestSourceY, middlePos, middleState, bottomPos, bottomState);
    }

    private boolean updateEdge(
        final BlockGetter level,
        final int index,
        final int oldTopEdgeY,
        final BlockPos topPos,
        final BlockState topState,
        final BlockPos bottomPos,
        final BlockState bottomState
    ) {
        int checkedEdgeY = topPos.getY();
        if (isEdgeOccluded(topState, bottomState)) {
            if (checkedEdgeY > oldTopEdgeY) {
                this.set(index, checkedEdgeY);
                return true;
            }
        } else if (checkedEdgeY == oldTopEdgeY) {
            this.set(index, this.findLowestSourceBelow(level, bottomPos, bottomState));
            return true;
        }

        return false;
    }

    private int findLowestSourceBelow(final BlockGetter level, final BlockPos startPos, final BlockState startState) {
        BlockPos.MutableBlockPos topPos = this.mutablePos1.set(startPos);
        BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset(startPos, Direction.DOWN);
        BlockState topState = startState;

        while (bottomPos.getY() >= this.minY) {
            BlockState bottomState = level.getBlockState(bottomPos);
            if (isEdgeOccluded(topState, bottomState)) {
                return topPos.getY();
            }

            topState = bottomState;
            topPos.set(bottomPos);
            bottomPos.move(Direction.DOWN);
        }

        return this.minY;
    }

    private static boolean isEdgeOccluded(final BlockState topState, final BlockState bottomState) {
        if (bottomState.getLightDampening() != 0) {
            return true;
        }

        VoxelShape topShape = LightEngine.getOcclusionShape(topState, Direction.DOWN);
        VoxelShape bottomShape = LightEngine.getOcclusionShape(bottomState, Direction.UP);
        return Shapes.faceShapeOccludes(topShape, bottomShape);
    }

    public int getLowestSourceY(final int x, final int z) {
        int value = this.get(index(x, z));
        return this.extendSourcesBelowWorld(value);
    }

    public int getHighestLowestSourceY() {
        int maxValue = this.minY;
        for (int v : this.sourceMap.values()) {
            if (v > maxValue) {
                maxValue = v;
            }
        }
        return this.extendSourcesBelowWorld(maxValue);
    }

    private void fill(final int lowestSourceY) {
        if (lowestSourceY == this.minY) {
            this.sourceMap.clear();
        } else {
            this.sourceMap.replaceAll((k, v) -> lowestSourceY);
        }
    }

    private void set(final int index, final int value) {
        this.sourceMap.put(index, value);
    }

    private int get(final int index) {
        return this.sourceMap.containsKey(index) ? this.sourceMap.get(index) : this.minY;
    }

    private int extendSourcesBelowWorld(final int value) {
        return value == this.minY ? Integer.MIN_VALUE : value;
    }

    private static int index(final int x, final int z) {
        return x + z * 16;
    }
}