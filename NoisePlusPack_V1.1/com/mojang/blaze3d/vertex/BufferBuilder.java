package com.mojang.blaze3d.vertex;

import java.nio.ByteOrder;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class BufferBuilder implements VertexConsumer {
   private static final int MAX_VERTEX_COUNT = 16777215;
   private static final long NOT_BUILDING = -1L;
   private static final long UNKNOWN_ELEMENT = -1L;
   private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
   private final ByteBufferBuilder buffer;
   private long vertexPointer = -1L;
   private int vertices;
   private final VertexFormat format;
   private final VertexFormat.Mode mode;
   private final boolean fastFormat;
   private final boolean fullFormat;
   private final int vertexSize;
   private final int initialElementsToFill;
   private final int[] offsetsByElement;
   private int elementsToFill;
   private boolean building = true;

   public BufferBuilder(ByteBufferBuilder p_342927_, VertexFormat.Mode p_344709_, VertexFormat p_342329_) {
      if (!p_342329_.contains(VertexFormatElement.POSITION)) {
         throw new IllegalArgumentException("Cannot build mesh with no position element");
      }

      this.buffer = p_342927_;
      this.mode = p_344709_;
      this.format = p_342329_;
      this.vertexSize = p_342329_.getVertexSize();
      this.initialElementsToFill = p_342329_.getElementsMask() & ~VertexFormatElement.POSITION.mask();
      this.offsetsByElement = p_342329_.getOffsetsByElement();
      boolean flag = p_342329_ == DefaultVertexFormat.NEW_ENTITY;
      boolean flag1 = p_342329_ == DefaultVertexFormat.BLOCK;
      this.fastFormat = flag || flag1;
      this.fullFormat = flag;
   }

   public @Nullable MeshData build() {
      this.ensureBuilding();
      this.endLastVertex();
      MeshData meshdata = this.storeMesh();
      this.building = false;
      this.vertexPointer = -1L;
      return meshdata;
   }

   public MeshData buildOrThrow() {
      MeshData meshdata = this.build();
      if (meshdata == null) {
         throw new IllegalStateException("BufferBuilder was empty");
      } else {
         return meshdata;
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

      ByteBufferBuilder.Result bytebufferbuilder$result = this.buffer.build();
      if (bytebufferbuilder$result == null) {
         return null;
      }

      int i = this.mode.indexCount(this.vertices);
      VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(this.vertices);
      return new MeshData(bytebufferbuilder$result, new MeshData.DrawState(this.format, this.vertices, i, this.mode, vertexformat$indextype));
   }

   private long beginVertex() {
      this.ensureBuilding();
      this.endLastVertex();
      if (this.vertices >= 16777215) {
         throw new IllegalStateException("Trying to write too many vertices (>16777215) into BufferBuilder");
      }

      this.vertices++;
      long i = this.buffer.reserve(this.vertexSize);
      this.vertexPointer = i;
      return i;
   }

   private long beginElement(VertexFormatElement p_343872_) {
      int i = this.elementsToFill;
      int j = i & ~p_343872_.mask();
      if (j == i) {
         return -1L;
      } else {
         this.elementsToFill = j;
         long k = this.vertexPointer;
         if (k == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
         } else {
            return k + this.offsetsByElement[p_343872_.id()];
         }
      }
   }

   private void endLastVertex() {
      if (this.vertices != 0) {
         if (this.elementsToFill != 0) {
            String s = VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + s);
         }

         if (this.mode == VertexFormat.Mode.LINES) {
            long i = this.buffer.reserve(this.vertexSize);
            MemoryUtil.memCopy(i - this.vertexSize, i, this.vertexSize);
            this.vertices++;
         }
      }
   }

   private static void putRgba(long p_344481_, int p_342528_) {
      int i = ARGB.toABGR(p_342528_);
      MemoryUtil.memPutInt(p_344481_, IS_LITTLE_ENDIAN ? i : Integer.reverseBytes(i));
   }

   private static void putPackedUv(long p_344069_, int p_342894_) {
      if (IS_LITTLE_ENDIAN) {
         MemoryUtil.memPutInt(p_344069_, p_342894_);
      } else {
         MemoryUtil.memPutShort(p_344069_, (short)(p_342894_ & 65535));
         MemoryUtil.memPutShort(p_344069_ + 2L, (short)(p_342894_ >> 16 & 65535));
      }
   }

   @Override
   public VertexConsumer addVertex(float p_342038_, float p_342902_, float p_344845_) {
      long i = this.beginVertex() + this.offsetsByElement[VertexFormatElement.POSITION.id()];
      this.elementsToFill = this.initialElementsToFill;
      MemoryUtil.memPutFloat(i, p_342038_);
      MemoryUtil.memPutFloat(i + 4L, p_342902_);
      MemoryUtil.memPutFloat(i + 8L, p_344845_);
      return this;
   }

   @Override
   public VertexConsumer setColor(int p_345246_, int p_343163_, int p_342676_, int p_345202_) {
      long i = this.beginElement(VertexFormatElement.COLOR);
      if (i != -1L) {
         MemoryUtil.memPutByte(i, (byte)p_345246_);
         MemoryUtil.memPutByte(i + 1L, (byte)p_343163_);
         MemoryUtil.memPutByte(i + 2L, (byte)p_342676_);
         MemoryUtil.memPutByte(i + 3L, (byte)p_345202_);
      }

      return this;
   }

   @Override
   public VertexConsumer setColor(int p_342265_) {
      long i = this.beginElement(VertexFormatElement.COLOR);
      if (i != -1L) {
         putRgba(i, p_342265_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv(float p_344538_, float p_343862_) {
      long i = this.beginElement(VertexFormatElement.UV0);
      if (i != -1L) {
         MemoryUtil.memPutFloat(i, p_344538_);
         MemoryUtil.memPutFloat(i + 4L, p_343862_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv1(int p_345138_, int p_344474_) {
      return this.uvShort((short)p_345138_, (short)p_344474_, VertexFormatElement.UV1);
   }

   @Override
   public VertexConsumer setOverlay(int p_343250_) {
      long i = this.beginElement(VertexFormatElement.UV1);
      if (i != -1L) {
         putPackedUv(i, p_343250_);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv2(int p_343260_, int p_345129_) {
      return this.uvShort((short)p_343260_, (short)p_345129_, VertexFormatElement.UV2);
   }

   @Override
   public VertexConsumer setLight(int p_342358_) {
      long i = this.beginElement(VertexFormatElement.UV2);
      if (i != -1L) {
         putPackedUv(i, p_342358_);
      }

      return this;
   }

   private VertexConsumer uvShort(short p_342039_, short p_345222_, VertexFormatElement p_344482_) {
      long i = this.beginElement(p_344482_);
      if (i != -1L) {
         MemoryUtil.memPutShort(i, p_342039_);
         MemoryUtil.memPutShort(i + 2L, p_345222_);
      }

      return this;
   }

   @Override
   public VertexConsumer setNormal(float p_342317_, float p_342276_, float p_342607_) {
      long i = this.beginElement(VertexFormatElement.NORMAL);
      if (i != -1L) {
         MemoryUtil.memPutByte(i, normalIntValue(p_342317_));
         MemoryUtil.memPutByte(i + 1L, normalIntValue(p_342276_));
         MemoryUtil.memPutByte(i + 2L, normalIntValue(p_342607_));
      }

      return this;
   }

   @Override
   public VertexConsumer setLineWidth(float p_456513_) {
      long i = this.beginElement(VertexFormatElement.LINE_WIDTH);
      if (i != -1L) {
         MemoryUtil.memPutFloat(i, p_456513_);
      }

      return this;
   }

   private static byte normalIntValue(float p_344123_) {
      return (byte)((int)(Mth.clamp(p_344123_, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   @Override
   public void addVertex(
      float p_343280_,
      float p_344969_,
      float p_343237_,
      int p_342708_,
      float p_345023_,
      float p_344850_,
      int p_344316_,
      int p_342457_,
      float p_344002_,
      float p_344052_,
      float p_343783_
   ) {
      if (this.fastFormat) {
         long i = this.beginVertex();
         MemoryUtil.memPutFloat(i + 0L, p_343280_);
         MemoryUtil.memPutFloat(i + 4L, p_344969_);
         MemoryUtil.memPutFloat(i + 8L, p_343237_);
         putRgba(i + 12L, p_342708_);
         MemoryUtil.memPutFloat(i + 16L, p_345023_);
         MemoryUtil.memPutFloat(i + 20L, p_344850_);
         long j;
         if (this.fullFormat) {
            putPackedUv(i + 24L, p_344316_);
            j = i + 28L;
         } else {
            j = i + 24L;
         }

         putPackedUv(j + 0L, p_342457_);
         MemoryUtil.memPutByte(j + 4L, normalIntValue(p_344002_));
         MemoryUtil.memPutByte(j + 5L, normalIntValue(p_344052_));
         MemoryUtil.memPutByte(j + 6L, normalIntValue(p_343783_));
      } else {
         VertexConsumer.super.addVertex(p_343280_, p_344969_, p_343237_, p_342708_, p_345023_, p_344850_, p_344316_, p_342457_, p_344002_, p_344052_, p_343783_);
      }
   }
}
