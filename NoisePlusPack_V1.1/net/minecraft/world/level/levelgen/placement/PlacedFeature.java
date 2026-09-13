package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PlacedFeature(Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
   public static final Codec<PlacedFeature> DIRECT_CODEC = RecordCodecBuilder.create(
      p_191788_ -> p_191788_.group(
            ConfiguredFeature.CODEC.fieldOf("feature").forGetter(p_204928_ -> p_204928_.feature),
            PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter(p_191796_ -> p_191796_.placement)
         )
         .apply(p_191788_, PlacedFeature::new)
   );
   public static final Codec<Holder<PlacedFeature>> CODEC = RegistryFileCodec.create(Registries.PLACED_FEATURE, DIRECT_CODEC);
   public static final Codec<HolderSet<PlacedFeature>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC);
   public static final Codec<List<HolderSet<PlacedFeature>>> LIST_OF_LISTS_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC, true)
      .listOf();

   public boolean place(WorldGenLevel p_226358_, ChunkGenerator p_226359_, RandomSource p_226360_, BlockPos p_226361_) {
      return this.placeWithContext(new PlacementContext(p_226358_, p_226359_, Optional.empty()), p_226360_, p_226361_);
   }

   public boolean placeWithBiomeCheck(WorldGenLevel p_226378_, ChunkGenerator p_226379_, RandomSource p_226380_, BlockPos p_226381_) {
      return this.placeWithContext(new PlacementContext(p_226378_, p_226379_, Optional.of(this)), p_226380_, p_226381_);
   }

   private boolean placeWithContext(PlacementContext p_226369_, RandomSource p_226370_, BlockPos p_226371_) {
      Stream<BlockPos> stream = Stream.of(p_226371_);

      for (PlacementModifier placementmodifier : this.placement) {
         stream = stream.flatMap(p_226376_ -> placementmodifier.getPositions(p_226369_, p_226370_, p_226376_));
      }

      ConfiguredFeature<?, ?> configuredfeature = this.feature.value();
      MutableBoolean mutableboolean = new MutableBoolean();
      stream.forEach(p_422237_ -> {
         if (configuredfeature.place(p_226369_.getLevel(), p_226369_.generator(), p_226370_, p_422237_)) {
            mutableboolean.setTrue();
            if (SharedConstants.DEBUG_FEATURE_COUNT) {
               FeatureCountTracker.featurePlaced(p_226369_.getLevel().getLevel(), configuredfeature, p_226369_.topFeature());
            }
         }
      });
      return mutableboolean.isTrue();
   }

   public Stream<ConfiguredFeature<?, ?>> getFeatures() {
      return this.feature.value().getFeatures();
   }

   @Override
   public String toString() {
      return "Placed " + this.feature;
   }
}
