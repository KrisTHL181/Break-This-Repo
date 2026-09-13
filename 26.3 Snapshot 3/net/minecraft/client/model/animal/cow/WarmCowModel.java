package net.minecraft.client.model.animal.cow;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class WarmCowModel extends CowModel {
   public WarmCowModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = createBaseCowModel();
      mesh.getRoot()
         .addOrReplaceChild(
            "head",
            CubeListBuilder.create()
               .texOffs(0, 0)
               .addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
               .texOffs(1, 33)
               .addBox(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F)
               .texOffs(27, 0)
               .addBox(-8.0F, -3.0F, -5.0F, 4.0F, 2.0F, 2.0F)
               .texOffs(39, 0)
               .addBox(-8.0F, -5.0F, -5.0F, 2.0F, 2.0F, 2.0F)
               .texOffs(27, 0)
               .mirror()
               .addBox(4.0F, -3.0F, -5.0F, 4.0F, 2.0F, 2.0F)
               .mirror(false)
               .texOffs(39, 0)
               .mirror()
               .addBox(6.0F, -5.0F, -5.0F, 2.0F, 2.0F, 2.0F)
               .mirror(false),
            PartPose.offset(0.0F, 4.0F, -8.0F)
         );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
