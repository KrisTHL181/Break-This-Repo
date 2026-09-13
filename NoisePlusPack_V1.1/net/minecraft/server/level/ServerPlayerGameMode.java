package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerPlayerGameMode {
   private static final double FLIGHT_DISABLE_RANGE = 1.0;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected ServerLevel level;
   protected final ServerPlayer player;
   private GameType gameModeForPlayer = GameType.DEFAULT_MODE;
   private @Nullable GameType previousGameModeForPlayer;
   private boolean isDestroyingBlock;
   private int destroyProgressStart;
   private BlockPos destroyPos = BlockPos.ZERO;
   private int gameTicks;
   private boolean hasDelayedDestroy;
   private BlockPos delayedDestroyPos = BlockPos.ZERO;
   private int delayedTickStart;
   private int lastSentState = -1;

   public ServerPlayerGameMode(ServerPlayer p_143472_) {
      this.player = p_143472_;
      this.level = p_143472_.level();
   }

   public boolean changeGameModeForPlayer(GameType p_143474_) {
      if (p_143474_ == this.gameModeForPlayer) {
         return false;
      }

      Abilities abilities = this.player.getAbilities();
      this.setGameModeForPlayer(p_143474_, this.gameModeForPlayer);
      if (abilities.flying && p_143474_ != GameType.SPECTATOR && this.isInRangeOfGround()) {
         abilities.flying = false;
      }

      this.player.onUpdateAbilities();
      this.level
         .getServer()
         .getPlayerList()
         .broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, this.player));
      this.level.updateSleepingPlayerList();
      if (p_143474_ == GameType.CREATIVE) {
         this.player.resetCurrentImpulseContext();
      }

      return true;
   }

   protected void setGameModeForPlayer(GameType p_9274_, @Nullable GameType p_9275_) {
      this.previousGameModeForPlayer = p_9275_;
      this.gameModeForPlayer = p_9274_;
      Abilities abilities = this.player.getAbilities();
      p_9274_.updatePlayerAbilities(abilities);
   }

   private boolean isInRangeOfGround() {
      List<VoxelShape> list = Entity.collectAllColliders(this.player, this.level, this.player.getBoundingBox());
      return list.isEmpty() && this.player.getAvailableSpaceBelow(1.0) < 1.0;
   }

   public GameType getGameModeForPlayer() {
      return this.gameModeForPlayer;
   }

   public @Nullable GameType getPreviousGameModeForPlayer() {
      return this.previousGameModeForPlayer;
   }

   public boolean isSurvival() {
      return this.gameModeForPlayer.isSurvival();
   }

   public boolean isCreative() {
      return this.gameModeForPlayer.isCreative();
   }

   public void tick() {
      this.gameTicks++;
      if (this.hasDelayedDestroy) {
         BlockState blockstate = this.level.getBlockState(this.delayedDestroyPos);
         if (blockstate.isAir()) {
            this.hasDelayedDestroy = false;
         } else {
            float f = this.incrementDestroyProgress(blockstate, this.delayedDestroyPos, this.delayedTickStart);
            if (f >= 1.0F) {
               this.hasDelayedDestroy = false;
               this.destroyBlock(this.delayedDestroyPos);
            }
         }
      } else if (this.isDestroyingBlock) {
         BlockState blockstate1 = this.level.getBlockState(this.destroyPos);
         if (blockstate1.isAir()) {
            this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
            this.lastSentState = -1;
            this.isDestroyingBlock = false;
         } else {
            this.incrementDestroyProgress(blockstate1, this.destroyPos, this.destroyProgressStart);
         }
      }
   }

   private float incrementDestroyProgress(BlockState p_9277_, BlockPos p_9278_, int p_9279_) {
      int i = this.gameTicks - p_9279_;
      float f = p_9277_.getDestroyProgress(this.player, this.player.level(), p_9278_) * (i + 1);
      int j = (int)(f * 10.0F);
      if (j != this.lastSentState) {
         this.level.destroyBlockProgress(this.player.getId(), p_9278_, j);
         this.lastSentState = j;
      }

      return f;
   }

   private void debugLogging(BlockPos p_215126_, boolean p_215127_, int p_215128_, String p_215129_) {
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.debug("Server ACK {} {} {} {}", new Object[]{p_215128_, p_215126_, p_215127_, p_215129_});
      }
   }

   public void handleBlockBreakAction(BlockPos p_215120_, ServerboundPlayerActionPacket.Action p_215121_, Direction p_215122_, int p_215123_, int p_215124_) {
      if (!this.player.isWithinBlockInteractionRange(p_215120_, 1.0)) {
         this.debugLogging(p_215120_, false, p_215124_, "too far");
      } else if (p_215120_.getY() > p_215123_) {
         this.player.connection.send(new ClientboundBlockUpdatePacket(p_215120_, this.level.getBlockState(p_215120_)));
         this.debugLogging(p_215120_, false, p_215124_, "too high");
      } else if (p_215121_ == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
         if (!this.level.mayInteract(this.player, p_215120_)) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(p_215120_, this.level.getBlockState(p_215120_)));
            this.debugLogging(p_215120_, false, p_215124_, "may not interact");
            return;
         }

         if (this.player.getAbilities().instabuild) {
            this.destroyAndAck(p_215120_, p_215124_, "creative destroy");
            return;
         }

         if (this.player.blockActionRestricted(this.level, p_215120_, this.gameModeForPlayer)) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(p_215120_, this.level.getBlockState(p_215120_)));
            this.debugLogging(p_215120_, false, p_215124_, "block action restricted");
            return;
         }

         this.destroyProgressStart = this.gameTicks;
         float f = 1.0F;
         BlockState blockstate = this.level.getBlockState(p_215120_);
         if (!blockstate.isAir()) {
            EnchantmentHelper.onHitBlock(
               this.level,
               this.player.getMainHandItem(),
               this.player,
               this.player,
               EquipmentSlot.MAINHAND,
               Vec3.atCenterOf(p_215120_),
               blockstate,
               p_343810_ -> this.player.onEquippedItemBroken(p_343810_, EquipmentSlot.MAINHAND)
            );
            blockstate.attack(this.level, p_215120_, this.player);
            f = blockstate.getDestroyProgress(this.player, this.player.level(), p_215120_);
         }

         if (!blockstate.isAir() && f >= 1.0F) {
            this.destroyAndAck(p_215120_, p_215124_, "insta mine");
         } else {
            if (this.isDestroyingBlock) {
               this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, this.level.getBlockState(this.destroyPos)));
               this.debugLogging(p_215120_, false, p_215124_, "abort destroying since another started (client insta mine, server disagreed)");
            }

            this.isDestroyingBlock = true;
            this.destroyPos = p_215120_.immutable();
            int i = (int)(f * 10.0F);
            this.level.destroyBlockProgress(this.player.getId(), p_215120_, i);
            this.debugLogging(p_215120_, true, p_215124_, "actual start of destroying");
            this.lastSentState = i;
         }
      } else if (p_215121_ == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
         if (p_215120_.equals(this.destroyPos)) {
            int j = this.gameTicks - this.destroyProgressStart;
            BlockState blockstate1 = this.level.getBlockState(p_215120_);
            if (!blockstate1.isAir()) {
               float f1 = blockstate1.getDestroyProgress(this.player, this.player.level(), p_215120_) * (j + 1);
               if (f1 >= 0.7F) {
                  this.isDestroyingBlock = false;
                  this.level.destroyBlockProgress(this.player.getId(), p_215120_, -1);
                  this.destroyAndAck(p_215120_, p_215124_, "destroyed");
                  return;
               }

               if (!this.hasDelayedDestroy) {
                  this.isDestroyingBlock = false;
                  this.hasDelayedDestroy = true;
                  this.delayedDestroyPos = p_215120_;
                  this.delayedTickStart = this.destroyProgressStart;
               }
            }
         }

         this.debugLogging(p_215120_, true, p_215124_, "stopped destroying");
      } else if (p_215121_ == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
         this.isDestroyingBlock = false;
         if (!Objects.equals(this.destroyPos, p_215120_)) {
            LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, p_215120_);
            this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
            this.debugLogging(p_215120_, true, p_215124_, "aborted mismatched destroying");
         }

         this.level.destroyBlockProgress(this.player.getId(), p_215120_, -1);
         this.debugLogging(p_215120_, true, p_215124_, "aborted destroying");
      }
   }

   public void destroyAndAck(BlockPos p_215117_, int p_215118_, String p_215119_) {
      if (this.destroyBlock(p_215117_)) {
         this.debugLogging(p_215117_, true, p_215118_, p_215119_);
      } else {
         this.player.connection.send(new ClientboundBlockUpdatePacket(p_215117_, this.level.getBlockState(p_215117_)));
         this.debugLogging(p_215117_, false, p_215118_, p_215119_);
      }
   }

   public boolean destroyBlock(BlockPos p_9281_) {
      BlockState blockstate1 = this.level.getBlockState(p_9281_);
      if (!this.player.getMainHandItem().canDestroyBlock(blockstate1, this.level, p_9281_, this.player)) {
         return false;
      }

      BlockEntity blockentity = this.level.getBlockEntity(p_9281_);
      Block block = blockstate1.getBlock();
      if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
         this.level.sendBlockUpdated(p_9281_, blockstate1, blockstate1, 3);
         return false;
      }

      if (this.player.blockActionRestricted(this.level, p_9281_, this.gameModeForPlayer)) {
         return false;
      }

      BlockState blockstate = block.playerWillDestroy(this.level, p_9281_, blockstate1, this.player);
      boolean flag1 = this.level.removeBlock(p_9281_, false);
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.info("server broke {} {} -> {}", new Object[]{p_9281_, blockstate, this.level.getBlockState(p_9281_)});
      }

      if (flag1) {
         block.destroy(this.level, p_9281_, blockstate);
      }

      if (this.player.preventsBlockDrops()) {
         return true;
      }

      ItemStack itemstack = this.player.getMainHandItem();
      ItemStack itemstack1 = itemstack.copy();
      boolean flag = this.player.hasCorrectToolForDrops(blockstate);
      itemstack.mineBlock(this.level, blockstate, p_9281_, this.player);
      if (flag1 && flag) {
         block.playerDestroy(this.level, this.player, p_9281_, blockstate, blockentity, itemstack1);
      }

      return true;
   }

   public InteractionResult useItem(ServerPlayer p_9262_, Level p_9263_, ItemStack p_9264_, InteractionHand p_9265_) {
      if (this.gameModeForPlayer == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      }

      if (p_9262_.getCooldowns().isOnCooldown(p_9264_)) {
         return InteractionResult.PASS;
      }

      int i = p_9264_.getCount();
      int j = p_9264_.getDamageValue();
      InteractionResult interactionresult = p_9264_.use(p_9263_, p_9262_, p_9265_);
      ItemStack itemstack;
      if (interactionresult instanceof InteractionResult.Success interactionresult$success) {
         itemstack = Objects.requireNonNullElse(interactionresult$success.heldItemTransformedTo(), p_9262_.getItemInHand(p_9265_));
      } else {
         itemstack = p_9262_.getItemInHand(p_9265_);
      }

      if (itemstack == p_9264_ && itemstack.getCount() == i && itemstack.getUseDuration(p_9262_) <= 0 && itemstack.getDamageValue() == j) {
         return interactionresult;
      }

      if (interactionresult instanceof InteractionResult.Fail && itemstack.getUseDuration(p_9262_) > 0 && !p_9262_.isUsingItem()) {
         return interactionresult;
      }

      if (p_9264_ != itemstack) {
         p_9262_.setItemInHand(p_9265_, itemstack);
      }

      if (itemstack.isEmpty()) {
         p_9262_.setItemInHand(p_9265_, ItemStack.EMPTY);
      }

      if (!p_9262_.isUsingItem()) {
         p_9262_.inventoryMenu.sendAllDataToRemote();
      }

      return interactionresult;
   }

   public InteractionResult useItemOn(ServerPlayer p_9266_, Level p_9267_, ItemStack p_9268_, InteractionHand p_9269_, BlockHitResult p_9270_) {
      BlockPos blockpos = p_9270_.getBlockPos();
      BlockState blockstate = p_9267_.getBlockState(blockpos);
      if (!blockstate.getBlock().isEnabled(p_9267_.enabledFeatures())) {
         return InteractionResult.FAIL;
      }

      if (this.gameModeForPlayer == GameType.SPECTATOR) {
         MenuProvider menuprovider = blockstate.getMenuProvider(p_9267_, blockpos);
         if (menuprovider != null) {
            p_9266_.openMenu(menuprovider);
            return InteractionResult.CONSUME;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         boolean flag = !p_9266_.getMainHandItem().isEmpty() || !p_9266_.getOffhandItem().isEmpty();
         boolean flag1 = p_9266_.isSecondaryUseActive() && flag;
         ItemStack itemstack = p_9268_.copy();
         if (!flag1) {
            InteractionResult interactionresult = blockstate.useItemOn(p_9266_.getItemInHand(p_9269_), p_9267_, p_9266_, p_9269_, p_9270_);
            if (interactionresult.consumesAction()) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(p_9266_, blockpos, itemstack);
               return interactionresult;
            }

            if (interactionresult instanceof InteractionResult.TryEmptyHandInteraction && p_9269_ == InteractionHand.MAIN_HAND) {
               InteractionResult interactionresult1 = blockstate.useWithoutItem(p_9267_, p_9266_, p_9270_);
               if (interactionresult1.consumesAction()) {
                  CriteriaTriggers.DEFAULT_BLOCK_USE.trigger(p_9266_, blockpos);
                  return interactionresult1;
               }
            }
         }

         if (!p_9268_.isEmpty() && !p_9266_.getCooldowns().isOnCooldown(p_9268_)) {
            UseOnContext useoncontext = new UseOnContext(p_9266_, p_9269_, p_9270_);
            InteractionResult interactionresult2;
            if (p_9266_.hasInfiniteMaterials()) {
               int i = p_9268_.getCount();
               interactionresult2 = p_9268_.useOn(useoncontext);
               p_9268_.setCount(i);
            } else {
               interactionresult2 = p_9268_.useOn(useoncontext);
            }

            if (interactionresult2.consumesAction()) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(p_9266_, blockpos, itemstack);
            }

            return interactionresult2;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public void setLevel(ServerLevel p_9261_) {
      this.level = p_9261_;
   }
}
