package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShelfBlock extends BaseEntityBlock implements SelectableSlotContainer, SideChainPartBlock, SimpleWaterloggedBlock {
   public static final MapCodec<ShelfBlock> CODEC = simpleCodec(ShelfBlock::new);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART = BlockStateProperties.SIDE_CHAIN_PART;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(
      Shapes.or(Block.box(0.0, 12.0, 11.0, 16.0, 16.0, 13.0), Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0), Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 13.0))
   );

   @Override
   public MapCodec<ShelfBlock> codec() {
      return CODEC;
   }

   public ShelfBlock(BlockBehaviour.Properties p_430841_) {
      super(p_430841_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED)
            .setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected VoxelShape getShape(BlockState p_423893_, BlockGetter p_423944_, BlockPos p_426956_, CollisionContext p_430064_) {
      return SHAPES.get(p_423893_.getValue(FACING));
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_430857_) {
      return true;
   }

   @Override
   protected boolean isPathfindable(BlockState p_431447_, PathComputationType p_429852_) {
      return p_429852_ == PathComputationType.WATER && p_431447_.getFluidState().is(FluidTags.WATER);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_423209_, BlockState p_423790_) {
      return new ShelfBlockEntity(p_423209_, p_423790_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_423094_) {
      p_423094_.add(FACING, POWERED, SIDE_CHAIN_PART, WATERLOGGED);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_426195_, ServerLevel p_427008_, BlockPos p_431070_, boolean p_423318_) {
      Containers.updateNeighboursAfterDestroy(p_426195_, p_427008_, p_431070_);
      this.updateNeighborsAfterPoweringDown(p_427008_, p_431070_, p_426195_);
   }

   @Override
   protected void neighborChanged(
      BlockState p_429770_, Level p_429118_, BlockPos p_425994_, Block p_428186_, @Nullable Orientation p_427876_, boolean p_427843_
   ) {
      if (!p_429118_.isClientSide()) {
         boolean flag = p_429118_.hasNeighborSignal(p_425994_);
         if (p_429770_.getValue(POWERED) != flag) {
            BlockState blockstate = p_429770_.setValue(POWERED, flag);
            if (!flag) {
               blockstate = blockstate.setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED);
            }

            p_429118_.setBlock(p_425994_, blockstate, 3);
            this.playSound(p_429118_, p_425994_, flag ? SoundEvents.SHELF_ACTIVATE : SoundEvents.SHELF_DEACTIVATE);
            p_429118_.gameEvent(flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, p_425994_, GameEvent.Context.of(blockstate));
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_426850_) {
      FluidState fluidstate = p_426850_.getLevel().getFluidState(p_426850_.getClickedPos());
      return this.defaultBlockState()
         .setValue(FACING, p_426850_.getHorizontalDirection().getOpposite())
         .setValue(POWERED, p_426850_.getLevel().hasNeighborSignal(p_426850_.getClickedPos()))
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   @Override
   public BlockState rotate(BlockState p_431351_, Rotation p_426103_) {
      return p_431351_.setValue(FACING, p_426103_.rotate(p_431351_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_430523_, Mirror p_429746_) {
      return p_430523_.rotate(p_429746_.getRotation(p_430523_.getValue(FACING)));
   }

   @Override
   public int getRows() {
      return 1;
   }

   @Override
   public int getColumns() {
      return 3;
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_431509_, BlockState p_425013_, Level p_425317_, BlockPos p_430171_, Player p_429634_, InteractionHand p_431676_, BlockHitResult p_424207_
   ) {
      if (p_425317_.getBlockEntity(p_430171_) instanceof ShelfBlockEntity shelfblockentity && !p_431676_.equals(InteractionHand.OFF_HAND)) {
         OptionalInt optionalint = this.getHitSlot(p_424207_, p_425013_.getValue(FACING));
         if (optionalint.isEmpty()) {
            return InteractionResult.PASS;
         }

         Inventory inventory = p_429634_.getInventory();
         if (p_425317_.isClientSide()) {
            return (InteractionResult)(inventory.getSelectedItem().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         }

         if (!p_425013_.getValue(POWERED)) {
            boolean flag1 = swapSingleItem(p_431509_, p_429634_, shelfblockentity, optionalint.getAsInt(), inventory);
            if (flag1) {
               this.playSound(p_425317_, p_430171_, p_431509_.isEmpty() ? SoundEvents.SHELF_TAKE_ITEM : SoundEvents.SHELF_SINGLE_SWAP);
            } else {
               if (p_431509_.isEmpty()) {
                  return InteractionResult.PASS;
               }

               this.playSound(p_425317_, p_430171_, SoundEvents.SHELF_PLACE_ITEM);
            }

            return InteractionResult.SUCCESS.heldItemTransformedTo(p_431509_);
         } else {
            ItemStack itemstack = inventory.getSelectedItem();
            boolean flag = this.swapHotbar(p_425317_, p_430171_, inventory);
            if (!flag) {
               return InteractionResult.CONSUME;
            }

            this.playSound(p_425317_, p_430171_, SoundEvents.SHELF_MULTI_SWAP);
            return itemstack == inventory.getSelectedItem()
               ? InteractionResult.SUCCESS
               : InteractionResult.SUCCESS.heldItemTransformedTo(inventory.getSelectedItem());
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static boolean swapSingleItem(ItemStack p_426834_, Player p_426407_, ShelfBlockEntity p_426736_, int p_428101_, Inventory p_428775_) {
      ItemStack itemstack = p_426736_.swapItemNoUpdate(p_428101_, p_426834_);
      ItemStack itemstack1 = p_426407_.hasInfiniteMaterials() && itemstack.isEmpty() ? p_426834_.copy() : itemstack;
      p_428775_.setItem(p_428775_.getSelectedSlot(), itemstack1);
      p_428775_.setChanged();
      p_426736_.setChanged(
         itemstack1.has(DataComponents.USE_EFFECTS) && !itemstack1.get(DataComponents.USE_EFFECTS).interactVibrations() ? null : GameEvent.ITEM_INTERACT_FINISH
      );
      return !itemstack.isEmpty();
   }

   private boolean swapHotbar(Level p_428643_, BlockPos p_427378_, Inventory p_429740_) {
      List<BlockPos> list = this.getAllBlocksConnectedTo(p_428643_, p_427378_);
      if (list.isEmpty()) {
         return false;
      }

      boolean flag = false;

      for (int i = 0; i < list.size(); i++) {
         ShelfBlockEntity shelfblockentity = (ShelfBlockEntity)p_428643_.getBlockEntity(list.get(i));
         if (shelfblockentity != null) {
            for (int j = 0; j < shelfblockentity.getContainerSize(); j++) {
               int k = 9 - (list.size() - i) * shelfblockentity.getContainerSize() + j;
               if (k >= 0 && k <= p_429740_.getContainerSize()) {
                  ItemStack itemstack = p_429740_.removeItemNoUpdate(k);
                  ItemStack itemstack1 = shelfblockentity.swapItemNoUpdate(j, itemstack);
                  if (!itemstack.isEmpty() || !itemstack1.isEmpty()) {
                     p_429740_.setItem(k, itemstack1);
                     flag = true;
                  }
               }
            }

            p_429740_.setChanged();
            shelfblockentity.setChanged(GameEvent.ENTITY_INTERACT);
         }
      }

      return flag;
   }

   @Override
   public SideChainPart getSideChainPart(BlockState p_427881_) {
      return p_427881_.getValue(SIDE_CHAIN_PART);
   }

   @Override
   public BlockState setSideChainPart(BlockState p_428651_, SideChainPart p_427544_) {
      return p_428651_.setValue(SIDE_CHAIN_PART, p_427544_);
   }

   @Override
   public Direction getFacing(BlockState p_423704_) {
      return p_423704_.getValue(FACING);
   }

   @Override
   public boolean isConnectable(BlockState p_430952_) {
      return p_430952_.is(BlockTags.WOODEN_SHELVES) && p_430952_.hasProperty(POWERED) && p_430952_.getValue(POWERED);
   }

   @Override
   public int getMaxChainLength() {
      return 3;
   }

   @Override
   protected void onPlace(BlockState p_425171_, Level p_429879_, BlockPos p_424425_, BlockState p_427738_, boolean p_422934_) {
      if (p_425171_.getValue(POWERED)) {
         this.updateSelfAndNeighborsOnPoweringUp(p_429879_, p_424425_, p_425171_, p_427738_);
      } else {
         this.updateNeighborsAfterPoweringDown(p_429879_, p_424425_, p_425171_);
      }
   }

   private void playSound(LevelAccessor p_424912_, BlockPos p_427653_, SoundEvent p_422508_) {
      p_424912_.playSound(null, p_427653_, p_422508_, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   protected FluidState getFluidState(BlockState p_429636_) {
      return p_429636_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_429636_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_426681_,
      LevelReader p_426222_,
      ScheduledTickAccess p_430140_,
      BlockPos p_426828_,
      Direction p_430398_,
      BlockPos p_422990_,
      BlockState p_424526_,
      RandomSource p_424827_
   ) {
      if (p_426681_.getValue(WATERLOGGED)) {
         p_430140_.scheduleTick(p_426828_, Fluids.WATER, Fluids.WATER.getTickDelay(p_426222_));
      }

      return super.updateShape(p_426681_, p_426222_, p_430140_, p_426828_, p_430398_, p_422990_, p_424526_, p_424827_);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_429252_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_429712_, Level p_424341_, BlockPos p_424278_, Direction p_423972_) {
      if (p_424341_.isClientSide()) {
         return 0;
      } else if (p_423972_ != p_429712_.getValue(FACING).getOpposite()) {
         return 0;
      } else if (p_424341_.getBlockEntity(p_424278_) instanceof ShelfBlockEntity shelfblockentity) {
         int k = shelfblockentity.getItem(0).isEmpty() ? 0 : 1;
         int i = shelfblockentity.getItem(1).isEmpty() ? 0 : 1;
         int j = shelfblockentity.getItem(2).isEmpty() ? 0 : 1;
         return k | i << 1 | j << 2;
      } else {
         return 0;
      }
   }
}
