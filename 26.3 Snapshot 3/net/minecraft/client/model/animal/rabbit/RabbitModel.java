package net.minecraft.client.model.animal.rabbit;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;

public abstract class RabbitModel extends EntityModel<RabbitRenderState> {
   protected static final String FRONT_LEGS = "frontlegs";
   protected static final String BACK_LEGS = "backlegs";
   protected static final String LEFT_HAUNCH = "left_haunch";
   protected static final String RIGHT_HAUNCH = "right_haunch";
   private final KeyframeAnimation hopAnimation;
   private final KeyframeAnimation idleHeadTiltAnimation;
   private final ModelPart head;

   public RabbitModel(final ModelPart root, final AnimationDefinition hop, final AnimationDefinition idleHeadTilt) {
      super(root);
      this.head = root.getChild("body").getChild("head");
      this.hopAnimation = hop.bake(root);
      this.idleHeadTiltAnimation = idleHeadTilt.bake(root);
   }

   public void setupAnim(final RabbitRenderState state) {
      super.setupAnim(state);
      if (!state.idleHeadTiltAnimationState.isStarted()) {
         this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
         this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      }

      this.hopAnimation.apply(state.hopAnimationState, state.ageInTicks);
      this.idleHeadTiltAnimation.apply(state.idleHeadTiltAnimationState, state.ageInTicks);
   }
}
