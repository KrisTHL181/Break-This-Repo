package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Services;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   static final Logger LOGGER = LogUtils.getLogger();
   private final Services services;
   final SkinTextureDownloader skinTextureDownloader;
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(Path p_299617_, final Services p_424560_, SkinTextureDownloader p_423962_, final Executor p_299732_) {
      this.services = p_424560_;
      this.skinTextureDownloader = p_423962_;
      this.skinTextures = new SkinManager.TextureCache(p_299617_, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(p_299617_, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(p_299617_, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofSeconds(15L))
         .build(
            new CacheLoader<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>>() {
               public CompletableFuture<Optional<PlayerSkin>> load(SkinManager.CacheKey p_298169_) {
                  return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                        Property property = p_298169_.packedTextures();
                        if (property == null) {
                           return MinecraftProfileTextures.EMPTY;
                        }

                        MinecraftProfileTextures minecraftprofiletextures = p_424560_.sessionService().unpackTextures(property);
                        if (minecraftprofiletextures.signatureState() == SignatureState.INVALID) {
                           SkinManager.LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", p_298169_.profileId());
                        }

                        return minecraftprofiletextures;
                     }, Util.backgroundExecutor().forName("unpackSkinTextures"))
                     .thenComposeAsync(p_308313_ -> SkinManager.this.registerTextures(p_298169_.profileId(), p_308313_), p_299732_)
                     .handle((p_423121_, p_374685_) -> {
                        if (p_374685_ != null) {
                           SkinManager.LOGGER.warn("Failed to load texture for profile {}", p_298169_.profileId, p_374685_);
                        }

                        return Optional.ofNullable(p_423121_);
                     });
               }
            }
         );
   }

   public Supplier<PlayerSkin> createLookup(GameProfile p_426545_, boolean p_424649_) {
      CompletableFuture<Optional<PlayerSkin>> completablefuture = this.get(p_426545_);
      PlayerSkin playerskin = DefaultPlayerSkin.get(p_426545_);
      if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
         return () -> playerskin;
      } else {
         Optional<PlayerSkin> optional = completablefuture.getNow(null);
         if (optional != null) {
            PlayerSkin playerskin1 = optional.filter(p_421052_ -> !p_424649_ || p_421052_.secure()).orElse(playerskin);
            return () -> playerskin1;
         } else {
            return () -> completablefuture.getNow(Optional.empty()).filter(p_421059_ -> !p_424649_ || p_421059_.secure()).orElse(playerskin);
         }
      }
   }

   public CompletableFuture<Optional<PlayerSkin>> get(GameProfile p_430689_) {
      if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
         PlayerSkin playerskin = DefaultPlayerSkin.get(p_430689_);
         return CompletableFuture.completedFuture(Optional.of(playerskin));
      } else {
         Property property = this.services.sessionService().getPackedTextures(p_430689_);
         return (CompletableFuture<Optional<PlayerSkin>>)this.skinCache.getUnchecked(new SkinManager.CacheKey(p_430689_.id(), property));
      }
   }

   CompletableFuture<PlayerSkin> registerTextures(UUID p_312099_, MinecraftProfileTextures p_313047_) {
      MinecraftProfileTexture minecraftprofiletexture = p_313047_.skin();
      CompletableFuture<ClientAsset.Texture> completablefuture;
      PlayerModelType playermodeltype;
      if (minecraftprofiletexture != null) {
         completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
         playermodeltype = PlayerModelType.byLegacyServicesName(minecraftprofiletexture.getMetadata("model"));
      } else {
         PlayerSkin playerskin = DefaultPlayerSkin.get(p_312099_);
         completablefuture = CompletableFuture.completedFuture(playerskin.body());
         playermodeltype = playerskin.model();
      }

      MinecraftProfileTexture minecraftprofiletexture2 = p_313047_.cape();
      CompletableFuture<ClientAsset.Texture> completablefuture1 = minecraftprofiletexture2 != null
         ? this.capeTextures.getOrLoad(minecraftprofiletexture2)
         : CompletableFuture.completedFuture(null);
      MinecraftProfileTexture minecraftprofiletexture1 = p_313047_.elytra();
      CompletableFuture<ClientAsset.Texture> completablefuture2 = minecraftprofiletexture1 != null
         ? this.elytraTextures.getOrLoad(minecraftprofiletexture1)
         : CompletableFuture.completedFuture(null);
      return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2)
         .thenApply(
            p_421050_ -> new PlayerSkin(
               completablefuture.join(),
               completablefuture1.join(),
               completablefuture2.join(),
               playermodeltype,
               p_313047_.signatureState() == SignatureState.SIGNED
            )
         );
   }

   @OnlyIn(Dist.CLIENT)
   record CacheKey(UUID profileId, @Nullable Property packedTextures) {
   }

   @OnlyIn(Dist.CLIENT)
   class TextureCache {
      private final Path root;
      private final Type type;
      private final Map<String, CompletableFuture<ClientAsset.Texture>> textures = new Object2ObjectOpenHashMap();

      TextureCache(final Path p_297921_, final Type p_298775_) {
         this.root = p_297921_;
         this.type = p_298775_;
      }

      public CompletableFuture<ClientAsset.Texture> getOrLoad(MinecraftProfileTexture p_300959_) {
         String s = p_300959_.getHash();
         CompletableFuture<ClientAsset.Texture> completablefuture = this.textures.get(s);
         if (completablefuture == null) {
            completablefuture = this.registerTexture(p_300959_);
            this.textures.put(s, completablefuture);
         }

         return completablefuture;
      }

      private CompletableFuture<ClientAsset.Texture> registerTexture(MinecraftProfileTexture p_300607_) {
         String s = Hashing.sha1().hashUnencodedChars(p_300607_.getHash()).toString();
         Identifier identifier = this.getTextureLocation(s);
         Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
         return SkinManager.this.skinTextureDownloader.downloadAndRegisterSkin(identifier, path, p_300607_.getUrl(), this.type == Type.SKIN);
      }

      private Identifier getTextureLocation(String p_297392_) {
         String s = switch (this.type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
            default -> throw new MatchException(null, null);
         };
         return Identifier.withDefaultNamespace(s + "/" + p_297392_);
      }
   }
}
