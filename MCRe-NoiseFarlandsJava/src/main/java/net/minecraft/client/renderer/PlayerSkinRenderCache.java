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
                public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> load(final ResolvableProfile profile) {
                    return profile.resolveProfile(PlayerSkinRenderCache.this.profileResolver)
                        .thenCompose(
                            resolvedProfile -> PlayerSkinRenderCache.this.skinManager
                                .get(resolvedProfile)
                                .thenApply(
                                    playerSkin -> playerSkin.map(skin -> PlayerSkinRenderCache.this.new RenderInfo(resolvedProfile, skin, profile.skinPatch()))
                                )
                        );
                }
            }
        );
    private final LoadingCache<ResolvableProfile, PlayerSkinRenderCache.RenderInfo> defaultSkinCache = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_DURATION)
        .build(new CacheLoader<ResolvableProfile, PlayerSkinRenderCache.RenderInfo>() {
            public PlayerSkinRenderCache.RenderInfo load(final ResolvableProfile profile) {
                GameProfile temporaryProfile = profile.partialProfile();
                return PlayerSkinRenderCache.this.new RenderInfo(temporaryProfile, DefaultPlayerSkin.get(temporaryProfile), profile.skinPatch());
            }
        });
    private final TextureManager textureManager;
    private final SkinManager skinManager;
    private final ProfileResolver profileResolver;

    public PlayerSkinRenderCache(final TextureManager textureManager, final SkinManager skinManager, final ProfileResolver profileResolver) {
        this.textureManager = textureManager;
        this.skinManager = skinManager;
        this.profileResolver = profileResolver;
    }

    public PlayerSkinRenderCache.RenderInfo getOrDefault(final ResolvableProfile profile) {
        PlayerSkinRenderCache.RenderInfo result = this.lookup(profile).getNow(Optional.empty()).orElse(null);
        return result != null ? result : this.defaultSkinCache.getUnchecked(profile);
    }

    public Supplier<PlayerSkinRenderCache.RenderInfo> createLookup(final ResolvableProfile profile) {
        PlayerSkinRenderCache.RenderInfo defaultForProfile = this.defaultSkinCache.getUnchecked(profile);
        CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> future = this.renderInfoCache.getUnchecked(profile);
        Optional<PlayerSkinRenderCache.RenderInfo> currentValue = future.getNow(null);
        if (currentValue != null) {
            PlayerSkinRenderCache.RenderInfo finalValue = currentValue.orElse(defaultForProfile);
            return () -> finalValue;
        } else {
            return () -> future.getNow(Optional.empty()).orElse(defaultForProfile);
        }
    }

    public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup(final ResolvableProfile profile) {
        return this.renderInfoCache.getUnchecked(profile);
    }

    private static RenderType playerSkinRenderType(final PlayerSkin playerSkin) {
        return SkullBlockRenderer.getPlayerSkinRenderType(playerSkin.body().texturePath());
    }

    @OnlyIn(Dist.CLIENT)
    public final class RenderInfo {
        private final GameProfile gameProfile;
        private final PlayerSkin playerSkin;
        private @Nullable RenderType itemRenderType;
        private @Nullable GpuTextureView textureView;
        private @Nullable GlyphRenderTypes glyphRenderTypes;

        public RenderInfo(final GameProfile gameProfile, final PlayerSkin playerSkin, final PlayerSkin.Patch patch) {
            this.gameProfile = gameProfile;
            this.playerSkin = playerSkin.with(patch);
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
        public boolean equals(final Object o) {
            return this == o
                || o instanceof PlayerSkinRenderCache.RenderInfo that && this.gameProfile.equals(that.gameProfile) && this.playerSkin.equals(that.playerSkin);
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + this.gameProfile.hashCode();
            return 31 * result + this.playerSkin.hashCode();
        }
    }
}