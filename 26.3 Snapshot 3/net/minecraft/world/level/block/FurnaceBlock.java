package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FurnaceBlock extends AbstractFurnaceBlock {
   protected FurnaceBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new FurnaceBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return createFurnaceTicker(level, type, BlockEntityTypes.FURNACE);
   }

   @Override
   protected void openContainer(final Level level, final BlockPos pos, final Player player) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof FurnaceBlockEntity) {
         player.openMenu((MenuProvider)blockEntity);
         player.awardStat(Stats.INTERACT_WITH_FURNACE);
      }
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(LIT)) {
         double x = pos.getX() + 0.5;
         double y = pos.getY();
         double z = pos.getZ() + 0.5;
         if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction direction = state.getValue(FACING);
         Direction.Axis axis = direction.getAxis();
         double r = 0.52;
         double ss = random.nextDouble() * 0.6 - 0.3;
         double dx = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : ss;
         double dy = random.nextDouble() * 6.0 / 16.0;
         double dz = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : ss;
         level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
         level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
      }
   }
}
