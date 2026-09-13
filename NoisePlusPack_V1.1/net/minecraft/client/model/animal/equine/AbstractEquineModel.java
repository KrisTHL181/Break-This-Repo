package net.minecraft.client.model.animal.equine;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractEquineModel<T extends EquineRenderState> extends EntityModel<T> {
   private static final float DEG_125 = 2.1816616F;
   private static final float DEG_60 = (float) (Math.PI / 3);
   private static final float DEG_45 = (float) (Math.PI / 4);
   private static final float DEG_30 = (float) (Math.PI / 6);
   private static final float DEG_15 = (float) (Math.PI / 12);
   protected static final String HEAD_PARTS = "head_parts";
   protected static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 16.2F, 1.36F, 2.7272F, 2.0F, 20.0F, Set.of("head_parts"));
   protected final ModelPart body;
   protected final ModelPart headParts;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart tail;

   public AbstractEquineModel(ModelPart p_450932_) {
      super(p_450932_);
      this.body = p_450932_.getChild("body");
      this.headParts = p_450932_.getChild("head_parts");
      this.rightHindLeg = p_450932_.getChild("right_hind_leg");
      this.leftHindLeg = p_450932_.getChild("left_hind_leg");
      this.rightFrontLeg = p_450932_.getChild("right_front_leg");
      this.leftFrontLeg = p_450932_.getChild("left_front_leg");
      this.tail = this.body.getChild("tail");
   }

   public static MeshDefinition createBodyMesh(CubeDeformation p_455841_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.05F)),
         PartPose.offset(0.0F, 11.0F, 5.0F)
      );
      PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
         "head_parts",
         CubeListBuilder.create().texOffs(0, 35).addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F),
         PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, p_455841_), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "mane", CubeListBuilder.create().texOffs(56, 36).addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, p_455841_), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "upper_mouth", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, p_455841_), PartPose.ZERO
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_455841_),
         PartPose.offset(4.0F, 14.0F, 7.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, p_455841_),
         PartPose.offset(-4.0F, 14.0F, 7.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_455841_),
         PartPose.offset(4.0F, 14.0F, -10.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, p_455841_),
         PartPose.offset(-4.0F, 14.0F, -10.0F)
      );
      partdefinition1.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(42, 36).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F, p_455841_),
         PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F)
      );
      partdefinition3.addOrReplaceChild(
         "left_ear", CubeListBuilder.create().texOffs(19, 16).addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F)), PartPose.ZERO
      );
      partdefinition3.addOrReplaceChild(
         "right_ear", CubeListBuilder.create().texOffs(19, 16).addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(-0.001F)), PartPose.ZERO
      );
      return meshdefinition;
   }

   public static MeshDefinition createBabyMesh(CubeDeformation p_461043_) {
      return BABY_TRANSFORMER.apply(createFullScaleBabyMesh(p_461043_));
   }

   protected static MeshDefinition createFullScaleBabyMesh(CubeDeformation p_459828_) {
      MeshDefinition meshdefinition = createBodyMesh(p_459828_);
      PartDefinition partdefinition = meshdefinition.getRoot();
      CubeDeformation cubedeformation = p_459828_.extend(0.0F, 5.5F, 0.0F);
      partdefinition.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, cubedeformation),
         PartPose.offset(4.0F, 14.0F, 7.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, cubedeformation),
         PartPose.offset(-4.0F, 14.0F, 7.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(48, 21).mirror().addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, cubedeformation),
         PartPose.offset(4.0F, 14.0F, -10.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(48, 21).addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, cubedeformation),
         PartPose.offset(-4.0F, 14.0F, -10.0F)
      );
      return meshdefinition;
   }

   public void setupAnim(T p_459898_) {
      super.setupAnim(p_459898_);
      float f = Mth.clamp(p_459898_.yRot, -20.0F, 20.0F);
      float f1 = p_459898_.xRot * (float) (Math.PI / 180.0);
      float f2 = p_459898_.walkAnimationSpeed;
      float f3 = p_459898_.walkAnimationPos;
      if (f2 > 0.2F) {
         f1 += Mth.cos(f3 * 0.8F) * 0.15F * f2;
      }

      float f4 = p_459898_.eatAnimation;
      float f5 = p_459898_.standAnimation;
      float f6 = 1.0F - f5;
      float f7 = p_459898_.feedingAnimation;
      boolean flag = p_459898_.animateTail;
      this.headParts.xRot = (float) (Math.PI / 6) + f1;
      this.headParts.yRot = f * (float) (Math.PI / 180.0);
      float f8 = p_459898_.isInWater ? 0.2F : 1.0F;
      float f9 = Mth.cos(f8 * f3 * 0.6662F + (float) Math.PI);
      float f10 = f9 * 0.8F * f2;
      float f11 = (1.0F - Math.max(f5, f4)) * ((float) (Math.PI / 6) + f1 + f7 * Mth.sin(p_459898_.ageInTicks) * 0.05F);
      this.headParts.xRot = f5 * ((float) (Math.PI / 12) + f1) + f4 * (2.1816616F + Mth.sin(p_459898_.ageInTicks) * 0.05F) + f11;
      this.headParts.yRot = f5 * f * (float) (Math.PI / 180.0) + (1.0F - Math.max(f5, f4)) * this.headParts.yRot;
      float f12 = p_459898_.ageScale;
      this.headParts.y = this.headParts.y + Mth.lerp(f4, Mth.lerp(f5, 0.0F, -8.0F * f12), 7.0F * f12);
      this.headParts.z = Mth.lerp(f5, this.headParts.z, -4.0F * f12);
      this.body.xRot = f5 * (float) (-Math.PI / 4) + f6 * this.body.xRot;
      float f13 = (float) (Math.PI / 12) * f5;
      float f14 = Mth.cos(p_459898_.ageInTicks * 0.6F + (float) Math.PI);
      this.leftFrontLeg.y -= 12.0F * f12 * f5;
      this.leftFrontLeg.z += 4.0F * f12 * f5;
      this.rightFrontLeg.y = this.leftFrontLeg.y;
      this.rightFrontLeg.z = this.leftFrontLeg.z;
      float f15 = ((float) (-Math.PI / 3) + f14) * f5 + f10 * f6;
      float f16 = ((float) (-Math.PI / 3) - f14) * f5 - f10 * f6;
      this.leftHindLeg.xRot = f13 - f9 * 0.5F * f2 * f6;
      this.rightHindLeg.xRot = f13 + f9 * 0.5F * f2 * f6;
      this.leftFrontLeg.xRot = f15;
      this.rightFrontLeg.xRot = f16;
      this.tail.xRot = (float) (Math.PI / 6) + f2 * 0.75F;
      this.tail.y += f2 * f12;
      this.tail.z += f2 * 2.0F * f12;
      if (flag) {
         this.tail.yRot = Mth.cos(p_459898_.ageInTicks * 0.7F);
      } else {
         this.tail.yRot = 0.0F;
      }
   }
}
