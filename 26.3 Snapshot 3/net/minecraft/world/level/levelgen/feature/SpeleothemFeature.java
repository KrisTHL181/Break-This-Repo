package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record SpeleothemFeature(
   BlockState baseBlock,
   BlockState pointedBlock,
   HolderSet<Block> replaceableBlocks,
   float chanceOfTallerGeneration,
   float chanceOfDirectionalSpread,
   float chanceOfSpreadRadius2,
   float chanceOfSpreadRadius3
) implements Feature {
   public static final MapCodec<SpeleothemFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockState.CODEC.fieldOf("base_block").forGetter(SpeleothemFeature::baseBlock),
            BlockState.CODEC.fieldOf("pointed_block").forGetter(SpeleothemFeature::pointedBlock),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(SpeleothemFeature::replaceableBlocks),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_taller_generation", 0.2F).forGetter(SpeleothemFeature::chanceOfTallerGeneration),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_directional_spread", 0.7F).forGetter(SpeleothemFeature::chanceOfDirectionalSpread),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_spread_radius2", 0.5F).forGetter(SpeleothemFeature::chanceOfSpreadRadius2),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_of_spread_radius3", 0.5F).forGetter(SpeleothemFeature::chanceOfSpreadRadius3)
         )
         .apply(i, SpeleothemFeature::new)
   );

   @Override
   public MapCodec<SpeleothemFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      Optional<Direction> tipDirection = this.getTipDirection(level, origin, random);
      if (tipDirection.isEmpty()) {
         return false;
      }

      BlockPos rootPos = origin.relative(tipDirection.get().getOpposite());
      this.createPatchOfBaseBlocks(level, random, rootPos);
      int height = random.nextFloat() < this.chanceOfTallerGeneration
            && SpeleothemUtils.isEmptyOrWater(level.getBlockState(origin.relative(tipDirection.get())))
         ? 2
         : 1;
      SpeleothemUtils.growSpeleothem(
         level, origin, tipDirection.get(), height, false, this.baseBlock.getBlock(), this.pointedBlock.getBlock(), this.replaceableBlocks
      );
      return true;
   }

   private Optional<Direction> getTipDirection(final LevelAccessor level, final BlockPos pos, final RandomSource random) {
      boolean canPlaceAbove = SpeleothemUtils.isBase(level.getBlockState(pos.above()), this.baseBlock.getBlock(), this.replaceableBlocks);
      boolean canPlaceBelow = SpeleothemUtils.isBase(level.getBlockState(pos.below()), this.baseBlock.getBlock(), this.replaceableBlocks);
      if (canPlaceAbove && canPlaceBelow) {
         return Optional.of(random.nextBoolean() ? Direction.DOWN : Direction.UP);
      } else if (canPlaceAbove) {
         return Optional.of(Direction.DOWN);
      } else {
         return canPlaceBelow ? Optional.of(Direction.UP) : Optional.empty();
      }
   }

   private void createPatchOfBaseBlocks(final LevelAccessor level, final RandomSource random, final BlockPos pos) {
      SpeleothemUtils.placeBaseBlockIfPossible(level, pos, this.baseBlock.getBlock(), this.replaceableBlocks);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (!(random.nextFloat() > this.chanceOfDirectionalSpread)) {
            BlockPos pos1 = pos.relative(direction);
            SpeleothemUtils.placeBaseBlockIfPossible(level, pos1, this.baseBlock.getBlock(), this.replaceableBlocks);
            if (!(random.nextFloat() > this.chanceOfSpreadRadius2)) {
               BlockPos pos2 = pos1.relative(Direction.getRandom(random));
               SpeleothemUtils.placeBaseBlockIfPossible(level, pos2, this.baseBlock.getBlock(), this.replaceableBlocks);
               if (!(random.nextFloat() > this.chanceOfSpreadRadius3)) {
                  BlockPos pos3 = pos2.relative(Direction.getRandom(random));
                  SpeleothemUtils.placeBaseBlockIfPossible(level, pos3, this.baseBlock.getBlock(), this.replaceableBlocks);
               }
            }
         }
      }
   }
}
