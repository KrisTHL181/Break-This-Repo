package net.minecraft.client.renderer.entity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.jspecify.annotations.Nullable;

public class PaintingRenderState extends EntityRenderState {
   public Direction direction = Direction.NORTH;
   public @Nullable PaintingVariant variant;
   public int[] lightCoordsPerBlock = new int[0];
}
