package net.minecraft.client.gui.components.debug;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryNoop implements DebugScreenEntry {
    private final boolean isAllowedWithReducedDebugInfo;

    public DebugEntryNoop() {
        this(false);
    }

    public DebugEntryNoop(final boolean isAllowedWithReducedDebugInfo) {
        this.isAllowedWithReducedDebugInfo = isAllowedWithReducedDebugInfo;
    }

    @Override
    public void display(
        final DebugScreenDisplayer displayer,
        final @Nullable Level serverOrClientLevel,
        final @Nullable LevelChunk clientChunk,
        final @Nullable LevelChunk serverChunk
    ) {
    }

    @Override
    public boolean isAllowed(final boolean reducedDebugInfo) {
        return this.isAllowedWithReducedDebugInfo || !reducedDebugInfo;
    }

    @Override
    public DebugEntryCategory category() {
        return DebugEntryCategory.RENDERER;
    }
}