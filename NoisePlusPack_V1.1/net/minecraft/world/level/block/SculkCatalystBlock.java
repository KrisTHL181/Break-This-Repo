package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class SculkCatalystBlock extends BaseEntityBlock {
   public static final MapCodec<SculkCatalystBlock> CODEC = simpleCodec(SculkCatalystBlock::new);
   public static final BooleanProperty PULSE = BlockStateProperties.BLOOM;
   private final IntProvider xpRange = ConstantInt.of(5);

   @Override
   public MapCodec<SculkCatalystBlock> codec() {
      return CODEC;
   }

   public SculkCatalystBlock(BlockBehaviour.Properties p_222090_) {
      super(p_222090_);
      this.registerDefaultState(this.stateDefinition.any().setValue(PULSE, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_222115_) {
      p_222115_.add(PULSE);
   }

   @Override
   protected void tick(BlockState p_222104_, ServerLevel p_222105_, BlockPos p_222106_, RandomSource p_222107_) {
      if (p_222104_.getValue(PULSE)) {
         p_222105_.setBlock(p_222106_, p_222104_.setValue(PULSE, false), 3);
      }
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_222117_, BlockState p_222118_) {
      return new SculkCatalystBlockEntity(p_222117_, p_222118_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_222100_, BlockState p_222101_, BlockEntityType<T> p_222102_) {
      return p_222100_.isClientSide() ? null : createTickerHelper(p_222102_, BlockEntityType.SCULK_CATALYST, SculkCatalystBlockEntity::serverTick);
   }

   @Override
   protected void spawnAfterBreak(BlockState p_222109_, ServerLevel p_222110_, BlockPos p_222111_, ItemStack p_222112_, boolean p_222113_) {
      super.spawnAfterBreak(p_222109_, p_222110_, p_222111_, p_222112_, p_222113_);
      if (p_222113_) {
         this.tryDropExperience(p_222110_, p_222111_, p_222112_, this.xpRange);
      }
   }
}
