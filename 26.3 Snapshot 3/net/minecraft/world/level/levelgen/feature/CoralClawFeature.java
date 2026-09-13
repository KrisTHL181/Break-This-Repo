package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record CoralClawFeature(Holder<PlacedFeature> feature) implements Feature {
   public static final MapCodec<CoralClawFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(CoralClawFeature::feature)).apply(i, CoralClawFeature::new)
   );

   @Override
   public MapCodec<CoralClawFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!this.feature.value().place(level, chunkGenerator, random, origin)) {
         return false;
      }

      Direction clawDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int nBranches = random.nextInt(2) + 2;
      List<Direction> possibleDirections = Util.toShuffledList(
         Stream.of(clawDirection, clawDirection.getClockWise(), clawDirection.getCounterClockWise()), random
      );

      for (Direction branchDirection : possibleDirections.subList(0, nBranches)) {
         BlockPos.MutableBlockPos mutPos = origin.mutable();
         int sidewayLength = random.nextInt(2) + 1;
         mutPos.move(branchDirection);
         int inwayLenth;
         Direction segmentDirection;
         if (branchDirection == clawDirection) {
            segmentDirection = clawDirection;
            inwayLenth = random.nextInt(3) + 2;
         } else {
            mutPos.move(Direction.UP);
            Direction[] segmentPossibleDirections = new Direction[]{branchDirection, Direction.UP};
            segmentDirection = Util.getRandom(segmentPossibleDirections, random);
            inwayLenth = random.nextInt(3) + 3;
         }

         for (int i = 0; i < sidewayLength && this.feature.value().place(level, chunkGenerator, random, mutPos); i++) {
            mutPos.move(segmentDirection);
         }

         mutPos.move(segmentDirection.getOpposite());
         mutPos.move(Direction.UP);

         for (int i = 0; i < inwayLenth; i++) {
            mutPos.move(clawDirection);
            if (!this.feature.value().place(level, chunkGenerator, random, mutPos)) {
               break;
            }

            if (random.nextFloat() < 0.25F) {
               mutPos.move(Direction.UP);
            }
         }
      }

      return true;
   }
}
