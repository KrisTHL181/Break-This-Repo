package net.minecraft.client.model.animal.goat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.GoatRenderState;

public class BabyGoatModel extends GoatModel {
   private static final String HEAD_MAIN = "HeadMain";

   public BabyGoatModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(29, 12).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(1.5F, 19.5F, 3.0F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(21, 12).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(-1.5F, 19.5F, 3.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(21, 5).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(-1.5F, 19.5F, -2.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(29, 5).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(1.5F, 19.5F, -2.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -2.3F, -4.5F, 6.0F, 5.0F, 9.0F).texOffs(0, 24).addBox(-2.5F, -2.2F, -4.0F, 5.0F, 4.0F, 8.0F),
         PartPose.offset(0.0F, 17.8F, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -3.8126F, -5.1548F, 4.0F, 4.0F, 6.0F),
         PartPose.offsetAndRotation(0.0F, 15.5F, -3.0F, 0.4363F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "right_horn",
         CubeListBuilder.create().texOffs(24, 0).mirror().addBox(0.0F, -4.5F, 0.0F, 1.0F, 2.0F, 1.0F).mirror(false),
         PartPose.offsetAndRotation(-1.5F, -1.5F, -1.0F, (float) (-Math.PI / 8), 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_horn",
         CubeListBuilder.create().texOffs(24, 0).mirror().addBox(2.0F, -4.5F, 0.0F, 1.0F, 2.0F, 1.0F).mirror(false),
         PartPose.offsetAndRotation(-1.5F, -1.5F, -1.0F, (float) (-Math.PI / 8), 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-2.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F).mirror(false),
         PartPose.offsetAndRotation(-1.7F, -2.3126F, 0.1452F, 0.0F, -0.5236F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
         PartPose.offsetAndRotation(1.7F, -2.3126F, 0.1452F, 0.0F, 0.5236F, 0.0F)
      );
      head.addOrReplaceChild(
         "HeadMain", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.5F, -4.0F, 4.0F, 4.0F, 6.0F), PartPose.offset(0.0F, -1.3126F, -1.1548F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   @Override
   public void setupAnim(final GoatRenderState state) {
      super.setupAnim(state);
      if (state.rammingXHeadRot == 0.0F) {
         this.head.xRot = (float) (Math.PI / 8);
      }
   }
}
