package net.minecraft.client.model.object.armorstand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.HumanoidArm;

public class ArmorStandModel extends ArmorStandArmorModel {
   private static final String RIGHT_BODY_STICK = "right_body_stick";
   private static final String LEFT_BODY_STICK = "left_body_stick";
   private static final String SHOULDER_STICK = "shoulder_stick";
   private static final String BASE_PLATE = "base_plate";
   private final ModelPart rightBodyStick;
   private final ModelPart leftBodyStick;
   private final ModelPart shoulderStick;
   private final ModelPart basePlate;

   public ArmorStandModel(final ModelPart root) {
      super(root);
      this.rightBodyStick = root.getChild("right_body_stick");
      this.leftBodyStick = root.getChild("left_body_stick");
      this.shoulderStick = root.getChild("shoulder_stick");
      this.basePlate = root.getChild("base_plate");
      this.hat.visible = false;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 26).addBox(-6.0F, 0.0F, -1.5F, 12.0F, 3.0F, 3.0F), PartPose.ZERO);
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild("right_body_stick", CubeListBuilder.create().texOffs(16, 0).addBox(-3.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.ZERO);
      root.addOrReplaceChild("left_body_stick", CubeListBuilder.create().texOffs(48, 16).addBox(1.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.ZERO);
      root.addOrReplaceChild("shoulder_stick", CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, 10.0F, -1.0F, 8.0F, 2.0F, 2.0F), PartPose.ZERO);
      root.addOrReplaceChild(
         "base_plate", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, 11.0F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   @Override
   public void setupAnim(final ArmorStandRenderState state) {
      super.setupAnim(state);
      this.basePlate.yRot = (float) (Math.PI / 180.0) * -state.yRot;
      this.leftArm.visible = state.showArms;
      this.rightArm.visible = state.showArms;
      this.basePlate.visible = state.showBasePlate;
      this.rightBodyStick.xRot = (float) (Math.PI / 180.0) * state.bodyPose.x();
      this.rightBodyStick.yRot = (float) (Math.PI / 180.0) * state.bodyPose.y();
      this.rightBodyStick.zRot = (float) (Math.PI / 180.0) * state.bodyPose.z();
      this.leftBodyStick.xRot = (float) (Math.PI / 180.0) * state.bodyPose.x();
      this.leftBodyStick.yRot = (float) (Math.PI / 180.0) * state.bodyPose.y();
      this.leftBodyStick.zRot = (float) (Math.PI / 180.0) * state.bodyPose.z();
      this.shoulderStick.xRot = (float) (Math.PI / 180.0) * state.bodyPose.x();
      this.shoulderStick.yRot = (float) (Math.PI / 180.0) * state.bodyPose.y();
      this.shoulderStick.zRot = (float) (Math.PI / 180.0) * state.bodyPose.z();
   }

   public void translateToHand(final ArmorStandRenderState state, final HumanoidArm arm, final PoseStack poseStack) {
      ModelPart modelPart = this.getArm(arm);
      boolean handVisible = modelPart.visible;
      modelPart.visible = true;
      super.translateToHand(state, arm, poseStack);
      modelPart.visible = handVisible;
   }
}
