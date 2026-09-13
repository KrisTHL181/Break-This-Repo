package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public abstract class BufferStorage {
   public static BufferStorage create(GLCapabilities p_408913_, Set<String> p_406550_) {
      if (p_408913_.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
         p_406550_.add("GL_ARB_buffer_storage");
         return new BufferStorage.Immutable();
      } else {
         return new BufferStorage.Mutable();
      }
   }

   public abstract GlBuffer createBuffer(DirectStateAccess var1, @Nullable Supplier<String> var2, @GpuBuffer.Usage int var3, long var4);

   public abstract GlBuffer createBuffer(DirectStateAccess var1, @Nullable Supplier<String> var2, @GpuBuffer.Usage int var3, ByteBuffer var4);

   public abstract GlBuffer.GlMappedView mapBuffer(DirectStateAccess var1, GlBuffer var2, long var3, long var5, int var7);

   @OnlyIn(Dist.CLIENT)
   static class Immutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(DirectStateAccess p_407964_, @Nullable Supplier<String> p_407920_, @GpuBuffer.Usage int p_408114_, long p_450165_) {
         int i = p_407964_.createBuffer();
         p_407964_.bufferStorage(i, p_450165_, p_408114_);
         ByteBuffer bytebuffer = this.tryMapBufferPersistent(p_407964_, p_408114_, i, p_450165_);
         return new GlBuffer(p_407920_, p_407964_, p_408114_, p_450165_, i, bytebuffer);
      }

      @Override
      public GlBuffer createBuffer(DirectStateAccess p_410114_, @Nullable Supplier<String> p_410547_, @GpuBuffer.Usage int p_410484_, ByteBuffer p_408084_) {
         int i = p_410114_.createBuffer();
         int j = p_408084_.remaining();
         p_410114_.bufferStorage(i, p_408084_, p_410484_);
         ByteBuffer bytebuffer = this.tryMapBufferPersistent(p_410114_, p_410484_, i, j);
         return new GlBuffer(p_410547_, p_410114_, p_410484_, j, i, bytebuffer);
      }

      private @Nullable ByteBuffer tryMapBufferPersistent(DirectStateAccess p_409012_, @GpuBuffer.Usage int p_407006_, int p_408347_, long p_458873_) {
         int i = 0;
         if ((p_407006_ & 1) != 0) {
            i |= 1;
         }

         if ((p_407006_ & 2) != 0) {
            i |= 18;
         }

         ByteBuffer bytebuffer;
         if (i != 0) {
            GlStateManager.clearGlErrors();
            bytebuffer = p_409012_.mapBufferRange(p_408347_, 0L, p_458873_, i | 64, p_407006_);
            if (bytebuffer == null) {
               throw new IllegalStateException("Can't persistently map buffer, opengl error " + GlStateManager._getError());
            }
         } else {
            bytebuffer = null;
         }

         return bytebuffer;
      }

      @Override
      public GlBuffer.GlMappedView mapBuffer(DirectStateAccess p_407274_, GlBuffer p_406624_, long p_459149_, long p_457379_, int p_409835_) {
         if (p_406624_.persistentBuffer == null) {
            throw new IllegalStateException("Somehow trying to map an unmappable buffer");
         } else if (p_459149_ > 2147483647L || p_457379_ > 2147483647L) {
            throw new IllegalArgumentException("Mapping buffers larger than 2GB is not supported");
         } else if (p_459149_ >= 0L && p_457379_ >= 0L) {
            return new GlBuffer.GlMappedView(() -> {
               if ((p_409835_ & 2) != 0) {
                  p_407274_.flushMappedBufferRange(p_406624_.handle, p_459149_, p_457379_, p_406624_.usage());
               }
            }, p_406624_, MemoryUtil.memSlice(p_406624_.persistentBuffer, (int)p_459149_, (int)p_457379_));
         } else {
            throw new IllegalArgumentException("Offset or length must be positive integer values");
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Mutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(DirectStateAccess p_408573_, @Nullable Supplier<String> p_408145_, @GpuBuffer.Usage int p_406482_, long p_455461_) {
         int i = p_408573_.createBuffer();
         p_408573_.bufferData(i, p_455461_, p_406482_);
         return new GlBuffer(p_408145_, p_408573_, p_406482_, p_455461_, i, null);
      }

      @Override
      public GlBuffer createBuffer(DirectStateAccess p_405969_, @Nullable Supplier<String> p_406227_, @GpuBuffer.Usage int p_409567_, ByteBuffer p_409249_) {
         int i = p_405969_.createBuffer();
         int j = p_409249_.remaining();
         p_405969_.bufferData(i, p_409249_, p_409567_);
         return new GlBuffer(p_406227_, p_405969_, p_409567_, j, i, null);
      }

      @Override
      public GlBuffer.GlMappedView mapBuffer(DirectStateAccess p_406544_, GlBuffer p_409331_, long p_457109_, long p_451916_, int p_409732_) {
         GlStateManager.clearGlErrors();
         ByteBuffer bytebuffer = p_406544_.mapBufferRange(p_409331_.handle, p_457109_, p_451916_, p_409732_, p_409331_.usage());
         if (bytebuffer == null) {
            throw new IllegalStateException("Can't map buffer, opengl error " + GlStateManager._getError());
         } else {
            return new GlBuffer.GlMappedView(() -> p_406544_.unmapBuffer(p_409331_.handle, p_409331_.usage()), p_409331_, bytebuffer);
         }
      }
   }
}
