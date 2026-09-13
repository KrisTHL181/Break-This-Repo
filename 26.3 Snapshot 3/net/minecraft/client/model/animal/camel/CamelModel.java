package net.minecraft.client.model.animal.camel;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.util.Mth;

public abstract class CamelModel extends EntityModel<CamelRenderState> {
   private static final float MAX_WALK_ANIMATION_SPEED = 2.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   protected final ModelPart head;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation sitAnimation;
   private final KeyframeAnimation sitPoseAnimation;
   private final KeyframeAnimation standupAnimation;
   private final KeyframeAnimation idleAnimation;
   private final KeyframeAnimation dashAnimation;

   public CamelModel(
      final ModelPart root,
      final AnimationDefinition walk,
      final AnimationDefinition sit,
      final AnimationDefinition sitPose,
      final AnimationDefinition standup,
      final AnimationDefinition idle,
      final AnimationDefinition dash
   ) {
      super(root);
      ModelPart body = root.getChild("body");
      this.head = body.getChild("head");
      this.walkAnimation = walk.bake(root);
      this.sitAnimation = sit.bake(root);
      this.sitPoseAnimation = sitPose.bake(root);
      this.standupAnimation = standup.bake(root);
      this.idleAnimation = idle.bake(root);
      this.dashAnimation = dash.bake(root);
   }

   public void setupAnim(final CamelRenderState state) {
      super.setupAnim(state);
      this.applyHeadRotation(state, state.yRot, state.xRot);
      this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
      this.sitAnimation.apply(state.sitAnimationState, state.ageInTicks);
      this.sitPoseAnimation.apply(state.sitPoseAnimationState, state.ageInTicks);
      this.standupAnimation.apply(state.sitUpAnimationState, state.ageInTicks);
      this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
      this.dashAnimation.apply(state.dashAnimationState, state.ageInTicks);
   }

   private void applyHeadRotation(final CamelRenderState state, float yRot, float xRot) {
      yRot = Mth.clamp(yRot, -30.0F, 30.0F);
      xRot = Mth.clamp(xRot, -25.0F, 45.0F);
      if (state.jumpCooldown > 0.0F) {
         float headRotation = 45.0F * state.jumpCooldown / 55.0F;
         xRot = Mth.clamp(xRot + headRotation, -25.0F, 70.0F);
      }

      this.head.yRot = yRot * (float) (Math.PI / 180.0);
      this.head.xRot = xRot * (float) (Math.PI / 180.0);
   }
}
