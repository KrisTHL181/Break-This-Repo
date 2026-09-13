package com.mojang.blaze3d.vertex;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Tesselator {
   private static final int MAX_BYTES = 786432;
   private final ByteBufferBuilder buffer;
   private static @Nullable Tesselator instance;

   public static void init() {
      if (instance != null) {
         throw new IllegalStateException("Tesselator has already been initialized");
      }

      instance = new Tesselator();
   }

   public static Tesselator getInstance() {
      if (instance == null) {
         throw new IllegalStateException("Tesselator has not been initialized");
      } else {
         return instance;
      }
   }

   public Tesselator(int p_85912_) {
      this.buffer = new ByteBufferBuilder(p_85912_);
   }

   public Tesselator() {
      this(786432);
   }

   public BufferBuilder begin(VertexFormat.Mode p_342351_, VertexFormat p_344902_) {
      return new BufferBuilder(this.buffer, p_342351_, p_344902_);
   }

   public void clear() {
      this.buffer.clear();
   }
}
