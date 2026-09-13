package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TextureContents(NativeImage image, @Nullable TextureMetadataSection metadata) implements Closeable {
   public static TextureContents load(ResourceManager p_377087_, Identifier p_455898_) throws IOException {
      Resource resource = p_377087_.getResourceOrThrow(p_455898_);

      NativeImage nativeimage;
      try (InputStream inputstream = resource.open()) {
         nativeimage = NativeImage.read(inputstream);
      }

      TextureMetadataSection texturemetadatasection = resource.metadata().getSection(TextureMetadataSection.TYPE).orElse(null);
      return new TextureContents(nativeimage, texturemetadatasection);
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
