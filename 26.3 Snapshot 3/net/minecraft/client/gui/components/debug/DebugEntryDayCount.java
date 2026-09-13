package net.minecraft.client.gui.components.debug;

import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.timeline.Timelines;
import org.jspecify.annotations.Nullable;

public class DebugEntryDayCount implements DebugScreenEntry {
   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      if (serverOrClientLevel != null) {
         ClockManager clockManager = serverOrClientLevel.clockManager();
         serverOrClientLevel.registryAccess()
            .get(Timelines.OVERWORLD_DAY)
            .ifPresent(timeline -> displayer.addLine("Day #" + timeline.value().getPeriodCount(clockManager)));
      }
   }
}
