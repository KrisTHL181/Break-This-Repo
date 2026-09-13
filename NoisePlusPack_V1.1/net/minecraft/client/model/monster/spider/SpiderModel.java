package net.minecraft.client.model.monster.spider;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpiderModel extends EntityModel<LivingEntityRenderState> {
   private static final String BODY_0 = "body0";
   private static final String BODY_1 = "body1";
   private static final String RIGHT_MIDDLE_FRONT_LEG = "right_middle_front_leg";
   private static final String LEFT_MIDDLE_FRONT_LEG = "left_middle_front_leg";
   private static final String RIGHT_MIDDLE_HIND_LEG = "right_middle_hind_leg";
   private static final String LEFT_MIDDLE_HIND_LEG = "left_middle_hind_leg";
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightMiddleHindLeg;
   private final ModelPart leftMiddleHindLeg;
   private final ModelPart rightMiddleFrontLeg;
   private final ModelPart leftMiddleFrontLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;

   public SpiderModel(ModelPart p_457405_) {
      super(p_457405_);
      this.head = p_457405_.getChild("head");
      this.rightHindLeg = p_457405_.getChild("right_hind_leg");
      this.leftHindLeg = p_457405_.getChild("left_hind_leg");
      this.rightMiddleHindLeg = p_457405_.getChild("right_middle_hind_leg");
      this.leftMiddleHindLeg = p_457405_.getChild("left_middle_hind_leg");
      this.rightMiddleFrontLeg = p_457405_.getChild("right_middle_front_leg");
      this.leftMiddleFrontLeg = p_457405_.getChild("left_middle_front_leg");
      this.rightFrontLeg = p_457405_.getChild("right_front_leg");
      this.leftFrontLeg = p_457405_.getChild("left_front_leg");
   }

   public static LayerDefinition createSpiderBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 15;
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(32, 4).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 15.0F, -3.0F)
      );
      partdefinition.addOrReplaceChild(
         "body0", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 15.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "body1", CubeListBuilder.create().texOffs(0, 12).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), PartPose.offset(0.0F, 15.0F, 9.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(18, 0).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F);
      float f = (float) (Math.PI / 4);
      float f1 = (float) (Math.PI / 8);
      partdefinition.addOrReplaceChild(
         "right_hind_leg", cubelistbuilder, PartPose.offsetAndRotation(-4.0F, 15.0F, 2.0F, 0.0F, (float) (Math.PI / 4), (float) (-Math.PI / 4))
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", cubelistbuilder1, PartPose.offsetAndRotation(4.0F, 15.0F, 2.0F, 0.0F, (float) (-Math.PI / 4), (float) (Math.PI / 4))
      );
      partdefinition.addOrReplaceChild(
         "right_middle_hind_leg", cubelistbuilder, PartPose.offsetAndRotation(-4.0F, 15.0F, 1.0F, 0.0F, (float) (Math.PI / 8), -0.58119464F)
      );
      partdefinition.addOrReplaceChild(
         "left_middle_hind_leg", cubelistbuilder1, PartPose.offsetAndRotation(4.0F, 15.0F, 1.0F, 0.0F, (float) (-Math.PI / 8), 0.58119464F)
      );
      partdefinition.addOrReplaceChild(
         "right_middle_front_leg", cubelistbuilder, PartPose.offsetAndRotation(-4.0F, 15.0F, 0.0F, 0.0F, (float) (-Math.PI / 8), -0.58119464F)
      );
      partdefinition.addOrReplaceChild(
         "left_middle_front_leg", cubelistbuilder1, PartPose.offsetAndRotation(4.0F, 15.0F, 0.0F, 0.0F, (float) (Math.PI / 8), 0.58119464F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg", cubelistbuilder, PartPose.offsetAndRotation(-4.0F, 15.0F, -1.0F, 0.0F, (float) (-Math.PI / 4), (float) (-Math.PI / 4))
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", cubelistbuilder1, PartPose.offsetAndRotation(4.0F, 15.0F, -1.0F, 0.0F, (float) (Math.PI / 4), (float) (Math.PI / 4))
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(LivingEntityRenderState p_451945_) {
      super.setupAnim(p_451945_);
      this.head.yRot = p_451945_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_451945_.xRot * (float) (Math.PI / 180.0);
      float f = p_451945_.walkAnimationPos * 0.6662F;
      float f1 = p_451945_.walkAnimationSpeed;
      float f2 = -(Mth.cos(f * 2.0F + 0.0F) * 0.4F) * f1;
      float f3 = -(Mth.cos(f * 2.0F + (float) Math.PI) * 0.4F) * f1;
      float f4 = -(Mth.cos(f * 2.0F + (float) (Math.PI / 2)) * 0.4F) * f1;
      float f5 = -(Mth.cos(f * 2.0F + (float) (Math.PI * 3.0 / 2.0)) * 0.4F) * f1;
      float f6 = Math.abs(Mth.sin(f + 0.0F) * 0.4F) * f1;
      float f7 = Math.abs(Mth.sin(f + (float) Math.PI) * 0.4F) * f1;
      float f8 = Math.abs(Mth.sin(f + (float) (Math.PI / 2)) * 0.4F) * f1;
      float f9 = Math.abs(Mth.sin(f + (float) (Math.PI * 3.0 / 2.0)) * 0.4F) * f1;
      this.rightHindLeg.yRot += f2;
      this.leftHindLeg.yRot -= f2;
      this.rightMiddleHindLeg.yRot += f3;
      this.leftMiddleHindLeg.yRot -= f3;
      this.rightMiddleFrontLeg.yRot += f4;
      this.leftMiddleFrontLeg.yRot -= f4;
      this.rightFrontLeg.yRot += f5;
      this.leftFrontLeg.yRot -= f5;
      this.rightHindLeg.zRot += f6;
      this.leftHindLeg.zRot -= f6;
      this.rightMiddleHindLeg.zRot += f7;
      this.leftMiddleHindLeg.zRot -= f7;
      this.rightMiddleFrontLeg.zRot += f8;
      this.leftMiddleFrontLeg.zRot -= f8;
      this.rightFrontLeg.zRot += f9;
      this.leftFrontLeg.zRot -= f9;
   }
}
