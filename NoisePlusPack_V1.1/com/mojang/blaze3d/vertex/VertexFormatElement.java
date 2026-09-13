package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.DontObfuscate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public record VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
   public static final int MAX_COUNT = 32;
   private static final @Nullable VertexFormatElement[] BY_ID = new VertexFormatElement[32];
   private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(32);
   public static final VertexFormatElement POSITION = register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
   public static final VertexFormatElement COLOR = register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
   public static final VertexFormatElement UV0 = register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
   public static final VertexFormatElement UV = UV0;
   public static final VertexFormatElement UV1 = register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
   public static final VertexFormatElement UV2 = register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
   public static final VertexFormatElement NORMAL = register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);
   public static final VertexFormatElement LINE_WIDTH = register(6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);

   public VertexFormatElement {
      if (id < 0 || id >= BY_ID.length) {
         throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
      }

      if (!this.supportsUsage(index, usage)) {
         throw new IllegalStateException("Multiple vertex elements of the same type other than UVs are not supported");
      }
   }

   public static VertexFormatElement register(
      int p_343820_, int p_343175_, VertexFormatElement.Type p_342455_, VertexFormatElement.Usage p_344304_, int p_343812_
   ) {
      VertexFormatElement vertexformatelement = new VertexFormatElement(p_343820_, p_343175_, p_342455_, p_344304_, p_343812_);
      if (BY_ID[p_343820_] != null) {
         throw new IllegalArgumentException("Duplicate element registration for: " + p_343820_);
      }

      BY_ID[p_343820_] = vertexformatelement;
      ELEMENTS.add(vertexformatelement);
      return vertexformatelement;
   }

   private boolean supportsUsage(int p_86043_, VertexFormatElement.Usage p_86044_) {
      return p_86043_ == 0 || p_86044_ == VertexFormatElement.Usage.UV;
   }

   @Override
   public String toString() {
      return this.count + "," + this.usage + "," + this.type + " (" + this.id + ")";
   }

   public int mask() {
      return 1 << this.id;
   }

   public int byteSize() {
      return this.type.size() * this.count;
   }

   public static @Nullable VertexFormatElement byId(int p_343405_) {
      return BY_ID[p_343405_];
   }

   public static Stream<VertexFormatElement> elementsFromMask(int p_344546_) {
      return ELEMENTS.stream().filter(p_447710_ -> (p_344546_ & p_447710_.mask()) != 0);
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public enum Type {
      FLOAT(4, "Float"),
      UBYTE(1, "Unsigned Byte"),
      BYTE(1, "Byte"),
      USHORT(2, "Unsigned Short"),
      SHORT(2, "Short"),
      UINT(4, "Unsigned Int"),
      INT(4, "Int");

      private final int size;
      private final String name;

      Type(final int p_86071_, final String p_86072_) {
         this.size = p_86071_;
         this.name = p_86072_;
      }

      public int size() {
         return this.size;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public enum Usage {
      POSITION("Position"),
      NORMAL("Normal"),
      COLOR("Vertex Color"),
      UV("UV"),
      GENERIC("Generic");

      private final String name;

      Usage(final String p_166975_) {
         this.name = p_166975_;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }
}
