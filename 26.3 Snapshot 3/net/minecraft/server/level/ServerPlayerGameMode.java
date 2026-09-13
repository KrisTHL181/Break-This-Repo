package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.triggers.CriteriaTriggers;
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

   public ServerPlayerGameMode(final ServerPlayer player) {
      this.player = player;
      this.level = player.level();
   }

   public boolean changeGameModeForPlayer(final GameType gameModeForPlayer) {
      if (gameModeForPlayer == this.gameModeForPlayer) {
         return false;
      }

      Abilities abilities = this.player.getAbilities();
      this.setGameModeForPlayer(gameModeForPlayer, this.gameModeForPlayer);
      if (abilities.flying && gameModeForPlayer != GameType.SPECTATOR && this.isInRangeOfGround()) {
         abilities.flying = false;
      }

      this.player.onUpdateAbilities();
      this.level
         .getServer()
         .getPlayerList()
         .broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, this.player));
      this.level.updateSleepingPlayerList();
      if (gameModeForPlayer == GameType.CREATIVE) {
         this.player.resetCurrentImpulseContext();
      }

      return true;
   }

   protected void setGameModeForPlayer(final GameType gameModeForPlayer, final @Nullable GameType previousGameModeForPlayer) {
      this.previousGameModeForPlayer = previousGameModeForPlayer;
      this.gameModeForPlayer = gameModeForPlayer;
      Abilities abilities = this.player.getAbilities();
      gameModeForPlayer.updatePlayerAbilities(abilities);
   }

   private boolean isInRangeOfGround() {
      List<VoxelShape> clipping = Entity.collectAllColliders(this.player, this.level, this.player.getBoundingBox());
      return clipping.isEmpty() && this.player.getAvailableSpaceBelow(1.0) < 1.0;
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
         BlockState blockState = this.level.getBlockState(this.delayedDestroyPos);
         if (blockState.isAir()) {
            this.hasDelayedDestroy = false;
         } else {
            float destroyProgress = this.incrementDestroyProgress(blockState, this.delayedDestroyPos, this.delayedTickStart);
            if (destroyProgress >= 1.0F) {
               this.hasDelayedDestroy = false;
               this.destroyBlock(this.delayedDestroyPos);
            }
         }
      } else if (this.isDestroyingBlock) {
         BlockState blockState = this.level.getBlockState(this.destroyPos);
         if (blockState.isAir()) {
            this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
            this.lastSentState = -1;
            this.isDestroyingBlock = false;
         } else {
            this.incrementDestroyProgress(blockState, this.destroyPos, this.destroyProgressStart);
         }
      }
   }

   private float incrementDestroyProgress(final BlockState blockState, final BlockPos delayedDestroyPos, final int destroyStartTick) {
      int ticksSpentDestroying = this.gameTicks - destroyStartTick;
      float destroyProgress = blockState.getDestroyProgress(this.player, this.player.level(), delayedDestroyPos) * (ticksSpentDestroying + 1);
      int state = (int)(destroyProgress * 10.0F);
      if (state != this.lastSentState) {
         this.level.destroyBlockProgress(this.player.getId(), delayedDestroyPos, state);
         this.lastSentState = state;
      }

      return destroyProgress;
   }

   private void debugLogging(final BlockPos pos, final boolean allGood, final int sequence, final String message) {
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.debug("Server ACK {} {} {} {}", new Object[]{sequence, pos, allGood, message});
      }
   }

   public void handleBlockBreakAction(
      final BlockPos pos, final ServerboundPlayerActionPacket.Action action, final Direction direction, final int maxY, final int sequence
   ) {
      if (!this.player.isWithinBlockInteractionRange(pos, 1.0)) {
         this.debugLogging(pos, false, sequence, "too far");
      } else if (pos.getY() > maxY) {
         this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
         this.debugLogging(pos, false, sequence, "too high");
      } else {
         if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            if (this.level.getServer().isUnderSpawnProtection(this.level, pos, this.player)) {
               this.player.sendSpawnProtectionMessage(pos);
               this.debugLogging(pos, false, sequence, "spawn protection");
               return;
            }

            if (!this.level.mayInteract(this.player, pos)) {
               this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
               this.debugLogging(pos, false, sequence, "may not interact");
               return;
            }

            if (this.player.getAbilities().instabuild) {
               this.destroyAndAck(pos, sequence, "creative destroy");
               return;
            }

            if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
               this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
               this.debugLogging(pos, false, sequence, "block action restricted");
               return;
            }

            this.destroyProgressStart = this.gameTicks;
            float progress = 1.0F;
            BlockState blockState = this.level.getBlockState(pos);
            if (!blockState.isAir()) {
               EnchantmentHelper.onHitBlock(
                  this.level,
                  this.player.getMainHandItem(),
                  this.player,
                  this.player,
                  EquipmentSlot.MAINHAND,
                  Vec3.atCenterOf(pos),
                  blockState,
                  item -> this.player.onEquippedItemBroken(item, EquipmentSlot.MAINHAND)
               );
               blockState.attack(this.level, pos, this.player);
               progress = blockState.getDestroyProgress(this.player, this.player.level(), pos);
            }

            if (!blockState.isAir() && progress >= 1.0F) {
               this.destroyAndAck(pos, sequence, "insta mine");
            } else {
               if (this.isDestroyingBlock) {
                  this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, this.level.getBlockState(this.destroyPos)));
                  this.debugLogging(pos, false, sequence, "abort destroying since another started (client insta mine, server disagreed)");
               }

               this.isDestroyingBlock = true;
               this.destroyPos = pos.immutable();
               int state = (int)(progress * 10.0F);
               this.level.destroyBlockProgress(this.player.getId(), pos, state);
               this.debugLogging(pos, true, sequence, "actual start of destroying");
               this.lastSentState = state;
            }
         } else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            if (pos.equals(this.destroyPos)) {
               int ticksSpentDestroying = this.gameTicks - this.destroyProgressStart;
               BlockState state = this.level.getBlockState(pos);
               if (!state.isAir()) {
                  float destroyProgress = state.getDestroyProgress(this.player, this.player.level(), pos) * (ticksSpentDestroying + 1);
                  if (destroyProgress >= 0.7F) {
                     this.isDestroyingBlock = false;
                     this.level.destroyBlockProgress(this.player.getId(), pos, -1);
                     this.destroyAndAck(pos, sequence, "destroyed");
                     return;
                  }

                  if (!this.hasDelayedDestroy) {
                     this.isDestroyingBlock = false;
                     this.hasDelayedDestroy = true;
                     this.delayedDestroyPos = pos;
                     this.delayedTickStart = this.destroyProgressStart;
                  }
               }
            }

            this.debugLogging(pos, true, sequence, "stopped destroying");
         } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            this.isDestroyingBlock = false;
            if (!Objects.equals(this.destroyPos, pos)) {
               LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, pos);
               this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
               this.debugLogging(pos, true, sequence, "aborted mismatched destroying");
            }

            this.level.destroyBlockProgress(this.player.getId(), pos, -1);
            this.debugLogging(pos, true, sequence, "aborted destroying");
         }
      }
   }

   public void destroyAndAck(final BlockPos pos, final int sequence, final String exitId) {
      if (this.destroyBlock(pos)) {
         this.debugLogging(pos, true, sequence, exitId);
      } else {
         this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
         this.debugLogging(pos, false, sequence, exitId);
      }
   }

   public boolean destroyBlock(final BlockPos pos) {
      BlockState state = this.level.getBlockState(pos);
      if (!this.player.getMainHandItem().canDestroyBlock(state, this.level, pos, this.player)) {
         return false;
      }

      BlockEntity blockEntity = this.level.getBlockEntity(pos);
      Block block = state.getBlock();
      if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
         this.level.sendBlockUpdated(pos, state, state, 3);
         return false;
      }

      if (this.player.blockActionRestricted(this.level, pos, this.gameModeForPlayer)) {
         return false;
      }

      BlockState adjustedState = block.playerWillDestroy(this.level, pos, state, this.player);
      boolean changed = this.level.removeBlock(pos, false);
      if (SharedConstants.DEBUG_BLOCK_BREAK) {
         LOGGER.info("server broke {} {} -> {}", new Object[]{pos, adjustedState, this.level.getBlockState(pos)});
      }

      if (changed) {
         block.destroy(this.level, pos, adjustedState);
      }

      if (this.player.preventsBlockDrops()) {
         return true;
      }

      ItemStack itemStack = this.player.getMainHandItem();
      ItemStack destroyedWith = itemStack.copy();
      boolean canDestroy = this.player.hasCorrectToolForDrops(adjustedState);
      itemStack.mineBlock(this.level, adjustedState, pos, this.player);
      if (changed && canDestroy) {
         block.playerDestroy(this.level, this.player, pos, adjustedState, blockEntity, destroyedWith);
      }

      return true;
   }

   public InteractionResult useItem(final ServerPlayer player, final Level level, final ItemStack itemStack, final InteractionHand hand) {
      if (this.gameModeForPlayer == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      }

      if (player.getCooldowns().isOnCooldown(itemStack)) {
         return InteractionResult.PASS;
      }

      int oldCount = itemStack.getCount();
      int oldDamage = itemStack.getDamageValue();
      InteractionResult result = itemStack.use(level, player, hand);
      ItemStack resultStack;
      if (result instanceof InteractionResult.Success success) {
         resultStack = Objects.requireNonNullElse(success.heldItemTransformedTo(), player.getItemInHand(hand));
      } else {
         resultStack = player.getItemInHand(hand);
      }

      if (resultStack == itemStack
         && resultStack.getCount() == oldCount
         && resultStack.getUseDuration(player) <= 0
         && resultStack.getDamageValue() == oldDamage) {
         return result;
      }

      if (result instanceof InteractionResult.Fail && resultStack.getUseDuration(player) > 0 && !player.isUsingItem()) {
         return result;
      }

      if (itemStack != resultStack) {
         player.setItemInHand(hand, resultStack);
      }

      if (resultStack.isEmpty()) {
         player.setItemInHand(hand, ItemStack.EMPTY);
      }

      if (!player.isUsingItem()) {
         player.inventoryMenu.sendAllDataToRemote();
      }

      return result;
   }

   public InteractionResult useItemOn(
      final ServerPlayer player, final Level level, final ItemStack itemStack, final InteractionHand hand, final BlockHitResult hitResult
   ) {
      BlockPos pos = hitResult.getBlockPos();
      BlockState state = level.getBlockState(pos);
      if (!state.getBlock().isEnabled(level.enabledFeatures())) {
         return InteractionResult.FAIL;
      }

      if (this.gameModeForPlayer == GameType.SPECTATOR) {
         MenuProvider menuProvider = state.getMenuProvider(level, pos);
         if (menuProvider != null) {
            player.openMenu(menuProvider);
            return InteractionResult.CONSUME;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         boolean haveSomethingInOurHands = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
         boolean suppressUsingBlock = player.isSecondaryUseActive() && haveSomethingInOurHands;
         ItemStack usedItemStack = itemStack.copy();
         if (!suppressUsingBlock) {
            InteractionResult itemUse = state.useItemOn(player.getItemInHand(hand), level, player, hand, hitResult);
            if (itemUse.consumesAction()) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, usedItemStack);
               return itemUse;
            }

            if (itemUse instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
               InteractionResult use = state.useWithoutItem(level, player, hitResult);
               if (use.consumesAction()) {
                  CriteriaTriggers.DEFAULT_BLOCK_USE.trigger(player, pos);
                  return use;
               }
            }
         }

         if (!itemStack.isEmpty() && !player.getCooldowns().isOnCooldown(itemStack)) {
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            InteractionResult success;
            if (player.hasInfiniteMaterials()) {
               int count = itemStack.getCount();
               success = itemStack.useOn(context);
               itemStack.setCount(count);
            } else {
               success = itemStack.useOn(context);
            }

            if (success.consumesAction()) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, usedItemStack);
            }

            return success;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public void setLevel(final ServerLevel newLevel) {
      this.level = newLevel;
   }
}
