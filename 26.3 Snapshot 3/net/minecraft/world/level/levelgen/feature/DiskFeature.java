package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record DiskFeature(BlockStateProvider stateProvider, BlockPredicate target, IntProvider radius, int halfHeight) implements Feature {
   public static final MapCodec<DiskFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(DiskFeature::stateProvider),
            BlockPredicate.CODEC.fieldOf("target").forGetter(DiskFeature::target),
            IntProviders.codec(0, 8).fieldOf("radius").forGetter(DiskFeature::radius),
            Codec.intRange(0, 4).fieldOf("half_height").forGetter(DiskFeature::halfHeight)
         )
         .apply(i, DiskFeature::new)
   );

   @Override
   public MapCodec<DiskFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      boolean placedAny = false;
      int originY = origin.getY();
      int top = originY + this.halfHeight;
      int bottom = originY - this.halfHeight - 1;
      int r = this.radius.sample(random);
      BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

      for (BlockPos columnPos : BlockPos.betweenClosed(origin.offset(-r, 0, -r), origin.offset(r, 0, r))) {
         int xd = columnPos.getX() - origin.getX();
         int zd = columnPos.getZ() - origin.getZ();
         if (xd * xd + zd * zd <= r * r) {
            placedAny |= this.placeColumn(level, random, top, bottom, mutablePos.set(columnPos));
         }
      }

      return placedAny;
   }

   private boolean placeColumn(final WorldGenLevel level, final RandomSource random, final int top, final int bottom, final BlockPos.MutableBlockPos pos) {
      boolean placedAny = false;
      boolean placedAbove = false;

      for (int y = top; y > bottom; y--) {
         pos.setY(y);
         if (this.target.test(level, pos)) {
            BlockState state = this.stateProvider.getOptionalState(level, random, pos);
            if (state != null) {
               level.setBlock(pos, state, 2);
               if (!placedAbove) {
                  this.markAboveForPostProcessing(level, pos);
               }

               placedAny = true;
               placedAbove = true;
            }
         } else {
            placedAbove = false;
         }
      }

      return placedAny;
   }
}
