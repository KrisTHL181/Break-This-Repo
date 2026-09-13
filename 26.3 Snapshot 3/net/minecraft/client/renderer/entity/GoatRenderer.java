package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.goat.BabyGoatModel;
import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.goat.Goat;

public class GoatRenderer extends AgeableMobRenderer<Goat, GoatRenderState, GoatModel> {
   private static final Identifier GOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/goat/goat.png");
   private static final Identifier BABY_GOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/goat/goat_baby.png");

   public GoatRenderer(final EntityRendererProvider.Context context) {
      super(context, new GoatModel(context.bakeLayer(ModelLayers.GOAT)), new BabyGoatModel(context.bakeLayer(ModelLayers.GOAT_BABY)), 0.7F);
   }

   public Identifier getTextureLocation(final GoatRenderState state) {
      return state.isBaby ? BABY_GOAT_LOCATION : GOAT_LOCATION;
   }

   public GoatRenderState createRenderState() {
      return new GoatRenderState();
   }

   public void extractRenderState(final Goat entity, final GoatRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.hasLeftHorn = entity.hasLeftHorn();
      state.hasRightHorn = entity.hasRightHorn();
      state.rammingXHeadRot = entity.getRammingXHeadRot();
   }
}
