package net.minecraft.world.level.block.piston;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PistonHeadBlock extends DirectionalBlock {
   public static final MapCodec<PistonHeadBlock> CODEC = simpleCodec(PistonHeadBlock::new);
   public static final EnumProperty<PistonType> TYPE = BlockStateProperties.PISTON_TYPE;
   public static final BooleanProperty SHORT = BlockStateProperties.SHORT;
   public static final int PLATFORM_THICKNESS = 4;
   private static final VoxelShape SHAPE_PLATFORM = Block.boxZ(16.0, 0.0, 4.0);
   private static final Map<Direction, VoxelShape> SHAPES_SHORT = Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 16.0)));
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 20.0)));

   @Override
   protected MapCodec<PistonHeadBlock> codec() {
      return CODEC;
   }

   public PistonHeadBlock(BlockBehaviour.Properties p_60259_) {
      super(p_60259_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT).setValue(SHORT, false));
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_60325_) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState p_60320_, BlockGetter p_60321_, BlockPos p_60322_, CollisionContext p_60323_) {
      return (p_60320_.getValue(SHORT) ? SHAPES_SHORT : SHAPES).get(p_60320_.getValue(FACING));
   }

   private boolean isFittingBase(BlockState p_60298_, BlockState p_60299_) {
      Block block = p_60298_.getValue(TYPE) == PistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
      return p_60299_.is(block) && p_60299_.getValue(PistonBaseBlock.EXTENDED) && p_60299_.getValue(FACING) == p_60298_.getValue(FACING);
   }

   @Override
   public BlockState playerWillDestroy(Level p_60265_, BlockPos p_60266_, BlockState p_60267_, Player p_60268_) {
      if (!p_60265_.isClientSide() && p_60268_.preventsBlockDrops()) {
         BlockPos blockpos = p_60266_.relative(p_60267_.getValue(FACING).getOpposite());
         if (this.isFittingBase(p_60267_, p_60265_.getBlockState(blockpos))) {
            p_60265_.destroyBlock(blockpos, false);
         }
      }

      return super.playerWillDestroy(p_60265_, p_60266_, p_60267_, p_60268_);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_395396_, ServerLevel p_391673_, BlockPos p_396415_, boolean p_397744_) {
      BlockPos blockpos = p_396415_.relative(p_395396_.getValue(FACING).getOpposite());
      if (this.isFittingBase(p_395396_, p_391673_.getBlockState(blockpos))) {
         p_391673_.destroyBlock(blockpos, true);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_60301_,
      LevelReader p_369952_,
      ScheduledTickAccess p_368902_,
      BlockPos p_60305_,
      Direction p_60302_,
      BlockPos p_60306_,
      BlockState p_60303_,
      RandomSource p_364503_
   ) {
      return p_60302_.getOpposite() == p_60301_.getValue(FACING) && !p_60301_.canSurvive(p_369952_, p_60305_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_60301_, p_369952_, p_368902_, p_60305_, p_60302_, p_60306_, p_60303_, p_364503_);
   }

   @Override
   protected boolean canSurvive(BlockState p_60288_, LevelReader p_60289_, BlockPos p_60290_) {
      BlockState blockstate = p_60289_.getBlockState(p_60290_.relative(p_60288_.getValue(FACING).getOpposite()));
      return this.isFittingBase(p_60288_, blockstate) || blockstate.is(Blocks.MOVING_PISTON) && blockstate.getValue(FACING) == p_60288_.getValue(FACING);
   }

   @Override
   protected void neighborChanged(BlockState p_60275_, Level p_60276_, BlockPos p_60277_, Block p_60278_, @Nullable Orientation p_360849_, boolean p_60280_) {
      if (p_60275_.canSurvive(p_60276_, p_60277_)) {
         p_60276_.neighborChanged(
            p_60277_.relative(p_60275_.getValue(FACING).getOpposite()),
            p_60278_,
            ExperimentalRedstoneUtils.withFront(p_360849_, p_60275_.getValue(FACING).getOpposite())
         );
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312951_, BlockPos p_60262_, BlockState p_60263_, boolean p_377775_) {
      return new ItemStack(p_60263_.getValue(TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
   }

   @Override
   protected BlockState rotate(BlockState p_60295_, Rotation p_60296_) {
      return p_60295_.setValue(FACING, p_60296_.rotate(p_60295_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_60292_, Mirror p_60293_) {
      return p_60292_.rotate(p_60293_.getRotation(p_60292_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60308_) {
      p_60308_.add(FACING, TYPE, SHORT);
   }

   @Override
   protected boolean isPathfindable(BlockState p_60270_, PathComputationType p_60273_) {
      return false;
   }
}
