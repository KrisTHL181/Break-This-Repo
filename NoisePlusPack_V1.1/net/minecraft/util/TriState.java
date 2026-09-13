package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;

public enum TriState implements StringRepresentable {
   TRUE("true"),
   FALSE("false"),
   DEFAULT("default");

   public static final Codec<TriState> CODEC = Codec.either(Codec.BOOL, StringRepresentable.fromEnum(TriState::values))
      .xmap(p_453200_ -> (TriState)p_453200_.map(TriState::from, Function.identity()), p_451665_ -> {
         return switch (p_451665_) {
            case TRUE -> Either.left(true);
            case FALSE -> Either.left(false);
            case DEFAULT -> Either.right(p_451665_);
         };
      });
   private final String name;

   TriState(final String p_451219_) {
      this.name = p_451219_;
   }

   public static TriState from(boolean p_459009_) {
      return p_459009_ ? TRUE : FALSE;
   }

   public boolean toBoolean(boolean p_361597_) {
      return switch (this) {
         case TRUE -> true;
         case FALSE -> false;
         default -> p_361597_;
      };
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
