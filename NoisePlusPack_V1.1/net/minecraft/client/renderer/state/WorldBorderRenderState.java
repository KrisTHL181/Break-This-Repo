package net.minecraft.client.renderer.state;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderState {
   public double minX;
   public double maxX;
   public double minZ;
   public double maxZ;
   public int tint;
   public double alpha;

   public List<WorldBorderRenderState.DistancePerDirection> closestBorder(double p_429505_, double p_431595_) {
      WorldBorderRenderState.DistancePerDirection[] aworldborderrenderstate$distanceperdirection = new WorldBorderRenderState.DistancePerDirection[]{
         new WorldBorderRenderState.DistancePerDirection(Direction.NORTH, p_431595_ - this.minZ),
         new WorldBorderRenderState.DistancePerDirection(Direction.SOUTH, this.maxZ - p_431595_),
         new WorldBorderRenderState.DistancePerDirection(Direction.WEST, p_429505_ - this.minX),
         new WorldBorderRenderState.DistancePerDirection(Direction.EAST, this.maxX - p_429505_)
      };
      return Arrays.stream(aworldborderrenderstate$distanceperdirection).sorted(Comparator.comparingDouble(p_429475_ -> p_429475_.distance)).toList();
   }

   public void reset() {
      this.alpha = 0.0;
   }

   @OnlyIn(Dist.CLIENT)
   public record DistancePerDirection(Direction direction, double distance) {
   }
}
