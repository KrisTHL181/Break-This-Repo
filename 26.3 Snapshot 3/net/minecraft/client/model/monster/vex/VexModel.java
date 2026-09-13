package net.minecraft.client.model.monster.vex;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class VexModel extends EntityModel<VexRenderState> implements ArmedModel<VexRenderState> {
   private final ModelPart body = this.root.getChild("body");
   private final ModelPart rightArm = this.body.getChild("right_arm");
   private final ModelPart leftArm = this.body.getChild("left_arm");
   private final ModelPart rightWing = this.body.getChild("right_wing");
   private final ModelPart leftWing = this.body.getChild("left_wing");
   private final ModelPart head = this.root.getChild("head");

   public VexModel(final ModelPart root) {
      super(root.getChild("root"), RenderTypes::entityTranslucent);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, -2.5F, 0.0F));
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, 1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-1.25F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(-1.75F, 0.25F, 0.0F)
      );
      body.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.75F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(1.75F, 0.25F, 0.0F)
      );
      body.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(16, 14).mirror().addBox(0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offset(0.5F, 1.0F, 1.0F)
      );
      body.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, 1.0F, 1.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(final VexRenderState state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      float movingArmZBob = Mth.cos(state.ageInTicks * 5.5F * (float) (Math.PI / 180.0)) * 0.1F;
      this.rightArm.zRot = (float) (Math.PI / 5) + movingArmZBob;
      this.leftArm.zRot = -((float) (Math.PI / 5) + movingArmZBob);
      if (state.isCharging) {
         this.body.xRot = 0.0F;
         this.setArmsCharging(!state.rightHandItemState.isEmpty(), !state.leftHandItemState.isEmpty(), movingArmZBob);
      } else {
         this.body.xRot = (float) (Math.PI / 20);
      }

      this.leftWing.yRot = 1.0995574F + Mth.cos(state.ageInTicks * 45.836624F * (float) (Math.PI / 180.0)) * (float) (Math.PI / 180.0) * 16.2F;
      this.rightWing.yRot = -this.leftWing.yRot;
      this.leftWing.xRot = 0.47123888F;
      this.leftWing.zRot = -0.47123888F;
      this.rightWing.xRot = 0.47123888F;
      this.rightWing.zRot = 0.47123888F;
   }

   private void setArmsCharging(final boolean hasItemInRightHand, final boolean hasItemInLeftHand, final float movingArmZBob) {
      if (!hasItemInRightHand && !hasItemInLeftHand) {
         this.rightArm.xRot = -1.2217305F;
         this.rightArm.yRot = (float) (Math.PI / 12);
         this.rightArm.zRot = -0.47123888F - movingArmZBob;
         this.leftArm.xRot = -1.2217305F;
         this.leftArm.yRot = (float) (-Math.PI / 12);
         this.leftArm.zRot = 0.47123888F + movingArmZBob;
      } else {
         if (hasItemInRightHand) {
            this.rightArm.xRot = (float) (Math.PI * 7.0 / 6.0);
            this.rightArm.yRot = (float) (Math.PI / 12);
            this.rightArm.zRot = -0.47123888F - movingArmZBob;
         }

         if (hasItemInLeftHand) {
            this.leftArm.xRot = (float) (Math.PI * 7.0 / 6.0);
            this.leftArm.yRot = (float) (-Math.PI / 12);
            this.leftArm.zRot = 0.47123888F + movingArmZBob;
         }
      }
   }

   public void translateToHand(final VexRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      boolean mainArm = arm == HumanoidArm.RIGHT;
      ModelPart activeArm = mainArm ? this.rightArm : this.leftArm;
      this.root.translateAndRotate(poseStack);
      this.body.translateAndRotate(poseStack);
      activeArm.translateAndRotate(poseStack);
      poseStack.scale(0.55F, 0.55F, 0.55F);
      this.offsetStackPosition(poseStack, mainArm);
   }

   private void offsetStackPosition(final PoseStack poseStack, final boolean mainArm) {
      if (mainArm) {
         poseStack.translate(0.046875, -0.15625, 0.078125);
      } else {
         poseStack.translate(-0.046875, -0.15625, 0.078125);
      }
   }
}
