package net.minecraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface InputWithModifiers {
   int NOT_DIGIT = -1;

   @InputConstants.Value int input();

   @InputWithModifiers.Modifiers int modifiers();

   default boolean isSelection() {
      return this.input() == 257 || this.input() == 32 || this.input() == 335;
   }

   default boolean isConfirmation() {
      return this.input() == 257 || this.input() == 335;
   }

   default boolean isEscape() {
      return this.input() == 256;
   }

   default boolean isLeft() {
      return this.input() == 263;
   }

   default boolean isRight() {
      return this.input() == 262;
   }

   default boolean isUp() {
      return this.input() == 265;
   }

   default boolean isDown() {
      return this.input() == 264;
   }

   default boolean isCycleFocus() {
      return this.input() == 258;
   }

   default int getDigit() {
      int value = this.input() - 48;
      return value >= 0 && value <= 9 ? value : -1;
   }

   default boolean hasAltDown() {
      return (this.modifiers() & 4) != 0;
   }

   default boolean hasShiftDown() {
      return (this.modifiers() & 1) != 0;
   }

   default boolean hasControlDown() {
      return (this.modifiers() & 2) != 0;
   }

   default boolean hasControlDownWithQuirk() {
      return (this.modifiers() & InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER) != 0;
   }

   default boolean isSelectAll() {
      return this.input() == 65 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
   }

   default boolean isCopy() {
      return this.input() == 67 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
   }

   default boolean isPaste() {
      return this.input() == 86 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
   }

   default boolean isCut() {
      return this.input() == 88 && this.hasControlDownWithQuirk() && !this.hasShiftDown() && !this.hasAltDown();
   }

   @Retention(RetentionPolicy.CLASS)
   @Target(ElementType.TYPE_USE)
   @interface Modifiers {
   }
}
