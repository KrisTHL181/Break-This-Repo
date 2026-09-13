package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlazeRenderer extends MobRenderer<Blaze, LivingEntityRenderState, BlazeModel> {
   private static final Identifier BLAZE_LOCATION = Identifier.withDefaultNamespace("textures/entity/blaze.png");

   public BlazeRenderer(EntityRendererProvider.Context p_173933_) {
      super(p_173933_, new BlazeModel(p_173933_.bakeLayer(ModelLayers.BLAZE)), 0.5F);
   }

   protected int getBlockLightLevel(Blaze p_113910_, BlockPos p_113911_) {
      return 15;
   }

   @Override
   public Identifier getTextureLocation(LivingEntityRenderState p_366152_) {
      return BLAZE_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }
}
