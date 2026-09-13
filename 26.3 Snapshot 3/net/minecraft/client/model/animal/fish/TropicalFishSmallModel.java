package net.minecraft.client.model.animal.fish;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.util.Mth;

public class TropicalFishSmallModel extends EntityModel<TropicalFishRenderState> {
   private final ModelPart tail;

   public TropicalFishSmallModel(final ModelPart root) {
      super(root);
      this.tail = root.getChild("tail");
   }

   public static LayerDefinition createBodyLayer(final CubeDeformation g) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      int yo = 22;
      root.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -3.0F, 2.0F, 3.0F, 6.0F, g), PartPose.offset(0.0F, 22.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(22, -6).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 6.0F, g), PartPose.offset(0.0F, 22.0F, 3.0F)
      );
      root.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(2, 16).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, g),
         PartPose.offsetAndRotation(-1.0F, 22.5F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      root.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(2, 12).addBox(0.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, g),
         PartPose.offsetAndRotation(1.0F, 22.5F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      root.addOrReplaceChild(
         "top_fin", CubeListBuilder.create().texOffs(10, -5).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 6.0F, g), PartPose.offset(0.0F, 20.5F, -3.0F)
      );
      return LayerDefinition.create(mesh, 32, 32);
   }

   public void setupAnim(final TropicalFishRenderState state) {
      super.setupAnim(state);
      float amplitudeMultiplier = state.isInWater ? 1.0F : 1.5F;
      this.tail.yRot = -amplitudeMultiplier * 0.45F * Mth.sin(0.6F * state.ageInTicks);
   }
}
