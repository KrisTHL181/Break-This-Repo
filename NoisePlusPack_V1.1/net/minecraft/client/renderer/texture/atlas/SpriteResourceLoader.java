package net.minecraft.client.renderer.texture.atlas;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SpriteResourceLoader {
   Logger LOGGER = LogUtils.getLogger();

   static SpriteResourceLoader create(Set<MetadataSectionType<?>> p_423718_) {
      return (p_448404_, p_448405_) -> {
         Optional<AnimationMetadataSection> optional;
         Optional<TextureMetadataSection> optional1;
         List<MetadataSectionType.WithValue<?>> list;
         try {
            ResourceMetadata resourcemetadata = p_448405_.metadata();
            optional = resourcemetadata.getSection(AnimationMetadataSection.TYPE);
            optional1 = resourcemetadata.getSection(TextureMetadataSection.TYPE);
            list = resourcemetadata.getTypedSections(p_423718_);
         } catch (Exception exception) {
            LOGGER.error("Unable to parse metadata from {}", p_448404_, exception);
            return null;
         }

         NativeImage nativeimage;
         try (InputStream inputstream = p_448405_.open()) {
            nativeimage = NativeImage.read(inputstream);
         } catch (IOException ioexception) {
            LOGGER.error("Using missing texture, unable to load {}", p_448404_, ioexception);
            return null;
         }

         FrameSize framesize;
         if (optional.isPresent()) {
            framesize = optional.get().calculateFrameSize(nativeimage.getWidth(), nativeimage.getHeight());
            if (!Mth.isMultipleOf(nativeimage.getWidth(), framesize.width()) || !Mth.isMultipleOf(nativeimage.getHeight(), framesize.height())) {
               LOGGER.error(
                  "Image {} size {},{} is not multiple of frame size {},{}",
                  new Object[]{p_448404_, nativeimage.getWidth(), nativeimage.getHeight(), framesize.width(), framesize.height()}
               );
               nativeimage.close();
               return null;
            }
         } else {
            framesize = new FrameSize(nativeimage.getWidth(), nativeimage.getHeight());
         }

         return new SpriteContents(p_448404_, framesize, nativeimage, optional, list, optional1);
      };
   }

   @Nullable SpriteContents loadSprite(Identifier var1, Resource var2);
}
