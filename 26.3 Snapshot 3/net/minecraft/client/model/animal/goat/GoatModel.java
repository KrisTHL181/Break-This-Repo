package net.minecraft.client.model.animal.goat;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GoatRenderState;

public class GoatModel extends QuadrupedModel<GoatRenderState> {
   public GoatModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(2, 61)
            .addBox("right ear", -6.0F, -11.0F, -10.0F, 3.0F, 2.0F, 1.0F)
            .texOffs(2, 61)
            .mirror()
            .addBox("left ear", 2.0F, -11.0F, -10.0F, 3.0F, 2.0F, 1.0F)
            .texOffs(23, 52)
            .addBox("goatee", -0.5F, -3.0F, -14.0F, 0.0F, 7.0F, 5.0F),
         PartPose.offset(1.0F, 14.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_horn", CubeListBuilder.create().texOffs(12, 55).addBox(-0.01F, -16.0F, -10.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "right_horn", CubeListBuilder.create().texOffs(12, 55).addBox(-2.99F, -16.0F, -10.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "nose",
         CubeListBuilder.create().texOffs(34, 46).addBox(-3.0F, -4.0F, -8.0F, 5.0F, 7.0F, 10.0F),
         PartPose.offsetAndRotation(0.0F, -8.0F, -8.0F, 0.9599F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(1, 1)
            .addBox(-4.0F, -17.0F, -7.0F, 9.0F, 11.0F, 16.0F)
            .texOffs(0, 28)
            .addBox(-5.0F, -18.0F, -8.0F, 11.0F, 14.0F, 11.0F),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(36, 29).addBox(0.0F, 4.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offset(1.0F, 14.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(49, 29).addBox(0.0F, 4.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offset(-3.0F, 14.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(49, 2).addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F), PartPose.offset(1.0F, 14.0F, -6.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(35, 2).addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F), PartPose.offset(-3.0F, 14.0F, -6.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final GoatRenderState state) {
      super.setupAnim(state);
      this.head.getChild("left_horn").visible = state.hasLeftHorn;
      this.head.getChild("right_horn").visible = state.hasRightHorn;
      if (state.rammingXHeadRot != 0.0F) {
         this.head.xRot = state.rammingXHeadRot;
      }
   }
}
