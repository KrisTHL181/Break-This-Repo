package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class DirectStateAccess {
   public static DirectStateAccess create(GLCapabilities p_396229_, Set<String> p_397048_, GraphicsWorkarounds p_426409_) {
      if (p_396229_.GL_ARB_direct_state_access && GlDevice.USE_GL_ARB_direct_state_access && !p_426409_.isGlOnDx12()) {
         p_397048_.add("GL_ARB_direct_state_access");
         return new DirectStateAccess.Core();
      } else {
         return new DirectStateAccess.Emulated();
      }
   }

   abstract int createBuffer();

   abstract void bufferData(int var1, long var2, @GpuBuffer.Usage int var4);

   abstract void bufferData(int var1, ByteBuffer var2, @GpuBuffer.Usage int var3);

   abstract void bufferSubData(int var1, long var2, ByteBuffer var4, @GpuBuffer.Usage int var5);

   abstract void bufferStorage(int var1, long var2, @GpuBuffer.Usage int var4);

   abstract void bufferStorage(int var1, ByteBuffer var2, @GpuBuffer.Usage int var3);

   abstract @Nullable ByteBuffer mapBufferRange(int var1, long var2, long var4, int var6, @GpuBuffer.Usage int var7);

   abstract void unmapBuffer(int var1, @GpuBuffer.Usage int var2);

   abstract int createFrameBufferObject();

   abstract void bindFrameBufferTextures(int var1, int var2, int var3, int var4, int var5);

   abstract void blitFrameBuffers(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12);

   abstract void flushMappedBufferRange(int var1, long var2, long var4, @GpuBuffer.Usage int var6);

   abstract void copyBufferSubData(int var1, int var2, long var3, long var5, long var7);

   @OnlyIn(Dist.CLIENT)
   static class Core extends DirectStateAccess {
      @Override
      int createBuffer() {
         GlStateManager.incrementTrackedBuffers();
         return ARBDirectStateAccess.glCreateBuffers();
      }

      @Override
      void bufferData(int p_406179_, long p_408610_, @GpuBuffer.Usage int p_406584_) {
         ARBDirectStateAccess.glNamedBufferData(p_406179_, p_408610_, GlConst.bufferUsageToGlEnum(p_406584_));
      }

      @Override
      void bufferData(int p_408499_, ByteBuffer p_408856_, @GpuBuffer.Usage int p_409210_) {
         ARBDirectStateAccess.glNamedBufferData(p_408499_, p_408856_, GlConst.bufferUsageToGlEnum(p_409210_));
      }

      @Override
      void bufferSubData(int p_410046_, long p_451144_, ByteBuffer p_405869_, @GpuBuffer.Usage int p_406234_) {
         ARBDirectStateAccess.glNamedBufferSubData(p_410046_, p_451144_, p_405869_);
      }

      @Override
      void bufferStorage(int p_406265_, long p_409759_, @GpuBuffer.Usage int p_409653_) {
         ARBDirectStateAccess.glNamedBufferStorage(p_406265_, p_409759_, GlConst.bufferUsageToGlFlag(p_409653_));
      }

      @Override
      void bufferStorage(int p_410353_, ByteBuffer p_407466_, @GpuBuffer.Usage int p_405931_) {
         ARBDirectStateAccess.glNamedBufferStorage(p_410353_, p_407466_, GlConst.bufferUsageToGlFlag(p_405931_));
      }

      @Override
      @Nullable ByteBuffer mapBufferRange(int p_406865_, long p_452102_, long p_452408_, int p_408097_, @GpuBuffer.Usage int p_405964_) {
         return ARBDirectStateAccess.glMapNamedBufferRange(p_406865_, p_452102_, p_452408_, p_408097_);
      }

      @Override
      void unmapBuffer(int p_406826_, int p_422933_) {
         ARBDirectStateAccess.glUnmapNamedBuffer(p_406826_);
      }

      @Override
      public int createFrameBufferObject() {
         return ARBDirectStateAccess.glCreateFramebuffers();
      }

      @Override
      public void bindFrameBufferTextures(int p_396835_, int p_394736_, int p_395996_, int p_397932_, @GpuBuffer.Usage int p_396105_) {
         ARBDirectStateAccess.glNamedFramebufferTexture(p_396835_, 36064, p_394736_, p_397932_);
         ARBDirectStateAccess.glNamedFramebufferTexture(p_396835_, 36096, p_395996_, p_397932_);
         if (p_396105_ != 0) {
            GlStateManager._glBindFramebuffer(p_396105_, p_396835_);
         }
      }

      @Override
      public void blitFrameBuffers(
         int p_395353_,
         int p_395149_,
         int p_393964_,
         int p_395294_,
         int p_395276_,
         int p_391710_,
         int p_393525_,
         int p_396971_,
         int p_392279_,
         int p_396123_,
         int p_397974_,
         int p_391707_
      ) {
         ARBDirectStateAccess.glBlitNamedFramebuffer(
            p_395353_, p_395149_, p_393964_, p_395294_, p_395276_, p_391710_, p_393525_, p_396971_, p_392279_, p_396123_, p_397974_, p_391707_
         );
      }

      @Override
      void flushMappedBufferRange(int p_406350_, long p_457475_, long p_454091_, @GpuBuffer.Usage int p_410098_) {
         ARBDirectStateAccess.glFlushMappedNamedBufferRange(p_406350_, p_457475_, p_454091_);
      }

      @Override
      void copyBufferSubData(int p_410798_, int p_410787_, long p_451329_, long p_451601_, long p_456123_) {
         ARBDirectStateAccess.glCopyNamedBufferSubData(p_410798_, p_410787_, p_451329_, p_451601_, p_456123_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Emulated extends DirectStateAccess {
      private int selectBufferBindTarget(@GpuBuffer.Usage int p_423808_) {
         if ((p_423808_ & 32) != 0) {
            return 34962;
         } else if ((p_423808_ & 64) != 0) {
            return 34963;
         } else {
            return (p_423808_ & 128) != 0 ? 35345 : 36663;
         }
      }

      @Override
      int createBuffer() {
         return GlStateManager._glGenBuffers();
      }

      @Override
      void bufferData(int p_405846_, long p_409202_, @GpuBuffer.Usage int p_407040_) {
         int i = this.selectBufferBindTarget(p_407040_);
         GlStateManager._glBindBuffer(i, p_405846_);
         GlStateManager._glBufferData(i, p_409202_, GlConst.bufferUsageToGlEnum(p_407040_));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferData(int p_409142_, ByteBuffer p_410122_, @GpuBuffer.Usage int p_407720_) {
         int i = this.selectBufferBindTarget(p_407720_);
         GlStateManager._glBindBuffer(i, p_409142_);
         GlStateManager._glBufferData(i, p_410122_, GlConst.bufferUsageToGlEnum(p_407720_));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferSubData(int p_409504_, long p_451504_, ByteBuffer p_406903_, @GpuBuffer.Usage int p_410589_) {
         int i = this.selectBufferBindTarget(p_410589_);
         GlStateManager._glBindBuffer(i, p_409504_);
         GlStateManager._glBufferSubData(i, p_451504_, p_406903_);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferStorage(int p_407779_, long p_407640_, @GpuBuffer.Usage int p_407863_) {
         int i = this.selectBufferBindTarget(p_407863_);
         GlStateManager._glBindBuffer(i, p_407779_);
         ARBBufferStorage.glBufferStorage(i, p_407640_, GlConst.bufferUsageToGlFlag(p_407863_));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferStorage(int p_406307_, ByteBuffer p_407965_, @GpuBuffer.Usage int p_407922_) {
         int i = this.selectBufferBindTarget(p_407922_);
         GlStateManager._glBindBuffer(i, p_406307_);
         ARBBufferStorage.glBufferStorage(i, p_407965_, GlConst.bufferUsageToGlFlag(p_407922_));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      @Nullable ByteBuffer mapBufferRange(int p_408912_, long p_451693_, long p_453585_, int p_407523_, @GpuBuffer.Usage int p_406540_) {
         int i = this.selectBufferBindTarget(p_406540_);
         GlStateManager._glBindBuffer(i, p_408912_);
         ByteBuffer bytebuffer = GlStateManager._glMapBufferRange(i, p_451693_, p_453585_, p_407523_);
         GlStateManager._glBindBuffer(i, 0);
         return bytebuffer;
      }

      @Override
      void unmapBuffer(int p_409724_, @GpuBuffer.Usage int p_431758_) {
         int i = this.selectBufferBindTarget(p_431758_);
         GlStateManager._glBindBuffer(i, p_409724_);
         GlStateManager._glUnmapBuffer(i);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void flushMappedBufferRange(int p_407800_, long p_458525_, long p_459922_, @GpuBuffer.Usage int p_406734_) {
         int i = this.selectBufferBindTarget(p_406734_);
         GlStateManager._glBindBuffer(i, p_407800_);
         GL30.glFlushMappedBufferRange(i, p_458525_, p_459922_);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void copyBufferSubData(int p_410808_, int p_410801_, long p_453548_, long p_455870_, long p_453205_) {
         GlStateManager._glBindBuffer(36662, p_410808_);
         GlStateManager._glBindBuffer(36663, p_410801_);
         GL31.glCopyBufferSubData(36662, 36663, p_453548_, p_455870_, p_453205_);
         GlStateManager._glBindBuffer(36662, 0);
         GlStateManager._glBindBuffer(36663, 0);
      }

      @Override
      public int createFrameBufferObject() {
         return GlStateManager.glGenFramebuffers();
      }

      @Override
      public void bindFrameBufferTextures(int p_397405_, int p_395460_, int p_393875_, int p_393114_, int p_397703_) {
         int i = p_397703_ == 0 ? 36009 : p_397703_;
         int j = GlStateManager.getFrameBuffer(i);
         GlStateManager._glBindFramebuffer(i, p_397405_);
         GlStateManager._glFramebufferTexture2D(i, 36064, 3553, p_395460_, p_393114_);
         GlStateManager._glFramebufferTexture2D(i, 36096, 3553, p_393875_, p_393114_);
         if (p_397703_ == 0) {
            GlStateManager._glBindFramebuffer(i, j);
         }
      }

      @Override
      public void blitFrameBuffers(
         int p_396366_,
         int p_393343_,
         int p_397226_,
         int p_396156_,
         int p_397178_,
         int p_396414_,
         int p_397943_,
         int p_396165_,
         int p_394958_,
         int p_393756_,
         int p_393868_,
         int p_394611_
      ) {
         int i = GlStateManager.getFrameBuffer(36008);
         int j = GlStateManager.getFrameBuffer(36009);
         GlStateManager._glBindFramebuffer(36008, p_396366_);
         GlStateManager._glBindFramebuffer(36009, p_393343_);
         GlStateManager._glBlitFrameBuffer(p_397226_, p_396156_, p_397178_, p_396414_, p_397943_, p_396165_, p_394958_, p_393756_, p_393868_, p_394611_);
         GlStateManager._glBindFramebuffer(36008, i);
         GlStateManager._glBindFramebuffer(36009, j);
      }
   }
}
