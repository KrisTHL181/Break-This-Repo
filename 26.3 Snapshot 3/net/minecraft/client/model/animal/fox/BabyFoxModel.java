package net.minecraft.client.model.animal.fox;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.FoxBabyAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;

public class BabyFoxModel extends FoxModel {
   private static final float MAX_WALK_ANIMATION_SPEED = 1.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   private final KeyframeAnimation babyWalkAnimation;

   public BabyFoxModel(final ModelPart root) {
      super(root);
      this.babyWalkAnimation = FoxBabyAnimation.FOX_BABY_WALK.bake(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition root = meshdefinition.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.0F, -2.125F, -5.125F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(18, 20)
            .addBox(-1.0F, 0.875F, -7.125F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(22, 8)
            .addBox(-3.0F, -4.125F, -4.125F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
            .texOffs(22, 11)
            .addBox(1.0F, -4.125F, -4.125F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 18.125F, 0.125F)
      );
      root.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.5F, 22.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.5F, 22.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-1.5F, 22.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.5F, 22.0F, 0.0F)
      );
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 10).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 20.0F, 2.0F)
      );
      body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -1.48F, -1.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -0.5F, 3.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   @Override
   protected void setSittingPose(final FoxRenderState state) {
      super.setSittingPose(state);
      this.body.xRot = -0.959931F;
      this.body.z = this.body.z - 4.5F * state.ageScale;
      this.body.y = this.body.y + 3.0F * state.ageScale;
      this.tail.y -= 0.6F;
      this.tail.z = this.tail.z - 2.0F * state.ageScale;
      this.tail.xRot = 0.95993114F;
      this.head.y -= 0.75F;
      this.head.z += 0.0F;
      this.rightFrontLeg.xRot = (float) (-Math.PI / 12);
      this.leftFrontLeg.xRot = (float) (-Math.PI / 12);
      this.rightFrontLeg.z -= 1.5F;
      this.leftFrontLeg.z -= 1.5F;
      this.rightFrontLeg.x += 0.01F;
      this.leftFrontLeg.x -= 0.01F;
      this.rightHindLeg.z -= 3.75F;
      this.leftHindLeg.z -= 3.75F;
      this.rightHindLeg.x += 0.01F;
      this.leftHindLeg.x -= 0.01F;
   }

   @Override
   protected void setSleepingPose(final FoxRenderState state) {
      super.setSleepingPose(state);
      this.body.zRot = (float) (-Math.PI / 2);
      this.body.xRot = (float) (-Math.PI / 18);
      this.body.y += 1.5F;
      this.body.z -= 1.5F;
      this.body.x -= 1.5F;
      this.tail.xRot = (float) (-Math.PI * 5.0 / 6.0);
      this.tail.xRot = -2.1816616F;
      this.tail.x -= 0.7F;
      this.tail.z += 0.6F;
      this.tail.y += 0.9F;
      this.head.x -= 2.0F;
      this.head.y += 2.8F;
      this.head.z -= 4.0F;
      this.head.yRot = (float) (-Math.PI * 2.0 / 3.0);
      this.head.zRot = 0.0F;
   }

   @Override
   protected void setWalkingPose(final FoxRenderState state) {
      super.setWalkingPose(state);
      this.babyWalkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.0F, 2.5F);
   }

   @Override
   protected void setCrouchingPose(final FoxRenderState state) {
      super.setCrouchingPose(state);
      this.body.y = this.body.y + state.crouchAmount / 6.0F;
   }
}
