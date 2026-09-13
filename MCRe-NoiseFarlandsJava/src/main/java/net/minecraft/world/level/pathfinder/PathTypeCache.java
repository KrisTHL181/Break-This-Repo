package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jspecify.annotations.Nullable;

/**
 * PathTypeCache — 路径类型缓存（MCRe NoiseFarlands 对象化版）
 * 原版以 BlockPos.asLong 打包为缓存键，本版以 BlockPos 对象为键。
 */
public class PathTypeCache {
    private static final int SIZE = 4096;
    private static final int MASK = 4095;
    private final BlockPos[] positions = new BlockPos[4096];
    private final PathType[] pathTypes = new PathType[4096];

    public PathType getOrCompute(final BlockGetter level, final BlockPos pos) {
        int index = index(pos);
        PathType cachedPathType = this.get(index, pos);
        return cachedPathType != null ? cachedPathType : this.compute(level, pos, index, pos);
    }

    private @Nullable PathType get(final int index, final BlockPos key) {
        return key.equals(this.positions[index]) ? this.pathTypes[index] : null;
    }

    private PathType compute(final BlockGetter level, final BlockPos pos, final int index, final BlockPos key) {
        PathType pathType = WalkNodeEvaluator.getPathTypeFromState(level, pos);
        this.positions[index] = key;
        this.pathTypes[index] = pathType;
        return pathType;
    }

    public void invalidate(final BlockPos pos) {
        int index = index(pos);
        if (pos.equals(this.positions[index])) {
            this.pathTypes[index] = null;
        }
    }

    private static int index(final BlockPos pos) {
        return (int)HashCommon.mix(pos.hashCode()) & MASK;
    }
}
