package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class UnknownGameTestException extends GameTestException {
   private final Throwable reason;

   public UnknownGameTestException(Throwable p_395861_) {
      super(p_395861_.getMessage());
      this.reason = p_395861_;
   }

   @Override
   public Component getDescription() {
      return Component.translatable("test.error.unknown", this.reason.getMessage());
   }
}
