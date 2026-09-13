package net.minecraft.world.level.entity;

import net.minecraft.server.level.FullChunkStatus;

public enum Visibility {
   HIDDEN(false, false),
   TRACKED(true, false),
   TICKING(true, true);

   private final boolean accessible;
   private final boolean ticking;

   Visibility(final boolean p_157689_, final boolean p_157690_) {
      this.accessible = p_157689_;
      this.ticking = p_157690_;
   }

   public boolean isTicking() {
      return this.ticking;
   }

   public boolean isAccessible() {
      return this.accessible;
   }

   public static Visibility fromFullChunkStatus(FullChunkStatus p_287651_) {
      if (p_287651_.isOrAfter(FullChunkStatus.ENTITY_TICKING)) {
         return TICKING;
      } else {
         return p_287651_.isOrAfter(FullChunkStatus.FULL) ? TRACKED : HIDDEN;
      }
   }
}
