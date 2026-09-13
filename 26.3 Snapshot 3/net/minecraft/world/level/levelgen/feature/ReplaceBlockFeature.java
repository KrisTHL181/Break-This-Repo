package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record ReplaceBlockFeature(List<BlockReplacement> replacements) implements Feature {
   public static final MapCodec<ReplaceBlockFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.list(BlockReplacement.CODEC).fieldOf("targets").forGetter(ReplaceBlockFeature::replacements)).apply(i, ReplaceBlockFeature::new)
   );

   @Override
   public MapCodec<ReplaceBlockFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      for (BlockReplacement replacement : this.replacements) {
         if (replacement.target().test(level.getBlockState(origin), origin, random)) {
            level.setBlock(origin, replacement.state(), 2);
            break;
         }
      }

      return true;
   }
}
