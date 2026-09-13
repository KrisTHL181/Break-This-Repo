package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public record WorldCoordinate(boolean relative, double value) {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));

   public double get(final double original) {
      return this.relative ? this.value + original : this.value;
   }

   public static WorldCoordinate parseDouble(final StringReader reader, final boolean center) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
      }

      if (!reader.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
      }

      boolean relative = isRelative(reader);
      int start = reader.getCursor();
      double value = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
      String number = reader.getString().substring(start, reader.getCursor());
      if (relative && number.isEmpty()) {
         return new WorldCoordinate(true, 0.0);
      }

      if (!number.contains(".") && !relative && center) {
         value += 0.5;
      }

      return new WorldCoordinate(relative, value);
   }

   public static WorldCoordinate parseInt(final StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
      }

      if (!reader.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(reader);
      }

      boolean relative = isRelative(reader);
      double value;
      if (reader.canRead() && reader.peek() != ' ') {
         value = relative ? reader.readDouble() : reader.readInt();
      } else {
         value = 0.0;
      }

      return new WorldCoordinate(relative, value);
   }

   public static boolean isRelative(final StringReader reader) {
      boolean relative;
      if (reader.peek() == '~') {
         relative = true;
         reader.skip();
      } else {
         relative = false;
      }

      return relative;
   }

   public boolean isRelative() {
      return this.relative;
   }
}
