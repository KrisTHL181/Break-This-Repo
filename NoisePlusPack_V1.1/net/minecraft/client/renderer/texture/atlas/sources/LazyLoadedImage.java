package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LazyLoadedImage {
   private final Identifier id;
   private final Resource resource;
   private final AtomicReference<@Nullable NativeImage> image = new AtomicReference<>();
   private final AtomicInteger referenceCount;

   public LazyLoadedImage(Identifier p_452137_, Resource p_266995_, int p_266778_) {
      this.id = p_452137_;
      this.resource = p_266995_;
      this.referenceCount = new AtomicInteger(p_266778_);
   }

   public NativeImage get() throws IOException {
      NativeImage nativeimage = this.image.get();
      if (nativeimage == null) {
         synchronized (this) {
            nativeimage = this.image.get();
            if (nativeimage == null) {
               try (InputStream inputstream = this.resource.open()) {
                  nativeimage = NativeImage.read(inputstream);
                  this.image.set(nativeimage);
               } catch (IOException ioexception) {
                  throw new IOException("Failed to load image " + this.id, ioexception);
               }
            }
         }
      }

      return nativeimage;
   }

   public void release() {
      int i = this.referenceCount.decrementAndGet();
      if (i <= 0) {
         NativeImage nativeimage = this.image.getAndSet(null);
         if (nativeimage != null) {
            nativeimage.close();
         }
      }
   }
}
