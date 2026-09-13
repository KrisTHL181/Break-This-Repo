package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestAssertException extends GameTestException {
   protected final Component message;
   protected final int tick;

   public GameTestAssertException(final Component message, final int tick) {
      super(message.getString());
      this.message = message;
      this.tick = tick;
   }

   @Override
   public Component getDescription() {
      return Component.translatable("test.error.tick", this.message, this.tick);
   }

   @Override
   public String getMessage() {
      return this.getDescription().getString();
   }
}
