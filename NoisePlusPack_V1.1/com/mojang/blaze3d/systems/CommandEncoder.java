package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface CommandEncoder {
   RenderPass createRenderPass(Supplier<String> var1, GpuTextureView var2, OptionalInt var3);

   RenderPass createRenderPass(Supplier<String> var1, GpuTextureView var2, OptionalInt var3, @Nullable GpuTextureView var4, OptionalDouble var5);

   void clearColorTexture(GpuTexture var1, int var2);

   void clearColorAndDepthTextures(GpuTexture var1, int var2, GpuTexture var3, double var4);

   void clearColorAndDepthTextures(GpuTexture var1, int var2, GpuTexture var3, double var4, int var6, int var7, int var8, int var9);

   void clearDepthTexture(GpuTexture var1, double var2);

   void writeToBuffer(GpuBufferSlice var1, ByteBuffer var2);

   GpuBuffer.MappedView mapBuffer(GpuBuffer var1, boolean var2, boolean var3);

   GpuBuffer.MappedView mapBuffer(GpuBufferSlice var1, boolean var2, boolean var3);

   void copyToBuffer(GpuBufferSlice var1, GpuBufferSlice var2);

   void writeToTexture(GpuTexture var1, NativeImage var2);

   void writeToTexture(GpuTexture var1, NativeImage var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10);

   void writeToTexture(GpuTexture var1, ByteBuffer var2, NativeImage.Format var3, int var4, int var5, int var6, int var7, int var8, int var9);

   void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6);

   void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6, int var7, int var8, int var9, int var10);

   void copyTextureToTexture(GpuTexture var1, GpuTexture var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9);

   void presentTexture(GpuTextureView var1);

   GpuFence createFence();

   GpuQuery timerQueryBegin();

   void timerQueryEnd(GpuQuery var1);
}
