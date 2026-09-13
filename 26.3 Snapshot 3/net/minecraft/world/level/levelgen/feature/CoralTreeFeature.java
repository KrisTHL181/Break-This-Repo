package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record CoralTreeFeature(Holder<PlacedFeature> feature) implements Feature {
   public static final MapCodec<CoralTreeFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(CoralTreeFeature::feature)).apply(i, CoralTreeFeature::new)
   );

   @Override
   public MapCodec<CoralTreeFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      BlockPos.MutableBlockPos mutPos = origin.mutable();
      int trunckHeight = random.nextInt(3) + 1;

      for (int i = 0; i < trunckHeight; i++) {
         if (!this.feature.value().place(level, chunkGenerator, random, mutPos)) {
            return true;
         }

         mutPos.move(Direction.UP);
      }

      BlockPos trunckTopPos = mutPos.immutable();
      int nBranches = random.nextInt(3) + 2;
      List<Direction> directions = Direction.Plane.HORIZONTAL.shuffledCopy(random);

      for (Direction branchDirection : directions.subList(0, nBranches)) {
         mutPos.set(trunckTopPos);
         mutPos.move(branchDirection);
         int branchHeight = random.nextInt(5) + 2;
         int segmentLength = 0;

         for (int j = 0; j < branchHeight && this.feature.value().place(level, chunkGenerator, random, mutPos); j++) {
            segmentLength++;
            mutPos.move(Direction.UP);
            if (j == 0 || segmentLength >= 2 && random.nextFloat() < 0.25F) {
               mutPos.move(branchDirection);
               segmentLength = 0;
            }
         }
      }

      return true;
   }
}
