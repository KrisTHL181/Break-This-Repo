package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestInstanceBlock extends BaseEntityBlock implements GameMasterBlock {
   public TestInstanceBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new TestInstanceBlockEntity(worldPosition, blockState);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof TestInstanceBlockEntity testInstance) {
         if (!player.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
         }

         if (player.level().isClientSide()) {
            player.openTestInstanceBlock(testInstance);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
