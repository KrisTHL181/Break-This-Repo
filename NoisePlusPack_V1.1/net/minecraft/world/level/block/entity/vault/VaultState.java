package net.minecraft.world.level.block.entity.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public enum VaultState implements StringRepresentable {
   INACTIVE("inactive", VaultState.LightLevel.HALF_LIT) {
      @Override
      protected void onEnter(ServerLevel p_330824_, BlockPos p_329235_, VaultConfig p_334137_, VaultSharedData p_334678_, boolean p_331058_) {
         p_334678_.setDisplayItem(ItemStack.EMPTY);
         p_330824_.levelEvent(3016, p_329235_, p_331058_ ? 1 : 0);
      }
   },
   ACTIVE("active", VaultState.LightLevel.LIT) {
      @Override
      protected void onEnter(ServerLevel p_329909_, BlockPos p_333646_, VaultConfig p_333985_, VaultSharedData p_334965_, boolean p_328901_) {
         if (!p_334965_.hasDisplayItem()) {
            VaultBlockEntity.Server.cycleDisplayItemFromLootTable(p_329909_, this, p_333985_, p_334965_, p_333646_);
         }

         p_329909_.levelEvent(3015, p_333646_, p_328901_ ? 1 : 0);
      }
   },
   UNLOCKING("unlocking", VaultState.LightLevel.LIT) {
      @Override
      protected void onEnter(ServerLevel p_334760_, BlockPos p_334274_, VaultConfig p_331308_, VaultSharedData p_327978_, boolean p_332846_) {
         p_334760_.playSound(null, p_334274_, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS);
      }
   },
   EJECTING("ejecting", VaultState.LightLevel.LIT) {
      @Override
      protected void onEnter(ServerLevel p_331552_, BlockPos p_331920_, VaultConfig p_332952_, VaultSharedData p_328350_, boolean p_333805_) {
         p_331552_.playSound(null, p_331920_, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS);
      }

      @Override
      protected void onExit(ServerLevel p_329515_, BlockPos p_333072_, VaultConfig p_328058_, VaultSharedData p_332218_) {
         p_329515_.playSound(null, p_333072_, SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS);
      }
   };

   private static final int UPDATE_CONNECTED_PLAYERS_TICK_RATE = 20;
   private static final int DELAY_BETWEEN_EJECTIONS_TICKS = 20;
   private static final int DELAY_AFTER_LAST_EJECTION_TICKS = 20;
   private static final int DELAY_BEFORE_FIRST_EJECTION_TICKS = 20;
   private final String stateName;
   private final VaultState.LightLevel lightLevel;

   VaultState(final String p_333421_, final VaultState.LightLevel p_333767_) {
      this.stateName = p_333421_;
      this.lightLevel = p_333767_;
   }

   @Override
   public String getSerializedName() {
      return this.stateName;
   }

   public int lightLevel() {
      return this.lightLevel.value;
   }

   public VaultState tickAndGetNext(ServerLevel p_334990_, BlockPos p_330620_, VaultConfig p_334025_, VaultServerData p_332760_, VaultSharedData p_333510_) {
      return switch (this) {
         case INACTIVE -> updateStateForConnectedPlayers(p_334990_, p_330620_, p_334025_, p_332760_, p_333510_, p_334025_.activationRange());
         case ACTIVE -> updateStateForConnectedPlayers(p_334990_, p_330620_, p_334025_, p_332760_, p_333510_, p_334025_.deactivationRange());
         case UNLOCKING -> {
            p_332760_.pauseStateUpdatingUntil(p_334990_.getGameTime() + 20L);
            yield EJECTING;
         }
         case EJECTING -> {
            if (p_332760_.getItemsToEject().isEmpty()) {
               p_332760_.markEjectionFinished();
               yield updateStateForConnectedPlayers(p_334990_, p_330620_, p_334025_, p_332760_, p_333510_, p_334025_.deactivationRange());
            } else {
               float f = p_332760_.ejectionProgress();
               this.ejectResultItem(p_334990_, p_330620_, p_332760_.popNextItemToEject(), f);
               p_333510_.setDisplayItem(p_332760_.getNextItemToEject());
               boolean flag = p_332760_.getItemsToEject().isEmpty();
               int i = flag ? 20 : 20;
               p_332760_.pauseStateUpdatingUntil(p_334990_.getGameTime() + i);
               yield EJECTING;
            }
         }
      };
   }

   private static VaultState updateStateForConnectedPlayers(
      ServerLevel p_330419_, BlockPos p_334068_, VaultConfig p_335667_, VaultServerData p_330976_, VaultSharedData p_330718_, double p_334799_
   ) {
      p_330718_.updateConnectedPlayersWithinRange(p_330419_, p_334068_, p_330976_, p_335667_, p_334799_);
      p_330976_.pauseStateUpdatingUntil(p_330419_.getGameTime() + 20L);
      return p_330718_.hasConnectedPlayers() ? ACTIVE : INACTIVE;
   }

   public void onTransition(
      ServerLevel p_332806_, BlockPos p_329339_, VaultState p_335389_, VaultConfig p_330996_, VaultSharedData p_333239_, boolean p_330399_
   ) {
      this.onExit(p_332806_, p_329339_, p_330996_, p_333239_);
      p_335389_.onEnter(p_332806_, p_329339_, p_330996_, p_333239_, p_330399_);
   }

   protected void onEnter(ServerLevel p_335827_, BlockPos p_330931_, VaultConfig p_331678_, VaultSharedData p_333706_, boolean p_330849_) {
   }

   protected void onExit(ServerLevel p_331983_, BlockPos p_331510_, VaultConfig p_327841_, VaultSharedData p_334150_) {
   }

   private void ejectResultItem(ServerLevel p_329632_, BlockPos p_331411_, ItemStack p_329283_, float p_332145_) {
      DefaultDispenseItemBehavior.spawnItem(p_329632_, p_329283_, 2, Direction.UP, Vec3.atBottomCenterOf(p_331411_).relative(Direction.UP, 1.2));
      p_329632_.levelEvent(3017, p_331411_, 0);
      p_329632_.playSound(null, p_331411_, SoundEvents.VAULT_EJECT_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F + 0.4F * p_332145_);
   }

   enum LightLevel {
      HALF_LIT(6),
      LIT(12);

      final int value;

      LightLevel(final int p_327951_) {
         this.value = p_327951_;
      }
   }
}
