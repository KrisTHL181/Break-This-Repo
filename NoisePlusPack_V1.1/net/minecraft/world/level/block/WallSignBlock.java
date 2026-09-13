package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WallSignBlock extends SignBlock {
   public static final MapCodec<WallSignBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422137_ -> p_422137_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec()).apply(p_422137_, WallSignBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 4.5, 12.5, 14.0, 16.0));

   @Override
   public MapCodec<WallSignBlock> codec() {
      return CODEC;
   }

   public WallSignBlock(WoodType p_58069_, BlockBehaviour.Properties p_58068_) {
      super(p_58069_, p_58068_.sound(p_58069_.soundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected VoxelShape getShape(BlockState p_58092_, BlockGetter p_58093_, BlockPos p_58094_, CollisionContext p_58095_) {
      return SHAPES.get(p_58092_.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(BlockState p_58073_, LevelReader p_58074_, BlockPos p_58075_) {
      return p_58074_.getBlockState(p_58075_.relative(p_58073_.getValue(FACING).getOpposite())).isSolid();
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_58071_) {
      BlockState blockstate = this.defaultBlockState();
      FluidState fluidstate = p_58071_.getLevel().getFluidState(p_58071_.getClickedPos());
      LevelReader levelreader = p_58071_.getLevel();
      BlockPos blockpos = p_58071_.getClickedPos();
      Direction[] adirection = p_58071_.getNearestLookingDirections();

      for (Direction direction : adirection) {
         if (direction.getAxis().isHorizontal()) {
            Direction direction1 = direction.getOpposite();
            blockstate = blockstate.setValue(FACING, direction1);
            if (blockstate.canSurvive(levelreader, blockpos)) {
               return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_58083_,
      LevelReader p_365394_,
      ScheduledTickAccess p_361332_,
      BlockPos p_58087_,
      Direction p_58084_,
      BlockPos p_58088_,
      BlockState p_58085_,
      RandomSource p_364074_
   ) {
      return p_58084_.getOpposite() == p_58083_.getValue(FACING) && !p_58083_.canSurvive(p_365394_, p_58087_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_58083_, p_365394_, p_361332_, p_58087_, p_58084_, p_58088_, p_58085_, p_364074_);
   }

   @Override
   public float getYRotationDegrees(BlockState p_278024_) {
      return p_278024_.getValue(FACING).toYRot();
   }

   @Override
   public Vec3 getSignHitboxCenterPosition(BlockState p_278316_) {
      return SHAPES.get(p_278316_.getValue(FACING)).bounds().getCenter();
   }

   @Override
   protected BlockState rotate(BlockState p_58080_, Rotation p_58081_) {
      return p_58080_.setValue(FACING, p_58081_.rotate(p_58080_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_58077_, Mirror p_58078_) {
      return p_58077_.rotate(p_58078_.getRotation(p_58077_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_58090_) {
      p_58090_.add(FACING, WATERLOGGED);
   }
}
