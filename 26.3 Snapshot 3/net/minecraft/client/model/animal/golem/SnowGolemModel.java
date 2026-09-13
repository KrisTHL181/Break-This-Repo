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

public class SnowGolemModel extends EntityModel<LivingEntityRenderState> {
   private static final String UPPER_BODY = "upper_body";
   private final ModelPart upperBody;
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart rightArm;

   public SnowGolemModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.leftArm = root.getChild("left_arm");
      this.rightArm = root.getChild("right_arm");
      this.upperBody = root.getChild("upper_body");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float yOffset = 4.0F;
      CubeDeformation deformation = new CubeDeformation(-0.5F);
      root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation), PartPose.offset(0.0F, 4.0F, 0.0F)
      );
      CubeListBuilder arm = CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 12.0F, 2.0F, 2.0F, deformation);
      root.addOrReplaceChild("left_arm", arm, PartPose.offsetAndRotation(5.0F, 6.0F, 1.0F, 0.0F, 0.0F, 1.0F));
      root.addOrReplaceChild("right_arm", arm, PartPose.offsetAndRotation(-5.0F, 6.0F, -1.0F, 0.0F, (float) Math.PI, -1.0F));
      root.addOrReplaceChild(
         "upper_body",
         CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F, deformation),
         PartPose.offset(0.0F, 13.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "lower_body",
         CubeListBuilder.create().texOffs(0, 36).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, deformation),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final LivingEntityRenderState state) {
      super.setupAnim(state);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.upperBody.yRot = state.yRot * (float) (Math.PI / 180.0) * 0.25F;
      float sin = Mth.sin(this.upperBody.yRot);
      float cos = Mth.cos(this.upperBody.yRot);
      this.leftArm.yRot = this.upperBody.yRot;
      this.rightArm.yRot = this.upperBody.yRot + (float) Math.PI;
      this.leftArm.x = cos * 5.0F;
      this.leftArm.z = -sin * 5.0F;
      this.rightArm.x = -cos * 5.0F;
      this.rightArm.z = sin * 5.0F;
   }

   public ModelPart getHead() {
      return this.head;
   }
}
