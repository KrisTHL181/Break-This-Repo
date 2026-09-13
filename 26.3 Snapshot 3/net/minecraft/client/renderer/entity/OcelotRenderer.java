package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultOcelotModel;
import net.minecraft.client.model.animal.feline.BabyOcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.Ocelot;

public class OcelotRenderer extends AgeableMobRenderer<Ocelot, FelineRenderState, AbstractFelineModel<FelineRenderState>> {
   private static final Identifier CAT_OCELOT_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/ocelot.png");
   private static final Identifier CAT_OCELOT_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/ocelot_baby.png");

   public OcelotRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultOcelotModel(context.bakeLayer(ModelLayers.OCELOT)), new BabyOcelotModel(context.bakeLayer(ModelLayers.OCELOT_BABY)), 0.4F);
   }

   public Identifier getTextureLocation(final FelineRenderState state) {
      return state.isBaby ? CAT_OCELOT_BABY_LOCATION : CAT_OCELOT_LOCATION;
   }

   public FelineRenderState createRenderState() {
      return new FelineRenderState();
   }

   public void extractRenderState(final Ocelot entity, final FelineRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isCrouching = entity.isCrouching();
      state.isSprinting = entity.isSprinting();
   }
}
