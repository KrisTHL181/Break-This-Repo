package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record ChunkIdentification(String level, String dimension, int x, int z) {
   public static ChunkIdentification from(final RecordedEvent event) {
      return new ChunkIdentification(event.getString("level"), event.getString("dimension"), event.getInt("chunkPosX"), event.getInt("chunkPosZ"));
   }
}
