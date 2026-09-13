package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public record Unstitcher(Identifier resource, List<Unstitcher.Region> regions, double xDivisor, double yDivisor) implements SpriteSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<Unstitcher> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Identifier.CODEC.fieldOf("resource").forGetter(Unstitcher::resource),
            ExtraCodecs.nonEmptyList(Unstitcher.Region.CODEC.listOf()).fieldOf("regions").forGetter(Unstitcher::regions),
            Codec.DOUBLE.optionalFieldOf("divisor_x", 1.0).forGetter(Unstitcher::xDivisor),
            Codec.DOUBLE.optionalFieldOf("divisor_y", 1.0).forGetter(Unstitcher::yDivisor)
         )
         .apply(i, Unstitcher::new)
   );

   @Override
   public void run(final ResourceManager resourceManager, final SpriteSource.Output output) {
      Identifier resourceId = TEXTURE_ID_CONVERTER.idToFile(this.resource);
      Optional<Resource> resource = resourceManager.getResource(resourceId);
      if (resource.isPresent()) {
         LazyLoadedImage image = new LazyLoadedImage(resourceId, resource.get(), this.regions.size());

         for (Unstitcher.Region region : this.regions) {
            output.add(region.sprite, new Unstitcher.RegionInstance(image, region, this.xDivisor, this.yDivisor));
         }
      } else {
         LOGGER.warn("Missing sprite: {}", resourceId);
      }
   }

   @Override
   public MapCodec<Unstitcher> codec() {
      return MAP_CODEC;
   }

   public record Region(Identifier sprite, double x, double y, double width, double height) {
      public static final Codec<Unstitcher.Region> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Identifier.CODEC.fieldOf("sprite").forGetter(Unstitcher.Region::sprite),
               Codec.DOUBLE.fieldOf("x").forGetter(Unstitcher.Region::x),
               Codec.DOUBLE.fieldOf("y").forGetter(Unstitcher.Region::y),
               Codec.DOUBLE.fieldOf("width").forGetter(Unstitcher.Region::width),
               Codec.DOUBLE.fieldOf("height").forGetter(Unstitcher.Region::height)
            )
            .apply(i, Unstitcher.Region::new)
      );
   }

   private static class RegionInstance implements SpriteSource.DiscardableLoader {
      private final LazyLoadedImage image;
      private final Unstitcher.Region region;
      private final double xDivisor;
      private final double yDivisor;

      private RegionInstance(final LazyLoadedImage image, final Unstitcher.Region region, final double xDivisor, final double yDivisor) {
         this.image = image;
         this.region = region;
         this.xDivisor = xDivisor;
         this.yDivisor = yDivisor;
      }

      @Override
      public SpriteContents get(final SpriteResourceLoader loader) {
         try {
            NativeImage fullImage = this.image.get();
            double xScale = fullImage.getWidth() / this.xDivisor;
            double yScale = fullImage.getHeight() / this.yDivisor;
            int x = Mth.floor(this.region.x * xScale);
            int y = Mth.floor(this.region.y * yScale);
            int width = Mth.floor(this.region.width * xScale);
            int height = Mth.floor(this.region.height * yScale);
            NativeImage target = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            fullImage.copyRect(target, x, y, 0, 0, width, height, false, false);
            return new SpriteContents(this.region.sprite, new FrameSize(width, height), target);
         } catch (Exception e) {
            Unstitcher.LOGGER.error("Failed to unstitch region {}", this.region.sprite, e);
         } finally {
            this.image.release();
         }

         return MissingTextureAtlasSprite.create();
      }

      @Override
      public void discard() {
         this.image.release();
      }
   }
}
