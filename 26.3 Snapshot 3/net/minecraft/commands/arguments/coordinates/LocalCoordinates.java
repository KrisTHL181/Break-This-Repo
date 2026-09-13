package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record LocalCoordinates(double left, double up, double forwards) implements Coordinates {
   public static final char PREFIX_LOCAL_COORDINATE = '^';

   @Override
   public Vec3 getPosition(final CommandSourceStack sender) {
      Vec3 source = sender.getAnchor().apply(sender);
      return Vec3.applyLocalCoordinatesToRotation(sender.getRotation(), new Vec3(this.left, this.up, this.forwards)).add(source.x, source.y, source.z);
   }

   @Override
   public Vec2 getRotation(final CommandSourceStack sender) {
      return Vec2.ZERO;
   }

   @Override
   public boolean isXRelative() {
      return true;
   }

   @Override
   public boolean isYRelative() {
      return true;
   }

   @Override
   public boolean isZRelative() {
      return true;
   }

   public static LocalCoordinates parse(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      double left = readDouble(reader, start);
      if (reader.canRead() && reader.peek() == ' ') {
         reader.skip();
         double up = readDouble(reader, start);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            double forwards = readDouble(reader, start);
            return new LocalCoordinates(left, up, forwards);
         } else {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
         }
      } else {
         reader.setCursor(start);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
      }
   }

   private static double readDouble(final StringReader reader, final int start) throws CommandSyntaxException {
      if (!reader.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
      } else if (reader.peek() != '^') {
         reader.setCursor(start);
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
      } else {
         reader.skip();
         return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
      }
   }
}
