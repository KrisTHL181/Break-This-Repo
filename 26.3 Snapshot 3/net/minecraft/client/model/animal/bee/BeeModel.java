package net.minecraft.client.model.animal.bee;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.util.Mth;

public abstract class BeeModel extends EntityModel<BeeRenderState> {
   protected static final String BONE = "bone";
   protected static final String STINGER = "stinger";
   protected static final String FRONT_LEGS = "front_legs";
   protected static final String MIDDLE_LEGS = "middle_legs";
   protected static final String BACK_LEGS = "back_legs";
   protected final ModelPart bone;
   private final ModelPart rightWing;
   private final ModelPart leftWing;
   private final ModelPart frontLeg;
   private final ModelPart midLeg;
   private final ModelPart backLeg;
   private final ModelPart stinger;

   public BeeModel(final ModelPart root) {
      super(root);
      this.bone = root.getChild("bone");
      ModelPart body = this.bone.getChild("body");
      this.stinger = body.getChild("stinger");
      this.rightWing = this.bone.getChild("right_wing");
      this.leftWing = this.bone.getChild("left_wing");
      this.frontLeg = this.bone.getChild("front_legs");
      this.midLeg = this.bone.getChild("middle_legs");
      this.backLeg = this.bone.getChild("back_legs");
   }

   public void setupAnim(final BeeRenderState state) {
      super.setupAnim(state);
      this.stinger.visible = state.hasStinger;
      if (!state.isOnGround) {
         float speed = state.ageInTicks * 120.32113F * (float) (Math.PI / 180.0);
         this.rightWing.yRot = 0.0F;
         this.rightWing.zRot = Mth.cos(speed) * (float) Math.PI * 0.15F;
         this.leftWing.xRot = this.rightWing.xRot;
         this.leftWing.yRot = this.rightWing.yRot;
         this.leftWing.zRot = -this.rightWing.zRot;
         this.frontLeg.xRot = (float) (Math.PI / 4);
         this.midLeg.xRot = (float) (Math.PI / 4);
         this.backLeg.xRot = (float) (Math.PI / 4);
      }

      if (!state.isAngry && !state.isOnGround) {
         float speed = Mth.cos(state.ageInTicks * 0.18F);
         this.bobUpAndDown(speed, state.ageInTicks);
      }

      float rollAmount = state.rollAmount;
      if (rollAmount > 0.0F) {
         this.bone.xRot = Mth.rotLerpRad(rollAmount, this.bone.xRot, 3.0915928F);
      }
   }

   protected void bobUpAndDown(final float speed, final float ageInTicks) {
      this.bone.xRot = 0.1F + speed * (float) Math.PI * 0.025F;
      this.bone.y = this.bone.y - Mth.cos(ageInTicks * 0.18F) * 0.9F;
      this.frontLeg.xRot = -speed * (float) Math.PI * 0.1F + (float) (Math.PI / 8);
      this.backLeg.xRot = -speed * (float) Math.PI * 0.05F + (float) (Math.PI / 4);
   }
}
