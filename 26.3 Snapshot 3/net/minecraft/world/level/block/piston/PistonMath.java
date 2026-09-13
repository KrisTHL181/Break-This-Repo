package net.minecraft.world.level.block.piston;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class PistonMath {
   public static AABB getMovementArea(final AABB aabb, final Direction direction, final double amount) {
      double delta = amount * direction.getAxisDirection().getStep();
      double min = Math.min(delta, 0.0);
      double max = Math.max(delta, 0.0);
      switch (direction) {
         case WEST:
            return new AABB(aabb.minX + min, aabb.minY, aabb.minZ, aabb.minX + max, aabb.maxY, aabb.maxZ);
         case EAST:
            return new AABB(aabb.maxX + min, aabb.minY, aabb.minZ, aabb.maxX + max, aabb.maxY, aabb.maxZ);
         case DOWN:
            return new AABB(aabb.minX, aabb.minY + min, aabb.minZ, aabb.maxX, aabb.minY + max, aabb.maxZ);
         case UP:
         default:
            return new AABB(aabb.minX, aabb.maxY + min, aabb.minZ, aabb.maxX, aabb.maxY + max, aabb.maxZ);
         case NORTH:
            return new AABB(aabb.minX, aabb.minY, aabb.minZ + min, aabb.maxX, aabb.maxY, aabb.minZ + max);
         case SOUTH:
            return new AABB(aabb.minX, aabb.minY, aabb.maxZ + min, aabb.maxX, aabb.maxY, aabb.maxZ + max);
      }
   }
}
