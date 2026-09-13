package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.datafixers.Products.P4;
import com.mojang.datafixers.Products.P5;
import com.mojang.datafixers.Products.P9;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public class ConcentricRingsStructurePlacement extends AbstractSpreadingStructurePlacement {
   public static final MapCodec<ConcentricRingsStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> codec(i).apply(i, ConcentricRingsStructurePlacement::new)
   );
   private final int distance;
   private final int spread;
   private final int count;
   private final HolderSet<Biome> preferredBiomes;

   private static P9<Mu<ConcentricRingsStructurePlacement>, Vec3i, AbstractSpreadingStructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<AbstractSpreadingStructurePlacement.ExclusionZone>, Integer, Integer, Integer, HolderSet<Biome>> codec(
      final Instance<ConcentricRingsStructurePlacement> i
   ) {
      P5<Mu<ConcentricRingsStructurePlacement>, Vec3i, AbstractSpreadingStructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<AbstractSpreadingStructurePlacement.ExclusionZone>> placement = placementCodec(
         i
      );
      P4<Mu<ConcentricRingsStructurePlacement>, Integer, Integer, Integer, HolderSet<Biome>> rings = i.group(
         Codec.intRange(0, 1023).fieldOf("distance").forGetter(ConcentricRingsStructurePlacement::distance),
         Codec.intRange(0, 1023).fieldOf("spread").forGetter(ConcentricRingsStructurePlacement::spread),
         Codec.intRange(1, 4095).fieldOf("count").forGetter(ConcentricRingsStructurePlacement::count),
         RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("preferred_biomes").forGetter(ConcentricRingsStructurePlacement::preferredBiomes)
      );
      return new P9(placement.t1(), placement.t2(), placement.t3(), placement.t4(), placement.t5(), rings.t1(), rings.t2(), rings.t3(), rings.t4());
   }

   public ConcentricRingsStructurePlacement(
      final Vec3i locateOffset,
      final AbstractSpreadingStructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
      final float frequency,
      final int salt,
      final Optional<AbstractSpreadingStructurePlacement.ExclusionZone> exclusionZone,
      final int distance,
      final int spread,
      final int count,
      final HolderSet<Biome> preferredBiomes
   ) {
      super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
      this.distance = distance;
      this.spread = spread;
      this.count = count;
      this.preferredBiomes = preferredBiomes;
   }

   public ConcentricRingsStructurePlacement(final int distance, final int spread, final int count, final HolderSet<Biome> preferredBiomes) {
      this(
         Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty(), distance, spread, count, preferredBiomes
      );
   }

   public int distance() {
      return this.distance;
   }

   public int spread() {
      return this.spread;
   }

   public int count() {
      return this.count;
   }

   public HolderSet<Biome> preferredBiomes() {
      return this.preferredBiomes;
   }

   @Override
   protected boolean isPlacementChunk(final ChunkGeneratorStructureState generatorState, final int sourceX, final int sourceZ) {
      List<ChunkPos> positions = generatorState.getRingPositionsFor(this);
      return positions == null ? false : positions.contains(new ChunkPos(sourceX, sourceZ));
   }

   @Override
   public MapCodec<ConcentricRingsStructurePlacement> codec() {
      return CODEC;
   }
}
