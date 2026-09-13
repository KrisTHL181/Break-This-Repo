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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonClothingLayer<S extends SkeletonRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final SkeletonModel<S> layerModel;
   private final Identifier clothesLocation;

   public SkeletonClothingLayer(RenderLayerParent<S, M> p_330715_, EntityModelSet p_334793_, ModelLayerLocation p_335699_, Identifier p_454320_) {
      super(p_330715_);
      this.clothesLocation = p_454320_;
      this.layerModel = new SkeletonModel<>(p_334793_.bakeLayer(p_335699_));
   }

   public void submit(PoseStack p_429921_, SubmitNodeCollector p_424464_, int p_431583_, S p_427661_, float p_428785_, float p_423645_) {
      coloredCutoutModelCopyLayerRender(this.layerModel, this.clothesLocation, p_429921_, p_424464_, p_431583_, p_427661_, -1, 1);
   }
}
