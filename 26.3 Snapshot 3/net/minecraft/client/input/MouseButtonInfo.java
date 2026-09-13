package net.minecraft.client.input;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public record MouseButtonInfo(@MouseButtonInfo.MouseButton int button, @InputWithModifiers.Modifiers int modifiers) implements InputWithModifiers {
   @Override
   public @MouseButtonInfo.MouseButton int input() {
      return this.button;
   }

   @Retention(RetentionPolicy.CLASS)
   @Target(ElementType.TYPE_USE)
   public @interface Action {
   }

   @Retention(RetentionPolicy.CLASS)
   @Target(ElementType.TYPE_USE)
   public @interface MouseButton {
   }
}
