package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
   private final SlimeModel model;

   public SlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> p_174536_, EntityModelSet p_174537_) {
      super(p_174536_);
      this.model = new SlimeModel(p_174537_.bakeLayer(ModelLayers.SLIME_OUTER));
   }

   public void submit(PoseStack p_423939_, SubmitNodeCollector p_429066_, int p_425663_, SlimeRenderState p_430262_, float p_427382_, float p_429878_) {
      boolean flag = p_430262_.appearsGlowing() && p_430262_.isInvisible;
      if (!p_430262_.isInvisible || flag) {
         int i = LivingEntityRenderer.getOverlayCoords(p_430262_, 0.0F);
         if (flag) {
            p_429066_.order(1)
               .submitModel(
                  this.model, p_430262_, p_423939_, RenderTypes.outline(SlimeRenderer.SLIME_LOCATION), p_425663_, i, -1, null, p_430262_.outlineColor, null
               );
         } else {
            p_429066_.order(1)
               .submitModel(
                  this.model,
                  p_430262_,
                  p_423939_,
                  RenderTypes.entityTranslucent(SlimeRenderer.SLIME_LOCATION),
                  p_425663_,
                  i,
                  -1,
                  null,
                  p_430262_.outlineColor,
                  null
               );
         }
      }
   }
}
