package net.minecraft.client.model.animal.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.CopperGolemAnimation;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.golem.CopperGolemState;

public class CopperGolemModel extends EntityModel<CopperGolemRenderState> implements ArmedModel<CopperGolemRenderState>, HeadedModel {
   private static final float MAX_WALK_ANIMATION_SPEED = 2.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   private static final float Z_FIGHT_MITIGATION = 0.015F;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart rightArm;
   private final ModelPart leftArm;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation walkWithItemAnimation;
   private final KeyframeAnimation idleAnimation;
   private final KeyframeAnimation interactionGetItem;
   private final KeyframeAnimation interactionGetNoItem;
   private final KeyframeAnimation interactionDropItem;
   private final KeyframeAnimation interactionDropNoItem;

   public CopperGolemModel(final ModelPart root) {
      super(root);
      this.body = root.getChild("body");
      this.head = this.body.getChild("head");
      this.rightArm = this.body.getChild("right_arm");
      this.leftArm = this.body.getChild("left_arm");
      this.walkAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK.bake(root);
      this.walkWithItemAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK_ITEM.bake(root);
      this.idleAnimation = CopperGolemAnimation.COPPER_GOLEM_IDLE.bake(root);
      this.interactionGetItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_GET.bake(root);
      this.interactionGetNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_NOGET.bake(root);
      this.interactionDropItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_DROP.bake(root);
      this.interactionDropNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_NODROP.bake(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
      PartDefinition root = meshDefinition.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F, new CubeDeformation(0.015F))
            .texOffs(56, 0)
            .addBox(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE)
            .texOffs(37, 8)
            .addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.015F))
            .texOffs(37, 0)
            .addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.015F)),
         PartPose.offset(0.0F, -6.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(-4.0F, -6.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(4.0F, -6.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(16, 27).addBox(0.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      return LayerDefinition.create(meshDefinition, 64, 64);
   }

   public static LayerDefinition createRunningPoseBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0F, 0.0F, 0.0F));
      PartDefinition root = meshDefinition.getRoot();
      PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-1.064F, -5.0F, 0.0F));
      body.addOrReplaceChild(
         "body_r1",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.02F, -6.116F, -3.5F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.1F, 0.1F, 0.7F, 0.1204F, -0.0064F, -0.0779F)
      );
      body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -5.1F, -5.0F, 8.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(56, 0)
            .addBox(-1.02F, -2.1F, -6.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(37, 8)
            .addBox(-1.02F, -9.1F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.015F))
            .texOffs(37, 0)
            .addBox(-2.0F, -13.1F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.015F)),
         PartPose.offset(0.7F, -5.6F, -1.8F)
      );
      PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
      right_arm.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.052F, -1.11F, -2.036F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.7F, -0.248F, -1.62F, 1.0036F, 0.0F, 0.0F)
      );
      PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
      left_arm.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.032F, -1.1F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.732F, 0.0F, 0.0F, -0.8715F, -0.0535F, -0.0449F)
      );
      PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.064F, -5.0F, 0.0F));
      right_leg.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-1.856F, -0.1F, -1.09F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.048F, 0.0F, -0.9F, -0.8727F, 0.0F, 0.0F)
      );
      PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(0.936F, -5.0F, 0.0F));
      left_leg.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.088F, -0.1F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshDefinition, 64, 64);
   }

   public static LayerDefinition createSittingPoseBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0F, 0.0F, 0.0F));
      PartDefinition root = meshDefinition.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(3, 19)
            .addBox(-3.0F, -4.0F, -4.525F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 15)
            .addBox(-4.0F, -3.0F, -3.525F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.0F, 2.325F)
      );
      body.addOrReplaceChild(
         "body_r1",
         CubeListBuilder.create().texOffs(3, 18).addBox(-4.0F, -3.0F, -2.2F, 8.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, -4.325F, 0.0F, 0.0F, -3.1416F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(37, 8)
            .addBox(-1.0F, -7.0F, -3.3F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.015F))
            .texOffs(37, 0)
            .addBox(-2.0F, -11.0F, -4.3F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.015F))
            .texOffs(0, 0)
            .addBox(-4.0F, -3.0F, -7.325F, 8.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(56, 0)
            .addBox(-1.0F, 0.0F, -8.325F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -6.0F, -0.2F)
      );
      PartDefinition right_arm = body.addOrReplaceChild(
         "right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0F, -5.6F, -1.8F, 0.4363F, 0.0F, 0.0F)
      );
      right_arm.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.075F, -0.9733F, -1.9966F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0893F, 0.1198F, -1.0472F, 0.0F, 0.0F)
      );
      PartDefinition left_arm = body.addOrReplaceChild(
         "left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, -5.6F, -1.7F, 0.4363F, 0.0F, 0.0F)
      );
      left_arm.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.075F, -1.0443F, -1.8997F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -0.0015F, -0.0808F, -1.0472F, 0.0F, 0.0F)
      );
      PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.1F, -2.1F, -2.075F));
      right_leg.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.05F, -1.9F, 1.075F, -1.5708F, 0.0F, 0.0F)
      );
      PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, -2.075F));
      left_leg.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.05F, -2.0F, 1.075F, -1.5708F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshDefinition, 64, 64);
   }

   public static LayerDefinition createStarPoseBodyLayer() {
      MeshDefinition meshDefinition = new MeshDefinition().transformed(p -> p.translated(0.0F, 0.0F, 0.0F));
      PartDefinition root = meshDefinition.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -5.0F, -5.0F, 8.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
            .texOffs(56, 0)
            .addBox(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(37, 8)
            .addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.015F))
            .texOffs(37, 0)
            .addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.015F)),
         PartPose.offset(0.0F, -6.0F, 0.0F)
      );
      PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
      right_arm.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.9199F)
      );
      right_arm.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(-1.0F, 7.4F, -1.0F));
      PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
      left_arm.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.9199F)
      );
      PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, -5.0F, 0.0F));
      right_leg.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.35F, 2.0F, 0.01F, 0.0F, 0.0F, 0.2618F)
      );
      PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.0F, -5.0F, 0.0F));
      left_leg.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.65F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
      );
      return LayerDefinition.create(meshDefinition, 64, 64);
   }

   public static LayerDefinition createEyesLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().retainPartsAndChildren(Set.of("eyes"));
         return mesh;
      });
   }

   public void setupAnim(final CopperGolemRenderState state) {
      super.setupAnim(state);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      if (state.rightHandItemState.isEmpty() && state.leftHandItemState.isEmpty()) {
         this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
      } else {
         this.walkWithItemAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
         this.poseHeldItemArmsIfStill();
      }

      this.idleAnimation.apply(state.idleAnimationState, state.ageInTicks);
      this.interactionGetItem.apply(state.interactionGetItem, state.ageInTicks);
      this.interactionGetNoItem.apply(state.interactionGetNoItem, state.ageInTicks);
      this.interactionDropItem.apply(state.interactionDropItem, state.ageInTicks);
      this.interactionDropNoItem.apply(state.interactionDropNoItem, state.ageInTicks);
   }

   public void translateToHand(final CopperGolemRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      this.root.translateAndRotate(poseStack);
      this.body.translateAndRotate(poseStack);
      ModelPart activeArm = arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
      activeArm.translateAndRotate(poseStack);
      if (state.copperGolemState.equals(CopperGolemState.IDLE)) {
         poseStack.mulPose(Axis.YP.rotationDegrees(arm == HumanoidArm.RIGHT ? -90.0F : 90.0F));
         poseStack.translate(0.0F, 0.0F, 0.125F);
      } else {
         poseStack.scale(0.55F, 0.55F, 0.55F);
         poseStack.translate(-0.125F, 0.3125F, -0.1875F);
      }
   }

   @Override
   public ModelPart getHead() {
      return this.head;
   }

   @Override
   public void translateToHead(final PoseStack poseStack) {
      this.body.translateAndRotate(poseStack);
      this.head.translateAndRotate(poseStack);
      poseStack.translate(0.0F, 0.125F, 0.0F);
      poseStack.scale(1.0625F, 1.0625F, 1.0625F);
   }

   public void applyBlockOnAntennaTransform(final PoseStack poseStack) {
      this.root.translateAndRotate(poseStack);
      this.body.translateAndRotate(poseStack);
      this.head.translateAndRotate(poseStack);
      poseStack.translate(0.0, -1.75, 0.0);
   }

   private void poseHeldItemArmsIfStill() {
      this.rightArm.xRot = Math.min(this.rightArm.xRot, -0.87266463F);
      this.leftArm.xRot = Math.min(this.leftArm.xRot, -0.87266463F);
      this.rightArm.yRot = Math.min(this.rightArm.yRot, -0.1134464F);
      this.leftArm.yRot = Math.max(this.leftArm.yRot, 0.1134464F);
      this.rightArm.zRot = Math.min(this.rightArm.zRot, -0.064577185F);
      this.leftArm.zRot = Math.max(this.leftArm.zRot, 0.064577185F);
   }
}
