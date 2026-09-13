package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderState extends BlockEntityRenderState {
   public float animationProgress;
   public Direction direction = Direction.NORTH;
   public float rotationDegrees;
   public SkullBlock.Type skullType = SkullBlock.Types.ZOMBIE;
   public RenderType renderType;
}
