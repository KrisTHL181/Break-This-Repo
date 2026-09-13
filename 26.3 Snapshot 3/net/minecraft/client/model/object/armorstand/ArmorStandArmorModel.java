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

public class ArmorStandArmorModel extends HumanoidModel<ArmorStandRenderState> {
   public ArmorStandArmorModel(final ModelPart root) {
      super(root);
   }

   public static ArmorModelSet<LayerDefinition> createArmorLayerSet(final CubeDeformation innerDeformation, final CubeDeformation outerDeformation) {
      return createArmorMeshSet(ArmorStandArmorModel::createBaseMesh, ADULT_ARMOR_PARTS_PER_SLOT, innerDeformation, outerDeformation)
         .map(mesh -> LayerDefinition.create(mesh, 64, 32));
   }

   private static MeshDefinition createBaseMesh(final CubeDeformation g) {
      MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0F);
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g), PartPose.offset(0.0F, 1.0F, 0.0F)
      );
      head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g.extend(0.5F)), PartPose.ZERO);
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F)),
         PartPose.offset(-1.9F, 11.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F)),
         PartPose.offset(1.9F, 11.0F, 0.0F)
      );
      return mesh;
   }

   public void setupAnim(final ArmorStandRenderState state) {
      super.setupAnim(state);
      this.head.xRot = (float) (Math.PI / 180.0) * state.headPose.x();
      this.head.yRot = (float) (Math.PI / 180.0) * state.headPose.y();
      this.head.zRot = (float) (Math.PI / 180.0) * state.headPose.z();
      this.body.xRot = (float) (Math.PI / 180.0) * state.bodyPose.x();
      this.body.yRot = (float) (Math.PI / 180.0) * state.bodyPose.y();
      this.body.zRot = (float) (Math.PI / 180.0) * state.bodyPose.z();
      this.leftArm.xRot = (float) (Math.PI / 180.0) * state.leftArmPose.x();
      this.leftArm.yRot = (float) (Math.PI / 180.0) * state.leftArmPose.y();
      this.leftArm.zRot = (float) (Math.PI / 180.0) * state.leftArmPose.z();
      this.rightArm.xRot = (float) (Math.PI / 180.0) * state.rightArmPose.x();
      this.rightArm.yRot = (float) (Math.PI / 180.0) * state.rightArmPose.y();
      this.rightArm.zRot = (float) (Math.PI / 180.0) * state.rightArmPose.z();
      this.leftLeg.xRot = (float) (Math.PI / 180.0) * state.leftLegPose.x();
      this.leftLeg.yRot = (float) (Math.PI / 180.0) * state.leftLegPose.y();
      this.leftLeg.zRot = (float) (Math.PI / 180.0) * state.leftLegPose.z();
      this.rightLeg.xRot = (float) (Math.PI / 180.0) * state.rightLegPose.x();
      this.rightLeg.yRot = (float) (Math.PI / 180.0) * state.rightLegPose.y();
      this.rightLeg.zRot = (float) (Math.PI / 180.0) * state.rightLegPose.z();
   }
}
