package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.endermite.EndermiteModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Endermite;

public class EndermiteRenderer extends MobRenderer<Endermite, LivingEntityRenderState, EndermiteModel> {
   private static final Identifier ENDERMITE_LOCATION = Identifier.withDefaultNamespace("textures/entity/endermite/endermite.png");

   public EndermiteRenderer(final EntityRendererProvider.Context context) {
      super(context, new EndermiteModel(context.bakeLayer(ModelLayers.ENDERMITE)), 0.3F);
   }

   @Override
   protected float getFlipDegrees() {
      return 180.0F;
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return ENDERMITE_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }
}
