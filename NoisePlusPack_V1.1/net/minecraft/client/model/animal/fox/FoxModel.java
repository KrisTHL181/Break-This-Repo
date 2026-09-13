package net.minecraft.client.model.animal.fox;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoxModel extends EntityModel<FoxRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 8.0F, 3.35F, Set.of("head"));
   public final ModelPart head;
   private final ModelPart body;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart tail;
   private static final int LEG_SIZE = 6;
   private static final float HEAD_HEIGHT = 16.5F;
   private static final float LEG_POS = 17.5F;
   private float legMotionPos;

   public FoxModel(ModelPart p_453105_) {
      super(p_453105_);
      this.head = p_453105_.getChild("head");
      this.body = p_453105_.getChild("body");
      this.rightHindLeg = p_453105_.getChild("right_hind_leg");
      this.leftHindLeg = p_453105_.getChild("left_hind_leg");
      this.rightFrontLeg = p_453105_.getChild("right_front_leg");
      this.leftFrontLeg = p_453105_.getChild("left_front_leg");
      this.tail = this.body.getChild("tail");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(1, 5).addBox(-3.0F, -2.0F, -5.0F, 8.0F, 6.0F, 6.0F), PartPose.offset(-1.0F, 16.5F, -3.0F)
      );
      partdefinition1.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(8, 1).addBox(-3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
      partdefinition1.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(15, 1).addBox(3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
      partdefinition1.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(6, 18).addBox(-1.0F, 2.01F, -8.0F, 4.0F, 2.0F, 3.0F), PartPose.ZERO);
      PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(24, 15).addBox(-3.0F, 3.999F, -3.5F, 6.0F, 11.0F, 6.0F),
         PartPose.offsetAndRotation(0.0F, 16.0F, -6.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeDeformation cubedeformation = new CubeDeformation(0.001F);
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(4, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, cubedeformation);
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(13, 24).addBox(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, cubedeformation);
      partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder1, PartPose.offset(-5.0F, 17.5F, 7.0F));
      partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(-1.0F, 17.5F, 7.0F));
      partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder1, PartPose.offset(-5.0F, 17.5F, 0.0F));
      partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder, PartPose.offset(-1.0F, 17.5F, 0.0F));
      partdefinition2.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(30, 0).addBox(2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 5.0F),
         PartPose.offsetAndRotation(-4.0F, 15.0F, -1.0F, -0.05235988F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 48, 32);
   }

   public void setupAnim(FoxRenderState p_459080_) {
      super.setupAnim(p_459080_);
      float f = p_459080_.walkAnimationSpeed;
      float f1 = p_459080_.walkAnimationPos;
      this.rightHindLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
      this.leftHindLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.rightFrontLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.leftFrontLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
      this.head.zRot = p_459080_.headRollAngle;
      this.rightHindLeg.visible = true;
      this.leftHindLeg.visible = true;
      this.rightFrontLeg.visible = true;
      this.leftFrontLeg.visible = true;
      float f2 = p_459080_.ageScale;
      if (p_459080_.isCrouching) {
         this.body.xRot += 0.10471976F;
         float f3 = p_459080_.crouchAmount;
         this.body.y += f3 * f2;
         this.head.y += f3 * f2;
      } else if (p_459080_.isSleeping) {
         this.body.zRot = (float) (-Math.PI / 2);
         this.body.y += 5.0F * f2;
         this.tail.xRot = (float) (-Math.PI * 5.0 / 6.0);
         if (p_459080_.isBaby) {
            this.tail.xRot = -2.1816616F;
            this.body.z += 2.0F;
         }

         this.head.x += 2.0F * f2;
         this.head.y += 2.99F * f2;
         this.head.yRot = (float) (-Math.PI * 2.0 / 3.0);
         this.head.zRot = 0.0F;
         this.rightHindLeg.visible = false;
         this.leftHindLeg.visible = false;
         this.rightFrontLeg.visible = false;
         this.leftFrontLeg.visible = false;
      } else if (p_459080_.isSitting) {
         this.body.xRot = (float) (Math.PI / 6);
         this.body.y -= 7.0F * f2;
         this.body.z += 3.0F * f2;
         this.tail.xRot = (float) (Math.PI / 4);
         this.tail.z -= 1.0F * f2;
         this.head.xRot = 0.0F;
         this.head.yRot = 0.0F;
         if (p_459080_.isBaby) {
            this.head.y--;
            this.head.z -= 0.375F;
         } else {
            this.head.y -= 6.5F;
            this.head.z += 2.75F;
         }

         this.rightHindLeg.xRot = (float) (-Math.PI * 5.0 / 12.0);
         this.rightHindLeg.y += 4.0F * f2;
         this.rightHindLeg.z -= 0.25F * f2;
         this.leftHindLeg.xRot = (float) (-Math.PI * 5.0 / 12.0);
         this.leftHindLeg.y += 4.0F * f2;
         this.leftHindLeg.z -= 0.25F * f2;
         this.rightFrontLeg.xRot = (float) (-Math.PI / 12);
         this.leftFrontLeg.xRot = (float) (-Math.PI / 12);
      }

      if (!p_459080_.isSleeping && !p_459080_.isFaceplanted && !p_459080_.isCrouching) {
         this.head.xRot = p_459080_.xRot * (float) (Math.PI / 180.0);
         this.head.yRot = p_459080_.yRot * (float) (Math.PI / 180.0);
      }

      if (p_459080_.isSleeping) {
         this.head.xRot = 0.0F;
         this.head.yRot = (float) (-Math.PI * 2.0 / 3.0);
         this.head.zRot = Mth.cos(p_459080_.ageInTicks * 0.027F) / 22.0F;
      }

      if (p_459080_.isCrouching) {
         float f4 = Mth.cos(p_459080_.ageInTicks) * 0.01F;
         this.body.yRot = f4;
         this.rightHindLeg.zRot = f4;
         this.leftHindLeg.zRot = f4;
         this.rightFrontLeg.zRot = f4 / 2.0F;
         this.leftFrontLeg.zRot = f4 / 2.0F;
      }

      if (p_459080_.isFaceplanted) {
         float f5 = 0.1F;
         this.legMotionPos += 0.67F;
         this.rightHindLeg.xRot = Mth.cos(this.legMotionPos * 0.4662F) * 0.1F;
         this.leftHindLeg.xRot = Mth.cos(this.legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
         this.rightFrontLeg.xRot = Mth.cos(this.legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
         this.leftFrontLeg.xRot = Mth.cos(this.legMotionPos * 0.4662F) * 0.1F;
      }
   }
}
