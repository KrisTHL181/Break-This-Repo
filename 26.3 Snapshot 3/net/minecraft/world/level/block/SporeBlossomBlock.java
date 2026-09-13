package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SporeBlossomBlock extends Block {
   private static final VoxelShape SHAPE = Block.column(12.0, 13.0, 16.0);
   private static final int ADD_PARTICLE_ATTEMPTS = 14;
   private static final int PARTICLE_XZ_RADIUS = 10;
   private static final int PARTICLE_Y_MAX = 10;

   public SporeBlossomBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return Block.canSupportCenter(level, pos.above(), Direction.DOWN) && !level.isWaterAt(pos);
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      return directionToNeighbour == Direction.UP && !this.canSurvive(state, level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      int plantX = pos.getX();
      int plantY = pos.getY();
      int plantZ = pos.getZ();
      double xFalling = plantX + random.nextDouble();
      double yFalling = plantY + 0.7;
      double zFalling = plantZ + random.nextDouble();
      level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, xFalling, yFalling, zFalling, 0.0, 0.0, 0.0);
      BlockPos.MutableBlockPos ambientPos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < 14; i++) {
         ambientPos.set(plantX + Mth.nextInt(random, -10, 10), plantY - random.nextInt(10), plantZ + Mth.nextInt(random, -10, 10));
         BlockState particlePosState = level.getBlockState(ambientPos);
         if (!particlePosState.isCollisionShapeFullBlock(level, ambientPos)) {
            level.addParticle(
               ParticleTypes.SPORE_BLOSSOM_AIR,
               ambientPos.getX() + random.nextDouble(),
               ambientPos.getY() + random.nextDouble(),
               ambientPos.getZ() + random.nextDouble(),
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }
}
