package net.minecraft.gametest.framework;

import net.minecraft.network.chat.Component;

public abstract class GameTestException extends RuntimeException {
   public GameTestException(String p_394946_) {
      super(p_394946_);
   }

   public abstract Component getDescription();
}
