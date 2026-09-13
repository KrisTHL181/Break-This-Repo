package net.minecraft.client.model.npc;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyVillagerModel extends VillagerModel {
   public BabyVillagerModel(final ModelPart root) {
      super(root);
   }

   public static MeshDefinition createNoHatModel() {
      MeshDefinition mesh = createBodyModel();
      mesh.getRoot().clearChild("head").clearRecursively();
      return mesh;
   }

   public static MeshDefinition createBodyModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, 17.5F, 0.0F));
      arms.addOrReplaceChild(
         "right_hand",
         CubeListBuilder.create()
            .texOffs(36, 15)
            .addBox(-1.0F, -2.4925F, -1.8401F, 2.0F, 4.0F, 2.0F)
            .texOffs(16, 15)
            .addBox(5.0F, -2.4925F, -1.8401F, 2.0F, 4.0F, 2.0F),
         PartPose.offsetAndRotation(-3.0F, 1.4025F, -0.9599F, -1.0472F, 0.0F, 0.0F)
      );
      arms.addOrReplaceChild(
         "middlearm_r1",
         CubeListBuilder.create().texOffs(24, 17).addBox(-2.0F, -0.9924F, -0.9825F, 4.0F, 2.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.9024F, -1.8175F, -1.0472F, 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(-1.0F, 21.5F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(1.0F, 21.5F, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -3.5F, 8.0F, 8.0F, 7.0F), PartPose.offset(0.0F, 16.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "hat",
         CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -4.0F, -3.5F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.3F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "hat_rim", CubeListBuilder.create().texOffs(0, 45).addBox(-7.0F, -0.5F, -6.0F, 14.0F, 1.0F, 12.0F), PartPose.offset(0.0F, -4.5F, 0.0F)
      );
      head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -2.0F, -4.0F));
      root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 15).addBox(-2.0F, -2.75F, -1.5F, 4.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 18.75F, 0.0F)
      );
      root.addOrReplaceChild(
         "bb_main",
         CubeListBuilder.create().texOffs(16, 21).addBox(-2.5F, -8.0F, -1.5F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.2F)),
         PartPose.offset(0.5F, 24.0F, 0.0F)
      );
      return mesh;
   }
}
