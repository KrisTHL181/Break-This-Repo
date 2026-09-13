package net.minecraft.client.model.animal.axolotl;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.util.Mth;

public class AdultAxolotlModel extends EntityModel<AxolotlRenderState> {
   private static final float SWIMMING_LEG_XROT = 1.8849558F;
   private final ModelPart tail;
   private final ModelPart leftHindLeg;
   private final ModelPart rightHindLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart body;
   private final ModelPart head;
   private final ModelPart topGills;
   private final ModelPart leftGills;
   private final ModelPart rightGills;

   public AdultAxolotlModel(final ModelPart root) {
      super(root);
      this.body = root.getChild("body");
      this.head = this.body.getChild("head");
      this.rightHindLeg = this.body.getChild("right_hind_leg");
      this.leftHindLeg = this.body.getChild("left_hind_leg");
      this.rightFrontLeg = this.body.getChild("right_front_leg");
      this.leftFrontLeg = this.body.getChild("left_front_leg");
      this.tail = this.body.getChild("tail");
      this.topGills = this.head.getChild("top_gills");
      this.leftGills = this.head.getChild("left_gills");
      this.rightGills = this.head.getChild("right_gills");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 11).addBox(-4.0F, -2.0F, -9.0F, 8.0F, 4.0F, 10.0F).texOffs(2, 17).addBox(0.0F, -3.0F, -8.0F, 0.0F, 5.0F, 9.0F),
         PartPose.offset(0.0F, 19.5F, 5.0F)
      );
      CubeDeformation fudge = new CubeDeformation(0.001F);
      PartDefinition head = body.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 1).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 5.0F, fudge), PartPose.offset(0.0F, 0.0F, -9.0F)
      );
      CubeListBuilder topGills = CubeListBuilder.create().texOffs(3, 37).addBox(-4.0F, -3.0F, 0.0F, 8.0F, 3.0F, 0.0F, fudge);
      CubeListBuilder leftGills = CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, fudge);
      CubeListBuilder rightGills = CubeListBuilder.create().texOffs(11, 40).addBox(0.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, fudge);
      head.addOrReplaceChild("top_gills", topGills, PartPose.offset(0.0F, -3.0F, -1.0F));
      head.addOrReplaceChild("left_gills", leftGills, PartPose.offset(-4.0F, 0.0F, -1.0F));
      head.addOrReplaceChild("right_gills", rightGills, PartPose.offset(4.0F, 0.0F, -1.0F));
      CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(2, 13).addBox(-1.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, fudge);
      CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(2, 13).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, fudge);
      body.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-3.5F, 1.0F, -1.0F));
      body.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(3.5F, 1.0F, -1.0F));
      body.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-3.5F, 1.0F, -8.0F));
      body.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(3.5F, 1.0F, -8.0F));
      body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(2, 19).addBox(0.0F, -3.0F, 0.0F, 0.0F, 5.0F, 12.0F), PartPose.offset(0.0F, 0.0F, 1.0F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final AxolotlRenderState state) {
      super.setupAnim(state);
      float playingDeadFactor = state.playingDeadFactor;
      float inWaterFactor = state.inWaterFactor;
      float onGroundFactor = state.onGroundFactor;
      float movingFactor = state.movingFactor;
      float notMovingFactor = 1.0F - movingFactor;
      float mirroredLegsFactor = 1.0F - Math.min(onGroundFactor, movingFactor);
      this.body.yRot = this.body.yRot + state.yRot * (float) (Math.PI / 180.0);
      this.setupSwimmingAnimation(state.ageInTicks, state.xRot, Math.min(movingFactor, inWaterFactor));
      this.setupWaterHoveringAnimation(state.ageInTicks, Math.min(notMovingFactor, inWaterFactor));
      this.setupGroundCrawlingAnimation(state.ageInTicks, Math.min(movingFactor, onGroundFactor));
      this.setupLayStillOnGroundAnimation(state.ageInTicks, Math.min(notMovingFactor, onGroundFactor));
      this.setupPlayDeadAnimation(playingDeadFactor);
      this.applyMirrorLegRotations(mirroredLegsFactor);
   }

   private void setupLayStillOnGroundAnimation(final float ageInTicks, final float factor) {
      if (!(factor <= 1.0E-5F)) {
         float animMoveSpeed = ageInTicks * 0.09F;
         float sineSway = Mth.sin(animMoveSpeed);
         float cosineSway = Mth.cos(animMoveSpeed);
         float movement = sineSway * sineSway - 2.0F * sineSway;
         float movement2 = cosineSway * cosineSway - 3.0F * sineSway;
         this.head.xRot += -0.09F * movement * factor;
         this.head.zRot += -0.2F * factor;
         this.tail.yRot += (-0.1F + 0.1F * movement) * factor;
         float gillAngle = (0.6F + 0.05F * movement2) * factor;
         this.topGills.xRot += gillAngle;
         this.leftGills.yRot -= gillAngle;
         this.rightGills.yRot += gillAngle;
         this.leftHindLeg.xRot += 1.1F * factor;
         this.leftHindLeg.yRot += 1.0F * factor;
         this.leftFrontLeg.xRot += 0.8F * factor;
         this.leftFrontLeg.yRot += 2.3F * factor;
         this.leftFrontLeg.zRot -= 0.5F * factor;
      }
   }

   private void setupGroundCrawlingAnimation(final float ageInTicks, final float factor) {
      if (!(factor <= 1.0E-5F)) {
         float animMoveSpeed = ageInTicks * 0.11F;
         float cosineSway = Mth.cos(animMoveSpeed);
         float hindLegYRotSway = (cosineSway * cosineSway - 2.0F * cosineSway) / 5.0F;
         float frontLegYRotSway = 0.7F * cosineSway;
         float headAndTailYRot = 0.09F * cosineSway * factor;
         this.head.yRot += headAndTailYRot;
         this.tail.yRot += headAndTailYRot;
         float gillAngle = (0.6F - 0.08F * (cosineSway * cosineSway + 2.0F * Mth.sin(animMoveSpeed))) * factor;
         this.topGills.xRot += gillAngle;
         this.leftGills.yRot -= gillAngle;
         this.rightGills.yRot += gillAngle;
         float hindLegXRot = 0.9424779F * factor;
         float frontLegXRot = 1.0995574F * factor;
         this.leftHindLeg.xRot += hindLegXRot;
         this.leftHindLeg.yRot += (1.5F - hindLegYRotSway) * factor;
         this.leftHindLeg.zRot += -0.1F * factor;
         this.leftFrontLeg.xRot += frontLegXRot;
         this.leftFrontLeg.yRot += ((float) (Math.PI / 2) - frontLegYRotSway) * factor;
         this.rightHindLeg.xRot += hindLegXRot;
         this.rightHindLeg.yRot += (-1.0F - hindLegYRotSway) * factor;
         this.rightFrontLeg.xRot += frontLegXRot;
         this.rightFrontLeg.yRot += ((float) (-Math.PI / 2) - frontLegYRotSway) * factor;
      }
   }

   private void setupWaterHoveringAnimation(final float ageInTicks, final float factor) {
      if (!(factor <= 1.0E-5F)) {
         float animMoveSpeed = ageInTicks * 0.075F;
         float cosineSway = Mth.cos(animMoveSpeed);
         float sineSway = Mth.sin(animMoveSpeed) * 0.15F;
         float bodyXRot = (-0.15F + 0.075F * cosineSway) * factor;
         this.body.xRot += bodyXRot;
         this.body.y -= sineSway * factor;
         this.head.xRot -= bodyXRot;
         this.topGills.xRot += 0.2F * cosineSway * factor;
         float gillYRot = (-0.3F * cosineSway - 0.19F) * factor;
         this.leftGills.yRot += gillYRot;
         this.rightGills.yRot -= gillYRot;
         this.leftHindLeg.xRot += ((float) (Math.PI * 3.0 / 4.0) - cosineSway * 0.11F) * factor;
         this.leftHindLeg.yRot += 0.47123894F * factor;
         this.leftHindLeg.zRot += 1.7278761F * factor;
         this.leftFrontLeg.xRot += ((float) (Math.PI / 4) - cosineSway * 0.2F) * factor;
         this.leftFrontLeg.yRot += 2.042035F * factor;
         this.tail.yRot += 0.5F * cosineSway * factor;
      }
   }

   private void setupSwimmingAnimation(final float ageInTicks, final float xRot, final float factor) {
      if (!(factor <= 1.0E-5F)) {
         float animMoveSpeed = ageInTicks * 0.33F;
         float sineSway = Mth.sin(animMoveSpeed);
         float cosineSway = Mth.cos(animMoveSpeed);
         float bodySway = 0.13F * sineSway;
         this.body.xRot += (xRot * (float) (Math.PI / 180.0) + bodySway) * factor;
         this.head.xRot -= bodySway * 1.8F * factor;
         this.body.y -= 0.45F * cosineSway * factor;
         this.topGills.xRot += (-0.5F * sineSway - 0.8F) * factor;
         float gillYRot = (0.3F * sineSway + 0.9F) * factor;
         this.leftGills.yRot += gillYRot;
         this.rightGills.yRot -= gillYRot;
         this.tail.yRot = this.tail.yRot + 0.3F * Mth.cos(animMoveSpeed * 0.9F) * factor;
         this.leftHindLeg.xRot += 1.8849558F * factor;
         this.leftHindLeg.yRot += -0.4F * sineSway * factor;
         this.leftHindLeg.zRot += (float) (Math.PI / 2) * factor;
         this.leftFrontLeg.xRot += 1.8849558F * factor;
         this.leftFrontLeg.yRot += (-0.2F * cosineSway - 0.1F) * factor;
         this.leftFrontLeg.zRot += (float) (Math.PI / 2) * factor;
      }
   }

   private void setupPlayDeadAnimation(final float factor) {
      if (!(factor <= 1.0E-5F)) {
         this.leftHindLeg.xRot += 1.4137167F * factor;
         this.leftHindLeg.yRot += 1.0995574F * factor;
         this.leftHindLeg.zRot += (float) (Math.PI / 4) * factor;
         this.leftFrontLeg.xRot += (float) (Math.PI / 4) * factor;
         this.leftFrontLeg.yRot += 2.042035F * factor;
         this.body.xRot += -0.15F * factor;
         this.body.zRot += 0.35F * factor;
      }
   }

   private void applyMirrorLegRotations(final float factor) {
      if (!(factor <= 1.0E-5F)) {
         this.rightHindLeg.xRot = this.rightHindLeg.xRot + this.leftHindLeg.xRot * factor;
         ModelPart var2 = this.rightHindLeg;
         var2.yRot = var2.yRot + -this.leftHindLeg.yRot * factor;
         var2 = this.rightHindLeg;
         var2.zRot = var2.zRot + -this.leftHindLeg.zRot * factor;
         this.rightFrontLeg.xRot = this.rightFrontLeg.xRot + this.leftFrontLeg.xRot * factor;
         var2 = this.rightFrontLeg;
         var2.yRot = var2.yRot + -this.leftFrontLeg.yRot * factor;
         var2 = this.rightFrontLeg;
         var2.zRot = var2.zRot + -this.leftFrontLeg.zRot * factor;
      }
   }
}
