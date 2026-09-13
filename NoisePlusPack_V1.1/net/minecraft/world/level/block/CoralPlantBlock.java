package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoralPlantBlock extends BaseCoralPlantTypeBlock {
   public static final MapCodec<CoralPlantBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422104_ -> p_422104_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(p_312756_ -> p_312756_.deadBlock), propertiesCodec())
         .apply(p_422104_, CoralPlantBlock::new)
   );
   private final Block deadBlock;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 15.0);

   @Override
   public MapCodec<CoralPlantBlock> codec() {
      return CODEC;
   }

   protected CoralPlantBlock(Block p_52175_, BlockBehaviour.Properties p_52176_) {
      super(p_52176_);
      this.deadBlock = p_52175_;
   }

   @Override
   protected void onPlace(BlockState p_52195_, Level p_52196_, BlockPos p_52197_, BlockState p_52198_, boolean p_52199_) {
      this.tryScheduleDieTick(p_52195_, p_52196_, p_52196_, p_52196_.random, p_52197_);
   }

   @Override
   protected void tick(BlockState p_221030_, ServerLevel p_221031_, BlockPos p_221032_, RandomSource p_221033_) {
      if (!scanForWater(p_221030_, p_221031_, p_221032_)) {
         p_221031_.setBlock(p_221032_, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false), 2);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_52183_,
      LevelReader p_368530_,
      ScheduledTickAccess p_368124_,
      BlockPos p_52187_,
      Direction p_52184_,
      BlockPos p_52188_,
      BlockState p_52185_,
      RandomSource p_362387_
   ) {
      if (p_52184_ == Direction.DOWN && !p_52183_.canSurvive(p_368530_, p_52187_)) {
         return Blocks.AIR.defaultBlockState();
      }

      this.tryScheduleDieTick(p_52183_, p_368530_, p_368124_, p_362387_, p_52187_);
      if (p_52183_.getValue(WATERLOGGED)) {
         p_368124_.scheduleTick(p_52187_, Fluids.WATER, Fluids.WATER.getTickDelay(p_368530_));
      }

      return super.updateShape(p_52183_, p_368530_, p_368124_, p_52187_, p_52184_, p_52188_, p_52185_, p_362387_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_52190_, BlockGetter p_52191_, BlockPos p_52192_, CollisionContext p_52193_) {
      return SHAPE;
   }
}
