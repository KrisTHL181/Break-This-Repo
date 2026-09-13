package net.minecraft.client.renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerSkinRenderCache {
   public static final RenderType DEFAULT_PLAYER_SKIN_RENDER_TYPE = playerSkinRenderType(DefaultPlayerSkin.getDefaultSkin());
   public static final Duration CACHE_DURATION = Duration.ofMinutes(5L);
   private final LoadingCache<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>> renderInfoCache = CacheBuilder.newBuilder()
      .expireAfterAccess(CACHE_DURATION)
      .build(
         new CacheLoader<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>>() {
            public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> load(ResolvableProfile p_430024_) {
               return p_430024_.resolveProfile(PlayerSkinRenderCache.this.profileResolver)
                  .thenCompose(
                     p_426899_ -> PlayerSkinRenderCache.this.skinManager
                        .get(p_426899_)
                        .thenApply(
                           p_431654_ -> p_431654_.map(p_430211_ -> PlayerSkinRenderCache.this.new RenderInfo(p_426899_, p_430211_, p_430024_.skinPatch()))
                        )
                  );
            }
         }
      );
   private final LoadingCache<ResolvableProfile, PlayerSkinRenderCache.RenderInfo> defaultSkinCache = CacheBuilder.newBuilder()
      .expireAfterAccess(CACHE_DURATION)
      .build(new CacheLoader<ResolvableProfile, PlayerSkinRenderCache.RenderInfo>() {
         public PlayerSkinRenderCache.RenderInfo load(ResolvableProfile p_431156_) {
            GameProfile gameprofile = p_431156_.partialProfile();
            return PlayerSkinRenderCache.this.new RenderInfo(gameprofile, DefaultPlayerSkin.get(gameprofile), p_431156_.skinPatch());
         }
      });
   final TextureManager textureManager;
   final SkinManager skinManager;
   final ProfileResolver profileResolver;

   public PlayerSkinRenderCache(TextureManager p_428881_, SkinManager p_423920_, ProfileResolver p_428966_) {
      this.textureManager = p_428881_;
      this.skinManager = p_423920_;
      this.profileResolver = p_428966_;
   }

   public PlayerSkinRenderCache.RenderInfo getOrDefault(ResolvableProfile p_429009_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.lookup(p_429009_).getNow(Optional.empty()).orElse(null);
      return playerskinrendercache$renderinfo != null
         ? playerskinrendercache$renderinfo
         : (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(p_429009_);
   }

   public Supplier<PlayerSkinRenderCache.RenderInfo> createLookup(ResolvableProfile p_426843_) {
      PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = (PlayerSkinRenderCache.RenderInfo)this.defaultSkinCache.getUnchecked(p_426843_);
      CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> completablefuture = (CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>)this.renderInfoCache
         .getUnchecked(p_426843_);
      Optional<PlayerSkinRenderCache.RenderInfo> optional = completablefuture.getNow(null);
      if (optional != null) {
         PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo1 = optional.orElse(playerskinrendercache$renderinfo);
         return () -> playerskinrendercache$renderinfo1;
      } else {
         return () -> completablefuture.getNow(Optional.empty()).orElse(playerskinrendercache$renderinfo);
      }
   }

   public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup(ResolvableProfile p_423443_) {
      return (CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>)this.renderInfoCache.getUnchecked(p_423443_);
   }

   static RenderType playerSkinRenderType(PlayerSkin p_427081_) {
      return SkullBlockRenderer.getPlayerSkinRenderType(p_427081_.body().texturePath());
   }

   @OnlyIn(Dist.CLIENT)
   public final class RenderInfo {
      private final GameProfile gameProfile;
      private final PlayerSkin playerSkin;
      private @Nullable RenderType itemRenderType;
      private @Nullable GpuTextureView textureView;
      private @Nullable GlyphRenderTypes glyphRenderTypes;

      public RenderInfo(final GameProfile p_431023_, final PlayerSkin p_427018_, final PlayerSkin.Patch p_426255_) {
         this.gameProfile = p_431023_;
         this.playerSkin = p_427018_.with(p_426255_);
      }

      public GameProfile gameProfile() {
         return this.gameProfile;
      }

      public PlayerSkin playerSkin() {
         return this.playerSkin;
      }

      public RenderType renderType() {
         if (this.itemRenderType == null) {
            this.itemRenderType = PlayerSkinRenderCache.playerSkinRenderType(this.playerSkin);
         }

         return this.itemRenderType;
      }

      public GpuTextureView textureView() {
         if (this.textureView == null) {
            this.textureView = PlayerSkinRenderCache.this.textureManager.getTexture(this.playerSkin.body().texturePath()).getTextureView();
         }

         return this.textureView;
      }

      public GlyphRenderTypes glyphRenderTypes() {
         if (this.glyphRenderTypes == null) {
            this.glyphRenderTypes = GlyphRenderTypes.createForColorTexture(this.playerSkin.body().texturePath());
         }

         return this.glyphRenderTypes;
      }

      @Override
      public boolean equals(Object p_431172_) {
         return this == p_431172_
            || p_431172_ instanceof PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo
               && this.gameProfile.equals(playerskinrendercache$renderinfo.gameProfile)
               && this.playerSkin.equals(playerskinrendercache$renderinfo.playerSkin);
      }

      @Override
      public int hashCode() {
         int i = 1;
         i = 31 * i + this.gameProfile.hashCode();
         return 31 * i + this.playerSkin.hashCode();
      }
   }
}
