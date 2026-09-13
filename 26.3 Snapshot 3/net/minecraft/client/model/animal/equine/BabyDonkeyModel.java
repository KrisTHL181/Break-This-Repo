package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.util.Mth;

public class BabyDonkeyModel extends DonkeyModel {
   public BabyDonkeyModel(final ModelPart root) {
      ModelPart body = root.getChild("body");
      ModelPart rightHindLeg = body.getChild("right_hind_leg");
      ModelPart leftHindLeg = body.getChild("left_hind_leg");
      ModelPart rightFrontLeg = body.getChild("right_front_leg");
      ModelPart leftFrontLeg = body.getChild("left_front_leg");
      ModelPart headParts = body.getChild("head_parts");
      ModelPart tail = body.getChild("tail");
      super(root, headParts, rightHindLeg, rightFrontLeg, leftHindLeg, leftFrontLeg, tail);
   }

   public static LayerDefinition createBabyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition body = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 13).addBox(-5.0F, -3.0F, -7.0F, 8.0F, 6.0F, 14.0F, new CubeDeformation(0.0F)),
         PartPose.offset(1.0F, 14.0F, 0.0F)
      );
      PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, 6.5F));
      tail.addOrReplaceChild(
         "tail_r1",
         CubeListBuilder.create().texOffs(24, 33).addBox(-2.5F, -1.0F, -0.5F, 3.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7418F, 0.0F, 0.0F)
      );
      body.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(12, 44).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.25F, 3.5F, 5.25F)
      );
      body.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(0, 44).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.4F, 3.5F, 5.4F)
      );
      body.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(12, 33).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.4F, 3.5F, -5.3F)
      );
      body.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.4F, 3.5F, -5.4F)
      );
      PartDefinition neck = body.addOrReplaceChild("head_parts", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, -5.0F));
      neck.addOrReplaceChild(
         "neck_r1",
         CubeListBuilder.create().texOffs(30, 9).addBox(-3.0F, -6.0F, -3.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F)
      );
      PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -5.0F, -3.0F));
      head.addOrReplaceChild(
         "head_r1",
         CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.6F, -8.4F, 6.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, 0.3927F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -6.5F, -0.3F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(2.0F, -3.5F, -1.0F, 0.48F, 0.0F, 0.48F)
      );
      head.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(22, 0).mirror().addBox(-2.0F, -6.5F, -0.3F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(-2.0F, -3.5F, -1.0F, 0.48F, 0.0F, -0.48F)
      );
      body.addOrReplaceChild("right_chest", CubeListBuilder.create(), PartPose.offset(-1.0F, 10.0F, 0.0F));
      body.addOrReplaceChild("left_chest", CubeListBuilder.create(), PartPose.offset(-1.0F, 10.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   @Override
   public void setupAnim(final DonkeyRenderState state) {
      super.setupAnim(state);
      state.xRot = -30.0F;
      float headRotXRad = state.xRot * (float) (Math.PI / 180.0);
      float eating = state.eatAnimation;
      float standing = state.standAnimation;
      float feedingAnim = state.feedingAnimation;
      float baseHeadAngle = (1.0F - Math.max(standing, eating)) * ((float) (Math.PI / 6) + headRotXRad + feedingAnim * Mth.sin(state.ageInTicks) * 0.05F);
      this.headParts.xRot = standing * ((float) (Math.PI / 12) + headRotXRad)
         + eating * ((float) (Math.PI / 2) + Mth.sin(state.ageInTicks) * 0.05F)
         + baseHeadAngle;
   }

   @Override
   protected void offsetLegPositionWhenStanding(final float standing) {
      this.leftHindLeg.y = Mth.lerp(standing, this.leftHindLeg.y, -0.3F);
      this.rightHindLeg.y = Mth.lerp(standing, this.leftHindLeg.y, -0.3F);
   }

   @Override
   protected float getLegStandAngle() {
      return (float) (Math.PI / 3);
   }

   @Override
   protected float getLegStandingYOffset() {
      return 1.0F;
   }

   @Override
   protected float getLegStandingZOffset() {
      return 0.5F;
   }

   @Override
   protected float getLegStandingXRotOffset() {
      return 0.0F;
   }

   @Override
   protected float getTailXRotOffset() {
      return (float) (-Math.PI / 4);
   }

   @Override
   protected void animateHeadPartsPlacement(final float eating, final float standing) {
      this.headParts.y = Mth.lerp(eating, this.headParts.y, -1.2F);
      this.headParts.z = Mth.lerp(standing, this.headParts.z, -3.6F);
   }
}
