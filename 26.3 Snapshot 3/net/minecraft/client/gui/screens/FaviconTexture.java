package net.minecraft.client.gui.screens;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class FaviconTexture implements AutoCloseable {
   private static final Identifier MISSING_LOCATION = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
   private static final int WIDTH = 64;
   private static final int HEIGHT = 64;
   private final TextureManager textureManager;
   private final Identifier textureLocation;
   private @Nullable DynamicTexture texture;
   private boolean closed;

   private FaviconTexture(final TextureManager textureManager, final Identifier textureLocation) {
      this.textureManager = textureManager;
      this.textureLocation = textureLocation;
   }

   public static FaviconTexture forWorld(final TextureManager textureManager, final String levelId) {
      return new FaviconTexture(
         textureManager,
         Identifier.withDefaultNamespace(
            "worlds/" + Util.sanitizeName(levelId, Identifier::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(levelId) + "/icon"
         )
      );
   }

   public static FaviconTexture forServer(final TextureManager textureManager, final String address) {
      return new FaviconTexture(textureManager, Identifier.withDefaultNamespace("servers/" + Hashing.sha1().hashUnencodedChars(address) + "/icon"));
   }

   public void upload(final NativeImage image) {
      if (image.getWidth() == 64 && image.getHeight() == 64) {
         try {
            this.checkOpen();
            if (this.texture == null) {
               this.texture = new DynamicTexture(() -> "Favicon " + this.textureLocation, image);
            } else {
               this.texture.setPixels(image);
               this.texture.upload();
            }

            this.textureManager.register(this.textureLocation, this.texture);
         } catch (Throwable t) {
            image.close();
            this.clear();
            throw t;
         }
      } else {
         image.close();
         throw new IllegalArgumentException("Icon must be 64x64, but was " + image.getWidth() + "x" + image.getHeight());
      }
   }

   public void clear() {
      this.checkOpen();
      if (this.texture != null) {
         this.textureManager.release(this.textureLocation);
         this.texture.close();
         this.texture = null;
      }
   }

   public Identifier textureLocation() {
      return this.texture != null ? this.textureLocation : MISSING_LOCATION;
   }

   @Override
   public void close() {
      this.clear();
      this.closed = true;
   }

   public boolean isClosed() {
      return this.closed;
   }

   private void checkOpen() {
      if (this.closed) {
         throw new IllegalStateException("Icon already closed");
      }
   }
}
