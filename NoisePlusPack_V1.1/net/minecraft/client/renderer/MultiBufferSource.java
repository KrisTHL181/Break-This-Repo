package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface MultiBufferSource {
   static MultiBufferSource.BufferSource immediate(ByteBufferBuilder p_344614_) {
      return immediateWithBuffers(Object2ObjectSortedMaps.emptyMap(), p_344614_);
   }

   static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> p_342750_, ByteBufferBuilder p_344601_) {
      return new MultiBufferSource.BufferSource(p_344601_, p_342750_);
   }

   VertexConsumer getBuffer(RenderType var1);

   @OnlyIn(Dist.CLIENT)
   class BufferSource implements MultiBufferSource {
      protected final ByteBufferBuilder sharedBuffer;
      protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
      protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
      protected @Nullable RenderType lastSharedType;

      protected BufferSource(ByteBufferBuilder p_344223_, SequencedMap<RenderType, ByteBufferBuilder> p_344104_) {
         this.sharedBuffer = p_344223_;
         this.fixedBuffers = p_344104_;
      }

      @Override
      public VertexConsumer getBuffer(RenderType p_451002_) {
         BufferBuilder bufferbuilder = this.startedBuilders.get(p_451002_);
         if (bufferbuilder != null && !p_451002_.canConsolidateConsecutiveGeometry()) {
            this.endBatch(p_451002_, bufferbuilder);
            bufferbuilder = null;
         }

         if (bufferbuilder != null) {
            return bufferbuilder;
         }

         ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.get(p_451002_);
         if (bytebufferbuilder != null) {
            bufferbuilder = new BufferBuilder(bytebufferbuilder, p_451002_.mode(), p_451002_.format());
         } else {
            if (this.lastSharedType != null) {
               this.endBatch(this.lastSharedType);
            }

            bufferbuilder = new BufferBuilder(this.sharedBuffer, p_451002_.mode(), p_451002_.format());
            this.lastSharedType = p_451002_;
         }

         this.startedBuilders.put(p_451002_, bufferbuilder);
         return bufferbuilder;
      }

      public void endLastBatch() {
         if (this.lastSharedType != null) {
            this.endBatch(this.lastSharedType);
            this.lastSharedType = null;
         }
      }

      public void endBatch() {
         this.endLastBatch();

         for (RenderType rendertype : this.fixedBuffers.keySet()) {
            this.endBatch(rendertype);
         }
      }

      public void endBatch(RenderType p_455992_) {
         BufferBuilder bufferbuilder = this.startedBuilders.remove(p_455992_);
         if (bufferbuilder != null) {
            this.endBatch(p_455992_, bufferbuilder);
         }
      }

      private void endBatch(RenderType p_455606_, BufferBuilder p_344480_) {
         MeshData meshdata = p_344480_.build();
         if (meshdata != null) {
            if (p_455606_.sortOnUpload()) {
               ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.getOrDefault(p_455606_, this.sharedBuffer);
               meshdata.sortQuads(bytebufferbuilder, RenderSystem.getProjectionType().vertexSorting());
            }

            p_455606_.draw(meshdata);
         }

         if (p_455606_.equals(this.lastSharedType)) {
            this.lastSharedType = null;
         }
      }
   }
}
