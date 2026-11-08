package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.BedPart;

public class BedRenderState extends BlockEntityRenderState {
   public DyeColor color = DyeColor.WHITE;
   public Direction facing = Direction.NORTH;
   public BedPart part;
}
