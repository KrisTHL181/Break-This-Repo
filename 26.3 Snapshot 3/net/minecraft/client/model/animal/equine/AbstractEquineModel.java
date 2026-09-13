package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.util.Mth;

public abstract class AbstractEquineModel<T extends EquineRenderState> extends EntityModel<T> {
   private static final float DEG_125 = 2.1816616F;
   private static final float DEG_60 = (float) (Math.PI / 3);
   private static final float DEG_45 = (float) (Math.PI / 4);
   private static final float DEG_30 = (float) (Math.PI / 6);
   private static final float DEG_15 = (float) (Math.PI / 12);
   protected static final String HEAD_PARTS = "head_parts";
   protected final ModelPart body;
   protected final ModelPart headParts;
   protected final ModelPart rightHindLeg;
   protected final ModelPart leftHindLeg;
   protected final ModelPart rightFrontLeg;
   protected final ModelPart leftFrontLeg;
   private final ModelPart tail;

   public AbstractEquineModel(final ModelPart root) {
      super(root);
      this.body = root.getChild("body");
      this.headParts = root.getChild("head_parts");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
      this.tail = this.body.getChild("tail");
   }

   public AbstractEquineModel(
      final ModelPart root,
      final ModelPart headParts,
      final ModelPart rightHindLeg,
      final ModelPart rightFrontLeg,
      final ModelPart leftHindLeg,
      final ModelPart leftFrontLeg,
      final ModelPart tail
   ) {
      super(root);
      this.body = root.getChild("body");
      this.headParts = headParts;
      this.rightHindLeg = rightHindLeg;
      this.leftHindLeg = leftHindLeg;
      this.rightFrontLeg = rightFrontLeg;
      this.leftFrontLeg = leftFrontLeg;
      this.tail = tail;
   }

   public static MeshDefinition createBodyMesh(final CubeDeformation g) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.05F)),
         PartPose.offset(0.0F, 11.0F, 5.0F)
      );
      PartDefinition headParts = root.addOrReplaceChild(
         "head_parts",
         CubeListBuilder.create().texOffs(0, 35).addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F),
         PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
      );
      PartDefinition head = headParts.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, g), PartPose.ZERO
      );
      headParts.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(56, 36).addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, g), PartPose.ZERO);
      headParts.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, g), PartPose.ZERO);
      root.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, g),
         PartPose.offset(4.0F, 14.0F, 7.0F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, g), PartPose.offset(-4.0F, 14.0F, 7.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, g),
         PartPose.offset(4.0F, 14.0F, -10.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, g), PartPose.offset(-4.0F, 14.0F, -10.0F)
      );
      body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(42, 36).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F, g),
         PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_ear", CubeListBuilder.create().texOffs(19, 16).addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F)), PartPose.ZERO
      );
      head.addOrReplaceChild(
         "right_ear", CubeListBuilder.create().texOffs(19, 16).addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F)), PartPose.ZERO
      );
      return mesh;
   }

   public void setupAnim(final T state) {
      super.setupAnim(state);
      float clampedYRot = Mth.clamp(state.yRot, -20.0F, 20.0F);
      float headRotXRad = state.xRot * (float) (Math.PI / 180.0);
      float animationSpeed = state.walkAnimationSpeed;
      float animationPos = state.walkAnimationPos;
      if (animationSpeed > 0.2F) {
         headRotXRad += Mth.cos(animationPos * 0.8F) * 0.15F * animationSpeed;
      }

      float eating = state.eatAnimation;
      float standing = state.standAnimation;
      float iStanding = 1.0F - standing;
      float feedingAnim = state.feedingAnimation;
      boolean animateTail = state.animateTail;
      this.headParts.xRot = (float) (Math.PI / 6) + headRotXRad;
      this.headParts.yRot = clampedYRot * (float) (Math.PI / 180.0);
      float waterMultiplier = state.isInWater ? 0.2F : 1.0F;
      float legAnim1 = Mth.cos(waterMultiplier * animationPos * 0.6662F + (float) Math.PI);
      float legXRotAnim = legAnim1 * 0.8F * animationSpeed;
      float baseHeadAngle = (1.0F - Math.max(standing, eating)) * ((float) (Math.PI / 6) + headRotXRad + feedingAnim * Mth.sin(state.ageInTicks) * 0.05F);
      this.headParts.xRot = standing * ((float) (Math.PI / 12) + headRotXRad) + eating * (2.1816616F + Mth.sin(state.ageInTicks) * 0.05F) + baseHeadAngle;
      this.headParts.yRot = standing * clampedYRot * (float) (Math.PI / 180.0) + (1.0F - Math.max(standing, eating)) * this.headParts.yRot;
      this.animateHeadPartsPlacement(eating, standing);
      this.body.xRot = standing * (float) (-Math.PI / 4) + iStanding * this.body.xRot;
      this.leftFrontLeg.y = this.leftFrontLeg.y - this.getLegStandingYOffset() * standing;
      this.leftFrontLeg.z = this.leftFrontLeg.z + this.getLegStandingZOffset() * standing;
      this.rightFrontLeg.y = this.leftFrontLeg.y;
      this.rightFrontLeg.z = this.leftFrontLeg.z;
      float standAngle = this.getLegStandAngle() * standing;
      float bobValue = Mth.cos(state.ageInTicks * 0.6F + (float) Math.PI);
      float legStandingXRotOffset = this.getLegStandingXRotOffset();
      float rlegRot = (legStandingXRotOffset + bobValue) * standing + legXRotAnim * iStanding;
      float llegRot = (legStandingXRotOffset - bobValue) * standing - legXRotAnim * iStanding;
      this.leftHindLeg.xRot = standAngle - legAnim1 * 0.5F * animationSpeed * iStanding;
      this.rightHindLeg.xRot = standAngle + legAnim1 * 0.5F * animationSpeed * iStanding;
      this.leftFrontLeg.xRot = rlegRot;
      this.rightFrontLeg.xRot = llegRot;
      this.offsetLegPositionWhenStanding(standing);
      float ageScale = state.ageScale;
      this.tail.xRot = this.getTailXRotOffset() + (float) (Math.PI / 6) + animationSpeed * 0.75F;
      this.tail.y += animationSpeed * ageScale;
      this.tail.z += animationSpeed * 2.0F * ageScale;
      if (animateTail) {
         this.tail.yRot = Mth.cos(state.ageInTicks * 0.7F);
      } else {
         this.tail.yRot = 0.0F;
      }
   }

   protected void offsetLegPositionWhenStanding(final float standing) {
   }

   protected float getLegStandAngle() {
      return (float) (Math.PI / 12);
   }

   protected float getLegStandingYOffset() {
      return 12.0F;
   }

   protected float getLegStandingZOffset() {
      return 4.0F;
   }

   protected float getLegStandingXRotOffset() {
      return (float) (-Math.PI / 3);
   }

   protected float getTailXRotOffset() {
      return 0.0F;
   }

   protected void animateHeadPartsPlacement(final float eating, final float standing) {
      this.headParts.y = this.headParts.y + Mth.lerp(eating, Mth.lerp(standing, 0.0F, -8.0F), 7.0F);
      this.headParts.z = Mth.lerp(standing, this.headParts.z, -4.0F);
   }
}
