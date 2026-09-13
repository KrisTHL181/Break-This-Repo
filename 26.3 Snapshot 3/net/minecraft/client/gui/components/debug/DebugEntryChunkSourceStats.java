package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryChunkSourceStats implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         displayer.addLine(minecraft.level.gatherChunkSourceStats());
      }

      if (serverOrClientLevel != null && serverOrClientLevel != minecraft.level) {
         displayer.addLine(serverOrClientLevel.gatherChunkSourceStats());
      }
   }

   @Override
   public boolean isAllowed(final boolean reducedDebugInfo) {
      return true;
   }
}
