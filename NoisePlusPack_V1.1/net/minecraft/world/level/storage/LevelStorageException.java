package net.minecraft.world.level.storage;

import net.minecraft.network.chat.Component;

public class LevelStorageException extends RuntimeException {
   private final Component messageComponent;

   public LevelStorageException(Component p_230805_) {
      super(p_230805_.getString());
      this.messageComponent = p_230805_;
   }

   public Component getMessageComponent() {
      return this.messageComponent;
   }
}
