package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;

public enum TriState implements StringRepresentable {
   TRUE("true"),
   FALSE("false"),
   DEFAULT("default");

   public static final Codec<TriState> CODEC = Codec.either(Codec.BOOL, StringRepresentable.fromEnum(TriState::values))
      .xmap(either -> (TriState)either.map(TriState::from, Function.identity()), triState -> {
         return switch (triState) {
            case TRUE -> Either.left(true);
            case FALSE -> Either.left(false);
            case DEFAULT -> Either.right(triState);
         };
      });
   private final String name;

   TriState(final String name) {
      this.name = name;
   }

   public static TriState from(final boolean value) {
      return value ? TRUE : FALSE;
   }

   public boolean toBoolean(final boolean defaultValue) {
      return switch (this) {
         case TRUE -> true;
         case FALSE -> false;
         default -> defaultValue;
      };
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
