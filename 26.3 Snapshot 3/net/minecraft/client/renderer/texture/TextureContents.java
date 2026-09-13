package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

public record TextureContents(NativeImage image, @Nullable TextureMetadataSection metadata) implements Closeable {
   public static TextureContents load(final ResourceManager resourceManager, final Identifier location) throws IOException {
      Resource resource = resourceManager.getResourceOrThrow(location);

      NativeImage image;
      try (InputStream is = resource.open()) {
         image = NativeImage.read(is);
      }

      TextureMetadataSection metadata = resource.metadata().getSection(TextureMetadataSection.TYPE).orElse(null);
      return new TextureContents(image, metadata);
   }

   public static TextureContents createMissing() {
      return new TextureContents(MissingTextureAtlasSprite.generateMissingImage(), null);
   }

   public boolean blur() {
      return this.metadata != null ? this.metadata.blur() : false;
   }

   public boolean clamp() {
      return this.metadata != null ? this.metadata.clamp() : false;
   }

   @Override
   public void close() {
      this.image.close();
   }
}
