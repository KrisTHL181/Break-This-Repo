package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuBuffer implements AutoCloseable {
   public static final int USAGE_MAP_READ = 1;
   public static final int USAGE_MAP_WRITE = 2;
   public static final int USAGE_HINT_CLIENT_STORAGE = 4;
   public static final int USAGE_COPY_DST = 8;
   public static final int USAGE_COPY_SRC = 16;
   public static final int USAGE_VERTEX = 32;
   public static final int USAGE_INDEX = 64;
   public static final int USAGE_UNIFORM = 128;
   public static final int USAGE_UNIFORM_TEXEL_BUFFER = 256;
   @GpuBuffer.Usage
   private final int usage;
   private final long size;

   public GpuBuffer(@GpuBuffer.Usage int p_361832_, long p_453551_) {
      this.size = p_453551_;
      this.usage = p_361832_;
   }

   public long size() {
      return this.size;
   }

   @GpuBuffer.Usage
   public int usage() {
      return this.usage;
   }

   public abstract boolean isClosed();

   @Override
   public abstract void close();

   public GpuBufferSlice slice(long p_461084_, long p_459598_) {
      if (p_461084_ >= 0L && p_459598_ >= 0L && p_461084_ + p_459598_ <= this.size) {
         return new GpuBufferSlice(this, p_461084_, p_459598_);
      } else {
         throw new IllegalArgumentException(
            "Offset of " + p_461084_ + " and length " + p_459598_ + " would put new slice outside buffer's range (of 0," + p_459598_ + ")"
         );
      }
   }

   public GpuBufferSlice slice() {
      return new GpuBufferSlice(this, 0L, this.size);
   }

   @OnlyIn(Dist.CLIENT)
   @DontObfuscate
   public interface MappedView extends AutoCloseable {
      ByteBuffer data();

      @Override
      void close();
   }

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   @OnlyIn(Dist.CLIENT)
   public @interface Usage {
   }
}
