package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
   public static final MapCodec<DriedGhastBlock> CODEC = simpleCodec(DriedGhastBlock::new);
   public static final int MAX_HYDRATION_LEVEL = 3;
   public static final IntegerProperty HYDRATION_LEVEL = BlockStateProperties.DRIED_GHAST_HYDRATION_LEVELS;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final int HYDRATION_TICK_DELAY = 5000;
   private static final VoxelShape SHAPE = Block.column(10.0, 10.0, 0.0, 10.0);

   @Override
   public MapCodec<DriedGhastBlock> codec() {
      return CODEC;
   }

   public DriedGhastBlock(BlockBehaviour.Properties p_409185_) {
      super(p_409185_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HYDRATION_LEVEL, 0).setValue(WATERLOGGED, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_408532_) {
      p_408532_.add(FACING, HYDRATION_LEVEL, WATERLOGGED);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_407458_,
      LevelReader p_408178_,
      ScheduledTickAccess p_407118_,
      BlockPos p_409751_,
      Direction p_409868_,
      BlockPos p_410676_,
      BlockState p_410607_,
      RandomSource p_408903_
   ) {
      if (p_407458_.getValue(WATERLOGGED)) {
         p_407118_.scheduleTick(p_409751_, Fluids.WATER, Fluids.WATER.getTickDelay(p_408178_));
      }

      return super.updateShape(p_407458_, p_408178_, p_407118_, p_409751_, p_409868_, p_410676_, p_410607_, p_408903_);
   }

   @Override
   public VoxelShape getShape(BlockState p_408150_, BlockGetter p_408217_, BlockPos p_410707_, CollisionContext p_409317_) {
      return SHAPE;
   }

   public int getHydrationLevel(BlockState p_407654_) {
      return p_407654_.getValue(HYDRATION_LEVEL);
   }

   private boolean isReadyToSpawn(BlockState p_408747_) {
      return this.getHydrationLevel(p_408747_) == 3;
   }

   @Override
   protected void tick(BlockState p_410355_, ServerLevel p_408755_, BlockPos p_410688_, RandomSource p_409015_) {
      if (p_410355_.getValue(WATERLOGGED)) {
         this.tickWaterlogged(p_410355_, p_408755_, p_410688_, p_409015_);
      } else {
         int i = this.getHydrationLevel(p_410355_);
         if (i > 0) {
            p_408755_.setBlock(p_410688_, p_410355_.setValue(HYDRATION_LEVEL, i - 1), 2);
            p_408755_.gameEvent(GameEvent.BLOCK_CHANGE, p_410688_, GameEvent.Context.of(p_410355_));
         }
      }
   }

   private void tickWaterlogged(BlockState p_406049_, ServerLevel p_410023_, BlockPos p_406811_, RandomSource p_407411_) {
      if (!this.isReadyToSpawn(p_406049_)) {
         p_410023_.playSound(null, p_406811_, SoundEvents.DRIED_GHAST_TRANSITION, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_410023_.setBlock(p_406811_, p_406049_.setValue(HYDRATION_LEVEL, this.getHydrationLevel(p_406049_) + 1), 2);
         p_410023_.gameEvent(GameEvent.BLOCK_CHANGE, p_406811_, GameEvent.Context.of(p_406049_));
      } else {
         this.spawnGhastling(p_410023_, p_406811_, p_406049_);
      }
   }

   private void spawnGhastling(ServerLevel p_409691_, BlockPos p_410120_, BlockState p_406426_) {
      p_409691_.removeBlock(p_410120_, false);
      HappyGhast happyghast = EntityType.HAPPY_GHAST.create(p_409691_, EntitySpawnReason.BREEDING);
      if (happyghast != null) {
         Vec3 vec3 = p_410120_.getBottomCenter();
         happyghast.setBaby(true);
         float f = Direction.getYRot(p_406426_.getValue(FACING));
         happyghast.setYHeadRot(f);
         happyghast.snapTo(vec3.x(), vec3.y(), vec3.z(), f, 0.0F);
         p_409691_.addFreshEntity(happyghast);
         p_409691_.playSound(null, happyghast, SoundEvents.GHASTLING_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   public void animateTick(BlockState p_410332_, Level p_406099_, BlockPos p_408939_, RandomSource p_409178_) {
      double d0 = p_408939_.getX() + 0.5;
      double d1 = p_408939_.getY() + 0.5;
      double d2 = p_408939_.getZ() + 0.5;
      if (!p_410332_.getValue(WATERLOGGED)) {
         if (p_409178_.nextInt(40) == 0 && p_406099_.getBlockState(p_408939_.below()).is(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS)) {
            p_406099_.playLocalSound(d0, d1, d2, SoundEvents.DRIED_GHAST_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         if (p_409178_.nextInt(6) == 0) {
            p_406099_.addParticle(ParticleTypes.WHITE_SMOKE, d0, d1, d2, 0.0, 0.02, 0.0);
         }
      } else {
         if (p_409178_.nextInt(40) == 0) {
            p_406099_.playLocalSound(d0, d1, d2, SoundEvents.DRIED_GHAST_AMBIENT_WATER, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         if (p_409178_.nextInt(6) == 0) {
            p_406099_.addParticle(
               ParticleTypes.HAPPY_VILLAGER,
               d0 + (p_409178_.nextFloat() * 2.0F - 1.0F) / 3.0F,
               d1 + 0.4,
               d2 + (p_409178_.nextFloat() * 2.0F - 1.0F) / 3.0F,
               0.0,
               p_409178_.nextFloat(),
               0.0
            );
         }
      }
   }

   @Override
   protected void randomTick(BlockState p_409379_, ServerLevel p_408492_, BlockPos p_406630_, RandomSource p_409352_) {
      if ((p_409379_.getValue(WATERLOGGED) || p_409379_.getValue(HYDRATION_LEVEL) > 0) && !p_408492_.getBlockTicks().hasScheduledTick(p_406630_, this)) {
         p_408492_.scheduleTick(p_406630_, this, 5000);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_410426_) {
      FluidState fluidstate = p_410426_.getLevel().getFluidState(p_410426_.getClickedPos());
      boolean flag = fluidstate.getType() == Fluids.WATER;
      return super.getStateForPlacement(p_410426_).setValue(WATERLOGGED, flag).setValue(FACING, p_410426_.getHorizontalDirection().getOpposite());
   }

   @Override
   protected FluidState getFluidState(BlockState p_409292_) {
      return p_409292_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_409292_);
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_408704_, BlockPos p_408154_, BlockState p_410312_, FluidState p_407237_) {
      if (!p_410312_.getValue(BlockStateProperties.WATERLOGGED) && p_407237_.getType() == Fluids.WATER) {
         if (!p_408704_.isClientSide()) {
            p_408704_.setBlock(p_408154_, p_410312_.setValue(BlockStateProperties.WATERLOGGED, true), 3);
            p_408704_.scheduleTick(p_408154_, p_407237_.getType(), p_407237_.getType().getTickDelay(p_408704_));
            p_408704_.playSound(null, p_408154_, SoundEvents.DRIED_GHAST_PLACE_IN_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void setPlacedBy(Level p_409809_, BlockPos p_408301_, BlockState p_410233_, @Nullable LivingEntity p_410420_, ItemStack p_406050_) {
      super.setPlacedBy(p_409809_, p_408301_, p_410233_, p_410420_, p_406050_);
      p_409809_.playSound(
         null,
         p_408301_,
         p_410233_.getValue(WATERLOGGED) ? SoundEvents.DRIED_GHAST_PLACE_IN_WATER : SoundEvents.DRIED_GHAST_PLACE,
         SoundSource.BLOCKS,
         1.0F,
         1.0F
      );
   }

   @Override
   public boolean isPathfindable(BlockState p_410548_, PathComputationType p_407974_) {
      return false;
   }
}
