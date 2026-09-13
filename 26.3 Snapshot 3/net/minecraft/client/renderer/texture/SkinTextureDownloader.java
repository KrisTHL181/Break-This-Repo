package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class SkinTextureDownloader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SKIN_WIDTH = 64;
   private static final int SKIN_HEIGHT = 64;
   private static final int LEGACY_SKIN_HEIGHT = 32;
   private final Proxy proxy;
   private final TextureManager textureManager;
   private final Executor mainThreadExecutor;

   public SkinTextureDownloader(final Proxy proxy, final TextureManager textureManager, final Executor mainThreadExecutor) {
      this.proxy = proxy;
      this.textureManager = textureManager;
      this.mainThreadExecutor = mainThreadExecutor;
   }

   public CompletableFuture<ClientAsset.Texture> downloadAndRegisterSkin(
      final Identifier textureId, final Path localCopy, final String url, final boolean processLegacySkin
   ) {
      ClientAsset.DownloadedTexture texture = new ClientAsset.DownloadedTexture(textureId, url);
      return CompletableFuture.<NativeImage>supplyAsync(() -> {
         NativeImage loadedSkin;
         try {
            loadedSkin = this.downloadSkin(localCopy, texture.url());
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }

         return processLegacySkin ? processLegacySkin(loadedSkin, texture.url()) : loadedSkin;
      }, Util.nonCriticalIoPool().forName("downloadTexture")).thenCompose(fixedSkin -> this.registerTextureInManager(texture, fixedSkin));
   }

   private NativeImage downloadSkin(final Path localCopy, final String url) throws IOException {
      if (Files.isRegularFile(localCopy)) {
         LOGGER.debug("Loading HTTP texture from local cache ({})", localCopy);

         try (InputStream inputStream = Files.newInputStream(localCopy)) {
            return NativeImage.read(inputStream);
         }
      } else {
         HttpURLConnection connection = null;
         LOGGER.debug("Downloading HTTP texture from {} to {}", url, localCopy);
         URI uri = URI.create(url);

         try {
            connection = (HttpURLConnection)uri.toURL().openConnection(this.proxy);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode / 100 != 2) {
               throw new IOException("Failed to open " + uri + ", HTTP error code: " + responseCode);
            }

            byte[] imageContents = connection.getInputStream().readAllBytes();

            try {
               FileUtil.createDirectoriesSafe(localCopy.getParent());
               Files.write(localCopy, imageContents);
            } catch (IOException e) {
               LOGGER.warn("Failed to cache texture {} in {}", url, localCopy);
            }

            return NativeImage.read(imageContents);
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }
      }
   }

   private CompletableFuture<ClientAsset.Texture> registerTextureInManager(final ClientAsset.Texture textureId, final NativeImage contents) {
      return CompletableFuture.supplyAsync(() -> {
         DynamicTexture texture = new DynamicTexture(textureId.texturePath()::toString, contents);
         this.textureManager.register(textureId.texturePath(), texture);
         return textureId;
      }, this.mainThreadExecutor);
   }

   private static NativeImage processLegacySkin(NativeImage image, final String url) {
      int height = image.getHeight();
      int width = image.getWidth();
      if (width == 64 && (height == 32 || height == 64)) {
         boolean isLegacy = height == 32;
         if (isLegacy) {
            NativeImage newImage = new NativeImage(64, 64, true);
            newImage.copyFrom(image);
            image.close();
            image = newImage;
            image.fillRect(0, 32, 64, 32, 0);
            image.copyRect(4, 16, 16, 32, 4, 4, true, false);
            image.copyRect(8, 16, 16, 32, 4, 4, true, false);
            image.copyRect(0, 20, 24, 32, 4, 12, true, false);
            image.copyRect(4, 20, 16, 32, 4, 12, true, false);
            image.copyRect(8, 20, 8, 32, 4, 12, true, false);
            image.copyRect(12, 20, 16, 32, 4, 12, true, false);
            image.copyRect(44, 16, -8, 32, 4, 4, true, false);
            image.copyRect(48, 16, -8, 32, 4, 4, true, false);
            image.copyRect(40, 20, 0, 32, 4, 12, true, false);
            image.copyRect(44, 20, -8, 32, 4, 12, true, false);
            image.copyRect(48, 20, -16, 32, 4, 12, true, false);
            image.copyRect(52, 20, -8, 32, 4, 12, true, false);
         }

         setNoAlpha(image, 0, 0, 32, 16);
         if (isLegacy) {
            doNotchTransparencyHack(image, 32, 0, 64, 32);
         }

         setNoAlpha(image, 0, 16, 64, 32);
         setNoAlpha(image, 16, 48, 48, 64);
         return image;
      } else {
         image.close();
         throw new IllegalStateException("Discarding incorrectly sized (" + width + "x" + height + ") skin texture from " + url);
      }
   }

   private static void doNotchTransparencyHack(final NativeImage image, final int x0, final int y0, final int x1, final int y1) {
      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            int pix = image.getPixel(x, y);
            if (ARGB.alpha(pix) < 128) {
               return;
            }
         }
      }

      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            image.setPixel(x, y, image.getPixel(x, y) & 16777215);
         }
      }
   }

   private static void setNoAlpha(final NativeImage image, final int x0, final int y0, final int x1, final int y1) {
      for (int x = x0; x < x1; x++) {
         for (int y = y0; y < y1; y++) {
            image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
         }
      }
   }
}
