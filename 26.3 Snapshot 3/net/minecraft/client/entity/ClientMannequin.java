package net.minecraft.client.entity;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ClientMannequin extends Mannequin implements ClientAvatarEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PlayerSkin DEFAULT_SKIN = DefaultPlayerSkin.get(Mannequin.DEFAULT_PROFILE.partialProfile());
   private final ClientAvatarState avatarState = new ClientAvatarState();
   private @Nullable CompletableFuture<Optional<PlayerSkin>> skinLookup;
   private PlayerSkin skin = DEFAULT_SKIN;
   private final PlayerSkinRenderCache skinRenderCache;

   public static void registerOverrides(final PlayerSkinRenderCache cache) {
      Mannequin.constructor = (type, level) -> level instanceof ClientLevel ? new ClientMannequin(level, cache) : new Mannequin(type, level);
   }

   public ClientMannequin(final Level level, final PlayerSkinRenderCache skinRenderCache) {
      super(level);
      this.skinRenderCache = skinRenderCache;
   }

   @Override
   public void tick() {
      super.tick();
      this.avatarState.tick(this.position(), this.getDeltaMovement());
      if (this.skinLookup != null && this.skinLookup.isDone()) {
         try {
            this.skinLookup.get().ifPresent(this::setSkin);
            this.skinLookup = null;
         } catch (Exception e) {
            LOGGER.error("Error when trying to look up skin", e);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (accessor.equals(DATA_PROFILE)) {
         this.updateSkin();
      }
   }

   private void updateSkin() {
      if (this.skinLookup != null) {
         CompletableFuture<Optional<PlayerSkin>> future = this.skinLookup;
         this.skinLookup = null;
         future.cancel(false);
      }

      this.skinLookup = this.skinRenderCache.lookup(this.getProfile()).thenApply(info -> info.map(PlayerSkinRenderCache.RenderInfo::playerSkin));
   }

   @Override
   public ClientAvatarState avatarState() {
      return this.avatarState;
   }

   @Override
   public PlayerSkin getSkin() {
      return this.skin;
   }

   private void setSkin(final PlayerSkin skin) {
      this.skin = skin;
   }

   @Override
   public @Nullable Component belowNameDisplay() {
      return this.getDescription();
   }

   @Override
   public Parrot.@Nullable Variant getParrotVariantOnShoulder(final boolean left) {
      return null;
   }

   @Override
   public boolean showExtraEars() {
      return false;
   }
}
