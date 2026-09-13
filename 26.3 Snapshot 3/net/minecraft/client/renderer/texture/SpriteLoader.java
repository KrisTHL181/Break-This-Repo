package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SpriteLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier location;
   private final int maxSupportedTextureSize;

   public SpriteLoader(final Identifier location, final int maxSupportedTextureSize) {
      this.location = location;
      this.maxSupportedTextureSize = maxSupportedTextureSize;
   }

   public static SpriteLoader create(final TextureAtlas atlas) {
      return new SpriteLoader(atlas.location(), atlas.maxSupportedTextureSize());
   }

   private SpriteLoader.Preparations stitch(final List<SpriteContents> sprites, final int maxMipmapLevels, final Executor executor) {
      try (Zone ignored = Profiler.get().zone(() -> "stitch " + this.location)) {
         int maxTextureSize = this.maxSupportedTextureSize;
         int minTexelSize = Integer.MAX_VALUE;
         int lowestOneBit = 1 << maxMipmapLevels;

         for (SpriteContents spriteInfo : sprites) {
            minTexelSize = Math.min(minTexelSize, Math.min(spriteInfo.width(), spriteInfo.height()));
            int lowestTextureBit = Math.min(Integer.lowestOneBit(spriteInfo.width()), Integer.lowestOneBit(spriteInfo.height()));
            if (lowestTextureBit < lowestOneBit) {
               LOGGER.warn(
                  "Texture {} with size {}x{} limits mip level from {} to {}",
                  new Object[]{spriteInfo.name(), spriteInfo.width(), spriteInfo.height(), Mth.log2(lowestOneBit), Mth.log2(lowestTextureBit)}
               );
               lowestOneBit = lowestTextureBit;
            }
         }

         int minSize = Math.min(minTexelSize, lowestOneBit);
         int minPowerOfTwo = Mth.log2(minSize);
         int mipLevel;
         if (minPowerOfTwo < maxMipmapLevels) {
            LOGGER.warn(
               "{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, maxMipmapLevels, minPowerOfTwo, minSize}
            );
            mipLevel = minPowerOfTwo;
         } else {
            mipLevel = maxMipmapLevels;
         }

         Options options = Minecraft.getInstance().options;
         int anisotropyBit = options.textureFiltering().get() != TextureFilteringMethod.ANISOTROPIC ? 0 : options.maxAnisotropyBit().get();
         Stitcher<SpriteContents> stitcher = new Stitcher<>(maxTextureSize, maxTextureSize, mipLevel, anisotropyBit);

         for (SpriteContents spriteInfo : sprites) {
            stitcher.registerSprite(spriteInfo);
         }

         try {
            stitcher.stitch();
         } catch (StitcherException e) {
            CrashReport report = CrashReport.forThrowable(e, "Stitching");
            CrashReportCategory category = report.addCategory("Stitcher");
            category.setDetail(
               "Sprites",
               e.getAllSprites().stream().map(s -> String.format(Locale.ROOT, "%s[%dx%d]", s.name(), s.width(), s.height())).collect(Collectors.joining(","))
            );
            category.setDetail("Max Texture Size", maxTextureSize);
            throw new ReportedException(report);
         }

         int width = stitcher.getWidth();
         int height = stitcher.getHeight();
         Map<Identifier, TextureAtlasSprite> result = this.getStitchedSprites(stitcher, width, height);
         TextureAtlasSprite missingSprite = result.get(MissingTextureAtlasSprite.getLocation());
         CompletableFuture<Void> readyForUpload = CompletableFuture.runAsync(
            () -> result.values().forEach(s -> s.contents().increaseMipLevel(mipLevel)), executor
         );
         return new SpriteLoader.Preparations(width, height, mipLevel, missingSprite, result, readyForUpload);
      }
   }

   private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
      final SpriteResourceLoader resourceLoader, final List<SpriteSource.Loader> sprites, final Executor executor
   ) {
      List<CompletableFuture<SpriteContents>> spriteFutures = sprites.stream()
         .map(supplier -> CompletableFuture.supplyAsync(() -> supplier.get(resourceLoader), executor))
         .toList();
      return Util.sequence(spriteFutures).thenApply(l -> l.stream().filter(Objects::nonNull).toList());
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
      final ResourceManager manager,
      final Identifier atlasInfoLocation,
      final int maxMipmapLevels,
      final Executor taskExecutor,
      final Set<MetadataSectionType<?>> additionalMetadata
   ) {
      SpriteResourceLoader spriteResourceLoader = SpriteResourceLoader.create(additionalMetadata);
      return CompletableFuture.<List<SpriteSource.Loader>>supplyAsync(() -> SpriteSourceList.load(manager, atlasInfoLocation).list(manager), taskExecutor)
         .thenCompose(sprites -> runSpriteSuppliers(spriteResourceLoader, (List<SpriteSource.Loader>)sprites, taskExecutor))
         .thenApply(resources -> this.stitch((List<SpriteContents>)resources, maxMipmapLevels, taskExecutor));
   }

   private Map<Identifier, TextureAtlasSprite> getStitchedSprites(final Stitcher<SpriteContents> stitcher, final int atlasWidth, final int atlasHeight) {
      Map<Identifier, TextureAtlasSprite> result = new HashMap<>();
      stitcher.gatherSprites(
         (contents, x, y, padding) -> result.put(contents.name(), new TextureAtlasSprite(this.location, contents, atlasWidth, atlasHeight, x, y, padding))
      );
      return result;
   }

   public record Preparations(
      int width, int height, int mipLevel, TextureAtlasSprite missing, Map<Identifier, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload
   ) {
      public @Nullable TextureAtlasSprite getSprite(final Identifier id) {
         return this.regions.get(id);
      }
   }
}
