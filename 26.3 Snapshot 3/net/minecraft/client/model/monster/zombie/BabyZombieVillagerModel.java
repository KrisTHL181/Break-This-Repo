package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;

public class BabyZombieVillagerModel<S extends ZombieVillagerRenderState> extends ZombieVillagerModel<S> {
   public BabyZombieVillagerModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 15)
            .addBox(-2.0F, -2.75F, -1.5F, 4.0F, 5.0F, 3.0F)
            .texOffs(16, 22)
            .addBox(-2.0F, -2.75F, -1.5F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.1F)),
         PartPose.offset(0.0F, 18.75F, 0.0F)
      );
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -3.5F, 8.0F, 8.0F, 7.0F), PartPose.offset(0.0F, 16.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "hat",
         CubeListBuilder.create().texOffs(0, 31).addBox(-4.0F, -4.0F, -3.5F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.3F)),
         PartPose.offset(0.0F, -4.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "hat_rim", CubeListBuilder.create().texOffs(0, 46).addBox(-7.0F, -0.5F, -6.0F, 14.0F, 1.0F, 12.0F), PartPose.offset(0.0F, -4.5F, 0.0F)
      );
      head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -1.0F, -4.0F));
      PartDefinition right_arm = root.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(24, 15).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-3.0F, 15.5F, 0.0F)
      );
      PartDefinition left_arm = root.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create().texOffs(16, 15).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, 15.5F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(-1.0F, 21.5F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(1.0F, 21.5F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createNoHatLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().clearChild("head").clearRecursively();
         return mesh;
      });
   }
}
