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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public WardenModel(ModelPart p_460181_) {
      super(p_460181_, RenderTypes::entityCutoutNoCull);
      this.bone = p_460181_.getChild("bone");
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
      this.attackAnimation = WardenAnimation.WARDEN_ATTACK.bake(p_460181_);
      this.sonicBoomAnimation = WardenAnimation.WARDEN_SONIC_BOOM.bake(p_460181_);
      this.diggingAnimation = WardenAnimation.WARDEN_DIG.bake(p_460181_);
      this.emergeAnimation = WardenAnimation.WARDEN_EMERGE.bake(p_460181_);
      this.roarAnimation = WardenAnimation.WARDEN_ROAR.bake(p_460181_);
      this.sniffAnimation = WardenAnimation.WARDEN_SNIFF.bake(p_460181_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -13.0F, -4.0F, 18.0F, 21.0F, 11.0F), PartPose.offset(0.0F, -21.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_ribcage", CubeListBuilder.create().texOffs(90, 11).addBox(-2.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F), PartPose.offset(-7.0F, -2.0F, -4.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_ribcage",
         CubeListBuilder.create().texOffs(90, 11).mirror().addBox(-7.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F).mirror(false),
         PartPose.offset(7.0F, -2.0F, -4.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -5.0F, 16.0F, 16.0F, 10.0F), PartPose.offset(0.0F, -13.0F, 0.0F)
      );
      partdefinition3.addOrReplaceChild(
         "right_tendril", CubeListBuilder.create().texOffs(52, 32).addBox(-16.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(-8.0F, -12.0F, 0.0F)
      );
      partdefinition3.addOrReplaceChild(
         "left_tendril", CubeListBuilder.create().texOffs(58, 0).addBox(0.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(8.0F, -12.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(44, 50).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(-13.0F, -13.0F, 1.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(0, 58).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(13.0F, -13.0F, 1.0F)
      );
      partdefinition1.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(76, 48).addBox(-3.1F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(-5.9F, -13.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(76, 76).addBox(-2.9F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(5.9F, -13.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public static LayerDefinition createTendrilsLayer() {
      return createBodyLayer().apply(p_460945_ -> {
         p_460945_.getRoot().retainExactParts(Set.of("left_tendril", "right_tendril"));
         return p_460945_;
      });
   }

   public static LayerDefinition createHeartLayer() {
      return createBodyLayer().apply(p_460370_ -> {
         p_460370_.getRoot().retainExactParts(Set.of("body"));
         return p_460370_;
      });
   }

   public static LayerDefinition createBioluminescentLayer() {
      return createBodyLayer().apply(p_456674_ -> {
         p_456674_.getRoot().retainExactParts(Set.of("head", "left_arm", "right_arm", "left_leg", "right_leg"));
         return p_456674_;
      });
   }

   public static LayerDefinition createPulsatingSpotsLayer() {
      return createBodyLayer().apply(p_454714_ -> {
         p_454714_.getRoot().retainExactParts(Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"));
         return p_454714_;
      });
   }

   public void setupAnim(WardenRenderState p_456556_) {
      super.setupAnim(p_456556_);
      this.animateHeadLookTarget(p_456556_.yRot, p_456556_.xRot);
      this.animateWalk(p_456556_.walkAnimationPos, p_456556_.walkAnimationSpeed);
      this.animateIdlePose(p_456556_.ageInTicks);
      this.animateTendrils(p_456556_, p_456556_.ageInTicks);
      this.attackAnimation.apply(p_456556_.attackAnimationState, p_456556_.ageInTicks);
      this.sonicBoomAnimation.apply(p_456556_.sonicBoomAnimationState, p_456556_.ageInTicks);
      this.diggingAnimation.apply(p_456556_.diggingAnimationState, p_456556_.ageInTicks);
      this.emergeAnimation.apply(p_456556_.emergeAnimationState, p_456556_.ageInTicks);
      this.roarAnimation.apply(p_456556_.roarAnimationState, p_456556_.ageInTicks);
      this.sniffAnimation.apply(p_456556_.sniffAnimationState, p_456556_.ageInTicks);
   }

   private void animateHeadLookTarget(float p_455935_, float p_452836_) {
      this.head.xRot = p_452836_ * (float) (Math.PI / 180.0);
      this.head.yRot = p_455935_ * (float) (Math.PI / 180.0);
   }

   private void animateIdlePose(float p_453846_) {
      float f = p_453846_ * 0.1F;
      float f1 = Mth.cos(f);
      float f2 = Mth.sin(f);
      this.head.zRot += 0.06F * f1;
      this.head.xRot += 0.06F * f2;
      this.body.zRot += 0.025F * f2;
      this.body.xRot += 0.025F * f1;
   }

   private void animateWalk(float p_451760_, float p_459449_) {
      float f = Math.min(0.5F, 3.0F * p_459449_);
      float f1 = p_451760_ * 0.8662F;
      float f2 = Mth.cos(f1);
      float f3 = Mth.sin(f1);
      float f4 = Math.min(0.35F, f);
      this.head.zRot += 0.3F * f3 * f;
      this.head.xRot = this.head.xRot + 1.2F * Mth.cos(f1 + (float) (Math.PI / 2)) * f4;
      this.body.zRot = 0.1F * f3 * f;
      this.body.xRot = 1.0F * f2 * f4;
      this.leftLeg.xRot = 1.0F * f2 * f;
      this.rightLeg.xRot = 1.0F * Mth.cos(f1 + (float) Math.PI) * f;
      this.leftArm.xRot = -(0.8F * f2 * f);
      this.leftArm.zRot = 0.0F;
      this.rightArm.xRot = -(0.8F * f3 * f);
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

   private void animateTendrils(WardenRenderState p_452185_, float p_450685_) {
      float f = p_452185_.tendrilAnimation * (float)(Math.cos(p_450685_ * 2.25) * Math.PI * 0.1F);
      this.leftTendril.xRot = f;
      this.rightTendril.xRot = -f;
   }
}
