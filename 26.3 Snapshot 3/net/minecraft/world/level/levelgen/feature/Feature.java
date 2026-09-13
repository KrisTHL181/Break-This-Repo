package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public interface Feature {
   Codec<Feature> DIRECT_CODEC = BuiltInRegistries.FEATURE_TYPE.byNameCodec().dispatch(Feature::codec, t -> t);
   Codec<Holder<Feature>> CODEC = RegistryFileCodec.create(Registries.FEATURE, DIRECT_CODEC);
   Codec<HolderSet<Feature>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.FEATURE, DIRECT_CODEC);

   MapCodec<? extends Feature> codec();

   boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin);

   default Stream<Holder<Feature>> getSubFeatures() {
      return Stream.empty();
   }

   default void setBlock(final LevelWriter level, final BlockPos pos, final BlockState blockState) {
      level.setBlockAndUpdate(pos, blockState);
   }

   default void safeSetBlock(final WorldGenLevel level, final BlockPos pos, final BlockState state, final Predicate<BlockState> canReplace) {
      if (canReplace.test(level.getBlockState(pos))) {
         level.setBlock(pos, state, 2);
      }
   }

   default void markAboveForPostProcessing(final WorldGenLevel level, final BlockPos placePos) {
      BlockPos.MutableBlockPos pos = placePos.mutable();

      for (int i = 0; i < 2; i++) {
         pos.move(Direction.UP);
         if (level.getBlockState(pos).isAir()) {
            return;
         }

         level.getChunk(pos).markPosForPostProcessing(pos);
      }
   }
}
