package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class JukeboxBlock extends BaseEntityBlock {
   public static final MapCodec<JukeboxBlock> CODEC = simpleCodec(JukeboxBlock::new);
   public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

   @Override
   public MapCodec<JukeboxBlock> codec() {
      return CODEC;
   }

   protected JukeboxBlock(BlockBehaviour.Properties p_54257_) {
      super(p_54257_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HAS_RECORD, false));
   }

   @Override
   public void setPlacedBy(Level p_54264_, BlockPos p_54265_, BlockState p_54266_, @Nullable LivingEntity p_54267_, ItemStack p_54268_) {
      super.setPlacedBy(p_54264_, p_54265_, p_54266_, p_54267_, p_54268_);
      TypedEntityData<BlockEntityType<?>> typedentitydata = p_54268_.get(DataComponents.BLOCK_ENTITY_DATA);
      if (typedentitydata != null && typedentitydata.contains("RecordItem")) {
         p_54264_.setBlock(p_54265_, p_54266_.setValue(HAS_RECORD, true), 2);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_54281_, Level p_54282_, BlockPos p_54283_, Player p_54284_, BlockHitResult p_54286_) {
      if (p_54281_.getValue(HAS_RECORD) && p_54282_.getBlockEntity(p_54283_) instanceof JukeboxBlockEntity jukeboxblockentity) {
         jukeboxblockentity.popOutTheItem();
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_345342_, BlockState p_343906_, Level p_342356_, BlockPos p_342905_, Player p_343973_, InteractionHand p_345093_, BlockHitResult p_345506_
   ) {
      if (p_343906_.getValue(HAS_RECORD)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      ItemStack itemstack = p_343973_.getItemInHand(p_345093_);
      InteractionResult interactionresult = JukeboxPlayable.tryInsertIntoJukebox(p_342356_, p_342905_, itemstack, p_343973_);
      return !interactionresult.consumesAction() ? InteractionResult.TRY_WITH_EMPTY_HAND : interactionresult;
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_392618_, ServerLevel p_394416_, BlockPos p_394064_, boolean p_391409_) {
      Containers.updateNeighboursAfterDestroy(p_392618_, p_394416_, p_394064_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153451_, BlockState p_153452_) {
      return new JukeboxBlockEntity(p_153451_, p_153452_);
   }

   @Override
   public boolean isSignalSource(BlockState p_273404_) {
      return true;
   }

   @Override
   public int getSignal(BlockState p_272942_, BlockGetter p_273232_, BlockPos p_273524_, Direction p_272902_) {
      return p_273232_.getBlockEntity(p_273524_) instanceof JukeboxBlockEntity jukeboxblockentity && jukeboxblockentity.getSongPlayer().isPlaying() ? 15 : 0;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_54275_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_54277_, Level p_54278_, BlockPos p_54279_, Direction p_426670_) {
      return p_54278_.getBlockEntity(p_54279_) instanceof JukeboxBlockEntity jukeboxblockentity ? jukeboxblockentity.getComparatorOutput() : 0;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54294_) {
      p_54294_.add(HAS_RECORD);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_239682_, BlockState p_239683_, BlockEntityType<T> p_239684_) {
      return p_239683_.getValue(HAS_RECORD) ? createTickerHelper(p_239684_, BlockEntityType.JUKEBOX, JukeboxBlockEntity::tick) : null;
   }
}
