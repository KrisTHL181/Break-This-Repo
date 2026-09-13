package net.minecraft.client.renderer.entity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderState extends EntityRenderState {
    public Direction direction = Direction.NORTH;
    public @Nullable PaintingVariant variant;
    public int[] lightCoordsPerBlock = new int[0];
}