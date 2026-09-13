package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public interface LevelHeightAccessor {
    int getHeight();

    int getMinY();

    default int getMaxY() {
        return this.getMinY() + this.getHeight() - 1;
    }

    // 🔧 MCRe：窗口钳制——section 相关 API 全局封顶到 WINDOW_SECTIONS，
    // 防止超高世界（height→±21.47亿）在 ChunkHolder/ChunkAccess 分配 1.34亿 数组 OOM。
    // 区块内部用 allSections 无限存储（见 WindowedChunk），Level 上报窗口大小即可。
    int WINDOW_SECTIONS = 34;

    default int getSectionsCount() {
        return Math.min(this.getMaxSectionY() - this.getMinSectionY() + 1, WINDOW_SECTIONS);
    }

    default int getMinSectionY() {
        return SectionPos.blockToSectionCoord(this.getMinY());
    }

    default int getMaxSectionY() {
        // 与 getMinSectionY 对齐：窗口最大 sectionY = minSectionY + WINDOW_SECTIONS - 1
        return this.getMinSectionY() + Math.min(this.getHeight() >> 4, WINDOW_SECTIONS) - 1;
    }

    default boolean isInsideBuildHeight(final BlockPos pos) {
        return this.isInsideBuildHeight(pos.getY());
    }

    default boolean isInsideBuildHeight(final int blockY) {
        return blockY >= this.getMinY() && blockY <= this.getMaxY();
    }

    default boolean isOutsideBuildHeight(final BlockPos pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    default boolean isOutsideBuildHeight(final int blockY) {
        return blockY < this.getMinY() || blockY > this.getMaxY();
    }

    default int getSectionIndex(final int blockY) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(blockY));
    }

    default int getSectionIndexFromSectionY(final int sectionY) {
        // 🔧 MCRe：窗口钳制，防止极端 sectionY 越界 WINDOW_SECTIONS 数组
        int raw = sectionY - this.getMinSectionY();
        if (raw < 0) {
            return 0;
        }
        int max = this.getSectionsCount() - 1;
        return raw > max ? max : raw;
    }

    default int getSectionYFromSectionIndex(final int sectionIndex) {
        // 🔧 MCRe：窗口钳制逆映射，保持与 getSectionIndexFromSectionY 一致
        return this.getMinSectionY() + sectionIndex;
    }

    static LevelHeightAccessor create(final int minY, final int height) {
        return new LevelHeightAccessor() {
            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public int getMinY() {
                return minY;
            }
        };
    }
}