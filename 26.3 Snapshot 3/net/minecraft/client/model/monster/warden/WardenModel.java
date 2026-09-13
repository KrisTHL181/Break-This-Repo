package net.minecraft.client.model.monster.warden;

import java.util.Set;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.WardenAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public class WardenModel extends EntityModel<WardenRenderState> {
   private static final float DEFAULT_ARM_X_Y = 13.0F;
   private static final float DEFAULT_ARM_Z = 1.0F;
   protected final ModelPart bone;
   protected final ModelPart body;
   protected final ModelPart head;
   protected final ModelPart rightTendril;
   protected final ModelPart leftTendril;
   protected final ModelPart leftLeg;
   protected final ModelPart leftArm;
   protected final ModelPart leftRibcage;
   protected final ModelPart rightArm;
   protected final ModelPart rightLeg;
   protected final ModelPart rightRibcage;
   private final KeyframeAnimation attackAnimation;
   private final KeyframeAnimation sonicBoomAnimation;
   private final KeyframeAnimation diggingAnimation;
   private final KeyframeAnimation emergeAnimation;
   private final KeyframeAnimation roarAnimation;
   private final KeyframeAnimation sniffAnimation;

   public WardenModel(final ModelPart root) {
      super(root, RenderTypes::entityCutout);
      this.bone = root.getChild("bone");
      this.body = this.bone.getChild("body");
      this.head = this.body.getChild("head");
      this.rightLeg = this.bone.getChild("right_leg");
      this.leftLeg = this.bone.getChild("left_leg");
      this.rightArm = this.body.getChild("right_arm");
      this.leftArm = this.body.getChild("left_arm");
      this.rightTendril = this.head.getChild("right_tendril");
      this.leftTendril = this.head.getChild("left_tendril");
      this.rightRibcage = this.body.getChild("right_ribcage");
      this.leftRibcage = this.body.getChild("left_ribcage");
      this.attackAnimation = WardenAnimation.WARDEN_ATTACK.bake(root);
      this.sonicBoomAnimation = WardenAnimation.WARDEN_SONIC_BOOM.bake(root);
      this.diggingAnimation = WardenAnimation.WARDEN_DIG.bake(root);
      this.emergeAnimation = WardenAnimation.WARDEN_EMERGE.bake(root);
      this.roarAnimation = WardenAnimation.WARDEN_ROAR.bake(root);
      this.sniffAnimation = WardenAnimation.WARDEN_SNIFF.bake(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition body = bone.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -13.0F, -4.0F, 18.0F, 21.0F, 11.0F), PartPose.offset(0.0F, -21.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_ribcage", CubeListBuilder.create().texOffs(90, 11).addBox(-2.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F), PartPose.offset(-7.0F, -2.0F, -4.0F)
      );
      body.addOrReplaceChild(
         "left_ribcage",
         CubeListBuilder.create().texOffs(90, 11).mirror().addBox(-7.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F).mirror(false),
         PartPose.offset(7.0F, -2.0F, -4.0F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -5.0F, 16.0F, 16.0F, 10.0F), PartPose.offset(0.0F, -13.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "right_tendril", CubeListBuilder.create().texOffs(52, 32).addBox(-16.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(-8.0F, -12.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_tendril", CubeListBuilder.create().texOffs(58, 0).addBox(0.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(8.0F, -12.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(44, 50).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(-13.0F, -13.0F, 1.0F)
      );
      body.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(0, 58).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(13.0F, -13.0F, 1.0F)
      );
      bone.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(76, 48).addBox(-3.1F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(-5.9F, -13.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(76, 76).addBox(-2.9F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(5.9F, -13.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public static LayerDefinition createTendrilsLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().retainExactParts(Set.of("left_tendril", "right_tendril"));
         return mesh;
      });
   }

   public static LayerDefinition createHeartLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().retainExactParts(Set.of("body"));
         return mesh;
      });
   }

   public static LayerDefinition createBioluminescentLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().retainExactParts(Set.of("head", "left_arm", "right_arm", "left_leg", "right_leg"));
         return mesh;
      });
   }

   public static LayerDefinition createPulsatingSpotsLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().retainExactParts(Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"));
         return mesh;
      });
   }

   public void setupAnim(final WardenRenderState state) {
      super.setupAnim(state);
      this.animateHeadLookTarget(state.yRot, state.xRot);
      this.animateWalk(state.walkAnimationPos, state.walkAnimationSpeed);
      this.animateIdlePose(state.ageInTicks);
      this.animateTendrils(state, state.ageInTicks);
      this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
      this.sonicBoomAnimation.apply(state.sonicBoomAnimationState, state.ageInTicks);
      this.diggingAnimation.apply(state.diggingAnimationState, state.ageInTicks);
      this.emergeAnimation.apply(state.emergeAnimationState, state.ageInTicks);
      this.roarAnimation.apply(state.roarAnimationState, state.ageInTicks);
      this.sniffAnimation.apply(state.sniffAnimationState, state.ageInTicks);
   }

   private void animateHeadLookTarget(final float yRot, final float xRot) {
      this.head.xRot = xRot * (float) (Math.PI / 180.0);
      this.head.yRot = yRot * (float) (Math.PI / 180.0);
   }

   private void animateIdlePose(final float ageInTicks) {
      float scaledAge = ageInTicks * 0.1F;
      float wobbleCosine = Mth.cos(scaledAge);
      float wobbleSine = Mth.sin(scaledAge);
      this.head.zRot += 0.06F * wobbleCosine;
      this.head.xRot += 0.06F * wobbleSine;
      this.body.zRot += 0.025F * wobbleSine;
      this.body.xRot += 0.025F * wobbleCosine;
   }

   private void animateWalk(final float animationPos, final float animationSpeed) {
      float speedModifier = Math.min(0.5F, 3.0F * animationSpeed);
      float adjustedPos = animationPos * 0.8662F;
      float adjustedPosCosine = Mth.cos(adjustedPos);
      float adjustedPosSine = Mth.sin(adjustedPos);
      float speedModifierWithMin = Math.min(0.35F, speedModifier);
      this.head.zRot += 0.3F * adjustedPosSine * speedModifier;
      this.head.xRot = this.head.xRot + 1.2F * Mth.cos(adjustedPos + (float) (Math.PI / 2)) * speedModifierWithMin;
      this.body.zRot = 0.1F * adjustedPosSine * speedModifier;
      this.body.xRot = 1.0F * adjustedPosCosine * speedModifierWithMin;
      this.leftLeg.xRot = 1.0F * adjustedPosCosine * speedModifier;
      this.rightLeg.xRot = 1.0F * Mth.cos(adjustedPos + (float) Math.PI) * speedModifier;
      this.leftArm.xRot = -(0.8F * adjustedPosCosine * speedModifier);
      this.leftArm.zRot = 0.0F;
      this.rightArm.xRot = -(0.8F * adjustedPosSine * speedModifier);
      this.rightArm.zRot = 0.0F;
      this.resetArmPoses();
   }

   private void resetArmPoses() {
      this.leftArm.yRot = 0.0F;
      this.leftArm.z = 1.0F;
      this.leftArm.x = 13.0F;
      this.leftArm.y = -13.0F;
      this.rightArm.yRot = 0.0F;
      this.rightArm.z = 1.0F;
      this.rightArm.x = -13.0F;
      this.rightArm.y = -13.0F;
   }

   private void animateTendrils(final WardenRenderState state, final float ageInTicks) {
      float tendrilXRot = state.tendrilAnimation * (float)(Math.cos(ageInTicks * 2.25) * Math.PI * 0.1F);
      this.leftTendril.xRot = tendrilXRot;
      this.rightTendril.xRot = -tendrilXRot;
   }
}
