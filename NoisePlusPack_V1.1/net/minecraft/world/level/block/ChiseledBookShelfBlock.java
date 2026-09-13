package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ChiseledBookShelfBlock extends BaseEntityBlock implements SelectableSlotContainer {
   public static final MapCodec<ChiseledBookShelfBlock> CODEC = simpleCodec(ChiseledBookShelfBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty SLOT_0_OCCUPIED = BlockStateProperties.SLOT_0_OCCUPIED;
   public static final BooleanProperty SLOT_1_OCCUPIED = BlockStateProperties.SLOT_1_OCCUPIED;
   public static final BooleanProperty SLOT_2_OCCUPIED = BlockStateProperties.SLOT_2_OCCUPIED;
   public static final BooleanProperty SLOT_3_OCCUPIED = BlockStateProperties.SLOT_3_OCCUPIED;
   public static final BooleanProperty SLOT_4_OCCUPIED = BlockStateProperties.SLOT_4_OCCUPIED;
   public static final BooleanProperty SLOT_5_OCCUPIED = BlockStateProperties.SLOT_5_OCCUPIED;
   private static final int MAX_BOOKS_IN_STORAGE = 6;
   private static final int BOOKS_PER_ROW = 3;
   public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = List.of(
      SLOT_0_OCCUPIED, SLOT_1_OCCUPIED, SLOT_2_OCCUPIED, SLOT_3_OCCUPIED, SLOT_4_OCCUPIED, SLOT_5_OCCUPIED
   );

   @Override
   public MapCodec<ChiseledBookShelfBlock> codec() {
      return CODEC;
   }

   @Override
   public int getRows() {
      return 2;
   }

   @Override
   public int getColumns() {
      return 3;
   }

   public ChiseledBookShelfBlock(BlockBehaviour.Properties p_249989_) {
      super(p_249989_);
      BlockState blockstate = this.stateDefinition.any().setValue(FACING, Direction.NORTH);

      for (BooleanProperty booleanproperty : SLOT_OCCUPIED_PROPERTIES) {
         blockstate = blockstate.setValue(booleanproperty, false);
      }

      this.registerDefaultState(blockstate);
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_336113_, BlockState p_329797_, Level p_331003_, BlockPos p_335104_, Player p_334454_, InteractionHand p_336011_, BlockHitResult p_329086_
   ) {
      if (p_331003_.getBlockEntity(p_335104_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity) {
         if (!p_336113_.is(ItemTags.BOOKSHELF_BOOKS)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }

         OptionalInt optionalint = this.getHitSlot(p_329086_, p_329797_.getValue(FACING));
         if (optionalint.isEmpty()) {
            return InteractionResult.PASS;
         }

         if (p_329797_.getValue(SLOT_OCCUPIED_PROPERTIES.get(optionalint.getAsInt()))) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }

         addBook(p_331003_, p_335104_, p_334454_, chiseledbookshelfblockentity, p_336113_, optionalint.getAsInt());
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_335003_, Level p_333933_, BlockPos p_333604_, Player p_334275_, BlockHitResult p_334482_) {
      if (p_333933_.getBlockEntity(p_333604_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity) {
         OptionalInt optionalint = this.getHitSlot(p_334482_, p_335003_.getValue(FACING));
         if (optionalint.isEmpty()) {
            return InteractionResult.PASS;
         }

         if (!p_335003_.getValue(SLOT_OCCUPIED_PROPERTIES.get(optionalint.getAsInt()))) {
            return InteractionResult.CONSUME;
         }

         removeBook(p_333933_, p_333604_, p_334275_, chiseledbookshelfblockentity, optionalint.getAsInt());
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   private static void addBook(
      Level p_262592_, BlockPos p_262669_, Player p_262572_, ChiseledBookShelfBlockEntity p_262606_, ItemStack p_262587_, int p_262692_
   ) {
      if (!p_262592_.isClientSide()) {
         p_262572_.awardStat(Stats.ITEM_USED.get(p_262587_.getItem()));
         SoundEvent soundevent = p_262587_.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_INSERT;
         p_262606_.setItem(p_262692_, p_262587_.consumeAndReturn(1, p_262572_));
         p_262592_.playSound(null, p_262669_, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   private static void removeBook(Level p_262654_, BlockPos p_262601_, Player p_262636_, ChiseledBookShelfBlockEntity p_262605_, int p_262673_) {
      if (!p_262654_.isClientSide()) {
         ItemStack itemstack = p_262605_.removeItem(p_262673_, 1);
         SoundEvent soundevent = itemstack.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
         p_262654_.playSound(null, p_262601_, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!p_262636_.getInventory().add(itemstack)) {
            p_262636_.drop(itemstack, false);
         }

         p_262654_.gameEvent(p_262636_, GameEvent.BLOCK_CHANGE, p_262601_);
      }
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_250440_, BlockState p_248729_) {
      return new ChiseledBookShelfBlockEntity(p_250440_, p_248729_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_250973_) {
      p_250973_.add(FACING);
      SLOT_OCCUPIED_PROPERTIES.forEach(p_261456_ -> p_250973_.add(p_261456_));
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_394831_, ServerLevel p_397362_, BlockPos p_395293_, boolean p_394170_) {
      Containers.updateNeighboursAfterDestroy(p_394831_, p_397362_, p_395293_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_251318_) {
      return this.defaultBlockState().setValue(FACING, p_251318_.getHorizontalDirection().getOpposite());
   }

   @Override
   public BlockState rotate(BlockState p_288975_, Rotation p_288993_) {
      return p_288975_.setValue(FACING, p_288993_.rotate(p_288975_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_289000_, Mirror p_288962_) {
      return p_289000_.rotate(p_288962_.getRotation(p_289000_.getValue(FACING)));
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_249302_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_249192_, Level p_252207_, BlockPos p_248999_, Direction p_431360_) {
      if (p_252207_.isClientSide()) {
         return 0;
      } else {
         return p_252207_.getBlockEntity(p_248999_) instanceof ChiseledBookShelfBlockEntity chiseledbookshelfblockentity
            ? chiseledbookshelfblockentity.getLastInteractedSlot() + 1
            : 0;
      }
   }
}
