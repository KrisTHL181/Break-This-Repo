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

public record HugeBrownMushroomFeature(BlockStateProvider capProvider, BlockStateProvider stemProvider, int foliageRadius, BlockPredicate canPlaceOn)
   implements AbstractHugeMushroomFeature {
   public static final MapCodec<HugeBrownMushroomFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("cap_provider").forGetter(HugeBrownMushroomFeature::capProvider),
            BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter(HugeBrownMushroomFeature::stemProvider),
            Codec.INT.optionalFieldOf("foliage_radius", 2).forGetter(HugeBrownMushroomFeature::foliageRadius),
            BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(HugeBrownMushroomFeature::canPlaceOn)
         )
         .apply(i, HugeBrownMushroomFeature::new)
   );

   @Override
   public MapCodec<HugeBrownMushroomFeature> codec() {
      return CODEC;
   }

   @Override
   public void makeCap(
      final WorldGenLevel level, final RandomSource random, final BlockPos origin, final int treeHeight, final BlockPos.MutableBlockPos blockPos
   ) {
      for (int dx = -this.foliageRadius; dx <= this.foliageRadius; dx++) {
         for (int dz = -this.foliageRadius; dz <= this.foliageRadius; dz++) {
            boolean minX = dx == -this.foliageRadius;
            boolean maxX = dx == this.foliageRadius;
            boolean minZ = dz == -this.foliageRadius;
            boolean maxZ = dz == this.foliageRadius;
            boolean xEdge = minX || maxX;
            boolean zEdge = minZ || maxZ;
            if (!xEdge || !zEdge) {
               blockPos.setWithOffset(origin, dx, treeHeight, dz);
               boolean west = minX || zEdge && dx == 1 - this.foliageRadius;
               boolean east = maxX || zEdge && dx == this.foliageRadius - 1;
               boolean north = minZ || xEdge && dz == 1 - this.foliageRadius;
               boolean south = maxZ || xEdge && dz == this.foliageRadius - 1;
               BlockState state = this.capProvider.getState(level, random, origin);
               if (state.hasProperty(HugeMushroomBlock.WEST)
                  && state.hasProperty(HugeMushroomBlock.EAST)
                  && state.hasProperty(HugeMushroomBlock.NORTH)
                  && state.hasProperty(HugeMushroomBlock.SOUTH)) {
                  state = state.setValue(HugeMushroomBlock.WEST, west)
                     .setValue(HugeMushroomBlock.EAST, east)
                     .setValue(HugeMushroomBlock.NORTH, north)
                     .setValue(HugeMushroomBlock.SOUTH, south);
               }

               this.placeMushroomBlock(level, blockPos, state);
            }
         }
      }
   }

   @Override
   public int getTreeRadiusForHeight(final int trunkHeight, final int treeHeight, final int leafRadius, final int yo) {
      return yo <= 3 ? 0 : leafRadius;
   }
}
