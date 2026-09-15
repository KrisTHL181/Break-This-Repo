package net.minecraft.server.level;

public enum FullChunkStatus {
   INACCESSIBLE,
   FULL,
   BLOCK_TICKING,
   ENTITY_TICKING;

   public boolean isOrAfter(FullChunkStatus p_287607_) {
      return this.ordinal() >= p_287607_.ordinal();
   }
}
