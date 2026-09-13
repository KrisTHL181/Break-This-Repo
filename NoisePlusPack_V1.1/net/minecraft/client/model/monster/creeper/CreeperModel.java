package net.minecraft.client.model.monster.creeper;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreeperModel extends EntityModel<CreeperRenderState> {
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private static final int Y_OFFSET = 6;

   public CreeperModel(ModelPart p_453517_) {
      super(p_453517_);
      this.head = p_453517_.getChild("head");
      this.leftHindLeg = p_453517_.getChild("right_hind_leg");
      this.rightHindLeg = p_453517_.getChild("left_hind_leg");
      this.leftFrontLeg = p_453517_.getChild("right_front_leg");
      this.rightFrontLeg = p_453517_.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_452121_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, p_452121_), PartPose.offset(0.0F, 6.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, p_452121_), PartPose.offset(0.0F, 6.0F, 0.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, p_452121_);
      partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-2.0F, 18.0F, 4.0F));
      partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(2.0F, 18.0F, 4.0F));
      partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder, PartPose.offset(-2.0F, 18.0F, -4.0F));
      partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder, PartPose.offset(2.0F, 18.0F, -4.0F));
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(CreeperRenderState p_459748_) {
      super.setupAnim(p_459748_);
      this.head.yRot = p_459748_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_459748_.xRot * (float) (Math.PI / 180.0);
      float f = p_459748_.walkAnimationSpeed;
      float f1 = p_459748_.walkAnimationPos;
      this.rightHindLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
      this.leftHindLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.rightFrontLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.leftFrontLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
   }
}
