package net.minecraft.client.model.player;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerEarsModel extends PlayerModel {
   public PlayerEarsModel(ModelPart p_450718_) {
      super(p_450718_, false);
   }

   public static LayerDefinition createEarsLayer() {
      MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
      PartDefinition partdefinition = meshdefinition.getRoot().clearRecursively();
      PartDefinition partdefinition1 = partdefinition.getChild("head");
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -6.0F, -1.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(1.0F));
      partdefinition1.addOrReplaceChild("left_ear", cubelistbuilder, PartPose.offset(-6.0F, -6.0F, 0.0F));
      partdefinition1.addOrReplaceChild("right_ear", cubelistbuilder, PartPose.offset(6.0F, -6.0F, 0.0F));
      return LayerDefinition.create(meshdefinition, 64, 64);
   }
}
