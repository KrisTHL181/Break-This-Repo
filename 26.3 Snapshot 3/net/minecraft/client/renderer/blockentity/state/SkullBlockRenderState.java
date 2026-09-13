package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;

public class SkullBlockRenderState extends BlockEntityRenderState {
   public float animationProgress;
   public Transformation transformation = Transformation.IDENTITY;
   public SkullBlock.Type skullType = SkullBlock.Types.ZOMBIE;
   public RenderType renderType;
}
