package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

public class AdultPiglinModel extends PiglinModel {
   public AdultPiglinModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
      PartDefinition head = addHead(CubeDeformation.NONE, mesh);
      head.clearChild("hat");
      return LayerDefinition.create(mesh, 64, 64);
   }

   @Override
   protected float getDefaultEarAngleInDegrees() {
      return 30.0F;
   }
}
