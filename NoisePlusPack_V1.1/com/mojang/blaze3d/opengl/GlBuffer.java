package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlBuffer extends GpuBuffer {
   protected static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
   protected boolean closed;
   protected final @Nullable Supplier<String> label;
   private final DirectStateAccess dsa;
   protected final int handle;
   protected @Nullable ByteBuffer persistentBuffer;

   protected GlBuffer(
      @Nullable Supplier<String> p_394612_,
      DirectStateAccess p_407552_,
      @GpuBuffer.Usage int p_395014_,
      long p_451380_,
      int p_395070_,
      @Nullable ByteBuffer p_408413_
   ) {
      super(p_395014_, p_451380_);
      this.label = p_394612_;
      this.dsa = p_407552_;
      this.handle = p_395070_;
      this.persistentBuffer = p_408413_;
      int i = (int)Math.min(p_451380_, 2147483647L);
      MEMORY_POOl.malloc(p_395070_, i);
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         if (this.persistentBuffer != null) {
            this.dsa.unmapBuffer(this.handle, this.usage());
            this.persistentBuffer = null;
         }

         GlStateManager._glDeleteBuffers(this.handle);
         MEMORY_POOl.free(this.handle);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GlMappedView implements GpuBuffer.MappedView {
      private final Runnable unmap;
      private final GlBuffer buffer;
      private final ByteBuffer data;
      private boolean closed;

      protected GlMappedView(Runnable p_410033_, GlBuffer p_409269_, ByteBuffer p_408733_) {
         this.unmap = p_410033_;
         this.buffer = p_409269_;
         this.data = p_408733_;
      }

      @Override
      public ByteBuffer data() {
         return this.data;
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            this.unmap.run();
         }
      }
   }
}
