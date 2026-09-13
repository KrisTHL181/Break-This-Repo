package net.minecraft.client.model.animal.turtle;

import java.util.function.Function;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public abstract class TurtleModel extends QuadrupedModel<TurtleRenderState> {
   public TurtleModel(final ModelPart root, final Function<Identifier, RenderType> renderType) {
      super(root, renderType);
   }

   public void setupAnim(final TurtleRenderState state) {
      super.setupAnim(state);
      float animationPos = state.walkAnimationPos;
      float animationSpeed = state.walkAnimationSpeed;
      if (state.isOnLand) {
         float layEgg = state.isLayingEgg ? 4.0F : 1.0F;
         float layEggAmplitude = state.isLayingEgg ? 2.0F : 1.0F;
         float swingPos = animationPos * 5.0F;
         float frontSwing = Mth.cos(layEgg * swingPos);
         float hindSwing = Mth.cos(swingPos);
         this.rightFrontLeg.yRot = -frontSwing * 8.0F * animationSpeed * layEggAmplitude;
         this.leftFrontLeg.yRot = frontSwing * 8.0F * animationSpeed * layEggAmplitude;
         this.rightHindLeg.yRot = -hindSwing * 3.0F * animationSpeed;
         this.leftHindLeg.yRot = hindSwing * 3.0F * animationSpeed;
      } else {
         float swingScale = 0.5F * animationSpeed;
         float swing = Mth.cos(animationPos * 0.6662F * 0.6F) * swingScale;
         this.rightHindLeg.xRot = swing;
         this.leftHindLeg.xRot = -swing;
         this.rightFrontLeg.zRot = -swing;
         this.leftFrontLeg.zRot = swing;
      }
   }
}
