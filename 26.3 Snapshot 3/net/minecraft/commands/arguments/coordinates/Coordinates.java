package net.minecraft.commands.arguments.coordinates;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface Coordinates {
   Vec3 getPosition(CommandSourceStack sender);

   Vec2 getRotation(CommandSourceStack sender);

   default BlockPos getBlockPos(final CommandSourceStack sender) {
      return BlockPos.containing(this.getPosition(sender));
   }

   boolean isXRelative();

   boolean isYRelative();

   boolean isZRelative();
}
