package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DriedGhastBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
   public static final int MAX_HYDRATION_LEVEL = 3;
   public static final IntegerProperty HYDRATION_LEVEL = BlockStateProperties.DRIED_GHAST_HYDRATION_LEVELS;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final int HYDRATION_TICK_DELAY = 5000;
   private static final VoxelShape SHAPE = Block.column(10.0, 10.0, 0.0, 10.0);

   public DriedGhastBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HYDRATION_LEVEL, 0).setValue(WATERLOGGED, false));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, HYDRATION_LEVEL, WATERLOGGED);
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
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   public int getHydrationLevel(final BlockState state) {
      return state.getValue(HYDRATION_LEVEL);
   }

   private boolean isReadyToSpawn(final BlockState state) {
      return this.getHydrationLevel(state) == 3;
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos position, final RandomSource random) {
      if (state.getValue(WATERLOGGED)) {
         this.tickWaterlogged(state, level, position, random);
      } else {
         int hydrationLevel = this.getHydrationLevel(state);
         if (hydrationLevel > 0) {
            level.setBlock(position, state.setValue(HYDRATION_LEVEL, hydrationLevel - 1), 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(state));
         }
      }
   }

   private void tickWaterlogged(final BlockState state, final ServerLevel level, final BlockPos position, final RandomSource random) {
      if (!this.isReadyToSpawn(state)) {
         level.playSound(null, position, SoundEvents.DRIED_GHAST_TRANSITION, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.setBlock(position, state.setValue(HYDRATION_LEVEL, this.getHydrationLevel(state) + 1), 2);
         level.gameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Context.of(state));
      } else {
         this.spawnGhastling(level, position, state);
      }
   }

   private void spawnGhastling(final ServerLevel level, final BlockPos position, final BlockState state) {
      level.removeBlock(position, false);
      HappyGhast ghastling = EntityTypes.HAPPY_GHAST.create(level, EntitySpawnReason.BREEDING);
      if (ghastling != null) {
         Vec3 spawnAt = Vec3.atBottomCenterOf(position);
         ghastling.setBaby(true);
         float blockRotation = Direction.getYRot(state.getValue(FACING));
         ghastling.setYHeadRot(blockRotation);
         ghastling.snapTo(spawnAt.x(), spawnAt.y(), spawnAt.z(), blockRotation, 0.0F);
         level.addFreshEntity(ghastling);
         level.playSound(null, ghastling, SoundEvents.GHASTLING_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.5;
      double z = pos.getZ() + 0.5;
      if (!state.getValue(WATERLOGGED)) {
         if (random.nextInt(40) == 0 && level.getBlockState(pos.below()).is(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS)) {
            level.playLocalSound(x, y, z, SoundEvents.DRIED_GHAST_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
         }
      } else {
         if (random.nextInt(40) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.DRIED_GHAST_AMBIENT_WATER, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         if (random.nextInt(6) == 0) {
            level.addParticle(
               ParticleTypes.HAPPY_VILLAGER,
               x + (random.nextFloat() * 2.0F - 1.0F) / 3.0F,
               y + 0.4,
               z + (random.nextFloat() * 2.0F - 1.0F) / 3.0F,
               0.0,
               random.nextFloat(),
               0.0
            );
         }
      }
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if ((state.getValue(WATERLOGGED) || state.getValue(HYDRATION_LEVEL) > 0) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
         level.scheduleTick(pos, this, 5000);
      }
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      boolean isWaterSource = replacedFluidState.is(Fluids.WATER);
      return super.getStateForPlacement(context).setValue(WATERLOGGED, isWaterSource).setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   public boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state, final FluidState fluidState) {
      if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(Fluids.WATER)) {
         if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.WATERLOGGED, true));
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            level.playSound(null, pos, SoundEvents.DRIED_GHAST_PLACE_IN_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      super.setPlacedBy(level, pos, state, by, itemStack);
      level.playSound(
         null, pos, state.getValue(WATERLOGGED) ? SoundEvents.DRIED_GHAST_PLACE_IN_WATER : SoundEvents.DRIED_GHAST_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F
      );
   }

   @Override
   public boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
