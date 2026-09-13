package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowSquidRenderer extends SquidRenderer<GlowSquid> {
   private static final Identifier GLOW_SQUID_LOCATION = Identifier.withDefaultNamespace("textures/entity/squid/glow_squid.png");

   public GlowSquidRenderer(EntityRendererProvider.Context p_174136_, SquidModel p_454420_, SquidModel p_456672_) {
      super(p_174136_, p_454420_, p_456672_);
   }

   @Override
   public Identifier getTextureLocation(SquidRenderState p_362361_) {
      return GLOW_SQUID_LOCATION;
   }

   protected int getBlockLightLevel(GlowSquid p_455584_, BlockPos p_174147_) {
      int i = (int)Mth.clampedLerp(1.0F - p_455584_.getDarkTicksRemaining() / 10.0F, 0.0F, 15.0F);
      return i == 15 ? 15 : Math.max(i, super.getBlockLightLevel(p_455584_, p_174147_));
   }
}
