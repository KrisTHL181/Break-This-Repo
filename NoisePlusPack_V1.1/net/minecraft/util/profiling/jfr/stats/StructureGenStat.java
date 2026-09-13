package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.world.level.ChunkPos;

public record StructureGenStat(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements TimedStat {
   public static StructureGenStat from(RecordedEvent p_378817_) {
      return new StructureGenStat(
         p_378817_.getDuration(),
         new ChunkPos(p_378817_.getInt("chunkPosX"), p_378817_.getInt("chunkPosX")),
         p_378817_.getString("structure"),
         p_378817_.getString("level"),
         p_378817_.getBoolean("success")
      );
   }
}
