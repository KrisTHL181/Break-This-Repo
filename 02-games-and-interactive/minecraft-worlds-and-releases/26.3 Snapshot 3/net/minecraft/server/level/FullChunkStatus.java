package net.minecraft.server.level;

public enum FullChunkStatus {
   INACCESSIBLE,
   FULL,
   BLOCK_TICKING,
   ENTITY_TICKING;

   public boolean isOrAfter(final FullChunkStatus step) {
      return this.ordinal() >= step.ordinal();
   }
}
