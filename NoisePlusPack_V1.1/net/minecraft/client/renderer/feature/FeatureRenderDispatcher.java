package net.minecraft.client.renderer.feature;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FeatureRenderDispatcher implements AutoCloseable {
   private final SubmitNodeStorage submitNodeStorage;
   private final BlockRenderDispatcher blockRenderDispatcher;
   private final MultiBufferSource.BufferSource bufferSource;
   private final AtlasManager atlasManager;
   private final OutlineBufferSource outlineBufferSource;
   private final MultiBufferSource.BufferSource crumblingBufferSource;
   private final Font font;
   private final ShadowFeatureRenderer shadowFeatureRenderer = new ShadowFeatureRenderer();
   private final FlameFeatureRenderer flameFeatureRenderer = new FlameFeatureRenderer();
   private final ModelFeatureRenderer modelFeatureRenderer = new ModelFeatureRenderer();
   private final ModelPartFeatureRenderer modelPartFeatureRenderer = new ModelPartFeatureRenderer();
   private final NameTagFeatureRenderer nameTagFeatureRenderer = new NameTagFeatureRenderer();
   private final TextFeatureRenderer textFeatureRenderer = new TextFeatureRenderer();
   private final LeashFeatureRenderer leashFeatureRenderer = new LeashFeatureRenderer();
   private final ItemFeatureRenderer itemFeatureRenderer = new ItemFeatureRenderer();
   private final CustomFeatureRenderer customFeatureRenderer = new CustomFeatureRenderer();
   private final BlockFeatureRenderer blockFeatureRenderer = new BlockFeatureRenderer();
   private final ParticleFeatureRenderer particleFeatureRenderer = new ParticleFeatureRenderer();

   public FeatureRenderDispatcher(
      SubmitNodeStorage p_429441_,
      BlockRenderDispatcher p_430298_,
      MultiBufferSource.BufferSource p_424449_,
      AtlasManager p_430924_,
      OutlineBufferSource p_427574_,
      MultiBufferSource.BufferSource p_426687_,
      Font p_431145_
   ) {
      this.submitNodeStorage = p_429441_;
      this.blockRenderDispatcher = p_430298_;
      this.bufferSource = p_424449_;
      this.atlasManager = p_430924_;
      this.outlineBufferSource = p_427574_;
      this.crumblingBufferSource = p_426687_;
      this.font = p_431145_;
   }

   public void renderAllFeatures() {
      ObjectIterator var1 = this.submitNodeStorage.getSubmitsPerOrder().values().iterator();

      while (var1.hasNext()) {
         SubmitNodeCollection submitnodecollection = (SubmitNodeCollection)var1.next();
         this.shadowFeatureRenderer.render(submitnodecollection, this.bufferSource);
         this.modelFeatureRenderer.render(submitnodecollection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
         this.modelPartFeatureRenderer.render(submitnodecollection, this.bufferSource, this.outlineBufferSource, this.crumblingBufferSource);
         this.flameFeatureRenderer.render(submitnodecollection, this.bufferSource, this.atlasManager);
         this.nameTagFeatureRenderer.render(submitnodecollection, this.bufferSource, this.font);
         this.textFeatureRenderer.render(submitnodecollection, this.bufferSource);
         this.leashFeatureRenderer.render(submitnodecollection, this.bufferSource);
         this.itemFeatureRenderer.render(submitnodecollection, this.bufferSource, this.outlineBufferSource);
         this.blockFeatureRenderer.render(submitnodecollection, this.bufferSource, this.blockRenderDispatcher, this.outlineBufferSource);
         this.customFeatureRenderer.render(submitnodecollection, this.bufferSource);
         this.particleFeatureRenderer.render(submitnodecollection);
      }

      this.submitNodeStorage.clear();
   }

   public void endFrame() {
      this.particleFeatureRenderer.endFrame();
   }

   public SubmitNodeStorage getSubmitNodeStorage() {
      return this.submitNodeStorage;
   }

   @Override
   public void close() {
      this.particleFeatureRenderer.close();
   }
}
