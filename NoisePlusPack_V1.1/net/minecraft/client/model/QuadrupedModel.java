package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadrupedModel<T extends LivingEntityRenderState> extends EntityModel<T> {
   protected final ModelPart head;
   protected final ModelPart body;
   protected final ModelPart rightHindLeg;
   protected final ModelPart leftHindLeg;
   protected final ModelPart rightFrontLeg;
   protected final ModelPart leftFrontLeg;

   protected QuadrupedModel(ModelPart p_170857_) {
      super(p_170857_);
      this.head = p_170857_.getChild("head");
      this.body = p_170857_.getChild("body");
      this.rightHindLeg = p_170857_.getChild("right_hind_leg");
      this.leftHindLeg = p_170857_.getChild("left_hind_leg");
      this.rightFrontLeg = p_170857_.getChild("right_front_leg");
      this.leftFrontLeg = p_170857_.getChild("left_front_leg");
   }

   public static MeshDefinition createBodyMesh(int p_170865_, boolean p_405920_, boolean p_410008_, CubeDeformation p_170866_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, p_170866_), PartPose.offset(0.0F, 18 - p_170865_, -6.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(28, 8).addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, p_170866_),
         PartPose.offsetAndRotation(0.0F, 17 - p_170865_, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      createLegs(partdefinition, p_405920_, p_410008_, p_170865_, p_170866_);
      return meshdefinition;
   }

   static void createLegs(PartDefinition p_405823_, boolean p_410357_, boolean p_406813_, int p_407777_, CubeDeformation p_409692_) {
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().mirror(p_406813_).texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, p_407777_, 4.0F, p_409692_);
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().mirror(p_410357_).texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, p_407777_, 4.0F, p_409692_);
      p_405823_.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-3.0F, 24 - p_407777_, 7.0F));
      p_405823_.addOrReplaceChild("left_hind_leg", cubelistbuilder1, PartPose.offset(3.0F, 24 - p_407777_, 7.0F));
      p_405823_.addOrReplaceChild("right_front_leg", cubelistbuilder, PartPose.offset(-3.0F, 24 - p_407777_, -5.0F));
      p_405823_.addOrReplaceChild("left_front_leg", cubelistbuilder1, PartPose.offset(3.0F, 24 - p_407777_, -5.0F));
   }

   public void setupAnim(T p_364834_) {
      super.setupAnim(p_364834_);
      this.head.xRot = p_364834_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_364834_.yRot * (float) (Math.PI / 180.0);
      float f = p_364834_.walkAnimationPos;
      float f1 = p_364834_.walkAnimationSpeed;
      this.rightHindLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
      this.leftHindLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * f1;
      this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * f1;
      this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
   }
}
