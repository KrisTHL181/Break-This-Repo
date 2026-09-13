package com.mojang.blaze3d.vertex;

import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import java.nio.ByteOrder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class BufferBuilder implements VertexConsumer {
   private static final int MAX_VERTEX_COUNT = 16777215;
   private static final long NOT_BUILDING = -1L;
   private static final long UNKNOWN_ELEMENT = -1L;
   private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
   private final ByteBufferBuilder buffer;
   private long vertexPointer = -1L;
   private int vertices;
   private final VertexFormat format;
   private final PrimitiveTopology primitiveTopology;
   private final boolean blockFormat;
   private final boolean entityFormat;
   private final int vertexSize;
   private final int initialElementsToFill;
   private int elementsToFill;
   private boolean building = true;
   private static final int POSITION_SEMANTIC_ID = 0;
   private static final int COLOR_SEMANTIC_ID = 1;
   private static final int UV0_SEMANTIC_ID = 2;
   private static final int UV1_SEMANTIC_ID = 3;
   private static final int UV2_SEMANTIC_ID = 4;
   private static final int UV3_SEMANTIC_ID = 5;
   private static final int NORMAL_SEMANTIC_ID = 6;
   private static final int LINE_WIDTH_SEMANTIC_ID = 7;
   private static final String[] elementNames = new String[]{"Position", "Color", "UV0", "UV1", "UV2", "UV3", "Normal", "LineWidth"};
   private final @Nullable VertexFormatElement[] elements = new VertexFormatElement[elementNames.length];

   public BufferBuilder(final ByteBufferBuilder buffer, final PrimitiveTopology primitiveTopology, final VertexFormat format) {
      if (!format.contains("Position")) {
         throw new IllegalArgumentException("Cannot build mesh with no position element");
      }

      this.buffer = buffer;
      this.primitiveTopology = primitiveTopology;
      this.format = format;
      this.vertexSize = format.getVertexSize();
      int elementsMask = 0;

      for (int i = 0; i < elementNames.length; i++) {
         String elementName = elementNames[i];
         VertexFormatElement element = format.getElement(elementName);
         if (element != null) {
            elementsMask |= 1 << i;
         }

         this.elements[i] = element;
      }

      this.initialElementsToFill = elementsMask & -2;
      this.blockFormat = format == DefaultVertexFormat.BLOCK;
      this.entityFormat = format == DefaultVertexFormat.ENTITY;
   }

   public @Nullable MeshData build() {
      this.ensureBuilding();
      this.endLastVertex();
      MeshData mesh = this.storeMesh();
      this.building = false;
      this.vertexPointer = -1L;
      return mesh;
   }

   public MeshData buildOrThrow() {
      MeshData buffer = this.build();
      if (buffer == null) {
         throw new IllegalStateException("BufferBuilder was empty");
      } else {
         return buffer;
      }
   }

   private void ensureBuilding() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   private @Nullable MeshData storeMesh() {
      if (this.vertices == 0) {
         return null;
      }

      ByteBufferBuilder.Result vertexBuffer = this.buffer.build();
      if (vertexBuffer == null) {
         return null;
      }

      int indices = this.primitiveTopology.indexCount(this.vertices);
      IndexType indexType = IndexType.least(this.vertices);
      return new MeshData(vertexBuffer, new MeshData.DrawState(this.format, this.vertices, indices, this.primitiveTopology, indexType));
   }

   private long beginVertex() {
      this.ensureBuilding();
      this.endLastVertex();
      if (this.vertices >= 16777215) {
         throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
      }

      this.vertices++;
      long pointer = this.buffer.reserve(this.vertexSize);
      this.vertexPointer = pointer;
      return pointer;
   }

   private long beginElement(final int semanticID) {
      int oldElements = this.elementsToFill;
      int newElements = oldElements & ~(1 << semanticID);
      VertexFormatElement element = this.elements[semanticID];
      if (newElements != oldElements && element != null) {
         this.elementsToFill = newElements;
         long vertexPointer = this.vertexPointer;
         if (vertexPointer == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
         } else {
            return vertexPointer + element.offset();
         }
      } else {
         return -1L;
      }
   }

   private void endLastVertex() {
      if (this.vertices != 0) {
         if (this.elementsToFill != 0) {
            String missingElements = IntStream.range(0, elementNames.length)
               .filter(i -> (this.elementsToFill & i) != 0)
               .mapToObj(i -> elementNames[i])
               .collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + missingElements);
         }

         if (this.primitiveTopology == PrimitiveTopology.LINES) {
            long pointer = this.buffer.reserve(this.vertexSize);
            MemoryUtil.memCopy(pointer - this.vertexSize, pointer, this.vertexSize);
            this.vertices++;
         }
      }
   }

   private static void putRgba(final long pointer, final int argb) {
      int abgr = ARGB.toABGR(argb);
      MemoryUtil.memPutInt(pointer, IS_LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
   }

   private static void putPackedUv(final long pointer, final int packedUv) {
      if (IS_LITTLE_ENDIAN) {
         MemoryUtil.memPutInt(pointer, packedUv);
      } else {
         MemoryUtil.memPutShort(pointer, (short)(packedUv & 65535));
         MemoryUtil.memPutShort(pointer + 2L, (short)(packedUv >> 16 & 65535));
      }
   }

   @Override
   public VertexConsumer addVertex(final float x, final float y, final float z) {
      VertexFormatElement positionElement = this.elements[0];
      long pointer = this.beginVertex() + positionElement.offset();
      this.elementsToFill = this.initialElementsToFill;
      putVec3f(pointer, x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
      long pointer = this.beginElement(1);
      if (pointer != -1L) {
         MemoryUtil.memPutByte(pointer, (byte)r);
         MemoryUtil.memPutByte(pointer + 1L, (byte)g);
         MemoryUtil.memPutByte(pointer + 2L, (byte)b);
         MemoryUtil.memPutByte(pointer + 3L, (byte)a);
      }

      return this;
   }

   @Override
   public VertexConsumer setColor(final int color) {
      long pointer = this.beginElement(1);
      if (pointer != -1L) {
         putRgba(pointer, color);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv(final float u, final float v) {
      long pointer = this.beginElement(2);
      if (pointer != -1L) {
         MemoryUtil.memPutFloat(pointer, u);
         MemoryUtil.memPutFloat(pointer + 4L, v);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv1(final int u, final int v) {
      return this.uvShort((short)u, (short)v, 3);
   }

   @Override
   public VertexConsumer setOverlay(final int packedOverlayCoords) {
      long pointer = this.beginElement(3);
      if (pointer != -1L) {
         putPackedUv(pointer, packedOverlayCoords);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv2(final int u, final int v) {
      return this.uvShort((short)u, (short)v, 4);
   }

   @Override
   public VertexConsumer setUv3(final float u, final float v) {
      long pointer = this.beginElement(5);
      if (pointer != -1L) {
         MemoryUtil.memPutFloat(pointer, u);
         MemoryUtil.memPutFloat(pointer + 4L, v);
      }

      return this;
   }

   @Override
   public VertexConsumer setLight(final int packedLightCoords) {
      long pointer = this.beginElement(4);
      if (pointer != -1L) {
         putPackedUv(pointer, packedLightCoords);
      }

      return this;
   }

   private VertexConsumer uvShort(final short u, final short v, final int semanticID) {
      long pointer = this.beginElement(semanticID);
      if (pointer != -1L) {
         MemoryUtil.memPutShort(pointer, u);
         MemoryUtil.memPutShort(pointer + 2L, v);
      }

      return this;
   }

   @Override
   public VertexConsumer setNormal(final float x, final float y, final float z) {
      long pointer = this.beginElement(6);
      if (pointer != -1L) {
         putNormals(pointer, x, y, z);
      }

      return this;
   }

   @Override
   public VertexConsumer setLineWidth(final float width) {
      long pointer = this.beginElement(7);
      if (pointer != -1L) {
         MemoryUtil.memPutFloat(pointer, width);
      }

      return this;
   }

   private static byte normalIntValue(final float c) {
      return (byte)((int)(Mth.clamp(c, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   private static void putVec3f(final long pointer, final float x, final float y, final float z) {
      MemoryUtil.memPutFloat(pointer, x);
      MemoryUtil.memPutFloat(pointer + 4L, y);
      MemoryUtil.memPutFloat(pointer + 8L, z);
   }

   private static void putNormals(final long pointer, final float nx, final float ny, final float nz) {
      MemoryUtil.memPutByte(pointer, normalIntValue(nx));
      MemoryUtil.memPutByte(pointer + 1L, normalIntValue(ny));
      MemoryUtil.memPutByte(pointer + 2L, normalIntValue(nz));
   }

   @Override
   public void addVertex(
      final float x,
      final float y,
      final float z,
      final int color,
      final float u,
      final float v,
      final int overlayCoords,
      final int lightCoords,
      final float nx,
      final float ny,
      final float nz
   ) {
      if (this.blockFormat) {
         long pointer = this.beginVertex();
         putVec3f(pointer, x, y, z);
         putRgba(pointer + 12L, color);
         MemoryUtil.memPutFloat(pointer + 16L, u);
         MemoryUtil.memPutFloat(pointer + 20L, v);
         putPackedUv(pointer + 24L, lightCoords);
      } else if (this.entityFormat) {
         long pointer = this.beginVertex();
         putVec3f(pointer, x, y, z);
         putRgba(pointer + 12L, color);
         MemoryUtil.memPutFloat(pointer + 16L, u);
         MemoryUtil.memPutFloat(pointer + 20L, v);
         putPackedUv(pointer + 24L, overlayCoords);
         putPackedUv(pointer + 28L, lightCoords);
         putNormals(pointer + 32L, nx, ny, nz);
      } else {
         VertexConsumer.super.addVertex(x, y, z, color, u, v, overlayCoords, lightCoords, nx, ny, nz);
      }
   }
}
