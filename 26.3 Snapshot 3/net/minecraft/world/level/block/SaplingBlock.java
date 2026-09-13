package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SaplingBlock extends VegetationBlock implements BonemealableBlock {
   public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
   public static final int BRIGHTNESS_FOR_SAPLING_GROWTH = 9;
   public static final int TICK_CHANCE_FOR_SAPLING_GROWTH = 7;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);
   protected final TreeGrower treeGrower;

   protected SaplingBlock(final TreeGrower treeGrower, final BlockBehaviour.Properties properties) {
      super(properties);
      this.treeGrower = treeGrower;
      this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
         this.advanceTree(level, pos, state, random);
      }
   }

   public void advanceTree(final ServerLevel level, final BlockPos pos, final BlockState state, final RandomSource random) {
      if (state.getValue(STAGE) == 0) {
         level.setBlock(pos, state.cycle(STAGE), 260);
      } else {
         this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
      }
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      if (level instanceof ServerLevel serverLevel) {
         if (!this.treeGrower.canGrow(serverLevel, pos, state)) {
            return false;
         }

         int heightOffset = this.treeGrower.getMinimumHeight(serverLevel).orElse(0);
         return level.isInsideBuildHeight(pos.above(heightOffset));
      } else {
         return false;
      }
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return level.getRandom().nextFloat() < 0.45;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      this.advanceTree(level, pos, state, random);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(STAGE);
   }
}
