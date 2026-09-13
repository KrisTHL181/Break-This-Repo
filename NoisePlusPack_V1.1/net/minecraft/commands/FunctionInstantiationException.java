package net.minecraft.commands;

import net.minecraft.network.chat.Component;

public class FunctionInstantiationException extends Exception {
   private final Component messageComponent;

   public FunctionInstantiationException(Component p_297947_) {
      super(p_297947_.getString());
      this.messageComponent = p_297947_;
   }

   public Component messageComponent() {
      return this.messageComponent;
   }
}
