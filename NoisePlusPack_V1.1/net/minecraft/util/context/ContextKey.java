package net.minecraft.util.context;

import net.minecraft.resources.Identifier;

public class ContextKey<T> {
   private final Identifier name;

   public ContextKey(Identifier p_460924_) {
      this.name = p_460924_;
   }

   public static <T> ContextKey<T> vanilla(String p_369920_) {
      return new ContextKey<>(Identifier.withDefaultNamespace(p_369920_));
   }

   public Identifier name() {
      return this.name;
   }

   @Override
   public String toString() {
      return "<parameter " + this.name + ">";
   }
}
