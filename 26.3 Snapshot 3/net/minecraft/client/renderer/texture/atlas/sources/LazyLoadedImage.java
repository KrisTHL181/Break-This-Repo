package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

public class LazyLoadedImage {
   private final Identifier id;
   private final Resource resource;
   private final AtomicReference<@Nullable NativeImage> image = new AtomicReference<>();
   private final AtomicInteger referenceCount;

   public LazyLoadedImage(final Identifier id, final Resource resource, final int count) {
      this.id = id;
      this.resource = resource;
      this.referenceCount = new AtomicInteger(count);
   }

   public NativeImage get() throws IOException {
      NativeImage nativeImage = this.image.get();
      if (nativeImage == null) {
         synchronized (this) {
            nativeImage = this.image.get();
            if (nativeImage == null) {
               try (InputStream stream = this.resource.open()) {
                  nativeImage = NativeImage.read(stream);
                  this.image.set(nativeImage);
               } catch (IOException e) {
                  throw new IOException("Failed to load image " + this.id, e);
               }
            }
         }
      }

      return nativeImage;
   }

   public void release() {
      int references = this.referenceCount.decrementAndGet();
      if (references <= 0) {
         NativeImage nativeImage = this.image.getAndSet(null);
         if (nativeImage != null) {
            nativeImage.close();
         }
      }
   }
}
