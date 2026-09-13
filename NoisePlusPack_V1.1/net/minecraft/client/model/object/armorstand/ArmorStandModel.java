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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmorStandModel extends ArmorStandArmorModel {
   private static final String RIGHT_BODY_STICK = "right_body_stick";
   private static final String LEFT_BODY_STICK = "left_body_stick";
   private static final String SHOULDER_STICK = "shoulder_stick";
   private static final String BASE_PLATE = "base_plate";
   private final ModelPart rightBodyStick;
   private final ModelPart leftBodyStick;
   private final ModelPart shoulderStick;
   private final ModelPart basePlate;

   public ArmorStandModel(ModelPart p_461062_) {
      super(p_461062_);
      this.rightBodyStick = p_461062_.getChild("right_body_stick");
      this.leftBodyStick = p_461062_.getChild("left_body_stick");
      this.shoulderStick = p_461062_.getChild("shoulder_stick");
      this.basePlate = p_461062_.getChild("base_plate");
      this.hat.visible = false;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(0.0F, 1.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 26).addBox(-6.0F, 0.0F, -1.5F, 12.0F, 3.0F, 3.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(-1.9F, 12.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.offset(1.9F, 12.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild("right_body_stick", CubeListBuilder.create().texOffs(16, 0).addBox(-3.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild("left_body_stick", CubeListBuilder.create().texOffs(48, 16).addBox(1.0F, 3.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild("shoulder_stick", CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, 10.0F, -1.0F, 8.0F, 2.0F, 2.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild(
         "base_plate", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, 11.0F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.offset(0.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   @Override
   public void setupAnim(ArmorStandRenderState p_459844_) {
      super.setupAnim(p_459844_);
      this.basePlate.yRot = (float) (Math.PI / 180.0) * -p_459844_.yRot;
      this.leftArm.visible = p_459844_.showArms;
      this.rightArm.visible = p_459844_.showArms;
      this.basePlate.visible = p_459844_.showBasePlate;
      this.rightBodyStick.xRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.x();
      this.rightBodyStick.yRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.y();
      this.rightBodyStick.zRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.z();
      this.leftBodyStick.xRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.x();
      this.leftBodyStick.yRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.y();
      this.leftBodyStick.zRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.z();
      this.shoulderStick.xRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.x();
      this.shoulderStick.yRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.y();
      this.shoulderStick.zRot = (float) (Math.PI / 180.0) * p_459844_.bodyPose.z();
   }

   public void translateToHand(ArmorStandRenderState p_450256_, HumanoidArm p_451004_, PoseStack p_460459_) {
      ModelPart modelpart = this.getArm(p_451004_);
      boolean flag = modelpart.visible;
      modelpart.visible = true;
      super.translateToHand(p_450256_, p_451004_, p_460459_);
      modelpart.visible = flag;
   }
}
