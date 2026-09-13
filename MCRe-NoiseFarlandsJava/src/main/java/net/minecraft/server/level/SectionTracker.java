package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

/**
 * SectionTracker — 区块节追踪器（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（SectionPos.asLong），本版以 SectionPos 对象为节点，哨兵为 null。
 */
public abstract class SectionTracker extends DynamicGraphMinFixedPoint<SectionPos> {
    protected SectionTracker(final int levelCount, final int minQueueSize, final int minMapSize) {
        super(levelCount, minQueueSize, minMapSize);
    }

    @Override
    protected void checkNeighborsAfterUpdate(final SectionPos node, final int level, final boolean onlyDecrease) {
        if (!onlyDecrease || level < this.levelCount - 2) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        SectionPos neighbor = node.offset(offsetX, offsetY, offsetZ);
                        if (!neighbor.equals(node)) {
                            this.checkNeighbor(node, neighbor, level, onlyDecrease);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected int getComputedLevel(final SectionPos node, final SectionPos knownParent, final int knownLevelFromParent) {
        int computedLevel = knownLevelFromParent;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    SectionPos neighbor = node.offset(offsetX, offsetY, offsetZ);
                    if (neighbor.equals(node)) {
                        neighbor = null;
                    }

                    if (!java.util.Objects.equals(neighbor, knownParent)) {
                        int costFromNeighbor = this.computeLevelFromNeighbor(neighbor, node, this.getLevel(neighbor));
                        if (computedLevel > costFromNeighbor) {
                            computedLevel = costFromNeighbor;
                        }

                        if (computedLevel == 0) {
                            return computedLevel;
                        }
                    }
                }
            }
        }

        return computedLevel;
    }

    @Override
    protected int computeLevelFromNeighbor(final SectionPos from, final SectionPos to, final int fromLevel) {
        return this.isSource(from) ? this.getLevelFromSource(to) : fromLevel + 1;
    }

    protected abstract int getLevelFromSource(SectionPos to);

    public void update(final SectionPos node, final int newLevelFrom, final boolean onlyDecreased) {
        this.checkEdge(null, node, newLevelFrom, onlyDecreased);
    }
}
