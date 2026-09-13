package net.minecraft.client.renderer.texture.atlas.sources;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record PalettedPermutations(List<Identifier> textures, Identifier paletteKey, Map<String, Identifier> permutations, String separator)
   implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final String DEFAULT_SEPARATOR = "_";
   public static final MapCodec<PalettedPermutations> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_448416_ -> p_448416_.group(
            Codec.list(Identifier.CODEC).fieldOf("textures").forGetter(PalettedPermutations::textures),
            Identifier.CODEC.fieldOf("palette_key").forGetter(PalettedPermutations::paletteKey),
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC).fieldOf("permutations").forGetter(PalettedPermutations::permutations),
            Codec.STRING.optionalFieldOf("separator", "_").forGetter(PalettedPermutations::separator)
         )
         .apply(p_448416_, PalettedPermutations::new)
   );

   public PalettedPermutations(List<Identifier> p_393197_, Identifier p_451182_, Map<String, Identifier> p_396863_) {
      this(p_393197_, p_451182_, p_396863_, "_");
   }

   @Override
   public void run(ResourceManager p_267219_, SpriteSource.Output p_267250_) {
      Supplier<int[]> supplier = Suppliers.memoize(() -> loadPaletteEntryFromImage(p_267219_, this.paletteKey));
      Map<String, Supplier<IntUnaryOperator>> map = new HashMap<>();
      this.permutations
         .forEach(
            (p_267108_, p_450829_) -> map.put(
               p_267108_, Suppliers.memoize(() -> createPaletteMapping(supplier.get(), loadPaletteEntryFromImage(p_267219_, p_450829_)))
            )
         );

      for (Identifier identifier : this.textures) {
         Identifier identifier1 = TEXTURE_ID_CONVERTER.idToFile(identifier);
         Optional<Resource> optional = p_267219_.getResource(identifier1);
         if (optional.isEmpty()) {
            LOGGER.warn("Unable to find texture {}", identifier1);
         } else {
            LazyLoadedImage lazyloadedimage = new LazyLoadedImage(identifier1, optional.get(), map.size());

            for (Entry<String, Supplier<IntUnaryOperator>> entry : map.entrySet()) {
               Identifier identifier2 = identifier.withSuffix(this.separator + entry.getKey());
               p_267250_.add(identifier2, new PalettedPermutations.PalettedSpriteSupplier(lazyloadedimage, entry.getValue(), identifier2));
            }
         }
      }
   }

   private static IntUnaryOperator createPaletteMapping(int[] p_266839_, int[] p_266776_) {
      if (p_266776_.length != p_266839_.length) {
         LOGGER.warn("Palette mapping has different sizes: {} and {}", p_266839_.length, p_266776_.length);
         throw new IllegalArgumentException();
      }

      Int2IntMap int2intmap = new Int2IntOpenHashMap(p_266776_.length);

      for (int i = 0; i < p_266839_.length; i++) {
         int j = p_266839_[i];
         if (ARGB.alpha(j) != 0) {
            int2intmap.put(ARGB.transparent(j), p_266776_[i]);
         }
      }

      return p_358029_ -> {
         int k = ARGB.alpha(p_358029_);
         if (k == 0) {
            return p_358029_;
         }

         int l = ARGB.transparent(p_358029_);
         int i1 = int2intmap.getOrDefault(l, ARGB.opaque(l));
         int j1 = ARGB.alpha(i1);
         return ARGB.color(k * j1 / 255, i1);
      };
   }

   private static int[] loadPaletteEntryFromImage(ResourceManager p_267184_, Identifier p_458273_) {
      Optional<Resource> optional = p_267184_.getResource(TEXTURE_ID_CONVERTER.idToFile(p_458273_));
      if (optional.isEmpty()) {
         LOGGER.error("Failed to load palette image {}", p_458273_);
         throw new IllegalArgumentException();
      }

      try (
         InputStream inputstream = optional.get().open();
         NativeImage nativeimage = NativeImage.read(inputstream);
      ) {
         return nativeimage.getPixels();
      } catch (Exception exception) {
         LOGGER.error("Couldn't load texture {}", p_458273_, exception);
         throw new IllegalArgumentException();
      }
   }

   @Override
   public MapCodec<PalettedPermutations> codec() {
      return MAP_CODEC;
   }

   @OnlyIn(Dist.CLIENT)
   record PalettedSpriteSupplier(LazyLoadedImage baseImage, Supplier<IntUnaryOperator> palette, Identifier permutationLocation)
      implements SpriteSource.DiscardableLoader {
      @Override
      public @Nullable SpriteContents get(SpriteResourceLoader p_300667_) {
         Object object;
         try {
            NativeImage nativeimage = this.baseImage.get().mappedCopy(this.palette.get());
            return new SpriteContents(this.permutationLocation, new FrameSize(nativeimage.getWidth(), nativeimage.getHeight()), nativeimage);
         } catch (IllegalArgumentException | IOException ioexception) {
            PalettedPermutations.LOGGER.error("unable to apply palette to {}", this.permutationLocation, ioexception);
            object = null;
         } finally {
            this.baseImage.release();
         }

         return (SpriteContents)object;
      }

      @Override
      public void discard() {
         this.baseImage.release();
      }
   }
}
