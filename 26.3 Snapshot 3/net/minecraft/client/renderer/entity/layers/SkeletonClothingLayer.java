package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

public class SkeletonClothingLayer<S extends SkeletonRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final SkeletonModel<S> layerModel;
   private final Identifier clothesLocation;

   public SkeletonClothingLayer(
      final RenderLayerParent<S, M> renderer, final EntityModelSet models, final ModelLayerLocation layerLocation, final Identifier clothesLocation
   ) {
      super(renderer);
      this.clothesLocation = clothesLocation;
      this.layerModel = new SkeletonModel<>(models.bakeLayer(layerLocation));
   }

   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      coloredCutoutModelCopyLayerRender(this.layerModel, this.clothesLocation, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
   }
}
