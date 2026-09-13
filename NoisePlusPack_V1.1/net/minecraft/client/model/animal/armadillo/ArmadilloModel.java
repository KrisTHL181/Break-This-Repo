package net.minecraft.client.model.animal.armadillo;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.ArmadilloAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmadilloModel extends EntityModel<ArmadilloRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.6F);
   private static final float MAX_DOWN_HEAD_ROTATION_EXTENT = 25.0F;
   private static final float MAX_UP_HEAD_ROTATION_EXTENT = 22.5F;
   private static final float MAX_WALK_ANIMATION_SPEED = 16.5F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 2.5F;
   private static final String HEAD_CUBE = "head_cube";
   private static final String RIGHT_EAR_CUBE = "right_ear_cube";
   private static final String LEFT_EAR_CUBE = "left_ear_cube";
   private final ModelPart body;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart cube;
   private final ModelPart head;
   private final ModelPart tail;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation rollOutAnimation;
   private final KeyframeAnimation rollUpAnimation;
   private final KeyframeAnimation peekAnimation;

   public ArmadilloModel(ModelPart p_452637_) {
      super(p_452637_);
      this.body = p_452637_.getChild("body");
      this.rightHindLeg = p_452637_.getChild("right_hind_leg");
      this.leftHindLeg = p_452637_.getChild("left_hind_leg");
      this.head = this.body.getChild("head");
      this.tail = this.body.getChild("tail");
      this.cube = p_452637_.getChild("cube");
      this.walkAnimation = ArmadilloAnimation.ARMADILLO_WALK.bake(p_452637_);
      this.rollOutAnimation = ArmadilloAnimation.ARMADILLO_ROLL_OUT.bake(p_452637_);
      this.rollUpAnimation = ArmadilloAnimation.ARMADILLO_ROLL_UP.bake(p_452637_);
      this.peekAnimation = ArmadilloAnimation.ARMADILLO_PEEK.bake(p_452637_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 20)
            .addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.3F))
            .texOffs(0, 40)
            .addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 21.0F, 4.0F)
      );
      partdefinition1.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(44, 53).addBox(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -11.0F));
      partdefinition2.addOrReplaceChild(
         "head_cube",
         CubeListBuilder.create().texOffs(43, 15).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, 0.0F));
      partdefinition3.addOrReplaceChild(
         "right_ear_cube",
         CubeListBuilder.create().texOffs(43, 10).addBox(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F)
      );
      PartDefinition partdefinition4 = partdefinition2.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0F, -2.0F, 0.0F));
      partdefinition4.addOrReplaceChild(
         "left_ear_cube",
         CubeListBuilder.create().texOffs(47, 10).addBox(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(51, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 21.0F, 4.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(42, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 21.0F, 4.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(51, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 21.0F, -4.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(42, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 21.0F, -4.0F)
      );
      partdefinition.addOrReplaceChild(
         "cube",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(ArmadilloRenderState p_453826_) {
      super.setupAnim(p_453826_);
      if (p_453826_.isHidingInShell) {
         this.body.skipDraw = true;
         this.leftHindLeg.visible = false;
         this.rightHindLeg.visible = false;
         this.tail.visible = false;
         this.cube.visible = true;
      } else {
         this.body.skipDraw = false;
         this.leftHindLeg.visible = true;
         this.rightHindLeg.visible = true;
         this.tail.visible = true;
         this.cube.visible = false;
         this.head.xRot = Mth.clamp(p_453826_.xRot, -22.5F, 25.0F) * (float) (Math.PI / 180.0);
         this.head.yRot = Mth.clamp(p_453826_.yRot, -32.5F, 32.5F) * (float) (Math.PI / 180.0);
      }

      this.walkAnimation.applyWalk(p_453826_.walkAnimationPos, p_453826_.walkAnimationSpeed, 16.5F, 2.5F);
      this.rollOutAnimation.apply(p_453826_.rollOutAnimationState, p_453826_.ageInTicks);
      this.rollUpAnimation.apply(p_453826_.rollUpAnimationState, p_453826_.ageInTicks);
      this.peekAnimation.apply(p_453826_.peekAnimationState, p_453826_.ageInTicks);
   }
}
