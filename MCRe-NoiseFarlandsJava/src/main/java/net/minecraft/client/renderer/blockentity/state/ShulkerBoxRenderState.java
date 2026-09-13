package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderState extends BlockEntityRenderState {
    public Direction direction = Direction.NORTH;
    public @Nullable DyeColor color;
    public float progress;
}