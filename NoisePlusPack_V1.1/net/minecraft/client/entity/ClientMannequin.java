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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientMannequin extends Mannequin implements ClientAvatarEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PlayerSkin DEFAULT_SKIN = DefaultPlayerSkin.get(Mannequin.DEFAULT_PROFILE.partialProfile());
   private final ClientAvatarState avatarState = new ClientAvatarState();
   private @Nullable CompletableFuture<Optional<PlayerSkin>> skinLookup;
   private PlayerSkin skin = DEFAULT_SKIN;
   private final PlayerSkinRenderCache skinRenderCache;

   public static void registerOverrides(PlayerSkinRenderCache p_427545_) {
      Mannequin.constructor = (p_426109_, p_429531_) -> p_429531_ instanceof ClientLevel
         ? new ClientMannequin(p_429531_, p_427545_)
         : new Mannequin(p_426109_, p_429531_);
   }

   public ClientMannequin(Level p_427009_, PlayerSkinRenderCache p_424494_) {
      super(p_427009_);
      this.skinRenderCache = p_424494_;
   }

   @Override
   public void tick() {
      super.tick();
      this.avatarState.tick(this.position(), this.getDeltaMovement());
      if (this.skinLookup != null && this.skinLookup.isDone()) {
         try {
            this.skinLookup.get().ifPresent(this::setSkin);
            this.skinLookup = null;
         } catch (Exception exception) {
            LOGGER.error("Error when trying to look up skin", exception);
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_423643_) {
      super.onSyncedDataUpdated(p_423643_);
      if (p_423643_.equals(DATA_PROFILE)) {
         this.updateSkin();
      }
   }

   private void updateSkin() {
      if (this.skinLookup != null) {
         CompletableFuture<Optional<PlayerSkin>> completablefuture = this.skinLookup;
         this.skinLookup = null;
         completablefuture.cancel(false);
      }

      this.skinLookup = this.skinRenderCache.lookup(this.getProfile()).thenApply(p_429837_ -> p_429837_.map(PlayerSkinRenderCache.RenderInfo::playerSkin));
   }

   @Override
   public ClientAvatarState avatarState() {
      return this.avatarState;
   }

   @Override
   public PlayerSkin getSkin() {
      return this.skin;
   }

   private void setSkin(PlayerSkin p_428073_) {
      this.skin = p_428073_;
   }

   @Override
   public @Nullable Component belowNameDisplay() {
      return this.getDescription();
   }

   @Override
   public Parrot.@Nullable Variant getParrotVariantOnShoulder(boolean p_429115_) {
      return null;
   }

   @Override
   public boolean showExtraEars() {
      return false;
   }
}
