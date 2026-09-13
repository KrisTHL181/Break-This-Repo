package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerBlock extends BaseEntityBlock {
   public static final MapCodec<TrialSpawnerBlock> CODEC = simpleCodec(TrialSpawnerBlock::new);
   public static final EnumProperty<TrialSpawnerState> STATE = BlockStateProperties.TRIAL_SPAWNER_STATE;
   public static final BooleanProperty OMINOUS = BlockStateProperties.OMINOUS;

   @Override
   public MapCodec<TrialSpawnerBlock> codec() {
      return CODEC;
   }

   public TrialSpawnerBlock(BlockBehaviour.Properties p_309401_) {
      super(p_309401_);
      this.registerDefaultState(this.stateDefinition.any().setValue(STATE, TrialSpawnerState.INACTIVE).setValue(OMINOUS, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_312861_) {
      p_312861_.add(STATE, OMINOUS);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_310402_, BlockState p_309509_) {
      return new TrialSpawnerBlockEntity(p_310402_, p_309509_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_312042_, BlockState p_312838_, BlockEntityType<T> p_310465_) {
      return p_312042_ instanceof ServerLevel serverlevel
         ? createTickerHelper(
            p_310465_,
            BlockEntityType.TRIAL_SPAWNER,
            (p_327270_, p_327271_, p_327272_, p_327273_) -> p_327273_.getTrialSpawner()
               .tickServer(serverlevel, p_327271_, p_327272_.getOptionalValue(BlockStateProperties.OMINOUS).orElse(false))
         )
         : createTickerHelper(
            p_310465_,
            BlockEntityType.TRIAL_SPAWNER,
            (p_327274_, p_327275_, p_327276_, p_327277_) -> p_327277_.getTrialSpawner()
               .tickClient(p_327274_, p_327275_, p_327276_.getOptionalValue(BlockStateProperties.OMINOUS).orElse(false))
         );
   }
}
