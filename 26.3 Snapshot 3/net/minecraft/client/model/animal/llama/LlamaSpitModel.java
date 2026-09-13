package net.minecraft.client.model.animal.llama;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class LlamaSpitModel extends EntityModel<EntityRenderState> {
   private static final String MAIN = "main";

   public LlamaSpitModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      int edge = 2;
      root.addOrReplaceChild(
         "main",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, -4.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 2.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, 2.0F, 2.0F, 2.0F, 2.0F),
         PartPose.ZERO
      );
      return LayerDefinition.create(mesh, 64, 32);
   }
}
