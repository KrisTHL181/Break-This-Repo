package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeetrootBlock extends CropBlock {
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final VoxelShape[] SHAPES = Block.boxes(3, age -> Block.column(16.0, 0.0, 2 + age * 2));

   public BeetrootBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   @Override
   public int getMaxAge() {
      return 3;
   }

   @Override
   protected ItemLike getBaseSeedId() {
      return Items.BEETROOT_SEEDS;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(3) != 0) {
         super.randomTick(state, level, pos, random);
      }
   }

   @Override
   protected int getBonemealAgeIncrease(final Level level) {
      return super.getBonemealAgeIncrease(level) / 3;
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }
}
