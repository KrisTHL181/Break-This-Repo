package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public abstract class AbstractClientPlayer extends Player implements ClientAvatarEntity {
   private @Nullable PlayerInfo playerInfo;
   private final boolean showExtraEars;
   private final ClientAvatarState clientAvatarState = new ClientAvatarState();

   public AbstractClientPlayer(final ClientLevel level, final GameProfile gameProfile) {
      super(level, gameProfile);
      this.showExtraEars = "deadmau5".equals(this.getGameProfile().name());
   }

   @Override
   public @Nullable GameType gameMode() {
      PlayerInfo info = this.getPlayerInfo();
      return info != null ? info.getGameMode() : null;
   }

   protected @Nullable PlayerInfo getPlayerInfo() {
      if (this.playerInfo == null) {
         this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
      }

      return this.playerInfo;
   }

   @Override
   public void tick() {
      this.clientAvatarState.tick(this.position(), this.getDeltaMovement());
      super.tick();
   }

   protected void addWalkedDistance(final float distance) {
      this.clientAvatarState.addWalkDistance(distance);
   }

   @Override
   public ClientAvatarState avatarState() {
      return this.clientAvatarState;
   }

   @Override
   public PlayerSkin getSkin() {
      PlayerInfo info = this.getPlayerInfo();
      return info == null ? DefaultPlayerSkin.get(this.getUUID()) : info.getSkin();
   }

   @Override
   public Parrot.@Nullable Variant getParrotVariantOnShoulder(final boolean left) {
      return (left ? this.getShoulderParrotLeft() : this.getShoulderParrotRight()).orElse(null);
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.avatarState().resetBob();
   }

   @Override
   public void aiStep() {
      this.updateBob();
      super.aiStep();
   }

   protected void updateBob() {
      float tBob;
      if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
         tBob = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
      } else {
         tBob = 0.0F;
      }

      this.avatarState().updateBob(tBob);
   }

   public float getFieldOfViewModifier(final boolean firstPerson, final float effectScale) {
      float modifier = 1.0F;
      if (this.getAbilities().flying) {
         modifier *= 1.1F;
      }

      float walkingSpeed = this.getAbilities().getWalkingSpeed();
      if (walkingSpeed != 0.0F) {
         float speedFactor = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / walkingSpeed;
         modifier *= (speedFactor + 1.0F) / 2.0F;
      }

      if (this.isUsingItem()) {
         if (this.getUseItem().is(Items.BOW)) {
            float scale = Math.min(this.getTicksUsingItem() / 20.0F, 1.0F);
            modifier *= 1.0F - Mth.square(scale) * 0.15F;
         } else if (firstPerson && this.isScoping()) {
            return 0.1F;
         }
      }

      return Mth.lerp(effectScale, 1.0F, modifier);
   }

   @Override
   public boolean showExtraEars() {
      return this.showExtraEars;
   }
}
