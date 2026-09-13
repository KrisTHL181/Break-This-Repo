package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpiderEyesLayer<M extends SpiderModel> extends EyesLayer<LivingEntityRenderState, M> {
   private static final RenderType SPIDER_EYES = RenderTypes.eyes(Identifier.withDefaultNamespace("textures/entity/spider_eyes.png"));

   public SpiderEyesLayer(RenderLayerParent<LivingEntityRenderState, M> p_117507_) {
      super(p_117507_);
   }

   @Override
   public RenderType renderType() {
      return SPIDER_EYES;
   }
}
