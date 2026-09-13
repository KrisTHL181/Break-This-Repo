package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record WorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) implements Coordinates {
   public static final WorldCoordinates ZERO_ROTATION = absolute(new Vec2(0.0F, 0.0F));

   @Override
   public Vec3 getPosition(CommandSourceStack p_120893_) {
      Vec3 vec3 = p_120893_.getPosition();
      return new Vec3(this.x.get(vec3.x), this.y.get(vec3.y), this.z.get(vec3.z));
   }

   @Override
   public Vec2 getRotation(CommandSourceStack p_120896_) {
      Vec2 vec2 = p_120896_.getRotation();
      return new Vec2((float)this.x.get(vec2.x), (float)this.y.get(vec2.y));
   }

   @Override
   public boolean isXRelative() {
      return this.x.isRelative();
   }

   @Override
   public boolean isYRelative() {
      return this.y.isRelative();
   }

   @Override
   public boolean isZRelative() {
      return this.z.isRelative();
   }

   public static WorldCoordinates parseInt(StringReader p_120888_) throws CommandSyntaxException {
      int i = p_120888_.getCursor();
      WorldCoordinate worldcoordinate = WorldCoordinate.parseInt(p_120888_);
      if (p_120888_.canRead() && p_120888_.peek() == ' ') {
         p_120888_.skip();
         WorldCoordinate worldcoordinate1 = WorldCoordinate.parseInt(p_120888_);
         if (p_120888_.canRead() && p_120888_.peek() == ' ') {
            p_120888_.skip();
            WorldCoordinate worldcoordinate2 = WorldCoordinate.parseInt(p_120888_);
            return new WorldCoordinates(worldcoordinate, worldcoordinate1, worldcoordinate2);
         } else {
            p_120888_.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120888_);
         }
      } else {
         p_120888_.setCursor(i);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120888_);
      }
   }

   public static WorldCoordinates parseDouble(StringReader p_120890_, boolean p_120891_) throws CommandSyntaxException {
      int i = p_120890_.getCursor();
      WorldCoordinate worldcoordinate = WorldCoordinate.parseDouble(p_120890_, p_120891_);
      if (p_120890_.canRead() && p_120890_.peek() == ' ') {
         p_120890_.skip();
         WorldCoordinate worldcoordinate1 = WorldCoordinate.parseDouble(p_120890_, false);
         if (p_120890_.canRead() && p_120890_.peek() == ' ') {
            p_120890_.skip();
            WorldCoordinate worldcoordinate2 = WorldCoordinate.parseDouble(p_120890_, p_120891_);
            return new WorldCoordinates(worldcoordinate, worldcoordinate1, worldcoordinate2);
         } else {
            p_120890_.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120890_);
         }
      } else {
         p_120890_.setCursor(i);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(p_120890_);
      }
   }

   public static WorldCoordinates absolute(double p_175086_, double p_175087_, double p_175088_) {
      return new WorldCoordinates(new WorldCoordinate(false, p_175086_), new WorldCoordinate(false, p_175087_), new WorldCoordinate(false, p_175088_));
   }

   public static WorldCoordinates absolute(Vec2 p_175090_) {
      return new WorldCoordinates(new WorldCoordinate(false, p_175090_.x), new WorldCoordinate(false, p_175090_.y), new WorldCoordinate(true, 0.0));
   }
}
