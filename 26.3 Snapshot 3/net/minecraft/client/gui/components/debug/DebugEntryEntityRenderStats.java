package net.minecraft.client.gui.components.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryEntityRenderStats implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      String stats = Minecraft.getInstance().levelExtractor.entityStatistics();
      if (stats != null) {
         displayer.addLine(stats);
      }
   }

   @Override
   public boolean isAllowed(final boolean reducedDebugInfo) {
      return true;
   }
}
