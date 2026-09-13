package net.minecraft.client.model.object.armorstand;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmorStandArmorModel extends HumanoidModel<ArmorStandRenderState> {
   public ArmorStandArmorModel(ModelPart p_456842_) {
      super(p_456842_);
   }

   public static ArmorModelSet<LayerDefinition> createArmorLayerSet(CubeDeformation p_452779_, CubeDeformation p_457454_) {
      return createArmorMeshSet(ArmorStandArmorModel::createBaseMesh, p_452779_, p_457454_).map(p_450577_ -> LayerDefinition.create(p_450577_, 64, 32));
   }

   private static MeshDefinition createBaseMesh(CubeDeformation p_454204_) {
      MeshDefinition meshdefinition = HumanoidModel.createMesh(p_454204_, 0.0F);
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, p_454204_), PartPose.offset(0.0F, 1.0F, 0.0F)
      );
      partdefinition1.addOrReplaceChild(
         "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, p_454204_.extend(0.5F)), PartPose.ZERO
      );
      partdefinition.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454204_.extend(-0.1F)),
         PartPose.offset(-1.9F, 11.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, p_454204_.extend(-0.1F)),
         PartPose.offset(1.9F, 11.0F, 0.0F)
      );
      return meshdefinition;
   }

   public void setupAnim(ArmorStandRenderState p_452764_) {
      super.setupAnim(p_452764_);
      this.head.xRot = (float) (Math.PI / 180.0) * p_452764_.headPose.x();
      this.head.yRot = (float) (Math.PI / 180.0) * p_452764_.headPose.y();
      this.head.zRot = (float) (Math.PI / 180.0) * p_452764_.headPose.z();
      this.body.xRot = (float) (Math.PI / 180.0) * p_452764_.bodyPose.x();
      this.body.yRot = (float) (Math.PI / 180.0) * p_452764_.bodyPose.y();
      this.body.zRot = (float) (Math.PI / 180.0) * p_452764_.bodyPose.z();
      this.leftArm.xRot = (float) (Math.PI / 180.0) * p_452764_.leftArmPose.x();
      this.leftArm.yRot = (float) (Math.PI / 180.0) * p_452764_.leftArmPose.y();
      this.leftArm.zRot = (float) (Math.PI / 180.0) * p_452764_.leftArmPose.z();
      this.rightArm.xRot = (float) (Math.PI / 180.0) * p_452764_.rightArmPose.x();
      this.rightArm.yRot = (float) (Math.PI / 180.0) * p_452764_.rightArmPose.y();
      this.rightArm.zRot = (float) (Math.PI / 180.0) * p_452764_.rightArmPose.z();
      this.leftLeg.xRot = (float) (Math.PI / 180.0) * p_452764_.leftLegPose.x();
      this.leftLeg.yRot = (float) (Math.PI / 180.0) * p_452764_.leftLegPose.y();
      this.leftLeg.zRot = (float) (Math.PI / 180.0) * p_452764_.leftLegPose.z();
      this.rightLeg.xRot = (float) (Math.PI / 180.0) * p_452764_.rightLegPose.x();
      this.rightLeg.yRot = (float) (Math.PI / 180.0) * p_452764_.rightLegPose.y();
      this.rightLeg.zRot = (float) (Math.PI / 180.0) * p_452764_.rightLegPose.z();
   }
}
