package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
   public static final MapCodec<BeetrootBlock> CODEC = simpleCodec(BeetrootBlock::new);
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final VoxelShape[] SHAPES = Block.boxes(3, p_395900_ -> Block.column(16.0, 0.0, 2 + p_395900_ * 2));

   @Override
   public MapCodec<BeetrootBlock> codec() {
      return CODEC;
   }

   public BeetrootBlock(BlockBehaviour.Properties p_49661_) {
      super(p_49661_);
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
   protected void randomTick(BlockState p_220778_, ServerLevel p_220779_, BlockPos p_220780_, RandomSource p_220781_) {
      if (p_220781_.nextInt(3) != 0) {
         super.randomTick(p_220778_, p_220779_, p_220780_, p_220781_);
      }
   }

   @Override
   protected int getBonemealAgeIncrease(Level p_49663_) {
      return super.getBonemealAgeIncrease(p_49663_) / 3;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49665_) {
      p_49665_.add(AGE);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49672_, BlockGetter p_49673_, BlockPos p_49674_, CollisionContext p_49675_) {
      return SHAPES[this.getAge(p_49672_)];
   }
}
