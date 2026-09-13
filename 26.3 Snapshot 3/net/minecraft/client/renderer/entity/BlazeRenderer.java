package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Blaze;

public class BlazeRenderer extends MobRenderer<Blaze, LivingEntityRenderState, BlazeModel> {
   private static final Identifier BLAZE_LOCATION = Identifier.withDefaultNamespace("textures/entity/blaze/blaze.png");

   public BlazeRenderer(final EntityRendererProvider.Context context) {
      super(context, new BlazeModel(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
   }

   protected int getBlockLightLevel(final Blaze entity, final BlockPos blockPos) {
      return 15;
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return BLAZE_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }
}
