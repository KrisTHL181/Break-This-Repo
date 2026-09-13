package net.minecraft.client.model.animal.wolf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.WolfRenderState;

public class AdultWolfModel extends WolfModel {
   private static final String REAL_HEAD = "real_head";
   private static final String UPPER_BODY = "upper_body";
   private static final String REAL_TAIL = "real_tail";
   private final ModelPart realHead = this.head.getChild("real_head");
   private final ModelPart realTail;
   private final ModelPart upperBody;
   private static final int LEG_SIZE = 8;

   public AdultWolfModel(final ModelPart root) {
      super(root);
      this.upperBody = root.getChild("upper_body");
      this.realTail = this.tail.getChild("real_tail");
   }

   public static MeshDefinition createBodyLayer(final CubeDeformation g) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float headHeight = 13.5F;
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
      head.addOrReplaceChild(
         "real_head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, g)
            .texOffs(16, 14)
            .addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, g)
            .texOffs(16, 14)
            .addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, g)
            .texOffs(0, 10)
            .addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, g),
         PartPose.ZERO
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, g),
         PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "upper_body",
         CubeListBuilder.create().texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, g),
         PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g);
      CubeListBuilder rightLeg = CubeListBuilder.create().mirror().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g);
      root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-2.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(0.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-2.5F, 16.0F, -4.0F));
      root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(0.5F, 16.0F, -4.0F));
      PartDefinition tail = root.addOrReplaceChild(
         "tail", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, (float) (Math.PI / 5), 0.0F, 0.0F)
      );
      tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g), PartPose.ZERO);
      return mesh;
   }

   @Override
   protected void setSittingPose(final WolfRenderState state) {
      super.setSittingPose(state);
      this.upperBody.y += 2.0F;
      this.upperBody.xRot = (float) (Math.PI * 2.0 / 5.0);
      this.upperBody.yRot = 0.0F;
   }

   @Override
   protected void shakeOffWater(final WolfRenderState state) {
      super.shakeOffWater(state);
      this.realHead.zRot = state.headRollAngle + state.getBodyRollAngle(0.0F);
      this.upperBody.zRot = state.getBodyRollAngle(-0.08F);
      this.realTail.zRot = state.getBodyRollAngle(-0.2F);
   }
}
