package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
   public static final MapCodec<SaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422122_ -> p_422122_.group(TreeGrower.CODEC.fieldOf("tree").forGetter(p_310598_ -> p_310598_.treeGrower), propertiesCodec())
         .apply(p_422122_, SaplingBlock::new)
   );
   public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 12.0);
   protected final TreeGrower treeGrower;

   @Override
   public MapCodec<? extends SaplingBlock> codec() {
      return CODEC;
   }

   protected SaplingBlock(TreeGrower p_311256_, BlockBehaviour.Properties p_55979_) {
      super(p_55979_);
      this.treeGrower = p_311256_;
      this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
   }

   @Override
   protected VoxelShape getShape(BlockState p_56008_, BlockGetter p_56009_, BlockPos p_56010_, CollisionContext p_56011_) {
      return SHAPE;
   }

   @Override
   protected void randomTick(BlockState p_222011_, ServerLevel p_222012_, BlockPos p_222013_, RandomSource p_222014_) {
      if (p_222012_.getMaxLocalRawBrightness(p_222013_.above()) >= 9 && p_222014_.nextInt(7) == 0) {
         this.advanceTree(p_222012_, p_222013_, p_222011_, p_222014_);
      }
   }

   public void advanceTree(ServerLevel p_222001_, BlockPos p_222002_, BlockState p_222003_, RandomSource p_222004_) {
      if (p_222003_.getValue(STAGE) == 0) {
         p_222001_.setBlock(p_222002_, p_222003_.cycle(STAGE), 260);
      } else {
         this.treeGrower.growTree(p_222001_, p_222001_.getChunkSource().getGenerator(), p_222002_, p_222003_, p_222004_);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256124_, BlockPos p_55992_, BlockState p_55993_) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level p_222006_, RandomSource p_222007_, BlockPos p_222008_, BlockState p_222009_) {
      return p_222006_.random.nextFloat() < 0.45;
   }

   @Override
   public void performBonemeal(ServerLevel p_221996_, RandomSource p_221997_, BlockPos p_221998_, BlockState p_221999_) {
      this.advanceTree(p_221996_, p_221998_, p_221999_, p_221997_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56001_) {
      p_56001_.add(STAGE);
   }
}
