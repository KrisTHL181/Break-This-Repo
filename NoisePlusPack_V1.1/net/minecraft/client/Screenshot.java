package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String SCREENSHOT_DIR = "screenshots";

   public static void grab(File p_92290_, RenderTarget p_92293_, Consumer<Component> p_92294_) {
      grab(p_92290_, null, p_92293_, 1, p_92294_);
   }

   public static void grab(File p_92296_, @Nullable String p_92297_, RenderTarget p_92300_, int p_407783_, Consumer<Component> p_92301_) {
      takeScreenshot(
         p_92300_,
         p_407783_,
         p_447863_ -> {
            File file1 = new File(p_92296_, "screenshots");
            file1.mkdir();
            File file2;
            if (p_92297_ == null) {
               file2 = getFile(file1);
            } else {
               file2 = new File(file1, p_92297_);
            }

            Util.ioPool()
               .execute(
                  () -> {
                     try {
                        NativeImage $$4x = p_447863_;

                        try {
                           p_447863_.writeToFile(file2);
                           Component component = Component.literal(file2.getName())
                              .withStyle(ChatFormatting.UNDERLINE)
                              .withStyle(p_389149_ -> p_389149_.withClickEvent(new ClickEvent.OpenFile(file2.getAbsoluteFile())));
                           p_92301_.accept(Component.translatable("screenshot.success", component));
                        } catch (Throwable throwable1) {
                           if (p_447863_ != null) {
                              try {
                                 $$4x.close();
                              } catch (Throwable throwable) {
                                 throwable1.addSuppressed(throwable);
                              }
                           }

                           throw throwable1;
                        }

                        if (p_447863_ != null) {
                           p_447863_.close();
                        }
                     } catch (Exception exception) {
                        LOGGER.warn("Couldn't save screenshot", exception);
                        p_92301_.accept(Component.translatable("screenshot.failure", exception.getMessage()));
                     }
                  }
               );
         }
      );
   }

   public static void takeScreenshot(RenderTarget p_92282_, Consumer<NativeImage> p_391783_) {
      takeScreenshot(p_92282_, 1, p_391783_);
   }

   public static void takeScreenshot(RenderTarget p_410184_, int p_407182_, Consumer<NativeImage> p_409284_) {
      int i = p_410184_.width;
      int j = p_410184_.height;
      GpuTexture gputexture = p_410184_.getColorTexture();
      if (gputexture == null) {
         throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
      }

      if (i % p_407182_ == 0 && j % p_407182_ == 0) {
         GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)i * j * gputexture.getFormat().pixelSize());
         CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToBuffer(
               gputexture,
               gpubuffer,
               0L,
               () -> {
                  try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                     int k = j / p_407182_;
                     int l = i / p_407182_;
                     NativeImage nativeimage = new NativeImage(l, k, false);

                     for (int i1 = 0; i1 < k; i1++) {
                        for (int j1 = 0; j1 < l; j1++) {
                           if (p_407182_ == 1) {
                              int i3 = gpubuffer$mappedview.data().getInt((j1 + i1 * i) * gputexture.getFormat().pixelSize());
                              nativeimage.setPixelABGR(j1, j - i1 - 1, i3 | 0xFF000000);
                           } else {
                              int k1 = 0;
                              int l1 = 0;
                              int i2 = 0;

                              for (int j2 = 0; j2 < p_407182_; j2++) {
                                 for (int k2 = 0; k2 < p_407182_; k2++) {
                                    int l2 = gpubuffer$mappedview.data()
                                       .getInt((j1 * p_407182_ + j2 + (i1 * p_407182_ + k2) * i) * gputexture.getFormat().pixelSize());
                                    k1 += ARGB.red(l2);
                                    l1 += ARGB.green(l2);
                                    i2 += ARGB.blue(l2);
                                 }
                              }

                              int j3 = p_407182_ * p_407182_;
                              nativeimage.setPixelABGR(j1, k - i1 - 1, ARGB.color(255, k1 / j3, l1 / j3, i2 / j3));
                           }
                        }
                     }

                     p_409284_.accept(nativeimage);
                  }

                  gpubuffer.close();
               },
               0
            );
      } else {
         throw new IllegalArgumentException("Image size is not divisible by downscale factor");
      }
   }

   private static File getFile(File p_92288_) {
      String s = Util.getFilenameFormattedDateTime();
      int i = 1;

      while (true) {
         File file1 = new File(p_92288_, s + (i == 1 ? "" : "_" + i) + ".png");
         if (!file1.exists()) {
            return file1;
         }

         i++;
      }
   }
}
