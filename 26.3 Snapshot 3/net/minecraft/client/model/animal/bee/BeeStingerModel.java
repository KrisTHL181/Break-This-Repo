package net.minecraft.client.model.animal.bee;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class BeeStingerModel extends Model<Unit> {
   public BeeStingerModel(final ModelPart root) {
      super(root, RenderTypes::entityCutoutCull);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      CubeListBuilder cross = CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F);
      root.addOrReplaceChild("cross_1", cross, PartPose.rotation((float) (Math.PI / 4), 0.0F, 0.0F));
      root.addOrReplaceChild("cross_2", cross, PartPose.rotation((float) (Math.PI * 3.0 / 4.0), 0.0F, 0.0F));
      return LayerDefinition.create(mesh, 16, 16);
   }
}
