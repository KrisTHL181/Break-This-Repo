package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;
   private static final int[][] DIRECTIONS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   public static ByteBuffer readResource(InputStream p_85304_) throws IOException {
      ReadableByteChannel readablebytechannel = Channels.newChannel(p_85304_);
      return readablebytechannel instanceof SeekableByteChannel seekablebytechannel
         ? readResource(readablebytechannel, (int)seekablebytechannel.size() + 1)
         : readResource(readablebytechannel, 8192);
   }

   private static ByteBuffer readResource(ReadableByteChannel p_273208_, int p_273297_) throws IOException {
      ByteBuffer bytebuffer = MemoryUtil.memAlloc(p_273297_);

      try {
         while (p_273208_.read(bytebuffer) != -1) {
            if (!bytebuffer.hasRemaining()) {
               bytebuffer = MemoryUtil.memRealloc(bytebuffer, bytebuffer.capacity() * 2);
            }
         }

         bytebuffer.flip();
         return bytebuffer;
      } catch (IOException ioexception) {
         MemoryUtil.memFree(bytebuffer);
         throw ioexception;
      }
   }

   public static void writeAsPNG(Path p_285286_, String p_285408_, GpuTexture p_396086_, int p_285400_, IntUnaryOperator p_284988_) {
      RenderSystem.assertOnRenderThread();
      long i = 0L;

      for (int j = 0; j <= p_285400_; j++) {
         i += (long)p_396086_.getFormat().pixelSize() * p_396086_.getWidth(j) * p_396086_.getHeight(j);
      }

      if (i > 2147483647L) {
         throw new IllegalArgumentException("Exporting textures larger than 2GB is not supported");
      }

      GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, i);
      CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
      Runnable runnable = () -> {
         try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
            int i1 = 0;

            for (int j1 = 0; j1 <= p_285400_; j1++) {
               int k1 = p_396086_.getWidth(j1);
               int l1 = p_396086_.getHeight(j1);

               try (NativeImage nativeimage = new NativeImage(k1, l1, false)) {
                  for (int i2 = 0; i2 < l1; i2++) {
                     for (int j2 = 0; j2 < k1; j2++) {
                        int k2 = gpubuffer$mappedview.data().getInt(i1 + (j2 + i2 * k1) * p_396086_.getFormat().pixelSize());
                        nativeimage.setPixelABGR(j2, i2, p_284988_.applyAsInt(k2));
                     }
                  }

                  Path path = p_285286_.resolve(p_285408_ + "_" + j1 + ".png");
                  nativeimage.writeToFile(path);
                  LOGGER.debug("Exported png to: {}", path.toAbsolutePath());
               } catch (IOException ioexception) {
                  LOGGER.debug("Unable to write: ", ioexception);
               }

               i1 += p_396086_.getFormat().pixelSize() * k1 * l1;
            }
         }

         gpubuffer.close();
      };
      AtomicInteger atomicinteger = new AtomicInteger();
      int k = 0;

      for (int l = 0; l <= p_285400_; l++) {
         commandencoder.copyTextureToBuffer(p_396086_, gpubuffer, k, () -> {
            if (atomicinteger.getAndIncrement() == p_285400_) {
               runnable.run();
            }
         }, l);
         k += p_396086_.getFormat().pixelSize() * p_396086_.getWidth(l) * p_396086_.getHeight(l);
      }
   }

   public static Path getDebugTexturePath(Path p_262015_) {
      return p_262015_.resolve("screenshots").resolve("debug");
   }

   public static Path getDebugTexturePath() {
      return getDebugTexturePath(Path.of("."));
   }

   public static void solidify(NativeImage p_452830_) {
      int i = p_452830_.getWidth();
      int j = p_452830_.getHeight();
      int[] aint = new int[i * j];
      int[] aint1 = new int[i * j];
      Arrays.fill(aint1, Integer.MAX_VALUE);
      IntArrayFIFOQueue intarrayfifoqueue = new IntArrayFIFOQueue();

      for (int k = 0; k < i; k++) {
         for (int l = 0; l < j; l++) {
            int i1 = p_452830_.getPixel(k, l);
            if (ARGB.alpha(i1) != 0) {
               int j1 = pack(k, l, i);
               aint1[j1] = 0;
               aint[j1] = i1;
               intarrayfifoqueue.enqueue(j1);
            }
         }
      }

      while (!intarrayfifoqueue.isEmpty()) {
         int j2 = intarrayfifoqueue.dequeueInt();
         int l2 = x(j2, i);
         int j3 = y(j2, i);

         for (int[] aint2 : DIRECTIONS) {
            int k1 = l2 + aint2[0];
            int l1 = j3 + aint2[1];
            int i2 = pack(k1, l1, i);
            if (k1 >= 0 && l1 >= 0 && k1 < i && l1 < j && aint1[i2] > aint1[j2] + 1) {
               aint1[i2] = aint1[j2] + 1;
               aint[i2] = aint[j2];
               intarrayfifoqueue.enqueue(i2);
            }
         }
      }

      for (int k2 = 0; k2 < i; k2++) {
         for (int i3 = 0; i3 < j; i3++) {
            int k3 = p_452830_.getPixel(k2, i3);
            if (ARGB.alpha(k3) == 0) {
               p_452830_.setPixel(k2, i3, ARGB.color(0, aint[pack(k2, i3, i)]));
            } else {
               p_452830_.setPixel(k2, i3, k3);
            }
         }
      }
   }

   public static void fillEmptyAreasWithDarkColor(NativeImage p_458408_) {
      int i = p_458408_.getWidth();
      int j = p_458408_.getHeight();
      int k = -1;
      int l = Integer.MAX_VALUE;

      for (int i1 = 0; i1 < i; i1++) {
         for (int j1 = 0; j1 < j; j1++) {
            int k1 = p_458408_.getPixel(i1, j1);
            int l1 = ARGB.alpha(k1);
            if (l1 != 0) {
               int i2 = ARGB.red(k1);
               int j2 = ARGB.green(k1);
               int k2 = ARGB.blue(k1);
               int l2 = i2 + j2 + k2;
               if (l2 < l) {
                  l = l2;
                  k = k1;
               }
            }
         }
      }

      int i3 = 3 * ARGB.red(k) / 4;
      int j3 = 3 * ARGB.green(k) / 4;
      int k3 = 3 * ARGB.blue(k) / 4;
      int l3 = ARGB.color(0, i3, j3, k3);

      for (int i4 = 0; i4 < i; i4++) {
         for (int j4 = 0; j4 < j; j4++) {
            int k4 = p_458408_.getPixel(i4, j4);
            if (ARGB.alpha(k4) == 0) {
               p_458408_.setPixel(i4, j4, l3);
            }
         }
      }
   }

   private static int pack(int p_451946_, int p_451637_, int p_456925_) {
      return p_451946_ + p_451637_ * p_456925_;
   }

   private static int x(int p_459521_, int p_458625_) {
      return p_459521_ % p_458625_;
   }

   private static int y(int p_453835_, int p_458830_) {
      return p_453835_ / p_458830_;
   }
}
