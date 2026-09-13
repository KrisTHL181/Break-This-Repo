package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetherWartBlock extends VegetationBlock {
   public static final MapCodec<NetherWartBlock> CODEC = simpleCodec(NetherWartBlock::new);
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final VoxelShape[] SHAPES = Block.boxes(3, p_397432_ -> Block.column(16.0, 0.0, 5 + p_397432_ * 3));

   @Override
   public MapCodec<NetherWartBlock> codec() {
      return CODEC;
   }

   protected NetherWartBlock(BlockBehaviour.Properties p_54971_) {
      super(p_54971_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   protected VoxelShape getShape(BlockState p_54986_, BlockGetter p_54987_, BlockPos p_54988_, CollisionContext p_54989_) {
      return SHAPES[p_54986_.getValue(AGE)];
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_54991_, BlockGetter p_54992_, BlockPos p_54993_) {
      return p_54991_.is(Blocks.SOUL_SAND);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_54979_) {
      return p_54979_.getValue(AGE) < 3;
   }

   @Override
   protected void randomTick(BlockState p_221806_, ServerLevel p_221807_, BlockPos p_221808_, RandomSource p_221809_) {
      int i = p_221806_.getValue(AGE);
      if (i < 3 && p_221809_.nextInt(10) == 0) {
         p_221806_ = p_221806_.setValue(AGE, i + 1);
         p_221807_.setBlock(p_221808_, p_221806_, 2);
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310014_, BlockPos p_54974_, BlockState p_54975_, boolean p_378545_) {
      return new ItemStack(Items.NETHER_WART);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54977_) {
      p_54977_.add(AGE);
   }
}
