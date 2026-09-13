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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public CopperGolemModel(ModelPart p_459356_) {
      super(p_459356_);
      this.body = p_459356_.getChild("body");
      this.head = this.body.getChild("head");
      this.rightArm = this.body.getChild("right_arm");
      this.leftArm = this.body.getChild("left_arm");
      this.walkAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK.bake(p_459356_);
      this.walkWithItemAnimation = CopperGolemAnimation.COPPER_GOLEM_WALK_ITEM.bake(p_459356_);
      this.idleAnimation = CopperGolemAnimation.COPPER_GOLEM_IDLE.bake(p_459356_);
      this.interactionGetItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_GET.bake(p_459356_);
      this.interactionGetNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_NOITEM_NOGET.bake(p_459356_);
      this.interactionDropItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_DROP.bake(p_459356_);
      this.interactionDropNoItem = CopperGolemAnimation.COPPER_GOLEM_CHEST_INTERACTION_ITEM_NODROP.bake(p_459356_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition().transformed(p_450361_ -> p_450361_.translated(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
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
      partdefinition1.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(-4.0F, -6.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(4.0F, -6.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(16, 27).addBox(0.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createRunningPoseBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition().transformed(p_460560_ -> p_460560_.translated(0.0F, 0.0F, 0.0F));
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-1.064F, -5.0F, 0.0F));
      partdefinition1.addOrReplaceChild(
         "body_r1",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.02F, -6.116F, -3.5F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.1F, 0.1F, 0.7F, 0.1204F, -0.0064F, -0.0779F)
      );
      partdefinition1.addOrReplaceChild(
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
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
      partdefinition2.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.052F, -1.11F, -2.036F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.7F, -0.248F, -1.62F, 1.0036F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition1.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
      partdefinition3.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.032F, -1.1F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.732F, 0.0F, 0.0F, -0.8715F, -0.0535F, -0.0449F)
      );
      PartDefinition partdefinition4 = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.064F, -5.0F, 0.0F));
      partdefinition4.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-1.856F, -0.1F, -1.09F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.048F, 0.0F, -0.9F, -0.8727F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(0.936F, -5.0F, 0.0F));
      partdefinition5.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.088F, -0.1F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createSittingPoseBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition().transformed(p_458468_ -> p_458468_.translated(0.0F, 0.0F, 0.0F));
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(3, 19)
            .addBox(-3.0F, -4.0F, -4.525F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 15)
            .addBox(-4.0F, -3.0F, -3.525F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.0F, 2.325F)
      );
      partdefinition1.addOrReplaceChild(
         "body_r1",
         CubeListBuilder.create().texOffs(3, 18).addBox(-4.0F, -3.0F, -2.2F, 8.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, -4.325F, 0.0F, 0.0F, -3.1416F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
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
      PartDefinition partdefinition3 = partdefinition1.addOrReplaceChild(
         "right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0F, -5.6F, -1.8F, 0.4363F, 0.0F, 0.0F)
      );
      partdefinition3.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-3.075F, -0.9733F, -1.9966F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0893F, 0.1198F, -1.0472F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition4 = partdefinition1.addOrReplaceChild(
         "left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, -5.6F, -1.7F, 0.4363F, 0.0F, 0.0F)
      );
      partdefinition4.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(0.075F, -1.0443F, -1.8997F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -0.0015F, -0.0808F, -1.0472F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.1F, -2.1F, -2.075F));
      partdefinition5.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.05F, -1.9F, 1.075F, -1.5708F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition6 = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, -2.075F));
      partdefinition6.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.05F, -2.0F, 1.075F, -1.5708F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createStarPoseBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition().transformed(p_456532_ -> p_456532_.translated(0.0F, 0.0F, 0.0F));
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -5.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
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
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
      partdefinition2.addOrReplaceChild(
         "right_arm_r1",
         CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.9199F)
      );
      partdefinition2.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(-1.0F, 7.4F, -1.0F));
      PartDefinition partdefinition3 = partdefinition1.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
      partdefinition3.addOrReplaceChild(
         "left_arm_r1",
         CubeListBuilder.create().texOffs(50, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.9199F)
      );
      PartDefinition partdefinition4 = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, -5.0F, 0.0F));
      partdefinition4.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.35F, 2.0F, 0.01F, 0.0F, 0.0F, 0.2618F)
      );
      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.0F, -5.0F, 0.0F));
      partdefinition5.addOrReplaceChild(
         "left_leg_r1",
         CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.65F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createEyesLayer() {
      return createBodyLayer().apply(p_457681_ -> {
         p_457681_.getRoot().retainPartsAndChildren(Set.of("eyes"));
         return p_457681_;
      });
   }

   public void setupAnim(CopperGolemRenderState p_455558_) {
      super.setupAnim(p_455558_);
      this.head.xRot = p_455558_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_455558_.yRot * (float) (Math.PI / 180.0);
      if (p_455558_.rightHandItemState.isEmpty() && p_455558_.leftHandItemState.isEmpty()) {
         this.walkAnimation.applyWalk(p_455558_.walkAnimationPos, p_455558_.walkAnimationSpeed, 2.0F, 2.5F);
      } else {
         this.walkWithItemAnimation.applyWalk(p_455558_.walkAnimationPos, p_455558_.walkAnimationSpeed, 2.0F, 2.5F);
         this.poseHeldItemArmsIfStill();
      }

      this.idleAnimation.apply(p_455558_.idleAnimationState, p_455558_.ageInTicks);
      this.interactionGetItem.apply(p_455558_.interactionGetItem, p_455558_.ageInTicks);
      this.interactionGetNoItem.apply(p_455558_.interactionGetNoItem, p_455558_.ageInTicks);
      this.interactionDropItem.apply(p_455558_.interactionDropItem, p_455558_.ageInTicks);
      this.interactionDropNoItem.apply(p_455558_.interactionDropNoItem, p_455558_.ageInTicks);
   }

   public void translateToHand(CopperGolemRenderState p_452685_, HumanoidArm p_454459_, PoseStack p_455271_) {
      this.root.translateAndRotate(p_455271_);
      this.body.translateAndRotate(p_455271_);
      ModelPart modelpart = p_454459_ == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
      modelpart.translateAndRotate(p_455271_);
      if (p_452685_.copperGolemState.equals(CopperGolemState.IDLE)) {
         p_455271_.mulPose(Axis.YP.rotationDegrees(p_454459_ == HumanoidArm.RIGHT ? -90.0F : 90.0F));
         p_455271_.translate(0.0F, 0.0F, 0.125F);
      } else {
         p_455271_.scale(0.55F, 0.55F, 0.55F);
         p_455271_.translate(-0.125F, 0.3125F, -0.1875F);
      }
   }

   @Override
   public ModelPart getHead() {
      return this.head;
   }

   @Override
   public void translateToHead(PoseStack p_459037_) {
      this.body.translateAndRotate(p_459037_);
      this.head.translateAndRotate(p_459037_);
      p_459037_.translate(0.0F, 0.125F, 0.0F);
      p_459037_.scale(1.0625F, 1.0625F, 1.0625F);
   }

   public void applyBlockOnAntennaTransform(PoseStack p_457550_) {
      this.root.translateAndRotate(p_457550_);
      this.body.translateAndRotate(p_457550_);
      this.head.translateAndRotate(p_457550_);
      p_457550_.translate(0.0, -2.25, 0.0);
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
