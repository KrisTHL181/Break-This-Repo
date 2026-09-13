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
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_SEPARATOR = "_";
    public static final MapCodec<PalettedPermutations> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.list(Identifier.CODEC).fieldOf("textures").forGetter(PalettedPermutations::textures),
                Identifier.CODEC.fieldOf("palette_key").forGetter(PalettedPermutations::paletteKey),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).fieldOf("permutations").forGetter(PalettedPermutations::permutations),
                Codec.STRING.optionalFieldOf("separator", "_").forGetter(PalettedPermutations::separator)
            )
            .apply(i, PalettedPermutations::new)
    );

    public PalettedPermutations(final List<Identifier> textures, final Identifier paletteKey, final Map<String, Identifier> permutations) {
        this(textures, paletteKey, permutations, "_");
    }

    @Override
    public void run(final ResourceManager resourceManager, final SpriteSource.Output output) {
        Supplier<int[]> paletteKeySupplier = Suppliers.memoize(() -> loadPaletteEntryFromImage(resourceManager, this.paletteKey));
        Map<String, Supplier<IntUnaryOperator>> palettes = new HashMap<>();
        this.permutations
            .forEach(
                (suffix, palette) -> palettes.put(
                    suffix, Suppliers.memoize(() -> createPaletteMapping(paletteKeySupplier.get(), loadPaletteEntryFromImage(resourceManager, palette)))
                )
            );

        for (Identifier textureLocation : this.textures) {
            Identifier textureId = TEXTURE_ID_CONVERTER.idToFile(textureLocation);
            Optional<Resource> resource = resourceManager.getResource(textureId);
            if (resource.isEmpty()) {
                LOGGER.warn("Unable to find texture {}", textureId);
            } else {
                LazyLoadedImage baseImage = new LazyLoadedImage(textureId, resource.get(), palettes.size());

                for (Entry<String, Supplier<IntUnaryOperator>> entry : palettes.entrySet()) {
                    Identifier permutationLocation = textureLocation.withSuffix(this.separator + entry.getKey());
                    output.add(permutationLocation, new PalettedPermutations.PalettedSpriteSupplier(baseImage, entry.getValue(), permutationLocation));
                }
            }
        }
    }

    private static IntUnaryOperator createPaletteMapping(final int[] keys, final int[] values) {
        if (values.length != keys.length) {
            LOGGER.warn("Palette mapping has different sizes: {} and {}", keys.length, values.length);
            throw new IllegalArgumentException();
        }

        Int2IntMap palette = new Int2IntOpenHashMap(values.length);

        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            if (ARGB.alpha(key) != 0) {
                palette.put(ARGB.transparent(key), values[i]);
            }
        }

        return pixel -> {
            int pixelAlpha = ARGB.alpha(pixel);
            if (pixelAlpha == 0) {
                return pixel;
            }

            int pixelRGB = ARGB.transparent(pixel);
            int value = palette.getOrDefault(pixelRGB, ARGB.opaque(pixelRGB));
            int valueAlpha = ARGB.alpha(value);
            return ARGB.color(pixelAlpha * valueAlpha / 255, value);
        };
    }

    private static int[] loadPaletteEntryFromImage(final ResourceManager resourceManager, final Identifier location) {
        Optional<Resource> resource = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(location));
        if (resource.isEmpty()) {
            LOGGER.error("Failed to load palette image {}", location);
            throw new IllegalArgumentException();
        }

        try (
            InputStream is = resource.get().open();
            NativeImage image = NativeImage.read(is);
        ) {
            return image.getPixels();
        } catch (Exception exception) {
            LOGGER.error("Couldn't load texture {}", location, exception);
            throw new IllegalArgumentException();
        }
    }

    @Override
    public MapCodec<PalettedPermutations> codec() {
        return MAP_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    private record PalettedSpriteSupplier(LazyLoadedImage baseImage, Supplier<IntUnaryOperator> palette, Identifier permutationLocation)
        implements SpriteSource.DiscardableLoader {
        @Override
        public @Nullable SpriteContents get(final SpriteResourceLoader loader) {
            try {
                NativeImage image = this.baseImage.get().mappedCopy(this.palette.get());
                return new SpriteContents(this.permutationLocation, new FrameSize(image.getWidth(), image.getHeight()), image);
            } catch (IOException | IllegalArgumentException e) {
                PalettedPermutations.LOGGER.error("unable to apply palette to {}", this.permutationLocation, e);
                return null;
            } finally {
                this.baseImage.release();
            }
        }

        @Override
        public void discard() {
            this.baseImage.release();
        }
    }
}