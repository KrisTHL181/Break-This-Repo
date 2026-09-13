package net.minecraft.client.model.animal.wolf;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.util.Mth;

public abstract class WolfModel extends EntityModel<WolfRenderState> {
   protected final ModelPart head;
   protected final ModelPart body;
   protected final ModelPart rightHindLeg;
   protected final ModelPart leftHindLeg;
   protected final ModelPart rightFrontLeg;
   protected final ModelPart leftFrontLeg;
   protected final ModelPart tail;

   public WolfModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.body = root.getChild("body");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
      this.tail = root.getChild("tail");
   }

   public void setupAnim(final WolfRenderState state) {
      super.setupAnim(state);
      float animationPos = state.walkAnimationPos;
      float animationSpeed = state.walkAnimationSpeed;
      if (state.isAngry) {
         this.tail.yRot = 0.0F;
      } else {
         this.tail.yRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
      }

      if (state.isSitting) {
         this.setSittingPose(state);
      } else {
         this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
         this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
         this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
         this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
      }

      this.shakeOffWater(state);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.tail.xRot = state.tailAngle;
   }

   protected void shakeOffWater(final WolfRenderState state) {
      this.body.zRot = state.getBodyRollAngle(-0.16F);
   }

   protected void setSittingPose(final WolfRenderState state) {
      float ageScale = state.ageScale;
      this.body.y += 4.0F * ageScale;
      this.body.z -= 2.0F * ageScale;
      this.body.xRot = (float) (Math.PI / 4);
      this.tail.y += 9.0F * ageScale;
      this.tail.z -= 2.0F * ageScale;
      this.rightHindLeg.y += 6.7F * ageScale;
      this.rightHindLeg.z -= 5.0F * ageScale;
      this.rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
      this.leftHindLeg.y += 6.7F * ageScale;
      this.leftHindLeg.z -= 5.0F * ageScale;
      this.leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
      this.rightFrontLeg.xRot = 5.811947F;
      this.rightFrontLeg.x += 0.01F * ageScale;
      this.rightFrontLeg.y += 1.0F * ageScale;
      this.leftFrontLeg.xRot = 5.811947F;
      this.leftFrontLeg.x -= 0.01F * ageScale;
      this.leftFrontLeg.y += 1.0F * ageScale;
   }
}
