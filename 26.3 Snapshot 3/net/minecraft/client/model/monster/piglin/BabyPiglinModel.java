package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BabyPiglinModel extends PiglinModel {
   public BabyPiglinModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 18.0F, -0.5F));
      PartDefinition head = root.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(21, 30).addBox(-1.5F, -3.0F, -4.5F, 3.0F, 3.0F, 1.0F).texOffs(0, 0).addBox(-4.5F, -6.0F, -3.5F, 9.0F, 6.0F, 7.0F),
         PartPose.offset(0.0F, 15.0F, 0.0F)
      );
      head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition leftear = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(4.2F, -4.0F, 0.0F));
      leftear.addOrReplaceChild(
         "left_ear_r1",
         CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
         PartPose.offsetAndRotation(1.0F, 1.75F, 0.0F, 0.0F, 0.0F, -0.6109F)
      );
      PartDefinition rightear = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-4.2F, -4.0F, 0.0F));
      rightear.addOrReplaceChild(
         "right_ear_r1",
         CubeListBuilder.create().texOffs(18, 13).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
         PartPose.offsetAndRotation(-1.0F, 1.75F, 0.0F, 0.0F, 0.0F, 0.6109F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(28, 13).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offset(4.0F, 15.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(10, 30).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F), PartPose.offset(-4.0F, 15.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(22, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(-1.5F, 20.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(10, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(1.5F, 20.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   @Override
   protected float getDefaultEarAngleInDegrees() {
      return 5.0F;
   }
}
