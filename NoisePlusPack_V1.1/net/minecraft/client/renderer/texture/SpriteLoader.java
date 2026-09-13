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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier location;
   private final int maxSupportedTextureSize;

   public SpriteLoader(Identifier p_454492_, int p_276121_) {
      this.location = p_454492_;
      this.maxSupportedTextureSize = p_276121_;
   }

   public static SpriteLoader create(TextureAtlas p_249085_) {
      return new SpriteLoader(p_249085_.location(), p_249085_.maxSupportedTextureSize());
   }

   private SpriteLoader.Preparations stitch(List<SpriteContents> p_262029_, int p_261919_, Executor p_261665_) {
      try (Zone zone = Profiler.get().zone(() -> "stitch " + this.location)) {
         int i = this.maxSupportedTextureSize;
         int j = Integer.MAX_VALUE;
         int k = 1 << p_261919_;

         for (SpriteContents spritecontents : p_262029_) {
            j = Math.min(j, Math.min(spritecontents.width(), spritecontents.height()));
            int l = Math.min(Integer.lowestOneBit(spritecontents.width()), Integer.lowestOneBit(spritecontents.height()));
            if (l < k) {
               LOGGER.warn(
                  "Texture {} with size {}x{} limits mip level from {} to {}",
                  new Object[]{spritecontents.name(), spritecontents.width(), spritecontents.height(), Mth.log2(k), Mth.log2(l)}
               );
               k = l;
            }
         }

         int j1 = Math.min(j, k);
         int k1 = Mth.log2(j1);
         int l1;
         if (k1 < p_261919_) {
            LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, p_261919_, k1, j1});
            l1 = k1;
         } else {
            l1 = p_261919_;
         }

         Options options = Minecraft.getInstance().options;
         int i1 = l1 != 0 && options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyBit().get() : 0;
         Stitcher<SpriteContents> stitcher = new Stitcher<>(i, i, l1, i1);

         for (SpriteContents spritecontents1 : p_262029_) {
            stitcher.registerSprite(spritecontents1);
         }

         try {
            stitcher.stitch();
         } catch (StitcherException stitcherexception) {
            CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
            crashreportcategory.setDetail(
               "Sprites",
               stitcherexception.getAllSprites()
                  .stream()
                  .map(p_448379_ -> String.format(Locale.ROOT, "%s[%dx%d]", p_448379_.name(), p_448379_.width(), p_448379_.height()))
                  .collect(Collectors.joining(","))
            );
            crashreportcategory.setDetail("Max Texture Size", i);
            throw new ReportedException(crashreport);
         }

         int i2 = stitcher.getWidth();
         int j2 = stitcher.getHeight();
         Map<Identifier, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, i2, j2);
         TextureAtlasSprite textureatlassprite = map.get(MissingTextureAtlasSprite.getLocation());
         CompletableFuture<Void> completablefuture = CompletableFuture.runAsync(
            () -> map.values().forEach(p_251202_ -> p_251202_.contents().increaseMipLevel(l1)), p_261665_
         );
         return new SpriteLoader.Preparations(i2, j2, l1, textureatlassprite, map, completablefuture);
      }
   }

   private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
      SpriteResourceLoader p_297457_, List<SpriteSource.Loader> p_261516_, Executor p_261791_
   ) {
      List<CompletableFuture<SpriteContents>> list = p_261516_.stream()
         .map(p_459391_ -> CompletableFuture.supplyAsync(() -> p_459391_.get(p_297457_), p_261791_))
         .toList();
      return Util.sequence(list).thenApply(p_252234_ -> p_252234_.stream().filter(Objects::nonNull).toList());
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
      ResourceManager p_262108_, Identifier p_459205_, int p_262104_, Executor p_261687_, Set<MetadataSectionType<?>> p_430900_
   ) {
      SpriteResourceLoader spriteresourceloader = SpriteResourceLoader.create(p_430900_);
      return CompletableFuture.<List<SpriteSource.Loader>>supplyAsync(() -> SpriteSourceList.load(p_262108_, p_459205_).list(p_262108_), p_261687_)
         .thenCompose(p_296297_ -> runSpriteSuppliers(spriteresourceloader, (List<SpriteSource.Loader>)p_296297_, p_261687_))
         .thenApply(p_261393_ -> this.stitch((List<SpriteContents>)p_261393_, p_262104_, p_261687_));
   }

   private Map<Identifier, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> p_276117_, int p_276111_, int p_276112_) {
      Map<Identifier, TextureAtlasSprite> map = new HashMap<>();
      p_276117_.gatherSprites(
         (p_448383_, p_448384_, p_448385_, p_448386_) -> map.put(
            p_448383_.name(), new TextureAtlasSprite(this.location, p_448383_, p_276111_, p_276112_, p_448384_, p_448385_, p_448386_)
         )
      );
      return map;
   }

   @OnlyIn(Dist.CLIENT)
   public record Preparations(
      int width, int height, int mipLevel, TextureAtlasSprite missing, Map<Identifier, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload
   ) {
      public @Nullable TextureAtlasSprite getSprite(Identifier p_459172_) {
         return this.regions.get(p_459172_);
      }
   }
}
