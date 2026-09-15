package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestTimeoutException extends GameTestException {
   protected final Component message;

   public GameTestTimeoutException(Component p_392190_) {
      super(p_392190_.getString());
      this.message = p_392190_;
   }

   @Override
   public Component getDescription() {
      return this.message;
   }
}
