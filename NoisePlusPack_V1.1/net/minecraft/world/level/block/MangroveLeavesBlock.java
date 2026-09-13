package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MangroveLeavesBlock extends TintedParticleLeavesBlock implements BonemealableBlock {
   public static final MapCodec<MangroveLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422117_ -> p_422117_.group(
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("leaf_particle_chance").forGetter(p_396597_ -> p_396597_.leafParticleChance), propertiesCodec()
         )
         .apply(p_422117_, MangroveLeavesBlock::new)
   );

   @Override
   public MapCodec<MangroveLeavesBlock> codec() {
      return CODEC;
   }

   public MangroveLeavesBlock(float p_395728_, BlockBehaviour.Properties p_221425_) {
      super(p_395728_, p_221425_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256534_, BlockPos p_256299_, BlockState p_255926_) {
      return p_256534_.getBlockState(p_256299_.below()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level p_221437_, RandomSource p_221438_, BlockPos p_221439_, BlockState p_221440_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221427_, RandomSource p_221428_, BlockPos p_221429_, BlockState p_221430_) {
      p_221427_.setBlock(p_221429_.below(), MangrovePropaguleBlock.createNewHangingPropagule(), 2);
   }

   @Override
   public BlockPos getParticlePos(BlockPos p_336154_) {
      return p_336154_.below();
   }
}
