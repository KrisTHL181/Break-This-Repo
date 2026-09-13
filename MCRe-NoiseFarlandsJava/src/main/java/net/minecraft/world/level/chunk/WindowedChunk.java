package net.minecraft.world.level.chunk;

import java.util.Map;
import net.minecraft.world.level.LevelHeightAccessor;

/**
 * 🔧 MCRe：窗口化区块接口（参考 inf_farlands 的 WindowedChunk）。
 *
 * <p>区块持有无限 Y 的 section 仓库（allSections —— 键为绝对 sectionY），
 * 对外只展示一个固定 34-section 窗口视图（windowSections + windowMinY）。
 * 这是支持任意高度世界（±21.47 亿）的前提——区块不再按全高分配 section 数组。
 *
 * @author MCRe Ultimate Scaler
 */
public interface WindowedChunk {

    /** 中心下方半径（下界 = center - 17） */
    int WINDOW_HALF_BELOW = 17;

    /** 中心上方半径（上界 = center + 16） */
    int WINDOW_HALF_ABOVE = 16;

    /** 窗口底部 sectionY */
    int getWindowMinY();

    /** 窗口顶部 sectionY */
    int getWindowMaxY();

    /** 重建窗口视图为 [sectionYMin, sectionYMax] */
    void buildWindow(int sectionYMin, int sectionYMax);

    /** 窗口滑到以 centerSectionY 为中心（34 section） */
    default void moveWindowTo(int centerSectionY) {
        this.buildWindow(centerSectionY - WINDOW_HALF_BELOW, centerSectionY + WINDOW_HALF_ABOVE);
    }

    /** 确保 sectionY 可见：窗口内不动，窗口外将窗口滑到该点 */
    default void expandWindowTo(int sectionY) {
        if (sectionY < this.getWindowMinY() || sectionY > this.getWindowMaxY()) {
            this.moveWindowTo(sectionY);
        }
    }

    /** 窗口相对索引 → 绝对 sectionY */
    int windowSectionYFromIndex(int index);

    /** 绝对 sectionY → 窗口相对索引 */
    int windowSectionIndexFromY(int sectionY);

    /** 无限 Y 的 section 仓库 */
    Map<Integer, LevelChunkSection> windowedAllSections();

    /** 区块的真实 LevelHeightAccessor（维度范围，非窗口感知） */
    LevelHeightAccessor levelHeightAccessor();
}