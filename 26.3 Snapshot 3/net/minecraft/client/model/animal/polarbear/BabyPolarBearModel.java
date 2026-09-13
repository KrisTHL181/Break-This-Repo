package net.minecraft.client.model.animal.polarbear;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyPolarBearModel extends PolarBearModel {
   public BabyPolarBearModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-4.0F, -3.5F, -6.0F, 8.0F, 7.0F, 12.0F), PartPose.offset(0.0F, 17.5F, 0.0F));
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.0F, -2.625F, -4.25F, 6.0F, 5.0F, 4.0F)
            .texOffs(20, 3)
            .addBox(-2.0F, 0.375F, -6.25F, 4.0F, 2.0F, 2.0F)
            .texOffs(20, 0)
            .addBox(-4.0F, -3.625F, -2.75F, 2.0F, 2.0F, 1.0F)
            .texOffs(26, 0)
            .addBox(2.0F, -3.625F, -2.75F, 2.0F, 2.0F, 1.0F),
         PartPose.offset(0.0F, 18.625F, -5.75F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(-2.5F, 21.5F, 4.5F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(2.5F, 21.5F, 4.5F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(-2.5F, 21.5F, -4.5F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(12, 28).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(2.5F, 21.5F, -4.5F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
