package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FontManager implements PreparableReloadListener, AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String FONTS_PATH = "fonts.json";
   public static final Identifier MISSING_FONT = Identifier.withDefaultNamespace("missing");
   private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   final FontSet missingFontSet;
   private final List<GlyphProvider> providersToClose = new ArrayList<>();
   private final Map<Identifier, FontSet> fontSets = new HashMap<>();
   private final TextureManager textureManager;
   private final FontManager.CachedFontProvider anyGlyphs = new FontManager.CachedFontProvider(false);
   private final FontManager.CachedFontProvider nonFishyGlyphs = new FontManager.CachedFontProvider(true);
   private final AtlasManager atlasManager;
   private final Map<Identifier, AtlasGlyphProvider> atlasProviders = new HashMap<>();
   final PlayerGlyphProvider playerProvider;

   public FontManager(TextureManager p_95005_, AtlasManager p_424821_, PlayerSkinRenderCache p_423649_) {
      this.textureManager = p_95005_;
      this.atlasManager = p_424821_;
      this.missingFontSet = this.createFontSet(MISSING_FONT, List.of(createFallbackProvider()), Set.of());
      this.playerProvider = new PlayerGlyphProvider(p_423649_);
   }

   private FontSet createFontSet(Identifier p_457016_, List<GlyphProvider.Conditional> p_424996_, Set<FontOption> p_427683_) {
      GlyphStitcher glyphstitcher = new GlyphStitcher(this.textureManager, p_457016_);
      FontSet fontset = new FontSet(glyphstitcher);
      fontset.reload(p_424996_, p_427683_);
      return fontset;
   }

   private static GlyphProvider.Conditional createFallbackProvider() {
      return new GlyphProvider.Conditional(new AllMissingGlyphProvider(), FontOption.Filter.ALWAYS_PASS);
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_426143_, Executor p_284975_, PreparableReloadListener.PreparationBarrier p_285160_, Executor p_285218_
   ) {
      return this.prepare(p_426143_.resourceManager(), p_284975_)
         .thenCompose(p_285160_::wait)
         .thenAcceptAsync(p_357671_ -> this.apply(p_357671_, Profiler.get()), p_285218_);
   }

   private CompletableFuture<FontManager.Preparation> prepare(ResourceManager p_285252_, Executor p_284969_) {
      List<CompletableFuture<FontManager.UnresolvedBuilderBundle>> list = new ArrayList<>();

      for (Entry<Identifier, List<Resource>> entry : FONT_DEFINITIONS.listMatchingResourceStacks(p_285252_).entrySet()) {
         Identifier identifier = FONT_DEFINITIONS.fileToId(entry.getKey());
         list.add(CompletableFuture.supplyAsync(() -> {
            List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> list1 = loadResourceStack(entry.getValue(), identifier);
            FontManager.UnresolvedBuilderBundle fontmanager$unresolvedbuilderbundle = new FontManager.UnresolvedBuilderBundle(identifier);

            for (Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional> pair : list1) {
               FontManager.BuilderId fontmanager$builderid = (FontManager.BuilderId)pair.getFirst();
               FontOption.Filter fontoption$filter = ((GlyphProviderDefinition.Conditional)pair.getSecond()).filter();
               ((GlyphProviderDefinition.Conditional)pair.getSecond()).definition().unpack().ifLeft(p_325337_ -> {
                  CompletableFuture<Optional<GlyphProvider>> completablefuture = this.safeLoad(fontmanager$builderid, p_325337_, p_285252_, p_284969_);
                  fontmanager$unresolvedbuilderbundle.add(fontmanager$builderid, fontoption$filter, completablefuture);
               }).ifRight(p_325345_ -> fontmanager$unresolvedbuilderbundle.add(fontmanager$builderid, fontoption$filter, p_325345_));
            }

            return fontmanager$unresolvedbuilderbundle;
         }, p_284969_));
      }

      return Util.sequence(list)
         .thenCompose(
            p_447985_ -> {
               List<CompletableFuture<Optional<GlyphProvider>>> list1 = p_447985_.stream()
                  .flatMap(FontManager.UnresolvedBuilderBundle::listBuilders)
                  .collect(Util.toMutableList());
               GlyphProvider.Conditional glyphprovider$conditional = createFallbackProvider();
               list1.add(CompletableFuture.completedFuture(Optional.of(glyphprovider$conditional.provider())));
               return Util.sequence(list1)
                  .thenCompose(
                     p_284618_ -> {
                        Map<Identifier, List<GlyphProvider.Conditional>> map = this.resolveProviders(p_447985_);
                        CompletableFuture<?>[] completablefuture = map.values()
                           .stream()
                           .map(p_284585_ -> CompletableFuture.runAsync(() -> this.finalizeProviderLoading(p_284585_, glyphprovider$conditional), p_284969_))
                           .toArray(CompletableFuture[]::new);
                        return CompletableFuture.allOf(completablefuture).thenApply(p_284595_ -> {
                           List<GlyphProvider> list2 = p_284618_.stream().flatMap(Optional::stream).toList();
                           return new FontManager.Preparation(map, list2);
                        });
                     }
                  );
            }
         );
   }

   private CompletableFuture<Optional<GlyphProvider>> safeLoad(
      FontManager.BuilderId p_285113_, GlyphProviderDefinition.Loader p_286561_, ResourceManager p_285424_, Executor p_285371_
   ) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return Optional.of(p_286561_.load(p_285424_));
         } catch (Exception exception) {
            LOGGER.warn("Failed to load builder {}, rejecting", p_285113_, exception);
            return Optional.empty();
         }
      }, p_285371_);
   }

   private Map<Identifier, List<GlyphProvider.Conditional>> resolveProviders(List<FontManager.UnresolvedBuilderBundle> p_285282_) {
      Map<Identifier, List<GlyphProvider.Conditional>> map = new HashMap<>();
      DependencySorter<Identifier, FontManager.UnresolvedBuilderBundle> dependencysorter = new DependencySorter<>();
      p_285282_.forEach(p_447987_ -> dependencysorter.addEntry(p_447987_.fontId, p_447987_));
      dependencysorter.orderByDependencies(
         (p_456287_, p_284621_) -> p_284621_.resolve(map::get).ifPresent(p_284590_ -> map.put(p_456287_, (List<GlyphProvider.Conditional>)p_284590_))
      );
      return map;
   }

   private void finalizeProviderLoading(List<GlyphProvider.Conditional> p_285520_, GlyphProvider.Conditional p_328834_) {
      p_285520_.add(0, p_328834_);
      IntSet intset = new IntOpenHashSet();

      for (GlyphProvider.Conditional glyphprovider$conditional : p_285520_) {
         intset.addAll(glyphprovider$conditional.provider().getSupportedGlyphs());
      }

      intset.forEach(p_420727_ -> {
         if (p_420727_ != 32) {
            for (GlyphProvider.Conditional glyphprovider$conditional1 : Lists.reverse(p_285520_)) {
               if (glyphprovider$conditional1.provider().getGlyph(p_420727_) != null) {
                  break;
               }
            }
         }
      });
   }

   private static Set<FontOption> getFontOptions(Options p_331588_) {
      Set<FontOption> set = EnumSet.noneOf(FontOption.class);
      if (p_331588_.forceUnicodeFont().get()) {
         set.add(FontOption.UNIFORM);
      }

      if (p_331588_.japaneseGlyphVariants().get()) {
         set.add(FontOption.JAPANESE_VARIANTS);
      }

      return set;
   }

   private void apply(FontManager.Preparation p_284939_, ProfilerFiller p_285407_) {
      p_285407_.push("closing");
      this.anyGlyphs.invalidate();
      this.nonFishyGlyphs.invalidate();
      this.fontSets.values().forEach(FontSet::close);
      this.fontSets.clear();
      this.providersToClose.forEach(GlyphProvider::close);
      this.providersToClose.clear();
      Set<FontOption> set = getFontOptions(Minecraft.getInstance().options);
      p_285407_.popPush("reloading");
      p_284939_.fontSets().forEach((p_447989_, p_447990_) -> this.fontSets.put(p_447989_, this.createFontSet(p_447989_, Lists.reverse(p_447990_), set)));
      this.providersToClose.addAll(p_284939_.allProviders);
      p_285407_.pop();
      if (!this.fontSets.containsKey(Minecraft.DEFAULT_FONT)) {
         throw new IllegalStateException("Default font failed to load");
      }

      this.atlasProviders.clear();
      this.atlasManager.forEach((p_454556_, p_420722_) -> this.atlasProviders.put(p_454556_, new AtlasGlyphProvider(p_420722_)));
   }

   public void updateOptions(Options p_335215_) {
      Set<FontOption> set = getFontOptions(p_335215_);

      for (FontSet fontset : this.fontSets.values()) {
         fontset.reload(set);
      }
   }

   private static List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> loadResourceStack(List<Resource> p_284976_, Identifier p_452509_) {
      List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> list = new ArrayList<>();

      for (Resource resource : p_284976_) {
         try (Reader reader = resource.openAsReader()) {
            JsonElement jsonelement = (JsonElement)GSON.fromJson(reader, JsonElement.class);
            FontManager.FontDefinitionFile fontmanager$fontdefinitionfile = (FontManager.FontDefinitionFile)FontManager.FontDefinitionFile.CODEC
               .parse(JsonOps.INSTANCE, jsonelement)
               .getOrThrow(JsonParseException::new);
            List<GlyphProviderDefinition.Conditional> list1 = fontmanager$fontdefinitionfile.providers;

            for (int i = list1.size() - 1; i >= 0; i--) {
               FontManager.BuilderId fontmanager$builderid = new FontManager.BuilderId(p_452509_, resource.sourcePackId(), i);
               list.add(Pair.of(fontmanager$builderid, list1.get(i)));
            }
         } catch (Exception exception) {
            LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", new Object[]{p_452509_, "fonts.json", resource.sourcePackId(), exception});
         }
      }

      return list;
   }

   public Font createFont() {
      return new Font(this.anyGlyphs);
   }

   public Font createFontFilterFishy() {
      return new Font(this.nonFishyGlyphs);
   }

   FontSet getFontSetRaw(Identifier p_451356_) {
      return this.fontSets.getOrDefault(p_451356_, this.missingFontSet);
   }

   GlyphSource getSpriteFont(FontDescription.AtlasSprite p_425039_) {
      AtlasGlyphProvider atlasglyphprovider = this.atlasProviders.get(p_425039_.atlasId());
      return atlasglyphprovider == null ? this.missingFontSet.source(false) : atlasglyphprovider.sourceForSprite(p_425039_.spriteId());
   }

   @Override
   public void close() {
      this.anyGlyphs.close();
      this.nonFishyGlyphs.close();
      this.fontSets.values().forEach(FontSet::close);
      this.providersToClose.forEach(GlyphProvider::close);
      this.missingFontSet.close();
   }

   @OnlyIn(Dist.CLIENT)
   record BuilderId(Identifier fontId, String pack, int index) {
      @Override
      public String toString() {
         return "(" + this.fontId + ": builder #" + this.index + " from pack " + this.pack + ")";
      }
   }

   @OnlyIn(Dist.CLIENT)
   record BuilderResult(FontManager.BuilderId id, FontOption.Filter filter, Either<CompletableFuture<Optional<GlyphProvider>>, Identifier> result) {
      public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, @Nullable List<GlyphProvider.Conditional>> p_284942_) {
         return (Optional<List<GlyphProvider.Conditional>>)this.result
            .map(
               p_325356_ -> ((Optional)p_325356_.join()).map(p_325357_ -> List.of(new GlyphProvider.Conditional(p_325357_, this.filter))),
               p_453820_ -> {
                  List<GlyphProvider.Conditional> list = p_284942_.apply(p_453820_);
                  if (list == null) {
                     FontManager.LOGGER
                        .warn(
                           "Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle",
                           p_453820_,
                           this.id
                        );
                     return Optional.empty();
                  } else {
                     return Optional.of(list.stream().map(this::mergeFilters).toList());
                  }
               }
            );
      }

      private GlyphProvider.Conditional mergeFilters(GlyphProvider.Conditional p_330532_) {
         return new GlyphProvider.Conditional(p_330532_.provider(), this.filter.merge(p_330532_.filter()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class CachedFontProvider implements Font.Provider, AutoCloseable {
      private final boolean nonFishyOnly;
      private volatile FontManager.CachedFontProvider.@Nullable CachedEntry lastEntry;
      private volatile @Nullable EffectGlyph whiteGlyph;

      CachedFontProvider(final boolean p_425677_) {
         this.nonFishyOnly = p_425677_;
      }

      public void invalidate() {
         this.lastEntry = null;
         this.whiteGlyph = null;
      }

      @Override
      public void close() {
         this.invalidate();
      }

      private GlyphSource getGlyphSource(FontDescription p_427754_) {
         return switch (p_427754_) {
            case FontDescription.Resource fontdescription$resource -> FontManager.this.getFontSetRaw(fontdescription$resource.id()).source(this.nonFishyOnly);
            case FontDescription.AtlasSprite fontdescription$atlassprite -> FontManager.this.getSpriteFont(fontdescription$atlassprite);
            case FontDescription.PlayerSprite fontdescription$playersprite -> FontManager.this.playerProvider.sourceForPlayer(fontdescription$playersprite);
            default -> FontManager.this.missingFontSet.source(this.nonFishyOnly);
         };
      }

      @Override
      public GlyphSource glyphs(FontDescription p_431701_) {
         FontManager.CachedFontProvider.CachedEntry fontmanager$cachedfontprovider$cachedentry = this.lastEntry;
         if (fontmanager$cachedfontprovider$cachedentry != null && p_431701_.equals(fontmanager$cachedfontprovider$cachedentry.description)) {
            return fontmanager$cachedfontprovider$cachedentry.source;
         }

         GlyphSource glyphsource = this.getGlyphSource(p_431701_);
         this.lastEntry = new FontManager.CachedFontProvider.CachedEntry(p_431701_, glyphsource);
         return glyphsource;
      }

      @Override
      public EffectGlyph effect() {
         EffectGlyph effectglyph = this.whiteGlyph;
         if (effectglyph == null) {
            effectglyph = FontManager.this.getFontSetRaw(FontDescription.DEFAULT.id()).whiteGlyph();
            this.whiteGlyph = effectglyph;
         }

         return effectglyph;
      }

      @OnlyIn(Dist.CLIENT)
      record CachedEntry(FontDescription description, GlyphSource source) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   record FontDefinitionFile(List<GlyphProviderDefinition.Conditional> providers) {
      public static final Codec<FontManager.FontDefinitionFile> CODEC = RecordCodecBuilder.create(
         p_325360_ -> p_325360_.group(
               GlyphProviderDefinition.Conditional.CODEC.listOf().fieldOf("providers").forGetter(FontManager.FontDefinitionFile::providers)
            )
            .apply(p_325360_, FontManager.FontDefinitionFile::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   record Preparation(Map<Identifier, List<GlyphProvider.Conditional>> fontSets, List<GlyphProvider> allProviders) {
   }

   @OnlyIn(Dist.CLIENT)
   record UnresolvedBuilderBundle(Identifier fontId, List<FontManager.BuilderResult> builders, Set<Identifier> dependencies)
      implements DependencySorter.Entry<Identifier> {
      public UnresolvedBuilderBundle(Identifier p_452474_) {
         this(p_452474_, new ArrayList<>(), new HashSet<>());
      }

      public void add(FontManager.BuilderId p_284935_, FontOption.Filter p_336303_, GlyphProviderDefinition.Reference p_334249_) {
         this.builders.add(new FontManager.BuilderResult(p_284935_, p_336303_, Either.right(p_334249_.id())));
         this.dependencies.add(p_334249_.id());
      }

      public void add(FontManager.BuilderId p_286837_, FontOption.Filter p_334374_, CompletableFuture<Optional<GlyphProvider>> p_331945_) {
         this.builders.add(new FontManager.BuilderResult(p_286837_, p_334374_, Either.left(p_331945_)));
      }

      private Stream<CompletableFuture<Optional<GlyphProvider>>> listBuilders() {
         return this.builders.stream().flatMap(p_285041_ -> p_285041_.result.left().stream());
      }

      public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, List<GlyphProvider.Conditional>> p_285118_) {
         List<GlyphProvider.Conditional> list = new ArrayList<>();

         for (FontManager.BuilderResult fontmanager$builderresult : this.builders) {
            Optional<List<GlyphProvider.Conditional>> optional = fontmanager$builderresult.resolve(p_285118_);
            if (!optional.isPresent()) {
               return Optional.empty();
            }

            list.addAll(optional.get());
         }

         return Optional.of(list);
      }

      @Override
      public void visitRequiredDependencies(Consumer<Identifier> p_285391_) {
         this.dependencies.forEach(p_285391_);
      }

      @Override
      public void visitOptionalDependencies(Consumer<Identifier> p_285405_) {
      }
   }
}
