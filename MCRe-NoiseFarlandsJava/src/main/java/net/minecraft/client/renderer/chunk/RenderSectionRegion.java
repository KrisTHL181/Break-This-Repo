package net.minecraft.client.renderer.chunk;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RenderSectionRegion implements BlockAndTintGetter {
    public static final int RADIUS = 1;
    public static final int SIZE = 3;
    private final int minSectionX;
    private final int minSectionY;
    private final int minSectionZ;
    private final SectionCopy[] sections;
    private final ClientLevel level;
    private final CardinalLighting cardinalLighting;
    private final LevelLightEngine lightEngine;

    public RenderSectionRegion(final ClientLevel level, final int minSectionX, final int minSectionY, final int minSectionZ, final SectionCopy[] sections) {
        this.level = level;
        this.minSectionX = minSectionX;
        this.minSectionY = minSectionY;
        this.minSectionZ = minSectionZ;
        this.sections = sections;
        this.cardinalLighting = level.cardinalLighting();
        this.lightEngine = level.getLightEngine();
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return this.getSectionRelative(
                this.relativeSection(pos.getX(), this.minSectionX),
                this.relativeSection(pos.getY(), this.minSectionY),
                this.relativeSection(pos.getZ(), this.minSectionZ)
            )
            .getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.getSectionRelative(
                this.relativeSection(pos.getX(), this.minSectionX),
                this.relativeSection(pos.getY(), this.minSectionY),
                this.relativeSection(pos.getZ(), this.minSectionZ)
            )
            .getBlockState(pos)
            .getFluidState();
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return this.cardinalLighting;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
        return this.getSectionRelative(
                this.relativeSection(pos.getX(), this.minSectionX),
                this.relativeSection(pos.getY(), this.minSectionY),
                this.relativeSection(pos.getZ(), this.minSectionZ)
            )
            .getBlockEntity(pos);
    }

    private SectionCopy getSectionRelative(final int relSectionX, final int relSectionY, final int relSectionZ) {
        return this.sections[relSectionX + relSectionY * 3 + relSectionZ * 9];
    }

    /**
     * far lands：世界方块坐标（int，真实值 mod 2^32）→ 相对本 region 基准的 section 偏移。
     * 原版用 blockCoord >> 4（算术右移），但坐标越过 2^31 时 int 溢出为负，
     * 算术右移得到错误 section。这里用 mod 2^32 无符号恢复：真实差 ∈ [-16, 47]（region 3×3 范围）。
     */
    private int relativeSection(final int posCoord, final int minSectionCoord) {
        long diff = (Integer.toUnsignedLong(posCoord) - ((long)minSectionCoord << 4 & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
        long signedDiff = diff >= 0x80000000L ? diff - 0x100000000L : diff;
        return (int)(signedDiff >> 4);
    }

    @Override
    public int getBlockTint(final BlockPos pos, final ColorResolver resolver) {
        return this.level.getBlockTint(pos, resolver);
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    public static int index(final int minSectionX, final int minSectionY, final int minSectionZ, final int sectionX, final int sectionY, final int sectionZ) {
        return sectionX - minSectionX + (sectionY - minSectionY) * 3 + (sectionZ - minSectionZ) * 3 * 3;
    }
}