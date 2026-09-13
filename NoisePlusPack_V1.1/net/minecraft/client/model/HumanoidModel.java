package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidModel<T extends HumanoidRenderState> extends EntityModel<T> implements ArmedModel<T>, HeadedModel {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 16.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head"));
   public static final float OVERLAY_SCALE = 0.25F;
   public static final float HAT_OVERLAY_SCALE = 0.5F;
   public static final float LEGGINGS_OVERLAY_SCALE = -0.1F;
   private static final float DUCK_WALK_ROTATION = 0.005F;
   private static final float SPYGLASS_ARM_ROT_Y = (float) (Math.PI / 12);
   private static final float SPYGLASS_ARM_ROT_X = 1.9198622F;
   private static final float SPYGLASS_ARM_CROUCH_ROT_X = (float) (Math.PI / 12);
   private static final float HIGHEST_SHIELD_BLOCKING_ANGLE = (float) (-Math.PI * 4.0 / 9.0);
   private static final float LOWEST_SHIELD_BLOCKING_ANGLE = 0.43633232F;
   private static final float HORIZONTAL_SHIELD_MOVEMENT_LIMIT = (float) (Math.PI / 6);
   public static final float TOOT_HORN_XROT_BASE = 1.4835298F;
   public static final float TOOT_HORN_YROT_BASE = (float) (Math.PI / 6);
   public final ModelPart head;
   public final ModelPart hat;
   public final ModelPart body;
   public final ModelPart rightArm;
   public final ModelPart leftArm;
   public final ModelPart rightLeg;
   public final ModelPart leftLeg;

   public HumanoidModel(ModelPart p_170677_) {
      this(p_170677_, RenderTypes::entityCutoutNoCull);
   }

   public HumanoidModel(ModelPart p_170679_, Function<Identifier, RenderType> p_170680_) {
      super(p_170679_, p_170680_);
      this.head = p_170679_.getChild("head");
      this.hat = this.head.getChild("hat");
      this.body = p_170679_.getChild("body");
      this.rightArm = p_170679_.getChild("right_arm");
      this.leftArm = p_170679_.getChild("left_arm");
      this.rightLeg = p_170679_.getChild("right_leg");
      this.leftLeg = p_170679_.getChild("left_leg");
   }

   public static MeshDefinition createMesh(CubeDeformation p_170682_, float p_170683_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, p_170682_), PartPose.offset(0.0F, 0.0F + p_170683_, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, p_170682_.extend(0.5F)), PartPose.ZERO
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, p_170682_),
         PartPose.offset(0.0F, 0.0F + p_170683_, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_170682_),
         PartPose.offset(-5.0F, 2.0F + p_170683_, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_170682_),
         PartPose.offset(5.0F, 2.0F + p_170683_, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_170682_),
         PartPose.offset(-1.9F, 12.0F + p_170683_, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_170682_),
         PartPose.offset(1.9F, 12.0F + p_170683_, 0.0F)
      );
      return meshdefinition;
   }

   public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation p_424715_, CubeDeformation p_426370_) {
      return createArmorMeshSet(HumanoidModel::createBaseArmorMesh, p_424715_, p_426370_);
   }

   protected static ArmorModelSet<MeshDefinition> createArmorMeshSet(
      Function<CubeDeformation, MeshDefinition> p_425316_, CubeDeformation p_424092_, CubeDeformation p_426116_
   ) {
      MeshDefinition meshdefinition = p_425316_.apply(p_426116_);
      meshdefinition.getRoot().retainPartsAndChildren(Set.of("head"));
      MeshDefinition meshdefinition1 = p_425316_.apply(p_426116_);
      meshdefinition1.getRoot().retainExactParts(Set.of("body", "left_arm", "right_arm"));
      MeshDefinition meshdefinition2 = p_425316_.apply(p_424092_);
      meshdefinition2.getRoot().retainExactParts(Set.of("left_leg", "right_leg", "body"));
      MeshDefinition meshdefinition3 = p_425316_.apply(p_426116_);
      meshdefinition3.getRoot().retainExactParts(Set.of("left_leg", "right_leg"));
      return new ArmorModelSet<>(meshdefinition, meshdefinition1, meshdefinition2, meshdefinition3);
   }

   private static MeshDefinition createBaseArmorMesh(CubeDeformation p_428399_) {
      MeshDefinition meshdefinition = createMesh(p_428399_, 0.0F);
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_428399_.extend(-0.1F)),
         PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_428399_.extend(-0.1F)),
         PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      return meshdefinition;
   }

   public void setupAnim(T p_364094_) {
      super.setupAnim(p_364094_);
      HumanoidModel.ArmPose humanoidmodel$armpose = p_364094_.leftArmPose;
      HumanoidModel.ArmPose humanoidmodel$armpose1 = p_364094_.rightArmPose;
      float f = p_364094_.swimAmount;
      boolean flag = p_364094_.isFallFlying;
      this.head.xRot = p_364094_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_364094_.yRot * (float) (Math.PI / 180.0);
      if (flag) {
         this.head.xRot = (float) (-Math.PI / 4);
      } else if (f > 0.0F) {
         this.head.xRot = Mth.rotLerpRad(f, this.head.xRot, (float) (-Math.PI / 4));
      }

      float f1 = p_364094_.walkAnimationPos;
      float f2 = p_364094_.walkAnimationSpeed;
      this.rightArm.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 2.0F * f2 * 0.5F / p_364094_.speedValue;
      this.leftArm.xRot = Mth.cos(f1 * 0.6662F) * 2.0F * f2 * 0.5F / p_364094_.speedValue;
      this.rightLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f2 / p_364094_.speedValue;
      this.leftLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f2 / p_364094_.speedValue;
      this.rightLeg.yRot = 0.005F;
      this.leftLeg.yRot = -0.005F;
      this.rightLeg.zRot = 0.005F;
      this.leftLeg.zRot = -0.005F;
      if (p_364094_.isPassenger) {
         this.rightArm.xRot += (float) (-Math.PI / 5);
         this.leftArm.xRot += (float) (-Math.PI / 5);
         this.rightLeg.xRot = -1.4137167F;
         this.rightLeg.yRot = (float) (Math.PI / 10);
         this.rightLeg.zRot = 0.07853982F;
         this.leftLeg.xRot = -1.4137167F;
         this.leftLeg.yRot = (float) (-Math.PI / 10);
         this.leftLeg.zRot = -0.07853982F;
      }

      boolean flag1 = p_364094_.mainArm == HumanoidArm.RIGHT;
      if (p_364094_.isUsingItem) {
         boolean flag2 = p_364094_.useItemHand == InteractionHand.MAIN_HAND;
         if (flag2 == flag1) {
            this.poseRightArm(p_364094_);
            if (!p_364094_.rightArmPose.affectsOffhandPose()) {
               this.poseLeftArm(p_364094_);
            }
         } else {
            this.poseLeftArm(p_364094_);
            if (!p_364094_.leftArmPose.affectsOffhandPose()) {
               this.poseRightArm(p_364094_);
            }
         }
      } else {
         boolean flag3 = flag1 ? humanoidmodel$armpose.isTwoHanded() : humanoidmodel$armpose1.isTwoHanded();
         if (flag1 != flag3) {
            this.poseLeftArm(p_364094_);
            if (!p_364094_.leftArmPose.affectsOffhandPose()) {
               this.poseRightArm(p_364094_);
            }
         } else {
            this.poseRightArm(p_364094_);
            if (!p_364094_.rightArmPose.affectsOffhandPose()) {
               this.poseLeftArm(p_364094_);
            }
         }
      }

      this.setupAttackAnimation(p_364094_);
      if (p_364094_.isCrouching) {
         this.body.xRot = 0.5F;
         this.rightArm.xRot += 0.4F;
         this.leftArm.xRot += 0.4F;
         this.rightLeg.z += 4.0F;
         this.leftLeg.z += 4.0F;
         this.head.y += 4.2F;
         this.body.y += 3.2F;
         this.leftArm.y += 3.2F;
         this.rightArm.y += 3.2F;
      }

      if (humanoidmodel$armpose1 != HumanoidModel.ArmPose.SPYGLASS) {
         AnimationUtils.bobModelPart(this.rightArm, p_364094_.ageInTicks, 1.0F);
      }

      if (humanoidmodel$armpose != HumanoidModel.ArmPose.SPYGLASS) {
         AnimationUtils.bobModelPart(this.leftArm, p_364094_.ageInTicks, -1.0F);
      }

      if (f > 0.0F) {
         float f7 = f1 % 26.0F;
         HumanoidArm humanoidarm = p_364094_.attackArm;
         float f3 = p_364094_.rightArmPose == HumanoidModel.ArmPose.SPEAR || humanoidarm == HumanoidArm.RIGHT && p_364094_.attackTime > 0.0F ? 0.0F : f;
         float f4 = p_364094_.leftArmPose == HumanoidModel.ArmPose.SPEAR || humanoidarm == HumanoidArm.LEFT && p_364094_.attackTime > 0.0F ? 0.0F : f;
         if (!p_364094_.isUsingItem) {
            if (f7 < 14.0F) {
               this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, 0.0F);
               this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, 0.0F);
               this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
               this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
               this.leftArm.zRot = Mth.rotLerpRad(
                  f4, this.leftArm.zRot, (float) Math.PI + 1.8707964F * this.quadraticArmUpdate(f7) / this.quadraticArmUpdate(14.0F)
               );
               this.rightArm.zRot = Mth.lerp(
                  f3, this.rightArm.zRot, (float) Math.PI - 1.8707964F * this.quadraticArmUpdate(f7) / this.quadraticArmUpdate(14.0F)
               );
            } else if (f7 >= 14.0F && f7 < 22.0F) {
               float f8 = (f7 - 14.0F) / 8.0F;
               this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, (float) (Math.PI / 2) * f8);
               this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, (float) (Math.PI / 2) * f8);
               this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
               this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
               this.leftArm.zRot = Mth.rotLerpRad(f4, this.leftArm.zRot, 5.012389F - 1.8707964F * f8);
               this.rightArm.zRot = Mth.lerp(f3, this.rightArm.zRot, 1.2707963F + 1.8707964F * f8);
            } else if (f7 >= 22.0F && f7 < 26.0F) {
               float f5 = (f7 - 22.0F) / 4.0F;
               this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * f5);
               this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * f5);
               this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
               this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
               this.leftArm.zRot = Mth.rotLerpRad(f4, this.leftArm.zRot, (float) Math.PI);
               this.rightArm.zRot = Mth.lerp(f3, this.rightArm.zRot, (float) Math.PI);
            }
         }

         float f9 = 0.3F;
         float f6 = 0.33333334F;
         this.leftLeg.xRot = Mth.lerp(f, this.leftLeg.xRot, 0.3F * Mth.cos(f1 * 0.33333334F + (float) Math.PI));
         this.rightLeg.xRot = Mth.lerp(f, this.rightLeg.xRot, 0.3F * Mth.cos(f1 * 0.33333334F));
      }
   }

   private void poseRightArm(T p_362371_) {
      switch (p_362371_.rightArmPose) {
         case EMPTY:
            this.rightArm.yRot = 0.0F;
            break;
         case ITEM:
            this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 10);
            this.rightArm.yRot = 0.0F;
            break;
         case BLOCK:
            this.poseBlockingArm(this.rightArm, true);
            break;
         case BOW_AND_ARROW:
            this.rightArm.yRot = -0.1F + this.head.yRot;
            this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
            this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            break;
         case THROW_TRIDENT:
            this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) Math.PI;
            this.rightArm.yRot = 0.0F;
            break;
         case CROSSBOW_CHARGE:
            AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, p_362371_.maxCrossbowChargeDuration, p_362371_.ticksUsingItem, true);
            break;
         case CROSSBOW_HOLD:
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
            break;
         case SPYGLASS:
            this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (p_362371_.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
            this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 12);
            break;
         case TOOT_HORN:
            this.rightArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
            this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 6);
            break;
         case BRUSH:
            this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 5);
            this.rightArm.yRot = 0.0F;
            break;
         case SPEAR:
            SpearAnimations.thirdPersonHandUse(this.rightArm, this.head, true, p_362371_.getUseItemStackForArm(HumanoidArm.RIGHT), p_362371_);
      }
   }

   private void poseLeftArm(T p_363560_) {
      switch (p_363560_.leftArmPose) {
         case EMPTY:
            this.leftArm.yRot = 0.0F;
            break;
         case ITEM:
            this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 10);
            this.leftArm.yRot = 0.0F;
            break;
         case BLOCK:
            this.poseBlockingArm(this.leftArm, false);
            break;
         case BOW_AND_ARROW:
            this.rightArm.yRot = -0.1F + this.head.yRot - 0.4F;
            this.leftArm.yRot = 0.1F + this.head.yRot;
            this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
            break;
         case THROW_TRIDENT:
            this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) Math.PI;
            this.leftArm.yRot = 0.0F;
            break;
         case CROSSBOW_CHARGE:
            AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, p_363560_.maxCrossbowChargeDuration, p_363560_.ticksUsingItem, false);
            break;
         case CROSSBOW_HOLD:
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, false);
            break;
         case SPYGLASS:
            this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (p_363560_.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
            this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 12);
            break;
         case TOOT_HORN:
            this.leftArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
            this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 6);
            break;
         case BRUSH:
            this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 5);
            this.leftArm.yRot = 0.0F;
            break;
         case SPEAR:
            SpearAnimations.thirdPersonHandUse(this.leftArm, this.head, false, p_363560_.getUseItemStackForArm(HumanoidArm.LEFT), p_363560_);
      }
   }

   private void poseBlockingArm(ModelPart p_312070_, boolean p_311335_) {
      p_312070_.xRot = p_312070_.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
      p_312070_.yRot = (p_311335_ ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + Mth.clamp(this.head.yRot, (float) (-Math.PI / 6), (float) (Math.PI / 6));
   }

   protected void setupAttackAnimation(T p_367078_) {
      float f = p_367078_.attackTime;
      if (!(f <= 0.0F)) {
         this.body.yRot = Mth.sin(Mth.sqrt(f) * (float) (Math.PI * 2)) * 0.2F;
         if (p_367078_.attackArm == HumanoidArm.LEFT) {
            this.body.yRot *= -1.0F;
         }

         float f1 = p_367078_.ageScale;
         this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F * f1;
         this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F * f1;
         this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F * f1;
         this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F * f1;
         this.rightArm.yRot = this.rightArm.yRot + this.body.yRot;
         this.leftArm.yRot = this.leftArm.yRot + this.body.yRot;
         this.leftArm.xRot = this.leftArm.xRot + this.body.yRot;
         switch (p_367078_.swingAnimationType) {
            case WHACK:
               float f2 = Ease.outQuart(f);
               float f3 = Mth.sin(f2 * (float) Math.PI);
               float f4 = Mth.sin(f * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
               ModelPart modelpart = this.getArm(p_367078_.attackArm);
               modelpart.xRot -= f3 * 1.2F + f4;
               modelpart.yRot = modelpart.yRot + this.body.yRot * 2.0F;
               modelpart.zRot = modelpart.zRot + Mth.sin(f * (float) Math.PI) * -0.4F;
            case NONE:
            default:
               break;
            case STAB:
               SpearAnimations.thirdPersonAttackHand(this, p_367078_);
         }
      }
   }

   private float quadraticArmUpdate(float p_102834_) {
      return -65.0F * p_102834_ + p_102834_ * p_102834_;
   }

   public void setAllVisible(boolean p_102880_) {
      this.head.visible = p_102880_;
      this.hat.visible = p_102880_;
      this.body.visible = p_102880_;
      this.rightArm.visible = p_102880_;
      this.leftArm.visible = p_102880_;
      this.rightLeg.visible = p_102880_;
      this.leftLeg.visible = p_102880_;
   }

   public void translateToHand(HumanoidRenderState p_422936_, HumanoidArm p_102854_, PoseStack p_102855_) {
      this.root.translateAndRotate(p_102855_);
      this.getArm(p_102854_).translateAndRotate(p_102855_);
   }

   public ModelPart getArm(HumanoidArm p_102852_) {
      return p_102852_ == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
   }

   @Override
   public ModelPart getHead() {
      return this.head;
   }

   @OnlyIn(Dist.CLIENT)
   public enum ArmPose {
      EMPTY(false, false),
      ITEM(false, false),
      BLOCK(false, false),
      BOW_AND_ARROW(true, true),
      THROW_TRIDENT(false, true),
      CROSSBOW_CHARGE(true, true),
      CROSSBOW_HOLD(true, true),
      SPYGLASS(false, false),
      TOOT_HORN(false, false),
      BRUSH(false, false),
      SPEAR(false, true) {
         @Override
         public <S extends ArmedEntityRenderState> void animateUseItem(
            S p_451431_, PoseStack p_460015_, float p_458986_, HumanoidArm p_456218_, ItemStack p_455006_
         ) {
            SpearAnimations.thirdPersonUseItem(p_451431_, p_460015_, p_458986_, p_456218_, p_455006_);
         }
      };

      private final boolean twoHanded;
      private final boolean affectsOffhandPose;

      ArmPose(final boolean p_102896_, final boolean p_456170_) {
         this.twoHanded = p_102896_;
         this.affectsOffhandPose = p_456170_;
      }

      public boolean isTwoHanded() {
         return this.twoHanded;
      }

      public boolean affectsOffhandPose() {
         return this.affectsOffhandPose;
      }

      public <S extends ArmedEntityRenderState> void animateUseItem(
         S p_454630_, PoseStack p_450679_, float p_452683_, HumanoidArm p_459813_, ItemStack p_458998_
      ) {
      }
   }
}
