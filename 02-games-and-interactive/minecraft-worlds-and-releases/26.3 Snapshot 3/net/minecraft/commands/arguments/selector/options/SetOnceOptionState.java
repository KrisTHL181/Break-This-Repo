package net.minecraft.commands.arguments.selector.options;

public class SetOnceOptionState {
   private boolean hasValue;

   public boolean canParse() {
      return !this.hasValue;
   }

   public void markParsed() {
      this.hasValue = true;
   }
}
