package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.armadillo.AdultArmadilloModel;
import net.minecraft.client.model.animal.armadillo.ArmadilloModel;
import net.minecraft.client.model.animal.armadillo.BabyArmadilloModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.armadillo.Armadillo;

public class ArmadilloRenderer extends AgeableMobRenderer<Armadillo, ArmadilloRenderState, ArmadilloModel> {
   private static final Identifier ARMADILLO_LOCATION = Identifier.withDefaultNamespace("textures/entity/armadillo/armadillo.png");
   private static final Identifier ARMADILLO_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/armadillo/armadillo_baby.png");

   public ArmadilloRenderer(final EntityRendererProvider.Context context) {
      super(
         context,
         new AdultArmadilloModel(context.bakeLayer(ModelLayers.ARMADILLO)),
         new BabyArmadilloModel(context.bakeLayer(ModelLayers.ARMADILLO_BABY)),
         0.4F
      );
   }

   public Identifier getTextureLocation(final ArmadilloRenderState state) {
      return state.isBaby ? ARMADILLO_BABY_LOCATION : ARMADILLO_LOCATION;
   }

   public ArmadilloRenderState createRenderState() {
      return new ArmadilloRenderState();
   }

   public void extractRenderState(final Armadillo entity, final ArmadilloRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isHidingInShell = entity.shouldHideInShell();
      state.peekAnimationState.copyFrom(entity.peekAnimationState);
      state.rollOutAnimationState.copyFrom(entity.rollOutAnimationState);
      state.rollUpAnimationState.copyFrom(entity.rollUpAnimationState);
   }
}
