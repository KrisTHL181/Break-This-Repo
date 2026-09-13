package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BedRenderState extends BlockEntityRenderState {
   public DyeColor color = DyeColor.WHITE;
   public Direction facing = Direction.NORTH;
   public boolean isHead;
}
