package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class BelowZeroRetrogen {
   private static final BitSet EMPTY = new BitSet(0);
   private static final Codec<BitSet> BITSET_CODEC = Codec.LONG_STREAM
      .xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> LongStream.of(bitSet.toLongArray()));
   private static final Codec<ChunkStatus> NON_EMPTY_CHUNK_STATUS = BuiltInRegistries.CHUNK_STATUS
      .byNameCodec()
      .comapFlatMap(
         status -> status == ChunkStatus.EMPTY ? DataResult.error(() -> "target_status cannot be empty") : DataResult.success(status), Function.identity()
      );
   public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            NON_EMPTY_CHUNK_STATUS.fieldOf("target_status").forGetter(BelowZeroRetrogen::targetStatus),
            BITSET_CODEC.lenientOptionalFieldOf("missing_bedrock")
               .forGetter(b -> b.missingBedrock.isEmpty() ? Optional.empty() : Optional.of(b.missingBedrock))
         )
         .apply(i, BelowZeroRetrogen::new)
   );
   private static final Set<ResourceKey<Biome>> RETAINED_RETROGEN_BIOMES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK);
   public static final LevelHeightAccessor UPGRADE_HEIGHT_ACCESSOR = new LevelHeightAccessor() {
      @Override
      public int getHeight() {
         return 64;
      }

      @Override
      public int getMinY() {
         return -64;
      }
   };
   private final ChunkStatus targetStatus;
   private final BitSet missingBedrock;

   private BelowZeroRetrogen(final ChunkStatus targetStatus, final Optional<BitSet> missingBedrock) {
      this.targetStatus = targetStatus;
      this.missingBedrock = missingBedrock.orElse(EMPTY);
   }

   public static void replaceOldBedrock(final ProtoChunk chunk) {
      int maxGeneratedBedrockY = 4;
      BlockPos.betweenClosed(0, 0, 0, 15, 4, 15).forEach(pos -> {
         if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) {
            chunk.setBlockState(pos, Blocks.DEEPSLATE.defaultBlockState());
         }
      });
   }

   public void applyBedrockMask(final ProtoChunk chunk) {
      LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
      int minY = heightAccessor.getMinY();
      int maxY = heightAccessor.getMaxY();

      for (int x = 0; x < 16; x++) {
         for (int z = 0; z < 16; z++) {
            if (this.hasBedrockHole(x, z)) {
               BlockPos.betweenClosed(x, minY, z, x, maxY, z).forEach(pos -> chunk.setBlockState(pos, Blocks.AIR.defaultBlockState()));
            }
         }
      }
   }

   public ChunkStatus targetStatus() {
      return this.targetStatus;
   }

   public boolean hasBedrockHoles() {
      return !this.missingBedrock.isEmpty();
   }

   public boolean hasBedrockHole(final int x, final int z) {
      return this.missingBedrock.get((z & 15) * 16 + (x & 15));
   }

   public static BiomeResolver getBiomeResolver(final BiomeResolver biomeResolver, final ChunkAccess protoChunk) {
      if (!protoChunk.isUpgrading()) {
         return biomeResolver;
      }

      Predicate<ResourceKey<Biome>> retainedBiomes = RETAINED_RETROGEN_BIOMES::contains;
      return (quartX, quartY, quartZ, sampler) -> {
         Holder<Biome> noiseBiome = biomeResolver.getNoiseBiome(quartX, quartY, quartZ, sampler);
         return noiseBiome.is(retainedBiomes) ? noiseBiome : protoChunk.getNoiseBiome(quartX, 0, quartZ);
      };
   }
}
