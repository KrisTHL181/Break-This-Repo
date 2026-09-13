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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record Unstitcher(Identifier resource, List<Unstitcher.Region> regions, double xDivisor, double yDivisor) implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<Unstitcher> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_448420_ -> p_448420_.group(
            Identifier.CODEC.fieldOf("resource").forGetter(Unstitcher::resource),
            ExtraCodecs.nonEmptyList(Unstitcher.Region.CODEC.listOf()).fieldOf("regions").forGetter(Unstitcher::regions),
            Codec.DOUBLE.optionalFieldOf("divisor_x", 1.0).forGetter(Unstitcher::xDivisor),
            Codec.DOUBLE.optionalFieldOf("divisor_y", 1.0).forGetter(Unstitcher::yDivisor)
         )
         .apply(p_448420_, Unstitcher::new)
   );

   @Override
   public void run(ResourceManager p_261498_, SpriteSource.Output p_261828_) {
      Identifier identifier = TEXTURE_ID_CONVERTER.idToFile(this.resource);
      Optional<Resource> optional = p_261498_.getResource(identifier);
      if (optional.isPresent()) {
         LazyLoadedImage lazyloadedimage = new LazyLoadedImage(identifier, optional.get(), this.regions.size());

         for (Unstitcher.Region unstitcher$region : this.regions) {
            p_261828_.add(unstitcher$region.sprite, new Unstitcher.RegionInstance(lazyloadedimage, unstitcher$region, this.xDivisor, this.yDivisor));
         }
      } else {
         LOGGER.warn("Missing sprite: {}", identifier);
      }
   }

   @Override
   public MapCodec<Unstitcher> codec() {
      return MAP_CODEC;
   }

   @OnlyIn(Dist.CLIENT)
   public record Region(Identifier sprite, double x, double y, double width, double height) {
      public static final Codec<Unstitcher.Region> CODEC = RecordCodecBuilder.create(
         p_448421_ -> p_448421_.group(
               Identifier.CODEC.fieldOf("sprite").forGetter(Unstitcher.Region::sprite),
               Codec.DOUBLE.fieldOf("x").forGetter(Unstitcher.Region::x),
               Codec.DOUBLE.fieldOf("y").forGetter(Unstitcher.Region::y),
               Codec.DOUBLE.fieldOf("width").forGetter(Unstitcher.Region::width),
               Codec.DOUBLE.fieldOf("height").forGetter(Unstitcher.Region::height)
            )
            .apply(p_448421_, Unstitcher.Region::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   static class RegionInstance implements SpriteSource.DiscardableLoader {
      private final LazyLoadedImage image;
      private final Unstitcher.Region region;
      private final double xDivisor;
      private final double yDivisor;

      RegionInstance(LazyLoadedImage p_266678_, Unstitcher.Region p_267197_, double p_266911_, double p_266789_) {
         this.image = p_266678_;
         this.region = p_267197_;
         this.xDivisor = p_266911_;
         this.yDivisor = p_266789_;
      }

      @Override
      public SpriteContents get(SpriteResourceLoader p_297928_) {
         try {
            NativeImage nativeimage = this.image.get();
            double d0 = nativeimage.getWidth() / this.xDivisor;
            double d1 = nativeimage.getHeight() / this.yDivisor;
            int i = Mth.floor(this.region.x * d0);
            int j = Mth.floor(this.region.y * d1);
            int k = Mth.floor(this.region.width * d0);
            int l = Mth.floor(this.region.height * d1);
            NativeImage nativeimage1 = new NativeImage(NativeImage.Format.RGBA, k, l, false);
            nativeimage.copyRect(nativeimage1, i, j, 0, 0, k, l, false, false);
            return new SpriteContents(this.region.sprite, new FrameSize(k, l), nativeimage1);
         } catch (Exception exception) {
            Unstitcher.LOGGER.error("Failed to unstitch region {}", this.region.sprite, exception);
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
