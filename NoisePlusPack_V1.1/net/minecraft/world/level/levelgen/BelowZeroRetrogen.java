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
      .xmap(p_188484_ -> BitSet.valueOf(p_188484_.toArray()), p_188482_ -> LongStream.of(p_188482_.toLongArray()));
   private static final Codec<ChunkStatus> NON_EMPTY_CHUNK_STATUS = BuiltInRegistries.CHUNK_STATUS
      .byNameCodec()
      .comapFlatMap(
         p_327451_ -> p_327451_ == ChunkStatus.EMPTY ? DataResult.error(() -> "target_status cannot be empty") : DataResult.success(p_327451_),
         Function.identity()
      );
   public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create(
      p_327450_ -> p_327450_.group(
            NON_EMPTY_CHUNK_STATUS.fieldOf("target_status").forGetter(BelowZeroRetrogen::targetStatus),
            BITSET_CODEC.lenientOptionalFieldOf("missing_bedrock")
               .forGetter(p_188480_ -> p_188480_.missingBedrock.isEmpty() ? Optional.empty() : Optional.of(p_188480_.missingBedrock))
         )
         .apply(p_327450_, BelowZeroRetrogen::new)
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

   private BelowZeroRetrogen(ChunkStatus p_335764_, Optional<BitSet> p_188465_) {
      this.targetStatus = p_335764_;
      this.missingBedrock = p_188465_.orElse(EMPTY);
   }

   public static void replaceOldBedrock(ProtoChunk p_188475_) {
      int i = 4;
      BlockPos.betweenClosed(0, 0, 0, 15, 4, 15).forEach(p_391033_ -> {
         if (p_188475_.getBlockState(p_391033_).is(Blocks.BEDROCK)) {
            p_188475_.setBlockState(p_391033_, Blocks.DEEPSLATE.defaultBlockState());
         }
      });
   }

   public void applyBedrockMask(ProtoChunk p_198222_) {
      LevelHeightAccessor levelheightaccessor = p_198222_.getHeightAccessorForGeneration();
      int i = levelheightaccessor.getMinY();
      int j = levelheightaccessor.getMaxY();

      for (int k = 0; k < 16; k++) {
         for (int l = 0; l < 16; l++) {
            if (this.hasBedrockHole(k, l)) {
               BlockPos.betweenClosed(k, i, l, k, j, l).forEach(p_391035_ -> p_198222_.setBlockState(p_391035_, Blocks.AIR.defaultBlockState()));
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

   public boolean hasBedrockHole(int p_198215_, int p_198216_) {
      return this.missingBedrock.get((p_198216_ & 15) * 16 + (p_198215_ & 15));
   }

   public static BiomeResolver getBiomeResolver(BiomeResolver p_204532_, ChunkAccess p_204533_) {
      if (!p_204533_.isUpgrading()) {
         return p_204532_;
      }

      Predicate<ResourceKey<Biome>> predicate = RETAINED_RETROGEN_BIOMES::contains;
      return (p_204538_, p_204539_, p_204540_, p_204541_) -> {
         Holder<Biome> holder = p_204532_.getNoiseBiome(p_204538_, p_204539_, p_204540_, p_204541_);
         return holder.is(predicate) ? holder : p_204533_.getNoiseBiome(p_204538_, 0, p_204540_);
      };
   }
}
