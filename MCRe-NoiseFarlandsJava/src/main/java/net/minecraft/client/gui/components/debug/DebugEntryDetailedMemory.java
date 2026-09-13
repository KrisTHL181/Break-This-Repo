package net.minecraft.client.gui.components.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryDetailedMemory implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("memory");
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    @Override
    public void display(
        final DebugScreenDisplayer displayer,
        final @Nullable Level serverOrClientLevel,
        final @Nullable LevelChunk clientChunk,
        final @Nullable LevelChunk serverChunk
    ) {
        displayer.addToGroup(
            GROUP,
            List.of(printMemoryUsage(this.memoryBean.getHeapMemoryUsage(), "heap"), printMemoryUsage(this.memoryBean.getNonHeapMemoryUsage(), "non-heap"))
        );
    }

    private static long bytesToMebibytes(final long used) {
        return used / 1024L / 1024L;
    }

    private static String printMemoryUsage(final MemoryUsage memoryUsage, final String type) {
        return String.format(
            Locale.ROOT,
            "Memory (%s): i=%03dMiB u=%03dMiB c=%03dMiB m=%03dMiB",
            type,
            bytesToMebibytes(memoryUsage.getInit()),
            bytesToMebibytes(memoryUsage.getUsed()),
            bytesToMebibytes(memoryUsage.getCommitted()),
            bytesToMebibytes(memoryUsage.getMax())
        );
    }

    @Override
    public boolean isAllowed(final boolean reducedDebugInfo) {
        return true;
    }
}