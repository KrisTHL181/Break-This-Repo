package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSkullBlock extends BaseEntityBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private final SkullBlock.Type type;

   public AbstractSkullBlock(SkullBlock.Type p_48745_, BlockBehaviour.Properties p_48746_) {
      super(p_48746_);
      this.type = p_48745_;
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
   }

   @Override
   protected abstract MapCodec<? extends AbstractSkullBlock> codec();

   @Override
   public BlockEntity newBlockEntity(BlockPos p_151996_, BlockState p_151997_) {
      return new SkullBlockEntity(p_151996_, p_151997_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_151992_, BlockState p_151993_, BlockEntityType<T> p_151994_) {
      if (p_151992_.isClientSide()) {
         boolean flag = p_151993_.is(Blocks.DRAGON_HEAD)
            || p_151993_.is(Blocks.DRAGON_WALL_HEAD)
            || p_151993_.is(Blocks.PIGLIN_HEAD)
            || p_151993_.is(Blocks.PIGLIN_WALL_HEAD);
         if (flag) {
            return createTickerHelper(p_151994_, BlockEntityType.SKULL, SkullBlockEntity::animation);
         }
      }

      return null;
   }

   public SkullBlock.Type getType() {
      return this.type;
   }

   @Override
   protected boolean isPathfindable(BlockState p_48750_, PathComputationType p_48753_) {
      return false;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_300725_) {
      p_300725_.add(POWERED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_300531_) {
      return this.defaultBlockState().setValue(POWERED, p_300531_.getLevel().hasNeighborSignal(p_300531_.getClickedPos()));
   }

   @Override
   protected void neighborChanged(
      BlockState p_299472_, Level p_297575_, BlockPos p_300147_, Block p_299103_, @Nullable Orientation p_369812_, boolean p_299691_
   ) {
      if (!p_297575_.isClientSide()) {
         boolean flag = p_297575_.hasNeighborSignal(p_300147_);
         if (flag != p_299472_.getValue(POWERED)) {
            p_297575_.setBlock(p_300147_, p_299472_.setValue(POWERED, flag), 2);
         }
      }
   }
}
