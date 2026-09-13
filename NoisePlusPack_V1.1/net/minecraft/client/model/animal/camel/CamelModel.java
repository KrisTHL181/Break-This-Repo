package net.minecraft.client.model.animal.camel;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.CamelAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CamelModel extends EntityModel<CamelRenderState> {
   private static final float MAX_WALK_ANIMATION_SPEED = 2.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.45F);
   protected final ModelPart head;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation sitAnimation;
   private final KeyframeAnimation sitPoseAnimation;
   private final KeyframeAnimation standupAnimation;
   private final KeyframeAnimation idleAnimation;
   private final KeyframeAnimation dashAnimation;

   public CamelModel(ModelPart p_450540_) {
      super(p_450540_);
      ModelPart modelpart = p_450540_.getChild("body");
      this.head = modelpart.getChild("head");
      this.walkAnimation = CamelAnimation.CAMEL_WALK.bake(p_450540_);
      this.sitAnimation = CamelAnimation.CAMEL_SIT.bake(p_450540_);
      this.sitPoseAnimation = CamelAnimation.CAMEL_SIT_POSE.bake(p_450540_);
      this.standupAnimation = CamelAnimation.CAMEL_STANDUP.bake(p_450540_);
      this.idleAnimation = CamelAnimation.CAMEL_IDLE.bake(p_450540_);
      this.dashAnimation = CamelAnimation.CAMEL_DASH.bake(p_450540_);
   }

   public static LayerDefinition createBodyLayer() {
      return LayerDefinition.create(createBodyMesh(), 128, 128);
   }

   protected static MeshDefinition createBodyMesh() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 25).addBox(-7.5F, -12.0F, -23.5F, 15.0F, 12.0F, 27.0F), PartPose.offset(0.0F, 4.0F, 9.5F)
      );
      partdefinition1.addOrReplaceChild(
         "hump", CubeListBuilder.create().texOffs(74, 0).addBox(-4.5F, -5.0F, -5.5F, 9.0F, 5.0F, 11.0F), PartPose.offset(0.0F, -12.0F, -10.0F)
      );
      partdefinition1.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(122, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 0.0F), PartPose.offset(0.0F, -9.0F, 3.5F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(60, 24)
            .addBox(-3.5F, -7.0F, -15.0F, 7.0F, 8.0F, 19.0F)
            .texOffs(21, 0)
            .addBox(-3.5F, -21.0F, -15.0F, 7.0F, 14.0F, 7.0F)
            .texOffs(50, 0)
            .addBox(-2.5F, -21.0F, -21.0F, 5.0F, 5.0F, 6.0F),
         PartPose.offset(0.0F, -3.0F, -19.5F)
      );
      partdefinition2.addOrReplaceChild(
         "left_ear", CubeListBuilder.create().texOffs(45, 0).addBox(-0.5F, 0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(2.5F, -21.0F, -9.5F)
      );
      partdefinition2.addOrReplaceChild(
         "right_ear", CubeListBuilder.create().texOffs(67, 0).addBox(-2.5F, 0.5F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(-2.5F, -21.0F, -9.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(58, 16).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F), PartPose.offset(4.9F, 1.0F, 9.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(94, 16).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F), PartPose.offset(-4.9F, 1.0F, 9.5F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F), PartPose.offset(4.9F, 1.0F, -10.5F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(0, 26).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 21.0F, 5.0F), PartPose.offset(-4.9F, 1.0F, -10.5F)
      );
      return meshdefinition;
   }

   public void setupAnim(CamelRenderState p_452981_) {
      super.setupAnim(p_452981_);
      this.applyHeadRotation(p_452981_, p_452981_.yRot, p_452981_.xRot);
      this.walkAnimation.applyWalk(p_452981_.walkAnimationPos, p_452981_.walkAnimationSpeed, 2.0F, 2.5F);
      this.sitAnimation.apply(p_452981_.sitAnimationState, p_452981_.ageInTicks);
      this.sitPoseAnimation.apply(p_452981_.sitPoseAnimationState, p_452981_.ageInTicks);
      this.standupAnimation.apply(p_452981_.sitUpAnimationState, p_452981_.ageInTicks);
      this.idleAnimation.apply(p_452981_.idleAnimationState, p_452981_.ageInTicks);
      this.dashAnimation.apply(p_452981_.dashAnimationState, p_452981_.ageInTicks);
   }

   private void applyHeadRotation(CamelRenderState p_450417_, float p_457570_, float p_453560_) {
      p_457570_ = Mth.clamp(p_457570_, -30.0F, 30.0F);
      p_453560_ = Mth.clamp(p_453560_, -25.0F, 45.0F);
      if (p_450417_.jumpCooldown > 0.0F) {
         float f = 45.0F * p_450417_.jumpCooldown / 55.0F;
         p_453560_ = Mth.clamp(p_453560_ + f, -25.0F, 70.0F);
      }

      this.head.yRot = p_457570_ * (float) (Math.PI / 180.0);
      this.head.xRot = p_453560_ * (float) (Math.PI / 180.0);
   }
}
