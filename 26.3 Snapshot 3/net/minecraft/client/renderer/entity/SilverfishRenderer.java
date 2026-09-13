package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Silverfish;

public class SilverfishRenderer extends MobRenderer<Silverfish, LivingEntityRenderState, SilverfishModel> {
   private static final Identifier SILVERFISH_LOCATION = Identifier.withDefaultNamespace("textures/entity/silverfish/silverfish.png");

   public SilverfishRenderer(final EntityRendererProvider.Context context) {
      super(context, new SilverfishModel(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
   }

   @Override
   protected float getFlipDegrees() {
      return 180.0F;
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return SILVERFISH_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }
}
