package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record ChunkIdentification(String level, String dimension, int x, int z) {
   public static ChunkIdentification from(RecordedEvent p_327718_) {
      return new ChunkIdentification(
         p_327718_.getString("level"), p_327718_.getString("dimension"), p_327718_.getInt("chunkPosX"), p_327718_.getInt("chunkPosZ")
      );
   }
}
