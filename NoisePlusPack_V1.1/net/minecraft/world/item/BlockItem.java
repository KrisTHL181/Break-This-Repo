package net.minecraft.world.item;

import java.util.Map;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class BlockItem extends Item {
   @Deprecated
   private final Block block;

   public BlockItem(Block p_40565_, Item.Properties p_40566_) {
      super(p_40566_);
      this.block = p_40565_;
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40581_) {
      InteractionResult interactionresult = this.place(new BlockPlaceContext(p_40581_));
      return !interactionresult.consumesAction() && p_40581_.getItemInHand().has(DataComponents.CONSUMABLE)
         ? super.use(p_40581_.getLevel(), p_40581_.getPlayer(), p_40581_.getHand())
         : interactionresult;
   }

   public InteractionResult place(BlockPlaceContext p_40577_) {
      if (!this.getBlock().isEnabled(p_40577_.getLevel().enabledFeatures())) {
         return InteractionResult.FAIL;
      }

      if (!p_40577_.canPlace()) {
         return InteractionResult.FAIL;
      }

      BlockPlaceContext blockplacecontext = this.updatePlacementContext(p_40577_);
      if (blockplacecontext == null) {
         return InteractionResult.FAIL;
      }

      BlockState blockstate = this.getPlacementState(blockplacecontext);
      if (blockstate == null) {
         return InteractionResult.FAIL;
      }

      if (!this.placeBlock(blockplacecontext, blockstate)) {
         return InteractionResult.FAIL;
      }

      BlockPos blockpos = blockplacecontext.getClickedPos();
      Level level = blockplacecontext.getLevel();
      Player player = blockplacecontext.getPlayer();
      ItemStack itemstack = blockplacecontext.getItemInHand();
      BlockState blockstate1 = level.getBlockState(blockpos);
      if (blockstate1.is(blockstate.getBlock())) {
         blockstate1 = this.updateBlockStateFromTag(blockpos, level, itemstack, blockstate1);
         this.updateCustomBlockEntityTag(blockpos, level, player, itemstack, blockstate1);
         updateBlockEntityComponents(level, blockpos, itemstack);
         blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);
         if (player instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
         }
      }

      SoundType soundtype = blockstate1.getSoundType();
      level.playSound(player, blockpos, this.getPlaceSound(blockstate1), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
      level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));
      itemstack.consume(1, player);
      return InteractionResult.SUCCESS;
   }

   protected SoundEvent getPlaceSound(BlockState p_40588_) {
      return p_40588_.getSoundType().getPlaceSound();
   }

   public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext p_40609_) {
      return p_40609_;
   }

   private static void updateBlockEntityComponents(Level p_333389_, BlockPos p_335748_, ItemStack p_334527_) {
      BlockEntity blockentity = p_333389_.getBlockEntity(p_335748_);
      if (blockentity != null) {
         blockentity.applyComponentsFromItemStack(p_334527_);
         blockentity.setChanged();
      }
   }

   protected boolean updateCustomBlockEntityTag(BlockPos p_40597_, Level p_40598_, @Nullable Player p_40599_, ItemStack p_40600_, BlockState p_40601_) {
      return updateCustomBlockEntityTag(p_40598_, p_40599_, p_40597_, p_40600_);
   }

   protected @Nullable BlockState getPlacementState(BlockPlaceContext p_40613_) {
      BlockState blockstate = this.getBlock().getStateForPlacement(p_40613_);
      return blockstate != null && this.canPlace(p_40613_, blockstate) ? blockstate : null;
   }

   private BlockState updateBlockStateFromTag(BlockPos p_40603_, Level p_40604_, ItemStack p_40605_, BlockState p_40606_) {
      BlockItemStateProperties blockitemstateproperties = p_40605_.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
      if (blockitemstateproperties.isEmpty()) {
         return p_40606_;
      }

      BlockState blockstate = blockitemstateproperties.apply(p_40606_);
      if (blockstate != p_40606_) {
         p_40604_.setBlock(p_40603_, blockstate, 2);
      }

      return blockstate;
   }

   protected boolean canPlace(BlockPlaceContext p_40611_, BlockState p_40612_) {
      Player player = p_40611_.getPlayer();
      return (!this.mustSurvive() || p_40612_.canSurvive(p_40611_.getLevel(), p_40611_.getClickedPos()))
         && p_40611_.getLevel().isUnobstructed(p_40612_, p_40611_.getClickedPos(), CollisionContext.placementContext(player));
   }

   protected boolean mustSurvive() {
      return true;
   }

   protected boolean placeBlock(BlockPlaceContext p_40578_, BlockState p_40579_) {
      return p_40578_.getLevel().setBlock(p_40578_.getClickedPos(), p_40579_, 11);
   }

   public static boolean updateCustomBlockEntityTag(Level p_40583_, @Nullable Player p_40584_, BlockPos p_40585_, ItemStack p_40586_) {
      if (p_40583_.isClientSide()) {
         return false;
      }

      TypedEntityData<BlockEntityType<?>> typedentitydata = p_40586_.get(DataComponents.BLOCK_ENTITY_DATA);
      if (typedentitydata != null) {
         BlockEntity blockentity = p_40583_.getBlockEntity(p_40585_);
         if (blockentity != null) {
            BlockEntityType<?> blockentitytype = blockentity.getType();
            if (blockentitytype != typedentitydata.type()) {
               return false;
            }

            if (blockentitytype.onlyOpCanSetNbt() && (p_40584_ == null || !p_40584_.canUseGameMasterBlocks())) {
               return false;
            }

            return typedentitydata.loadInto(blockentity, p_40583_.registryAccess());
         }
      }

      return false;
   }

   @Override
   public boolean shouldPrintOpWarning(ItemStack p_377642_, @Nullable Player p_377092_) {
      if (p_377092_ != null && p_377092_.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
         TypedEntityData<BlockEntityType<?>> typedentitydata = p_377642_.get(DataComponents.BLOCK_ENTITY_DATA);
         if (typedentitydata != null) {
            return typedentitydata.type().onlyOpCanSetNbt();
         }
      }

      return false;
   }

   public Block getBlock() {
      return this.block;
   }

   public void registerBlocks(Map<Block, Item> p_40607_, Item p_40608_) {
      p_40607_.put(this.getBlock(), p_40608_);
   }

   @Override
   public boolean canFitInsideContainerItems() {
      return !(this.getBlock() instanceof ShulkerBoxBlock);
   }

   @Override
   public void onDestroyed(ItemEntity p_150700_) {
      ItemContainerContents itemcontainercontents = p_150700_.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
      if (itemcontainercontents != null) {
         ItemUtils.onContainerDestroyed(p_150700_, itemcontainercontents.nonEmptyItemsCopy());
      }
   }

   public static void setBlockEntityData(ItemStack p_186339_, BlockEntityType<?> p_186340_, TagValueOutput p_406086_) {
      p_406086_.discard("id");
      if (p_406086_.isEmpty()) {
         p_186339_.remove(DataComponents.BLOCK_ENTITY_DATA);
      } else {
         BlockEntity.addEntityType(p_406086_, p_186340_);
         p_186339_.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(p_186340_, p_406086_.buildResult()));
      }
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.getBlock().requiredFeatures();
   }
}
