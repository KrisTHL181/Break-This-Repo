package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record LocalCoordinates(double left, double up, double forwards) implements Coordinates {
   public static final char PREFIX_LOCAL_COORDINATE = '^';

   @Override
   public Vec3 getPosition(CommandSourceStack p_119912_) {
      Vec3 vec3 = p_119912_.getAnchor().apply(p_119912_);
      return Vec3.applyLocalCoordinatesToRotation(p_119912_.getRotation(), new Vec3(this.left, this.up, this.forwards)).add(vec3.x, vec3.y, vec3.z);
   }

   @Override
   public Vec2 getRotation(CommandSourceStack p_119915_) {
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

   public static LocalCoordinates parse(StringReader p_119907_) throws CommandSyntaxException {
      int i = p_119907_.getCursor();
      double d0 = readDouble(p_119907_, i);
      if (p_119907_.canRead() && p_119907_.peek() == ' ') {
         p_119907_.skip();
         double d1 = readDouble(p_119907_, i);
         if (p_119907_.canRead() && p_119907_.peek() == ' ') {
            p_119907_.skip();
            double d2 = readDouble(p_119907_, i);
            return new LocalCoordinates(d0, d1, d2);
         } else {
            p_119907_.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_119907_);
         }
      } else {
         p_119907_.setCursor(i);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_119907_);
      }
   }

   private static double readDouble(StringReader p_119909_, int p_119910_) throws CommandSyntaxException {
      if (!p_119909_.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(p_119909_);
      } else if (p_119909_.peek() != '^') {
         p_119909_.setCursor(p_119910_);
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_119909_);
      } else {
         p_119909_.skip();
         return p_119909_.canRead() && p_119909_.peek() != ' ' ? p_119909_.readDouble() : 0.0;
      }
   }
}
