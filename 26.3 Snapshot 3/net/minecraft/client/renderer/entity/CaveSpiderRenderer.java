package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.spider.CaveSpider;

public class CaveSpiderRenderer extends SpiderRenderer<CaveSpider> {
   private static final Identifier CAVE_SPIDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/spider/cave_spider.png");

   public CaveSpiderRenderer(final EntityRendererProvider.Context context) {
      super(context, ModelLayers.CAVE_SPIDER);
      this.shadowRadius = 0.56F;
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return CAVE_SPIDER_LOCATION;
   }
}
