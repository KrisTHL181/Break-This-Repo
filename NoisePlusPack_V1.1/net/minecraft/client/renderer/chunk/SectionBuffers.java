package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SectionBuffers implements AutoCloseable {
   private GpuBuffer vertexBuffer;
   private @Nullable GpuBuffer indexBuffer;
   private int indexCount;
   private VertexFormat.IndexType indexType;

   public SectionBuffers(GpuBuffer p_408356_, @Nullable GpuBuffer p_408653_, int p_409167_, VertexFormat.IndexType p_405985_) {
      this.vertexBuffer = p_408356_;
      this.indexBuffer = p_408653_;
      this.indexCount = p_409167_;
      this.indexType = p_405985_;
   }

   public GpuBuffer getVertexBuffer() {
      return this.vertexBuffer;
   }

   public @Nullable GpuBuffer getIndexBuffer() {
      return this.indexBuffer;
   }

   public void setIndexBuffer(@Nullable GpuBuffer p_406609_) {
      this.indexBuffer = p_406609_;
   }

   public int getIndexCount() {
      return this.indexCount;
   }

   public VertexFormat.IndexType getIndexType() {
      return this.indexType;
   }

   public void setIndexType(VertexFormat.IndexType p_409241_) {
      this.indexType = p_409241_;
   }

   public void setIndexCount(int p_406950_) {
      this.indexCount = p_406950_;
   }

   public void setVertexBuffer(GpuBuffer p_407204_) {
      this.vertexBuffer = p_407204_;
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }
}
