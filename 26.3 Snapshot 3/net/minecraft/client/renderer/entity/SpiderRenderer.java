package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.spider.Spider;

public class SpiderRenderer<T extends Spider> extends MobRenderer<T, LivingEntityRenderState, SpiderModel> {
   private static final Identifier SPIDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/spider/spider.png");

   public SpiderRenderer(final EntityRendererProvider.Context context) {
      this(context, ModelLayers.SPIDER);
   }

   public SpiderRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation model) {
      super(context, new SpiderModel(context.bakeLayer(model)), 0.8F);
      this.addLayer(new SpiderEyesLayer<>(this));
   }

   @Override
   protected float getFlipDegrees() {
      return 180.0F;
   }

   @Override
   public Identifier getTextureLocation(final LivingEntityRenderState state) {
      return SPIDER_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public void extractRenderState(final T entity, final LivingEntityRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
   }
}
