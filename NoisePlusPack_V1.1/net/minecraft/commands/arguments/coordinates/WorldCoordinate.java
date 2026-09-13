package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public record WorldCoordinate(boolean relative, double value) {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));

   public double get(double p_120868_) {
      return this.relative ? this.value + p_120868_ : this.value;
   }

   public static WorldCoordinate parseDouble(StringReader p_120872_, boolean p_120873_) throws CommandSyntaxException {
      if (p_120872_.canRead() && p_120872_.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_120872_);
      }

      if (!p_120872_.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(p_120872_);
      }

      boolean flag = isRelative(p_120872_);
      int i = p_120872_.getCursor();
      double d0 = p_120872_.canRead() && p_120872_.peek() != ' ' ? p_120872_.readDouble() : 0.0;
      String s = p_120872_.getString().substring(i, p_120872_.getCursor());
      if (flag && s.isEmpty()) {
         return new WorldCoordinate(true, 0.0);
      }

      if (!s.contains(".") && !flag && p_120873_) {
         d0 += 0.5;
      }

      return new WorldCoordinate(flag, d0);
   }

   public static WorldCoordinate parseInt(StringReader p_120870_) throws CommandSyntaxException {
      if (p_120870_.canRead() && p_120870_.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_120870_);
      }

      if (!p_120870_.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(p_120870_);
      }

      boolean flag = isRelative(p_120870_);
      double d0;
      if (p_120870_.canRead() && p_120870_.peek() != ' ') {
         d0 = flag ? p_120870_.readDouble() : p_120870_.readInt();
      } else {
         d0 = 0.0;
      }

      return new WorldCoordinate(flag, d0);
   }

   public static boolean isRelative(StringReader p_120875_) {
      boolean flag;
      if (p_120875_.peek() == '~') {
         flag = true;
         p_120875_.skip();
      } else {
         flag = false;
      }

      return flag;
   }

   public boolean isRelative() {
      return this.relative;
   }
}
