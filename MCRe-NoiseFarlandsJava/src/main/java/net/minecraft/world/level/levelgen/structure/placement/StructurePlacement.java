package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.datafixers.Products.P5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public abstract class StructurePlacement {
    public static final Codec<StructurePlacement> CODEC = BuiltInRegistries.STRUCTURE_PLACEMENT
        .byNameCodec()
        .dispatch(StructurePlacement::type, StructurePlacementType::codec);
    private static final int HIGHLY_ARBITRARY_RANDOM_SALT = 10387320;
    private final Vec3i locateOffset;
    private final StructurePlacement.FrequencyReductionMethod frequencyReductionMethod;
    private final float frequency;
    private final int salt;
    private final Optional<StructurePlacement.ExclusionZone> exclusionZone;

    protected static <S extends StructurePlacement> P5<Mu<S>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>> placementCodec(
        final Instance<S> i
    ) {
        return i.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(StructurePlacement::locateOffset),
            StructurePlacement.FrequencyReductionMethod.CODEC
                .optionalFieldOf("frequency_reduction_method", StructurePlacement.FrequencyReductionMethod.DEFAULT)
                .forGetter(StructurePlacement::frequencyReductionMethod),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(StructurePlacement::frequency),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(StructurePlacement::salt),
            StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(StructurePlacement::exclusionZone)
        );
    }

    protected StructurePlacement(
        final Vec3i locateOffset,
        final StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
        final float frequency,
        final int salt,
        final Optional<StructurePlacement.ExclusionZone> exclusionZone
    ) {
        this.locateOffset = locateOffset;
        this.frequencyReductionMethod = frequencyReductionMethod;
        this.frequency = frequency;
        this.salt = salt;
        this.exclusionZone = exclusionZone;
    }

    protected Vec3i locateOffset() {
        return this.locateOffset;
    }

    protected StructurePlacement.FrequencyReductionMethod frequencyReductionMethod() {
        return this.frequencyReductionMethod;
    }

    protected float frequency() {
        return this.frequency;
    }

    protected int salt() {
        return this.salt;
    }

    protected Optional<StructurePlacement.ExclusionZone> exclusionZone() {
        return this.exclusionZone;
    }

    public boolean isStructureChunk(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ) {
        return this.isPlacementChunk(state, sourceX, sourceZ)
            && this.applyAdditionalChunkRestrictions(sourceX, sourceZ, state.getLevelSeed())
            && this.applyInteractionsWithOtherStructures(state, sourceX, sourceZ);
    }

    public boolean applyAdditionalChunkRestrictions(final int sourceX, final int sourceZ, final long levelSeed) {
        return !(this.frequency < 1.0F) || this.frequencyReductionMethod.shouldGenerate(levelSeed, this.salt, sourceX, sourceZ, this.frequency);
    }

    public boolean applyInteractionsWithOtherStructures(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ) {
        return !this.exclusionZone.isPresent() || !this.exclusionZone.get().isPlacementForbidden(state, sourceX, sourceZ);
    }

    protected abstract boolean isPlacementChunk(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ);

    public BlockPos getLocatePos(final ChunkPos chunkPos) {
        return new BlockPos((int)chunkPos.getMinBlockX(), 0, (int)chunkPos.getMinBlockZ()).offset(this.locateOffset());
    }

    public abstract StructurePlacementType<?> type();

    private static boolean probabilityReducer(final long seed, final int salt, final int sourceX, final int sourceZ, final float probability) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, salt, sourceX, sourceZ);
        return random.nextFloat() < probability;
    }

    private static boolean legacyProbabilityReducerWithDouble(final long seed, final int salt, final int sourceX, final int sourceZ, final float probability) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(seed, sourceX, sourceZ);
        return random.nextDouble() < probability;
    }

    private static boolean legacyArbitrarySaltProbabilityReducer(final long seed, final int salt, final int sourceX, final int sourceZ, final float probability) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, sourceX, sourceZ, 10387320);
        return random.nextFloat() < probability;
    }

    private static boolean legacyPillagerOutpostReducer(final long seed, final int salt, final int sourceX, final int sourceZ, final float probability) {
        int cx = sourceX >> 4;
        int cz = sourceZ >> 4;
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setSeed(cx ^ cz << 4 ^ seed);
        random.nextInt();
        return random.nextInt((int)(1.0F / probability)) == 0;
    }

    @Deprecated
    public record ExclusionZone(Holder<StructureSet> otherSet, int chunkCount) {
        public static final Codec<StructurePlacement.ExclusionZone> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    RegistryFileCodec.create(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC, false)
                        .fieldOf("other_set")
                        .forGetter(StructurePlacement.ExclusionZone::otherSet),
                    Codec.intRange(1, 16).fieldOf("chunk_count").forGetter(StructurePlacement.ExclusionZone::chunkCount)
                )
                .apply(i, StructurePlacement.ExclusionZone::new)
        );

        private boolean isPlacementForbidden(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ) {
            return state.hasStructureChunkInRange(this.otherSet, sourceX, sourceZ, this.chunkCount);
        }
    }

    @FunctionalInterface
    public interface FrequencyReducer {
        boolean shouldGenerate(long seed, final int salt, final int sourceX, final int sourceZ, float probability);
    }

    public enum FrequencyReductionMethod implements StringRepresentable {
        DEFAULT("default", StructurePlacement::probabilityReducer),
        LEGACY_TYPE_1("legacy_type_1", StructurePlacement::legacyPillagerOutpostReducer),
        LEGACY_TYPE_2("legacy_type_2", StructurePlacement::legacyArbitrarySaltProbabilityReducer),
        LEGACY_TYPE_3("legacy_type_3", StructurePlacement::legacyProbabilityReducerWithDouble);

        public static final Codec<StructurePlacement.FrequencyReductionMethod> CODEC = StringRepresentable.fromEnum(
            StructurePlacement.FrequencyReductionMethod::values
        );
        private final String name;
        private final StructurePlacement.FrequencyReducer reducer;

        FrequencyReductionMethod(final String name, final StructurePlacement.FrequencyReducer reducer) {
            this.name = name;
            this.reducer = reducer;
        }

        public boolean shouldGenerate(final long seed, final int salt, final int sourceX, final int sourceZ, final float probability) {
            return this.reducer.shouldGenerate(seed, salt, sourceX, sourceZ, probability);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}