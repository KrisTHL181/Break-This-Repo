package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SmokerBlock extends AbstractFurnaceBlock {
   protected SmokerBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new SmokerBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return createFurnaceTicker(level, type, BlockEntityTypes.SMOKER);
   }

   @Override
   protected void openContainer(final Level level, final BlockPos pos, final Player player) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof SmokerBlockEntity) {
         player.openMenu((MenuProvider)blockEntity);
         player.awardStat(Stats.INTERACT_WITH_SMOKER);
      }
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(LIT)) {
         double x = pos.getX() + 0.5;
         double y = pos.getY();
         double z = pos.getZ() + 0.5;
         if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         level.addParticle(ParticleTypes.SMOKE, x, y + 1.1, z, 0.0, 0.0, 0.0);
      }
   }
}
