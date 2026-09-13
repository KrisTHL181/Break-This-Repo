package net.minecraft.client.gui.components.debug;

import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryVersion implements DebugScreenEntry {
    @Override
    public void display(
        final DebugScreenDisplayer displayer, final @Nullable Level level, final @Nullable LevelChunk clientChunk, final @Nullable LevelChunk serverChunk
    ) {
        displayer.addPriorityLine(
            "Minecraft - MCRe NoiseFarlandsJava "
                + SharedConstants.getCurrentVersion().name()
                + " ("
                + Minecraft.getInstance().getLaunchedVersion()
                + "/"
                + ClientBrandRetriever.getClientModName()
                + ")"
        );
    }

    @Override
    public boolean isAllowed(final boolean reducedDebugInfo) {
        return true;
    }
}