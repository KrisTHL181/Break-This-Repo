package net.minecraft.client.resources.model;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class AtlasManager implements PreparableReloadListener, MaterialSet, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final List<AtlasManager.AtlasConfig> KNOWN_ATLASES = List.of(
      new AtlasManager.AtlasConfig(Sheets.ARMOR_TRIMS_SHEET, AtlasIds.ARMOR_TRIMS, false),
      new AtlasManager.AtlasConfig(Sheets.BANNER_SHEET, AtlasIds.BANNER_PATTERNS, false),
      new AtlasManager.AtlasConfig(Sheets.BED_SHEET, AtlasIds.BEDS, false),
      new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_BLOCKS, AtlasIds.BLOCKS, true),
      new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_ITEMS, AtlasIds.ITEMS, false),
      new AtlasManager.AtlasConfig(Sheets.CHEST_SHEET, AtlasIds.CHESTS, false),
      new AtlasManager.AtlasConfig(Sheets.DECORATED_POT_SHEET, AtlasIds.DECORATED_POT, false),
      new AtlasManager.AtlasConfig(Sheets.GUI_SHEET, AtlasIds.GUI, false, Set.of(GuiMetadataSection.TYPE)),
      new AtlasManager.AtlasConfig(Sheets.MAP_DECORATIONS_SHEET, AtlasIds.MAP_DECORATIONS, false),
      new AtlasManager.AtlasConfig(Sheets.PAINTINGS_SHEET, AtlasIds.PAINTINGS, false),
      new AtlasManager.AtlasConfig(TextureAtlas.LOCATION_PARTICLES, AtlasIds.PARTICLES, false),
      new AtlasManager.AtlasConfig(Sheets.SHIELD_SHEET, AtlasIds.SHIELD_PATTERNS, false),
      new AtlasManager.AtlasConfig(Sheets.SHULKER_SHEET, AtlasIds.SHULKER_BOXES, false),
      new AtlasManager.AtlasConfig(Sheets.SIGN_SHEET, AtlasIds.SIGNS, false),
      new AtlasManager.AtlasConfig(Sheets.CELESTIAL_SHEET, AtlasIds.CELESTIALS, false)
   );
   public static final PreparableReloadListener.StateKey<AtlasManager.PendingStitchResults> PENDING_STITCH = new PreparableReloadListener.StateKey<>();
   private final Map<Identifier, AtlasManager.AtlasEntry> atlasByTexture = new HashMap<>();
   private final Map<Identifier, AtlasManager.AtlasEntry> atlasById = new HashMap<>();
   private Map<Material, TextureAtlasSprite> materialLookup = Map.of();
   private int maxMipmapLevels;

   public AtlasManager(TextureManager p_425187_, int p_424632_) {
      for (AtlasManager.AtlasConfig atlasmanager$atlasconfig : KNOWN_ATLASES) {
         TextureAtlas textureatlas = new TextureAtlas(atlasmanager$atlasconfig.textureId);
         p_425187_.register(atlasmanager$atlasconfig.textureId, textureatlas);
         AtlasManager.AtlasEntry atlasmanager$atlasentry = new AtlasManager.AtlasEntry(textureatlas, atlasmanager$atlasconfig);
         this.atlasByTexture.put(atlasmanager$atlasconfig.textureId, atlasmanager$atlasentry);
         this.atlasById.put(atlasmanager$atlasconfig.definitionLocation, atlasmanager$atlasentry);
      }

      this.maxMipmapLevels = p_424632_;
   }

   public TextureAtlas getAtlasOrThrow(Identifier p_457247_) {
      AtlasManager.AtlasEntry atlasmanager$atlasentry = this.atlasById.get(p_457247_);
      if (atlasmanager$atlasentry == null) {
         throw new IllegalArgumentException("Invalid atlas id: " + p_457247_);
      } else {
         return atlasmanager$atlasentry.atlas();
      }
   }

   public void forEach(BiConsumer<Identifier, TextureAtlas> p_429856_) {
      this.atlasById.forEach((p_459671_, p_424411_) -> p_429856_.accept(p_459671_, p_424411_.atlas));
   }

   public void updateMaxMipLevel(int p_431474_) {
      this.maxMipmapLevels = p_431474_;
   }

   @Override
   public void close() {
      this.materialLookup = Map.of();
      this.atlasById.values().forEach(AtlasManager.AtlasEntry::close);
      this.atlasById.clear();
      this.atlasByTexture.clear();
   }

   @Override
   public TextureAtlasSprite get(Material p_430287_) {
      TextureAtlasSprite textureatlassprite = this.materialLookup.get(p_430287_);
      if (textureatlassprite != null) {
         return textureatlassprite;
      } else {
         Identifier identifier = p_430287_.atlasLocation();
         AtlasManager.AtlasEntry atlasmanager$atlasentry = this.atlasByTexture.get(identifier);
         if (atlasmanager$atlasentry == null) {
            throw new IllegalArgumentException("Invalid atlas texture id: " + identifier);
         } else {
            return atlasmanager$atlasentry.atlas().missingSprite();
         }
      }
   }

   @Override
   public void prepareSharedState(PreparableReloadListener.SharedState p_428177_) {
      int i = this.atlasById.size();
      List<AtlasManager.PendingStitch> list = new ArrayList<>(i);
      Map<Identifier, CompletableFuture<SpriteLoader.Preparations>> map = new HashMap<>(i);
      List<CompletableFuture<?>> list1 = new ArrayList<>(i);
      this.atlasById.forEach((p_450204_, p_422422_) -> {
         CompletableFuture<SpriteLoader.Preparations> completablefuture1 = new CompletableFuture<>();
         map.put(p_450204_, completablefuture1);
         list.add(new AtlasManager.PendingStitch(p_422422_, completablefuture1));
         list1.add(completablefuture1.thenCompose(SpriteLoader.Preparations::readyForUpload));
      });
      CompletableFuture<?> completablefuture = CompletableFuture.allOf(list1.toArray(CompletableFuture[]::new));
      p_428177_.set(PENDING_STITCH, new AtlasManager.PendingStitchResults(list, map, completablefuture));
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_430828_, Executor p_423488_, PreparableReloadListener.PreparationBarrier p_424354_, Executor p_426574_
   ) {
      AtlasManager.PendingStitchResults atlasmanager$pendingstitchresults = p_430828_.get(PENDING_STITCH);
      ResourceManager resourcemanager = p_430828_.resourceManager();
      atlasmanager$pendingstitchresults.pendingStitches
         .forEach(p_424397_ -> p_424397_.entry.scheduleLoad(resourcemanager, p_423488_, this.maxMipmapLevels).whenComplete((p_431547_, p_427170_) -> {
            if (p_431547_ != null) {
               p_424397_.preparations.complete(p_431547_);
            } else {
               p_424397_.preparations.completeExceptionally(p_427170_);
            }
         }));
      return atlasmanager$pendingstitchresults.allReadyToUpload
         .thenCompose(p_424354_::wait)
         .thenAcceptAsync(p_448430_ -> this.updateSpriteMaps(atlasmanager$pendingstitchresults), p_426574_);
   }

   private void updateSpriteMaps(AtlasManager.PendingStitchResults p_450644_) {
      this.materialLookup = p_450644_.joinAndUpload();
      Map<Identifier, TextureAtlasSprite> map = new HashMap<>();
      this.materialLookup
         .forEach(
            (p_448427_, p_448428_) -> {
               if (!p_448427_.texture().equals(MissingTextureAtlasSprite.getLocation())) {
                  TextureAtlasSprite textureatlassprite = map.putIfAbsent(p_448427_.texture(), p_448428_);
                  if (textureatlassprite != null) {
                     LOGGER.warn(
                        "Duplicate sprite {} from atlas {}, already defined in atlas {}. This will be rejected in a future version",
                        new Object[]{p_448427_.texture(), p_448427_.atlasLocation(), textureatlassprite.atlasLocation()}
                     );
                  }
               }
            }
         );
   }

   @OnlyIn(Dist.CLIENT)
   public record AtlasConfig(Identifier textureId, Identifier definitionLocation, boolean createMipmaps, Set<MetadataSectionType<?>> additionalMetadata) {
      public AtlasConfig(Identifier p_459311_, Identifier p_453131_, boolean p_423864_) {
         this(p_459311_, p_453131_, p_423864_, Set.of());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record AtlasEntry(TextureAtlas atlas, AtlasManager.AtlasConfig config) implements AutoCloseable {
      @Override
      public void close() {
         this.atlas.clearTextureData();
      }

      CompletableFuture<SpriteLoader.Preparations> scheduleLoad(ResourceManager p_426265_, Executor p_427032_, int p_422729_) {
         return SpriteLoader.create(this.atlas)
            .loadAndStitch(p_426265_, this.config.definitionLocation, this.config.createMipmaps ? p_422729_ : 0, p_427032_, this.config.additionalMetadata);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record PendingStitch(AtlasManager.AtlasEntry entry, CompletableFuture<SpriteLoader.Preparations> preparations) {
      public void joinAndUpload(Map<Material, TextureAtlasSprite> p_428167_) {
         SpriteLoader.Preparations spriteloader$preparations = this.preparations.join();
         this.entry.atlas.upload(spriteloader$preparations);
         spriteloader$preparations.regions().forEach((p_448432_, p_448433_) -> p_428167_.put(new Material(this.entry.config.textureId, p_448432_), p_448433_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class PendingStitchResults {
      final List<AtlasManager.PendingStitch> pendingStitches;
      private final Map<Identifier, CompletableFuture<SpriteLoader.Preparations>> stitchFuturesById;
      final CompletableFuture<?> allReadyToUpload;

      PendingStitchResults(
         List<AtlasManager.PendingStitch> p_429294_, Map<Identifier, CompletableFuture<SpriteLoader.Preparations>> p_431643_, CompletableFuture<?> p_429588_
      ) {
         this.pendingStitches = p_429294_;
         this.stitchFuturesById = p_431643_;
         this.allReadyToUpload = p_429588_;
      }

      public Map<Material, TextureAtlasSprite> joinAndUpload() {
         Map<Material, TextureAtlasSprite> map = new HashMap<>();
         this.pendingStitches.forEach(p_430881_ -> p_430881_.joinAndUpload(map));
         return map;
      }

      public CompletableFuture<SpriteLoader.Preparations> get(Identifier p_451187_) {
         return Objects.requireNonNull(this.stitchFuturesById.get(p_451187_));
      }
   }
}
