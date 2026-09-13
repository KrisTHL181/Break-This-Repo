package net.minecraft.client.model.animal.dolphin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyDolphinModel extends DolphinModel {
   public BabyDolphinModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(20, 0).addBox(-3.0F, -2.5F, -4.0F, 6.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 21.5F, 0.0F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.5F, -4.0F, 6.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 1.0F, -4.0F)
      );
      head.addOrReplaceChild(
         "nose",
         CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.5F, -4.0F)
      );
      body.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(34, 18).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(1.8F, 0.85F, -2.6F, 0.8727F, 0.0F, 1.7017F)
      );
      body.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(48, 18).mirror().addBox(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false),
         PartPose.offsetAndRotation(-1.8F, 0.85F, -2.6F, 0.8727F, 0.0F, -1.7017F)
      );
      PartDefinition tail = body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 1.0F, 4.0F)
      );
      tail.addOrReplaceChild(
         "tail_fin",
         CubeListBuilder.create().texOffs(22, 13).addBox(-4.0F, -0.5F, -1.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 6.0F)
      );
      body.addOrReplaceChild(
         "back_fin",
         CubeListBuilder.create().texOffs(42, 0).addBox(-0.5F, -1.0F, 1.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -1.0F, -2.7F, 0.8727F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
