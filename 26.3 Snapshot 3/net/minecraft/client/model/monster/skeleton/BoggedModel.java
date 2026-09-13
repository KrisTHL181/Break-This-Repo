package net.minecraft.client.model.monster.skeleton;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BoggedRenderState;

public class BoggedModel extends SkeletonModel<BoggedRenderState> {
   private final ModelPart mushrooms;

   public BoggedModel(final ModelPart root) {
      super(root);
      this.mushrooms = root.getChild("head").getChild("mushrooms");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition root = mesh.getRoot();
      SkeletonModel.createDefaultSkeletonMesh(root);
      PartDefinition mushrooms = root.getChild("head").addOrReplaceChild("mushrooms", CubeListBuilder.create(), PartPose.ZERO);
      mushrooms.addOrReplaceChild(
         "red_mushroom_1",
         CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      mushrooms.addOrReplaceChild(
         "red_mushroom_2",
         CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F)
      );
      mushrooms.addOrReplaceChild(
         "brown_mushroom_1",
         CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      mushrooms.addOrReplaceChild(
         "brown_mushroom_2",
         CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F)
      );
      mushrooms.addOrReplaceChild(
         "brown_mushroom_3",
         CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI / 4))
      );
      mushrooms.addOrReplaceChild(
         "brown_mushroom_4",
         CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
         PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 4.0))
      );
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final BoggedRenderState state) {
      super.setupAnim(state);
      this.mushrooms.visible = !state.isSheared;
   }
}
