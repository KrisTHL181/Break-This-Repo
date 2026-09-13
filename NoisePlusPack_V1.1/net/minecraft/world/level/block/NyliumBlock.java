package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.lighting.LightEngine;

public class NyliumBlock extends Block implements BonemealableBlock {
   public static final MapCodec<NyliumBlock> CODEC = simpleCodec(NyliumBlock::new);

   @Override
   public MapCodec<NyliumBlock> codec() {
      return CODEC;
   }

   protected NyliumBlock(BlockBehaviour.Properties p_55057_) {
      super(p_55057_);
   }

   private static boolean canBeNylium(BlockState p_55079_, LevelReader p_55080_, BlockPos p_55081_) {
      BlockPos blockpos = p_55081_.above();
      BlockState blockstate = p_55080_.getBlockState(blockpos);
      int i = LightEngine.getLightBlockInto(p_55079_, blockstate, Direction.UP, blockstate.getLightBlock());
      return i < 15;
   }

   @Override
   protected void randomTick(BlockState p_221835_, ServerLevel p_221836_, BlockPos p_221837_, RandomSource p_221838_) {
      if (!canBeNylium(p_221835_, p_221836_, p_221837_)) {
         p_221836_.setBlockAndUpdate(p_221837_, Blocks.NETHERRACK.defaultBlockState());
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256194_, BlockPos p_256152_, BlockState p_256389_) {
      return p_256194_.getBlockState(p_256152_.above()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level p_221830_, RandomSource p_221831_, BlockPos p_221832_, BlockState p_221833_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221825_, RandomSource p_221826_, BlockPos p_221827_, BlockState p_221828_) {
      BlockState blockstate = p_221825_.getBlockState(p_221827_);
      BlockPos blockpos = p_221827_.above();
      ChunkGenerator chunkgenerator = p_221825_.getChunkSource().getGenerator();
      Registry<ConfiguredFeature<?, ?>> registry = p_221825_.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
      if (blockstate.is(Blocks.CRIMSON_NYLIUM)) {
         this.place(registry, NetherFeatures.CRIMSON_FOREST_VEGETATION_BONEMEAL, p_221825_, chunkgenerator, p_221826_, blockpos);
      } else if (blockstate.is(Blocks.WARPED_NYLIUM)) {
         this.place(registry, NetherFeatures.WARPED_FOREST_VEGETATION_BONEMEAL, p_221825_, chunkgenerator, p_221826_, blockpos);
         this.place(registry, NetherFeatures.NETHER_SPROUTS_BONEMEAL, p_221825_, chunkgenerator, p_221826_, blockpos);
         if (p_221826_.nextInt(8) == 0) {
            this.place(registry, NetherFeatures.TWISTING_VINES_BONEMEAL, p_221825_, chunkgenerator, p_221826_, blockpos);
         }
      }
   }

   private void place(
      Registry<ConfiguredFeature<?, ?>> p_255879_,
      ResourceKey<ConfiguredFeature<?, ?>> p_256032_,
      ServerLevel p_255631_,
      ChunkGenerator p_256445_,
      RandomSource p_255709_,
      BlockPos p_256019_
   ) {
      p_255879_.get(p_256032_).ifPresent(p_255920_ -> p_255920_.value().place(p_255631_, p_256445_, p_255709_, p_256019_));
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
