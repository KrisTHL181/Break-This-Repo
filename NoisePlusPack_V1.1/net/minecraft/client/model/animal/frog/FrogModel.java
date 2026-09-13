package net.minecraft.client.model.animal.frog;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.FrogAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrogModel extends EntityModel<FrogRenderState> {
   private static final float MAX_WALK_ANIMATION_SPEED = 1.5F;
   private static final float MAX_SWIM_ANIMATION_SPEED = 1.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   private final ModelPart body = this.root.getChild("body");
   private final ModelPart head = this.body.getChild("head");
   private final ModelPart eyes = this.head.getChild("eyes");
   private final ModelPart tongue = this.body.getChild("tongue");
   private final ModelPart leftArm = this.body.getChild("left_arm");
   private final ModelPart rightArm = this.body.getChild("right_arm");
   private final ModelPart leftLeg = this.root.getChild("left_leg");
   private final ModelPart rightLeg = this.root.getChild("right_leg");
   private final ModelPart croakingBody = this.body.getChild("croaking_body");
   private final KeyframeAnimation jumpAnimation;
   private final KeyframeAnimation croakAnimation;
   private final KeyframeAnimation tongueAnimation;
   private final KeyframeAnimation swimAnimation;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation idleWaterAnimation;

   public FrogModel(ModelPart p_452598_) {
      super(p_452598_.getChild("root"));
      this.jumpAnimation = FrogAnimation.FROG_JUMP.bake(p_452598_);
      this.croakAnimation = FrogAnimation.FROG_CROAK.bake(p_452598_);
      this.tongueAnimation = FrogAnimation.FROG_TONGUE.bake(p_452598_);
      this.swimAnimation = FrogAnimation.FROG_SWIM.bake(p_452598_);
      this.walkAnimation = FrogAnimation.FROG_WALK.bake(p_452598_);
      this.idleWaterAnimation = FrogAnimation.FROG_IDLE_WATER.bake(p_452598_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(3, 1).addBox(-3.5F, -2.0F, -8.0F, 7.0F, 3.0F, 9.0F).texOffs(23, 22).addBox(-3.5F, -1.0F, -8.0F, 7.0F, 0.0F, 9.0F),
         PartPose.offset(0.0F, -2.0F, 4.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(23, 13).addBox(-3.5F, -1.0F, -7.0F, 7.0F, 0.0F, 9.0F).texOffs(0, 13).addBox(-3.5F, -2.0F, -7.0F, 7.0F, 3.0F, 9.0F),
         PartPose.offset(0.0F, -2.0F, -1.0F)
      );
      PartDefinition partdefinition4 = partdefinition3.addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.offset(-0.5F, 0.0F, 2.0F));
      partdefinition4.addOrReplaceChild(
         "right_eye", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(-1.5F, -3.0F, -6.5F)
      );
      partdefinition4.addOrReplaceChild(
         "left_eye", CubeListBuilder.create().texOffs(0, 5).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(2.5F, -3.0F, -6.5F)
      );
      partdefinition2.addOrReplaceChild(
         "croaking_body",
         CubeListBuilder.create().texOffs(26, 5).addBox(-3.5F, -0.1F, -2.9F, 7.0F, 2.0F, 3.0F, new CubeDeformation(-0.1F)),
         PartPose.offset(0.0F, -1.0F, -5.0F)
      );
      PartDefinition partdefinition5 = partdefinition2.addOrReplaceChild(
         "tongue", CubeListBuilder.create().texOffs(17, 13).addBox(-2.0F, 0.0F, -7.1F, 4.0F, 0.0F, 7.0F), PartPose.offset(0.0F, -1.01F, 1.0F)
      );
      PartDefinition partdefinition6 = partdefinition2.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F), PartPose.offset(4.0F, -1.0F, -6.5F)
      );
      partdefinition6.addOrReplaceChild(
         "left_hand", CubeListBuilder.create().texOffs(18, 40).addBox(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F), PartPose.offset(0.0F, 3.0F, -1.0F)
      );
      PartDefinition partdefinition7 = partdefinition2.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(0, 38).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 3.0F), PartPose.offset(-4.0F, -1.0F, -6.5F)
      );
      partdefinition7.addOrReplaceChild(
         "right_hand", CubeListBuilder.create().texOffs(2, 40).addBox(-4.0F, 0.01F, -5.0F, 8.0F, 0.0F, 8.0F), PartPose.offset(0.0F, 3.0F, 0.0F)
      );
      PartDefinition partdefinition8 = partdefinition1.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(14, 25).addBox(-1.0F, 0.0F, -2.0F, 3.0F, 3.0F, 4.0F), PartPose.offset(3.5F, -3.0F, 4.0F)
      );
      partdefinition8.addOrReplaceChild(
         "left_foot", CubeListBuilder.create().texOffs(2, 32).addBox(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F), PartPose.offset(2.0F, 3.0F, 0.0F)
      );
      PartDefinition partdefinition9 = partdefinition1.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 3.0F, 4.0F), PartPose.offset(-3.5F, -3.0F, 4.0F)
      );
      partdefinition9.addOrReplaceChild(
         "right_foot", CubeListBuilder.create().texOffs(18, 32).addBox(-4.0F, 0.01F, -4.0F, 8.0F, 0.0F, 8.0F), PartPose.offset(-2.0F, 3.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 48, 48);
   }

   public void setupAnim(FrogRenderState p_452022_) {
      super.setupAnim(p_452022_);
      this.jumpAnimation.apply(p_452022_.jumpAnimationState, p_452022_.ageInTicks);
      this.croakAnimation.apply(p_452022_.croakAnimationState, p_452022_.ageInTicks);
      this.tongueAnimation.apply(p_452022_.tongueAnimationState, p_452022_.ageInTicks);
      if (p_452022_.isSwimming) {
         this.swimAnimation.applyWalk(p_452022_.walkAnimationPos, p_452022_.walkAnimationSpeed, 1.0F, 2.5F);
      } else {
         this.walkAnimation.applyWalk(p_452022_.walkAnimationPos, p_452022_.walkAnimationSpeed, 1.5F, 2.5F);
      }

      this.idleWaterAnimation.apply(p_452022_.swimIdleAnimationState, p_452022_.ageInTicks);
      this.croakingBody.visible = p_452022_.croakAnimationState.isStarted();
   }
}
