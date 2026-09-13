package net.minecraft.client.model.animal.cow;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyCowModel extends CowModel {
   public BabyCowModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 18)
            .addBox(-3.0F, -4.569F, -4.8333F, 6.0F, 6.0F, 5.0F)
            .texOffs(8, 29)
            .addBox(3.0F, -5.569F, -3.8333F, 1.0F, 2.0F, 1.0F)
            .texOffs(4, 29)
            .mirror()
            .addBox(-4.0F, -5.569F, -3.8333F, 1.0F, 2.0F, 1.0F)
            .mirror(false)
            .texOffs(12, 29)
            .addBox(-2.0F, -1.569F, -5.8333F, 4.0F, 3.0F, 1.0F),
         PartPose.offset(0.0F, 13.569F, -5.1667F)
      );
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -7.0F, -1.0F, 8.0F, 6.0F, 12.0F), PartPose.offset(3.0F, 19.0F, -5.0F));
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(22, 18).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(-2.5F, 18.0F, -3.5F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(34, 18).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(2.5F, 18.0F, -3.5F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(22, 27).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(-2.5F, 18.0F, 3.5F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(34, 27).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(2.5F, 18.0F, 3.5F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
