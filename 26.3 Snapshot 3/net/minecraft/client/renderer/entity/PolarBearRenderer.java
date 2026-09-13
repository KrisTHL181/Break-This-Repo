package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.polarbear.PolarBear;

public class PolarBearRenderer extends AgeableMobRenderer<PolarBear, PolarBearRenderState, PolarBearModel> {
   private static final Identifier BEAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/bear/polarbear.png");
   private static final Identifier BABY_BEAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/bear/polarbear_baby.png");

   public PolarBearRenderer(final EntityRendererProvider.Context context) {
      super(context, new PolarBearModel(context.bakeLayer(ModelLayers.POLAR_BEAR)), new PolarBearModel(context.bakeLayer(ModelLayers.POLAR_BEAR_BABY)), 0.9F);
   }

   public Identifier getTextureLocation(final PolarBearRenderState state) {
      return state.isBaby ? BABY_BEAR_LOCATION : BEAR_LOCATION;
   }

   public PolarBearRenderState createRenderState() {
      return new PolarBearRenderState();
   }

   public void extractRenderState(final PolarBear entity, final PolarBearRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.standScale = entity.getStandingAnimationScale(partialTicks);
   }
}
