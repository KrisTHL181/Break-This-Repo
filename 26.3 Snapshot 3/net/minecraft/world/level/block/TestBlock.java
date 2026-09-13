package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final EnumProperty<TestBlockMode> MODE = BlockStateProperties.TEST_BLOCK_MODE;

   public TestBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new TestBlockEntity(worldPosition, blockState);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockItemStateProperties stateProperties = context.getItemInHand().get(DataComponents.BLOCK_STATE);
      BlockState toPlace = this.defaultBlockState();
      if (stateProperties != null) {
         TestBlockMode mode = stateProperties.get(MODE);
         if (mode != null) {
            toPlace = toPlace.setValue(MODE, mode);
         }
      }

      return toPlace;
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(MODE);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof TestBlockEntity testBlockEntity) {
         if (!player.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
         }

         if (level.isClientSide()) {
            player.openTestBlock(testBlockEntity);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      TestBlockEntity testBlock = getServerTestBlockEntity(level, pos);
      if (testBlock != null) {
         testBlock.reset();
      }
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      TestBlockEntity testBlock = getServerTestBlockEntity(level, pos);
      if (testBlock != null) {
         if (testBlock.getMode() != TestBlockMode.START) {
            boolean shouldTrigger = level.hasNeighborSignal(pos);
            boolean isPowered = testBlock.isPowered();
            if (shouldTrigger && !isPowered) {
               testBlock.setPowered(true);
               testBlock.trigger();
            } else if (!shouldTrigger && isPowered) {
               testBlock.setPowered(false);
            }
         }
      }
   }

   private static @Nullable TestBlockEntity getServerTestBlockEntity(final Level level, final BlockPos pos) {
      return level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(pos) instanceof TestBlockEntity testBlockEntity ? testBlockEntity : null;
   }

   @Override
   public int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
      if (state.getValue(MODE) != TestBlockMode.START) {
         return 0;
      } else if (level.getBlockEntity(pos) instanceof TestBlockEntity testBlock) {
         return testBlock.isPowered() ? 15 : 0;
      } else {
         return 0;
      }
   }

   @Override
   protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
      ItemStack itemStack = super.getCloneItemStack(level, pos, state, includeData);
      return setModeOnStack(itemStack, state.getValue(MODE));
   }

   public static ItemStack setModeOnStack(final ItemStack itemStack, final TestBlockMode mode) {
      itemStack.set(DataComponents.BLOCK_STATE, itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(MODE, mode));
      return itemStack;
   }
}
