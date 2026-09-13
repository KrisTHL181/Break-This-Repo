package com.mojang.renderpearl.backend.opengl;

import com.mojang.renderpearl.api.commands.GpuFence;

public class GlFence implements GpuFence {
   private final GlCommandEncoder encoder;
   private final long submitIndex;
   private boolean closedOrCompleted;

   GlFence(final GlCommandEncoder encoder) {
      this.encoder = encoder;
      this.submitIndex = encoder.currentSubmitIndex();
   }

   @Override
   public void close() {
      this.closedOrCompleted = true;
   }

   @Override
   public boolean awaitCompletion(final long timeoutNS) {
      if (this.closedOrCompleted) {
         return true;
      }

      this.closedOrCompleted = this.encoder.awaitSubmit(this.submitIndex, timeoutNS);
      return this.closedOrCompleted;
   }
}
