package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public class GameTestAssertException extends GameTestException {
   protected final Component message;
   protected final int tick;

   public GameTestAssertException(Component p_392311_, int p_396655_) {
      super(p_392311_.getString());
      this.message = p_392311_;
      this.tick = p_396655_;
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
