package net.minecraft.client.model.monster.slime;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SulfurCubeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class SulfurCubeModel extends EntityModel<SulfurCubeRenderState> implements HeadedModel {
   public SulfurCubeModel(final ModelPart root) {
      super(root, RenderTypes::entityTranslucent);
   }

   public static LayerDefinition createOuterBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -9.0F, -9.0F, 18.0F, 18.0F, 18.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 128, 128);
   }

   public static LayerDefinition createInnerBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 36).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 128, 128);
   }

   @Override
   public ModelPart getHead() {
      return this.root().getChild("cube");
   }
}
