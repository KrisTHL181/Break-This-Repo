package com.mojang.blaze3d.vertex;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class MeshData implements AutoCloseable {
   private final ByteBufferBuilder.Result vertexBuffer;
   private ByteBufferBuilder.@Nullable Result indexBuffer;
   private final MeshData.DrawState drawState;

   public MeshData(ByteBufferBuilder.Result p_345436_, MeshData.DrawState p_343210_) {
      this.vertexBuffer = p_345436_;
      this.drawState = p_343210_;
   }

   private static CompactVectorArray unpackQuadCentroids(ByteBuffer p_342486_, int p_344467_, VertexFormat p_342165_) {
      int i = p_342165_.getOffset(VertexFormatElement.POSITION);
      if (i == -1) {
         throw new IllegalArgumentException("Cannot identify quad centers with no position element");
      }

      FloatBuffer floatbuffer = p_342486_.asFloatBuffer();
      int j = p_342165_.getVertexSize() / 4;
      int k = j * 4;
      int l = p_344467_ / 4;
      CompactVectorArray compactvectorarray = new CompactVectorArray(l);

      for (int i1 = 0; i1 < l; i1++) {
         int j1 = i1 * k + i;
         int k1 = j1 + j * 2;
         float f = floatbuffer.get(j1 + 0);
         float f1 = floatbuffer.get(j1 + 1);
         float f2 = floatbuffer.get(j1 + 2);
         float f3 = floatbuffer.get(k1 + 0);
         float f4 = floatbuffer.get(k1 + 1);
         float f5 = floatbuffer.get(k1 + 2);
         float f6 = (f + f3) / 2.0F;
         float f7 = (f1 + f4) / 2.0F;
         float f8 = (f2 + f5) / 2.0F;
         compactvectorarray.set(i1, f6, f7, f8);
      }

      return compactvectorarray;
   }

   public ByteBuffer vertexBuffer() {
      return this.vertexBuffer.byteBuffer();
   }

   public @Nullable ByteBuffer indexBuffer() {
      return this.indexBuffer != null ? this.indexBuffer.byteBuffer() : null;
   }

   public MeshData.DrawState drawState() {
      return this.drawState;
   }

   public MeshData.@Nullable SortState sortQuads(ByteBufferBuilder p_344832_, VertexSorting p_343251_) {
      if (this.drawState.mode() != VertexFormat.Mode.QUADS) {
         return null;
      }

      CompactVectorArray compactvectorarray = unpackQuadCentroids(this.vertexBuffer.byteBuffer(), this.drawState.vertexCount(), this.drawState.format());
      MeshData.SortState meshdata$sortstate = new MeshData.SortState(compactvectorarray, this.drawState.indexType());
      this.indexBuffer = meshdata$sortstate.buildSortedIndexBuffer(p_344832_, p_343251_);
      return meshdata$sortstate;
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
   }

   @OnlyIn(Dist.CLIENT)
   public record SortState(CompactVectorArray centroids, VertexFormat.IndexType indexType) {
      public ByteBufferBuilder.@Nullable Result buildSortedIndexBuffer(ByteBufferBuilder p_342323_, VertexSorting p_342363_) {
         int[] aint = p_342363_.sort(this.centroids);
         long i = p_342323_.reserve(aint.length * 6 * this.indexType.bytes);
         IntConsumer intconsumer = this.indexWriter(i, this.indexType);

         for (int j : aint) {
            intconsumer.accept(j * 4 + 0);
            intconsumer.accept(j * 4 + 1);
            intconsumer.accept(j * 4 + 2);
            intconsumer.accept(j * 4 + 2);
            intconsumer.accept(j * 4 + 3);
            intconsumer.accept(j * 4 + 0);
         }

         return p_342323_.build();
      }

      private IntConsumer indexWriter(long p_342999_, VertexFormat.IndexType p_343431_) {
         MutableLong mutablelong = new MutableLong(p_342999_);

         return switch (p_343431_) {
            case SHORT -> p_344551_ -> MemoryUtil.memPutShort(mutablelong.getAndAdd(2L), (short)p_344551_);
            case INT -> p_342795_ -> MemoryUtil.memPutInt(mutablelong.getAndAdd(4L), p_342795_);
         };
      }
   }
}
