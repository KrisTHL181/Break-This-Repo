package net.minecraft.client.renderer.chunk;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SectionCopy {
    private final Map<BlockPos, BlockEntity> blockEntities;
    private final @Nullable PalettedContainer<BlockState> section;
    private final boolean debug;
    private final LevelHeightAccessor levelHeightAccessor;

    public SectionCopy(final LevelChunk levelChunk, final int sectionIndex) {
        this.levelHeightAccessor = levelChunk;
        this.debug = levelChunk.getLevel().isDebug();
        this.blockEntities = ImmutableMap.copyOf(levelChunk.getBlockEntities());
        if (levelChunk instanceof EmptyLevelChunk) {
            this.section = null;
        } else {
            LevelChunkSection[] sections = levelChunk.getSections();
            if (sectionIndex >= 0 && sectionIndex < sections.length) {
                LevelChunkSection levelChunkSection = sections[sectionIndex];
                this.section = levelChunkSection.hasOnlyAir() ? null : levelChunkSection.getStates().copy();
            } else {
                this.section = null;
            }
        }
    }

    public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
        return this.blockEntities.get(pos);
    }

    public BlockState getBlockState(final BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.debug) {
            BlockState blockState = null;
            if (y == 60) {
                blockState = Blocks.BARRIER.defaultBlockState();
            }

            if (y == 70) {
                blockState = DebugLevelSource.getBlockStateFor(x, z);
            }

            return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
        } else {
            if (this.section == null) {
                return Blocks.AIR.defaultBlockState();
            }

            try {
                return this.section.get(x & 15, y & 15, z & 15);
            } catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Getting block state");
                CrashReportCategory category = report.addCategory("Block being got");
                category.setDetail("Location", () -> CrashReportCategory.formatLocation(this.levelHeightAccessor, x, y, z));
                throw new ReportedException(report);
            }
        }
    }
}