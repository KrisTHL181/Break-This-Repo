package net.minecraft.client.model.animal.allay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class AllayModel extends EntityModel<AllayRenderState> implements ArmedModel<AllayRenderState> {
   private final ModelPart head = this.root.getChild("head");
   private final ModelPart body = this.root.getChild("body");
   private final ModelPart right_arm = this.body.getChild("right_arm");
   private final ModelPart left_arm = this.body.getChild("left_arm");
   private final ModelPart right_wing = this.body.getChild("right_wing");
   private final ModelPart left_wing = this.body.getChild("left_wing");
   private static final float FLYING_ANIMATION_X_ROT = (float) (Math.PI / 4);
   private static final float MAX_HAND_HOLDING_ITEM_X_ROT_RAD = -1.134464F;
   private static final float MIN_HAND_HOLDING_ITEM_X_ROT_RAD = (float) (-Math.PI / 3);

   public AllayModel(final ModelPart root) {
      super(root.getChild("root"), RenderTypes::entityTranslucent);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, 0.0F));
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -3.99F, 0.0F)
      );
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 10)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
            .texOffs(0, 16)
            .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(23, 0).addBox(-0.75F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(-1.75F, 0.5F, 0.0F)
      );
      body.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(23, 6).addBox(-0.25F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)),
         PartPose.offset(1.75F, 0.5F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-0.5F, 0.0F, 0.6F)
      );
      body.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(16, 14).addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.5F, 0.0F, 0.6F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(final AllayRenderState state) {
      super.setupAnim(state);
      float animationSpeed = state.walkAnimationSpeed;
      float animationPos = state.walkAnimationPos;
      float flapSpeed = state.ageInTicks * 20.0F * (float) (Math.PI / 180.0) + animationPos;
      float flapAmount = Mth.cos(flapSpeed) * (float) Math.PI * 0.15F + animationSpeed;
      float idleBobSpeed = state.ageInTicks * 9.0F * (float) (Math.PI / 180.0);
      float flyingFactor = Math.min(animationSpeed / 0.3F, 1.0F);
      float idleBobFactor = 1.0F - flyingFactor;
      float holdingItemFactor = state.holdingAnimationProgress;
      if (state.isDancing) {
         float danceSpeed = state.ageInTicks * 8.0F * (float) (Math.PI / 180.0) + animationSpeed;
         float danceFrequency = Mth.cos(danceSpeed) * 16.0F * (float) (Math.PI / 180.0);
         float spinningRotation = state.spinningProgress;
         float headTiltZ = Mth.cos(danceSpeed) * 14.0F * (float) (Math.PI / 180.0);
         float headTiltY = Mth.cos(danceSpeed) * 30.0F * (float) (Math.PI / 180.0);
         this.root.yRot = state.isSpinning ? (float) (Math.PI * 4) * spinningRotation : this.root.yRot;
         this.root.zRot = danceFrequency * (1.0F - spinningRotation);
         this.head.yRot = headTiltY * (1.0F - spinningRotation);
         this.head.zRot = headTiltZ * (1.0F - spinningRotation);
      } else {
         this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
         this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      }

      this.right_wing.xRot = 0.43633232F * (1.0F - flyingFactor);
      this.right_wing.yRot = (float) (-Math.PI / 4) + flapAmount;
      this.left_wing.xRot = 0.43633232F * (1.0F - flyingFactor);
      this.left_wing.yRot = (float) (Math.PI / 4) - flapAmount;
      this.body.xRot = flyingFactor * (float) (Math.PI / 4);
      float armFlyingRotX = holdingItemFactor * Mth.lerp(flyingFactor, (float) (-Math.PI / 3), -1.134464F);
      this.root.y = this.root.y + (float)Math.cos(idleBobSpeed) * 0.25F * idleBobFactor;
      this.right_arm.xRot = armFlyingRotX;
      this.left_arm.xRot = armFlyingRotX;
      float armIdleBobFactor = idleBobFactor * (1.0F - holdingItemFactor);
      float armIdleBobAmount = 0.43633232F - Mth.cos(idleBobSpeed + (float) (Math.PI * 3.0 / 2.0)) * (float) Math.PI * 0.075F * armIdleBobFactor;
      this.left_arm.zRot = -armIdleBobAmount;
      this.right_arm.zRot = armIdleBobAmount;
      this.right_arm.yRot = 0.27925268F * holdingItemFactor;
      this.left_arm.yRot = -0.27925268F * holdingItemFactor;
   }

   public void translateToHand(final AllayRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      float yOffset = 1.0F;
      float zOffset = 3.0F;
      this.root.translateAndRotate(poseStack);
      this.body.translateAndRotate(poseStack);
      poseStack.translate(0.0F, 0.0625F, 0.1875F);
      poseStack.mulPose(Axis.XP.rotation(this.right_arm.xRot));
      poseStack.scale(0.7F, 0.7F, 0.7F);
      poseStack.translate(0.0625F, 0.0F, 0.0F);
   }
}
