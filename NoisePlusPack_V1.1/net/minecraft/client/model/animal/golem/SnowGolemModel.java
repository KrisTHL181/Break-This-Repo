package net.minecraft.client.model.animal.golem;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemModel extends EntityModel<LivingEntityRenderState> {
   private static final String UPPER_BODY = "upper_body";
   private final ModelPart upperBody;
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart rightArm;

   public SnowGolemModel(ModelPart p_450930_) {
      super(p_450930_);
      this.head = p_450930_.getChild("head");
      this.leftArm = p_450930_.getChild("left_arm");
      this.rightArm = p_450930_.getChild("right_arm");
      this.upperBody = p_450930_.getChild("upper_body");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = 4.0F;
      CubeDeformation cubedeformation = new CubeDeformation(-0.5F);
      partdefinition.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubedeformation), PartPose.offset(0.0F, 4.0F, 0.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 12.0F, 2.0F, 2.0F, cubedeformation);
      partdefinition.addOrReplaceChild("left_arm", cubelistbuilder, PartPose.offsetAndRotation(5.0F, 6.0F, 1.0F, 0.0F, 0.0F, 1.0F));
      partdefinition.addOrReplaceChild("right_arm", cubelistbuilder, PartPose.offsetAndRotation(-5.0F, 6.0F, -1.0F, 0.0F, (float) Math.PI, -1.0F));
      partdefinition.addOrReplaceChild(
         "upper_body",
         CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, cubedeformation),
         PartPose.offset(0.0F, 13.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "lower_body",
         CubeListBuilder.create().texOffs(0, 36).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, cubedeformation),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState p_454385_) {
      super.setupAnim(p_454385_);
      this.head.yRot = p_454385_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_454385_.xRot * (float) (Math.PI / 180.0);
      this.upperBody.yRot = p_454385_.yRot * (float) (Math.PI / 180.0) * 0.25F;
      float f = Mth.sin(this.upperBody.yRot);
      float f1 = Mth.cos(this.upperBody.yRot);
      this.leftArm.yRot = this.upperBody.yRot;
      this.rightArm.yRot = this.upperBody.yRot + (float) Math.PI;
      this.leftArm.x = f1 * 5.0F;
      this.leftArm.z = -f * 5.0F;
      this.rightArm.x = -f1 * 5.0F;
      this.rightArm.z = f * 5.0F;
   }

   public ModelPart getHead() {
      return this.head;
   }
}
