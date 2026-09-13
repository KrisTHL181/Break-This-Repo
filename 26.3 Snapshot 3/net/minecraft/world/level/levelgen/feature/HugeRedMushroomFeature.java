package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record HugeRedMushroomFeature(BlockStateProvider capProvider, BlockStateProvider stemProvider, int foliageRadius, BlockPredicate canPlaceOn)
   implements AbstractHugeMushroomFeature {
   public static final MapCodec<HugeRedMushroomFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("cap_provider").forGetter(HugeRedMushroomFeature::capProvider),
            BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter(HugeRedMushroomFeature::stemProvider),
            Codec.INT.optionalFieldOf("foliage_radius", 2).forGetter(HugeRedMushroomFeature::foliageRadius),
            BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(HugeRedMushroomFeature::canPlaceOn)
         )
         .apply(i, HugeRedMushroomFeature::new)
   );

   @Override
   public MapCodec<HugeRedMushroomFeature> codec() {
      return CODEC;
   }

   @Override
   public void makeCap(
      final WorldGenLevel level, final RandomSource random, final BlockPos origin, final int treeHeight, final BlockPos.MutableBlockPos blockPos
   ) {
      for (int dy = treeHeight - 3; dy <= treeHeight; dy++) {
         int radius = dy < treeHeight ? this.foliageRadius : this.foliageRadius - 1;
         int center = this.foliageRadius - 2;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               boolean minX = dx == -radius;
               boolean maxX = dx == radius;
               boolean minZ = dz == -radius;
               boolean maxZ = dz == radius;
               boolean xEdge = minX || maxX;
               boolean zEdge = minZ || maxZ;
               if (dy >= treeHeight || xEdge != zEdge) {
                  blockPos.setWithOffset(origin, dx, dy, dz);
                  BlockState state = this.capProvider.getState(level, random, origin);
                  if (state.hasProperty(HugeMushroomBlock.WEST)
                     && state.hasProperty(HugeMushroomBlock.EAST)
                     && state.hasProperty(HugeMushroomBlock.NORTH)
                     && state.hasProperty(HugeMushroomBlock.SOUTH)
                     && state.hasProperty(HugeMushroomBlock.UP)) {
                     state = state.setValue(HugeMushroomBlock.UP, dy >= treeHeight - 1)
                        .setValue(HugeMushroomBlock.WEST, dx < -center)
                        .setValue(HugeMushroomBlock.EAST, dx > center)
                        .setValue(HugeMushroomBlock.NORTH, dz < -center)
                        .setValue(HugeMushroomBlock.SOUTH, dz > center);
                  }

                  this.placeMushroomBlock(level, blockPos, state);
               }
            }
         }
      }
   }

   @Override
   public int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo) {
      int radius = 0;
      if (yo < treeHeight && yo >= treeHeight - 3) {
         radius = leafRadius;
      } else if (yo == treeHeight) {
         radius = leafRadius;
      }

      return radius;
   }
}
