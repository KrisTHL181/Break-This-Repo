package net.minecraft.client.renderer.state.level;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;

public class WorldBorderRenderState {
   public double minX;
   public double maxX;
   public double minZ;
   public double maxZ;
   public int tint;
   public double alpha;

   public List<WorldBorderRenderState.DistancePerDirection> closestBorder(final double x, final double z) {
      WorldBorderRenderState.DistancePerDirection[] directions = new WorldBorderRenderState.DistancePerDirection[]{
         new WorldBorderRenderState.DistancePerDirection(Direction.NORTH, z - this.minZ),
         new WorldBorderRenderState.DistancePerDirection(Direction.SOUTH, this.maxZ - z),
         new WorldBorderRenderState.DistancePerDirection(Direction.WEST, x - this.minX),
         new WorldBorderRenderState.DistancePerDirection(Direction.EAST, this.maxX - x)
      };
      return Arrays.stream(directions).sorted(Comparator.comparingDouble(d -> d.distance)).toList();
   }

   public void reset() {
      this.alpha = 0.0;
   }

   public record DistancePerDirection(Direction direction, double distance) {
   }
}
