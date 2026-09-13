package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NetherWorldCarver extends CaveWorldCarver {
   public NetherWorldCarver(Codec<CaveCarverConfiguration> p_64873_) {
      super(p_64873_);
      this.liquids = ImmutableSet.of(Fluids.LAVA, Fluids.WATER);
   }

   @Override
   protected int getCaveBound() {
      return 10;
   }

   @Override
   protected float getThickness(RandomSource p_224907_) {
      return (p_224907_.nextFloat() * 2.0F + p_224907_.nextFloat()) * 2.0F;
   }

   @Override
   protected double getYScale() {
      return 5.0;
   }

   protected boolean carveBlock(
      CarvingContext p_190731_,
      CaveCarverConfiguration p_190732_,
      ChunkAccess p_190733_,
      Function<BlockPos, Holder<Biome>> p_190734_,
      CarvingMask p_190735_,
      BlockPos.MutableBlockPos p_190736_,
      BlockPos.MutableBlockPos p_190737_,
      Aquifer p_190738_,
      MutableBoolean p_190739_
   ) {
      if (this.canReplaceBlock(p_190732_, p_190733_.getBlockState(p_190736_))) {
         BlockState blockstate;
         if (p_190736_.getY() <= p_190731_.getMinGenY() + 31) {
            blockstate = LAVA.createLegacyBlock();
         } else {
            blockstate = CAVE_AIR;
         }

         p_190733_.setBlockState(p_190736_, blockstate);
         return true;
      } else {
         return false;
      }
   }
}
