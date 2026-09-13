package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record WorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z) implements Coordinates {
   public static final WorldCoordinates ZERO_ROTATION = absolute(new Vec2(0.0F, 0.0F));

   @Override
   public Vec3 getPosition(final CommandSourceStack sender) {
      Vec3 pos = sender.getPosition();
      return new Vec3(this.x.get(pos.x), this.y.get(pos.y), this.z.get(pos.z));
   }

   @Override
   public Vec2 getRotation(final CommandSourceStack sender) {
      Vec2 rot = sender.getRotation();
      return new Vec2((float)this.x.get(rot.x), (float)this.y.get(rot.y));
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

   public static WorldCoordinates parseInt(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      WorldCoordinate x = WorldCoordinate.parseInt(reader);
      if (reader.canRead() && reader.peek() == ' ') {
         reader.skip();
         WorldCoordinate y = WorldCoordinate.parseInt(reader);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            WorldCoordinate z = WorldCoordinate.parseInt(reader);
            return new WorldCoordinates(x, y, z);
         } else {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
         }
      } else {
         reader.setCursor(start);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
      }
   }

   public static WorldCoordinates parseDouble(final StringReader reader, final boolean centerCorrect) throws CommandSyntaxException {
      int start = reader.getCursor();
      WorldCoordinate x = WorldCoordinate.parseDouble(reader, centerCorrect);
      if (reader.canRead() && reader.peek() == ' ') {
         reader.skip();
         WorldCoordinate y = WorldCoordinate.parseDouble(reader, false);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            WorldCoordinate z = WorldCoordinate.parseDouble(reader, centerCorrect);
            return new WorldCoordinates(x, y, z);
         } else {
            reader.setCursor(start);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
         }
      } else {
         reader.setCursor(start);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
      }
   }

   public static WorldCoordinates absolute(final double x, final double y, final double z) {
      return new WorldCoordinates(new WorldCoordinate(false, x), new WorldCoordinate(false, y), new WorldCoordinate(false, z));
   }

   public static WorldCoordinates absolute(final Vec2 rotation) {
      return new WorldCoordinates(new WorldCoordinate(false, rotation.x), new WorldCoordinate(false, rotation.y), new WorldCoordinate(true, 0.0));
   }
}
