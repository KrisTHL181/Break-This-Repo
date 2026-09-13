package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.armadillo.ArmadilloModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmadilloRenderer extends AgeableMobRenderer<Armadillo, ArmadilloRenderState, ArmadilloModel> {
   private static final Identifier ARMADILLO_LOCATION = Identifier.withDefaultNamespace("textures/entity/armadillo.png");

   public ArmadilloRenderer(EntityRendererProvider.Context p_333160_) {
      super(
         p_333160_, new ArmadilloModel(p_333160_.bakeLayer(ModelLayers.ARMADILLO)), new ArmadilloModel(p_333160_.bakeLayer(ModelLayers.ARMADILLO_BABY)), 0.4F
      );
   }

   public Identifier getTextureLocation(ArmadilloRenderState p_457517_) {
      return ARMADILLO_LOCATION;
   }

   public ArmadilloRenderState createRenderState() {
      return new ArmadilloRenderState();
   }

   public void extractRenderState(Armadillo p_367646_, ArmadilloRenderState p_370188_, float p_364241_) {
      super.extractRenderState(p_367646_, p_370188_, p_364241_);
      p_370188_.isHidingInShell = p_367646_.shouldHideInShell();
      p_370188_.peekAnimationState.copyFrom(p_367646_.peekAnimationState);
      p_370188_.rollOutAnimationState.copyFrom(p_367646_.rollOutAnimationState);
      p_370188_.rollUpAnimationState.copyFrom(p_367646_.rollUpAnimationState);
   }
}
