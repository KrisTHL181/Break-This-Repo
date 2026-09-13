package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SequencedMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderBuffers {
   private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
   private final SectionBufferBuilderPool sectionBufferPool;
   private final MultiBufferSource.BufferSource bufferSource;
   private final MultiBufferSource.BufferSource crumblingBufferSource;
   private final OutlineBufferSource outlineBufferSource;

   public RenderBuffers(int p_312933_) {
      this.sectionBufferPool = SectionBufferBuilderPool.allocate(p_312933_);
      SequencedMap<RenderType, ByteBufferBuilder> sequencedmap = Util.make(new Object2ObjectLinkedOpenHashMap(), p_357875_ -> {
         p_357875_.put(Sheets.solidBlockSheet(), this.fixedBufferPack.buffer(ChunkSectionLayer.SOLID));
         p_357875_.put(Sheets.cutoutBlockSheet(), this.fixedBufferPack.buffer(ChunkSectionLayer.CUTOUT));
         p_357875_.put(Sheets.translucentItemSheet(), this.fixedBufferPack.buffer(ChunkSectionLayer.TRANSLUCENT));
         put(p_357875_, Sheets.translucentBlockItemSheet());
         put(p_357875_, Sheets.shieldSheet());
         put(p_357875_, Sheets.bedSheet());
         put(p_357875_, Sheets.shulkerBoxSheet());
         put(p_357875_, Sheets.signSheet());
         put(p_357875_, Sheets.hangingSignSheet());
         p_357875_.put(Sheets.chestSheet(), new ByteBufferBuilder(786432));
         put(p_357875_, RenderTypes.armorEntityGlint());
         put(p_357875_, RenderTypes.glint());
         put(p_357875_, RenderTypes.glintTranslucent());
         put(p_357875_, RenderTypes.entityGlint());
         put(p_357875_, RenderTypes.waterMask());
      });
      this.bufferSource = MultiBufferSource.immediateWithBuffers(sequencedmap, new ByteBufferBuilder(786432));
      this.outlineBufferSource = new OutlineBufferSource();
      SequencedMap<RenderType, ByteBufferBuilder> sequencedmap1 = Util.make(
         new Object2ObjectLinkedOpenHashMap(), p_357874_ -> ModelBakery.DESTROY_TYPES.forEach(p_448189_ -> put(p_357874_, p_448189_))
      );
      this.crumblingBufferSource = MultiBufferSource.immediateWithBuffers(sequencedmap1, new ByteBufferBuilder(0));
   }

   private static void put(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> p_110102_, RenderType p_454052_) {
      p_110102_.put(p_454052_, new ByteBufferBuilder(p_454052_.bufferSize()));
   }

   public SectionBufferBuilderPack fixedBufferPack() {
      return this.fixedBufferPack;
   }

   public SectionBufferBuilderPool sectionBufferPool() {
      return this.sectionBufferPool;
   }

   public MultiBufferSource.BufferSource bufferSource() {
      return this.bufferSource;
   }

   public MultiBufferSource.BufferSource crumblingBufferSource() {
      return this.crumblingBufferSource;
   }

   public OutlineBufferSource outlineBufferSource() {
      return this.outlineBufferSource;
   }
}
