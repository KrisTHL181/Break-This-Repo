package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RopesLayer<M extends HappyGhastModel> extends RenderLayer<HappyGhastRenderState, M> {
   private final RenderType ropes;
   private final HappyGhastModel adultModel;
   private final HappyGhastModel babyModel;

   public RopesLayer(RenderLayerParent<HappyGhastRenderState, M> p_408307_, EntityModelSet p_408757_, Identifier p_451381_) {
      super(p_408307_);
      this.ropes = RenderTypes.entityCutoutNoCull(p_451381_);
      this.adultModel = new HappyGhastModel(p_408757_.bakeLayer(ModelLayers.HAPPY_GHAST_ROPES));
      this.babyModel = new HappyGhastModel(p_408757_.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_ROPES));
   }

   public void submit(PoseStack p_427272_, SubmitNodeCollector p_428209_, int p_428944_, HappyGhastRenderState p_430468_, float p_429494_, float p_429110_) {
      if (p_430468_.isLeashHolder && p_430468_.bodyItem.is(ItemTags.HARNESSES)) {
         HappyGhastModel happyghastmodel = p_430468_.isBaby ? this.babyModel : this.adultModel;
         p_428209_.submitModel(happyghastmodel, p_430468_, p_427272_, this.ropes, p_428944_, OverlayTexture.NO_OVERLAY, p_430468_.outlineColor, null);
      }
   }
}
