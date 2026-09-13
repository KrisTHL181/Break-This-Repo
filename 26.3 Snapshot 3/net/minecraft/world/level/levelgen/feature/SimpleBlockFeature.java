package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockFeature(BlockStateProvider toPlace, boolean scheduleTick) implements Feature {
   public static final MapCodec<SimpleBlockFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("to_place").forGetter(SimpleBlockFeature::toPlace),
            Codec.BOOL.optionalFieldOf("schedule_tick", false).forGetter(SimpleBlockFeature::scheduleTick)
         )
         .apply(i, SimpleBlockFeature::new)
   );

   public SimpleBlockFeature(final BlockStateProvider toPlace) {
      this(toPlace, false);
   }

   @Override
   public MapCodec<SimpleBlockFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      BlockState stateToPlace = this.toPlace.getOptionalState(level, random, origin);
      if (stateToPlace == null) {
         return false;
      }

      if (!stateToPlace.canSurvive(level, origin)) {
         return false;
      }

      if (stateToPlace.getBlock() instanceof DoublePlantBlock) {
         BlockState aboveState = level.getBlockState(origin.above());
         if (!aboveState.isAir() && (!Objects.equals(stateToPlace.getFluidState(), aboveState.getFluidState()) || !aboveState.canBeReplaced())) {
            return false;
         }

         DoublePlantBlock.placeAt(level, stateToPlace, origin, 2);
      } else if (stateToPlace.getBlock() instanceof MossyCarpetBlock) {
         MossyCarpetBlock.placeAt(level, origin, level.getRandom(), 2);
      } else {
         level.setBlock(origin, stateToPlace, 2);
      }

      if (this.scheduleTick) {
         level.scheduleTick(origin, level.getBlockState(origin).getBlock(), 1);
      }

      return true;
   }
}
