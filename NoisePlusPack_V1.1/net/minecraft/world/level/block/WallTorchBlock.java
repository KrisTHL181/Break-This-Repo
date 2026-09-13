package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WallTorchBlock extends TorchBlock {
   public static final MapCodec<WallTorchBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422139_ -> p_422139_.group(PARTICLE_OPTIONS_FIELD.forGetter(p_312123_ -> p_312123_.flameParticle), propertiesCodec())
         .apply(p_422139_, WallTorchBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(5.0, 3.0, 13.0, 11.0, 16.0));

   @Override
   public MapCodec<WallTorchBlock> codec() {
      return CODEC;
   }

   protected WallTorchBlock(SimpleParticleType p_312024_, BlockBehaviour.Properties p_58123_) {
      super(p_312024_, p_58123_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected VoxelShape getShape(BlockState p_58152_, BlockGetter p_58153_, BlockPos p_58154_, CollisionContext p_58155_) {
      return getShape(p_58152_);
   }

   public static VoxelShape getShape(BlockState p_58157_) {
      return SHAPES.get(p_58157_.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(BlockState p_58133_, LevelReader p_58134_, BlockPos p_58135_) {
      return canSurvive(p_58134_, p_58135_, p_58133_.getValue(FACING));
   }

   public static boolean canSurvive(LevelReader p_328236_, BlockPos p_331554_, Direction p_330599_) {
      BlockPos blockpos = p_331554_.relative(p_330599_.getOpposite());
      BlockState blockstate = p_328236_.getBlockState(blockpos);
      return blockstate.isFaceSturdy(p_328236_, blockpos, p_330599_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_58126_) {
      BlockState blockstate = this.defaultBlockState();
      LevelReader levelreader = p_58126_.getLevel();
      BlockPos blockpos = p_58126_.getClickedPos();
      Direction[] adirection = p_58126_.getNearestLookingDirections();

      for (Direction direction : adirection) {
         if (direction.getAxis().isHorizontal()) {
            Direction direction1 = direction.getOpposite();
            blockstate = blockstate.setValue(FACING, direction1);
            if (blockstate.canSurvive(levelreader, blockpos)) {
               return blockstate;
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_58143_,
      LevelReader p_365982_,
      ScheduledTickAccess p_363189_,
      BlockPos p_58147_,
      Direction p_58144_,
      BlockPos p_58148_,
      BlockState p_58145_,
      RandomSource p_366545_
   ) {
      return p_58144_.getOpposite() == p_58143_.getValue(FACING) && !p_58143_.canSurvive(p_365982_, p_58147_) ? Blocks.AIR.defaultBlockState() : p_58143_;
   }

   @Override
   public void animateTick(BlockState p_222660_, Level p_222661_, BlockPos p_222662_, RandomSource p_222663_) {
      Direction direction = p_222660_.getValue(FACING);
      double d0 = p_222662_.getX() + 0.5;
      double d1 = p_222662_.getY() + 0.7;
      double d2 = p_222662_.getZ() + 0.5;
      double d3 = 0.22;
      double d4 = 0.27;
      Direction direction1 = direction.getOpposite();
      p_222661_.addParticle(ParticleTypes.SMOKE, d0 + 0.27 * direction1.getStepX(), d1 + 0.22, d2 + 0.27 * direction1.getStepZ(), 0.0, 0.0, 0.0);
      p_222661_.addParticle(this.flameParticle, d0 + 0.27 * direction1.getStepX(), d1 + 0.22, d2 + 0.27 * direction1.getStepZ(), 0.0, 0.0, 0.0);
   }

   @Override
   protected BlockState rotate(BlockState p_58140_, Rotation p_58141_) {
      return p_58140_.setValue(FACING, p_58141_.rotate(p_58140_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_58137_, Mirror p_58138_) {
      return p_58137_.rotate(p_58138_.getRotation(p_58137_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_58150_) {
      p_58150_.add(FACING);
   }
}
