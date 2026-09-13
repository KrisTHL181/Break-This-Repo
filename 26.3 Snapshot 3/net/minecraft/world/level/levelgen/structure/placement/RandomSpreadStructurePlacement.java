package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class RandomSpreadStructurePlacement extends AbstractSpreadingStructurePlacement {
   // ===== 修改：显式指定 mapCodec 类型参数，并显式指定 validate lambda 参数类型 =====
   public static final MapCodec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.<RandomSpreadStructurePlacement>mapCodec(
         i -> AbstractSpreadingStructurePlacement.<RandomSpreadStructurePlacement>placementCodec(i)
            .and(
               i.group(
                  Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                  Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                  RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType)
               )
            )
            .apply(i, RandomSpreadStructurePlacement::new)
      )
      .validate((RandomSpreadStructurePlacement c) -> RandomSpreadStructurePlacement.validate(c));

   private final int spacing;
   private final int separation;
   private final RandomSpreadType spreadType;

   private static DataResult<RandomSpreadStructurePlacement> validate(final RandomSpreadStructurePlacement c) {
      return c.spacing <= c.separation ? DataResult.error(() -> "Spacing has to be larger than separation") : DataResult.success(c);
   }

   public RandomSpreadStructurePlacement(
      final Vec3i locateOffset,
      final AbstractSpreadingStructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
      final float frequency,
      final int salt,
      final Optional<AbstractSpreadingStructurePlacement.ExclusionZone> exclusionZone,
      final int spacing,
      final int separation,
      final RandomSpreadType spreadType
   ) {
      super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
      this.spacing = spacing;
      this.separation = separation;
      this.spreadType = spreadType;
   }

   public RandomSpreadStructurePlacement(final int spacing, final int separation, final RandomSpreadType spreadType, final int salt) {
      this(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(), spacing, separation, spreadType);
   }

   public int spacing() {
      return this.spacing;
   }

   public int separation() {
      return this.separation;
   }

   public RandomSpreadType spreadType() {
      return this.spreadType;
   }

   public ChunkPos getPotentialStructureChunk(final long seed, final int sourceX, final int sourceZ) {
      int spacedGridX = Math.floorDiv(sourceX, this.spacing);
      int spacedGridZ = Math.floorDiv(sourceZ, this.spacing);
      WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
      random.setLargeFeatureWithSalt(seed, spacedGridX, spacedGridZ, this.salt());
      int limit = this.spacing - this.separation;
      int spreadX = this.spreadType.evaluate(random, limit);
      int spreadZ = this.spreadType.evaluate(random, limit);
      return new ChunkPos(spacedGridX * this.spacing + spreadX, spacedGridZ * this.spacing + spreadZ);
   }

   @Override
   protected boolean isPlacementChunk(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ) {
      ChunkPos chunkPos = this.getPotentialStructureChunk(state.getLevelSeed(), sourceX, sourceZ);
      return chunkPos.x() == sourceX && chunkPos.z() == sourceZ;
   }

   @Override
   public MapCodec<RandomSpreadStructurePlacement> codec() {
      return CODEC;
   }
}