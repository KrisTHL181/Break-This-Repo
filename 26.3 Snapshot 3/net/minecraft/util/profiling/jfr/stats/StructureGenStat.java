package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.world.level.ChunkPos;

public record StructureGenStat(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements TimedStat {
   public static StructureGenStat from(final RecordedEvent event) {
      return new StructureGenStat(
         event.getDuration(),
         new ChunkPos(event.getInt("chunkPosX"), event.getInt("chunkPosX")),
         event.getString("structure"),
         event.getString("level"),
         event.getBoolean("success")
      );
   }
}
