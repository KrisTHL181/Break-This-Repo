package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomSequences extends SavedData {
   public static final Codec<RandomSequences> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.fieldOf("salt").forGetter(RandomSequences::salt),
            Codec.BOOL.optionalFieldOf("include_world_seed", true).forGetter(RandomSequences::includeWorldSeed),
            Codec.BOOL.optionalFieldOf("include_sequence_id", true).forGetter(RandomSequences::includeSequenceId),
            Codec.unboundedMap(Identifier.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(rs -> rs.sequences)
         )
         .apply(i, RandomSequences::new)
   );
   public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("random_sequences"), RandomSequences::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
   );
   private int salt;
   private boolean includeWorldSeed = true;
   private boolean includeSequenceId = true;
   private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public RandomSequences() {
   }

   private RandomSequences(final int salt, final boolean includeWorldSeed, final boolean includeSequenceId, final Map<Identifier, RandomSequence> sequences) {
      this.salt = salt;
      this.includeWorldSeed = includeWorldSeed;
      this.includeSequenceId = includeSequenceId;
      this.sequences.putAll(sequences);
   }

   public RandomSource get(final Identifier key, final long worldSeed) {
      RandomSource random = this.sequences.computeIfAbsent(key, rl -> this.createSequence(rl, worldSeed)).random();
      return new RandomSequences.DirtyMarkingRandomSource(random);
   }

   private RandomSequence createSequence(final Identifier key, final long worldSeed) {
      return this.createSequence(key, worldSeed, this.salt, this.includeWorldSeed, this.includeSequenceId);
   }

   private RandomSequence createSequence(
      final Identifier key, final long worldSeed, final int salt, final boolean includeWorldSeed, final boolean includeSequenceId
   ) {
      long seed = (includeWorldSeed ? worldSeed : 0L) ^ salt;
      return new RandomSequence(seed, includeSequenceId ? Optional.of(key) : Optional.empty());
   }

   public void forAllSequences(final BiConsumer<Identifier, RandomSequence> consumer) {
      this.sequences.forEach(consumer);
   }

   public void setSeedDefaults(final int salt, final boolean includeWorldSeed, final boolean includeSequenceId) {
      this.salt = salt;
      this.includeWorldSeed = includeWorldSeed;
      this.includeSequenceId = includeSequenceId;
   }

   public int clear() {
      int count = this.sequences.size();
      this.sequences.clear();
      return count;
   }

   public void reset(final Identifier id, final long worldSeed) {
      this.sequences.put(id, this.createSequence(id, worldSeed));
   }

   public void reset(final Identifier id, final long worldSeed, final int salt, final boolean includeWorldSeed, final boolean includeSequenceId) {
      this.sequences.put(id, this.createSequence(id, worldSeed, salt, includeWorldSeed, includeSequenceId));
   }

   private int salt() {
      return this.salt;
   }

   private boolean includeWorldSeed() {
      return this.includeWorldSeed;
   }

   private boolean includeSequenceId() {
      return this.includeSequenceId;
   }

   private class DirtyMarkingRandomSource implements RandomSource {
      private final RandomSource random;

      private DirtyMarkingRandomSource(final RandomSource random) {
         this.random = random;
      }

      @Override
      public RandomSource fork() {
         RandomSequences.this.setDirty();
         return this.random.fork();
      }

      @Override
      public PositionalRandomFactory forkPositional() {
         RandomSequences.this.setDirty();
         return this.random.forkPositional();
      }

      @Override
      public void setSeed(final long seed) {
         RandomSequences.this.setDirty();
         this.random.setSeed(seed);
      }

      @Override
      public int nextInt() {
         RandomSequences.this.setDirty();
         return this.random.nextInt();
      }

      @Override
      public int nextInt(final int bound) {
         RandomSequences.this.setDirty();
         return this.random.nextInt(bound);
      }

      @Override
      public long nextLong() {
         RandomSequences.this.setDirty();
         return this.random.nextLong();
      }

      @Override
      public boolean nextBoolean() {
         RandomSequences.this.setDirty();
         return this.random.nextBoolean();
      }

      @Override
      public float nextFloat() {
         RandomSequences.this.setDirty();
         return this.random.nextFloat();
      }

      @Override
      public double nextDouble() {
         RandomSequences.this.setDirty();
         return this.random.nextDouble();
      }

      @Override
      public double nextGaussian() {
         RandomSequences.this.setDirty();
         return this.random.nextGaussian();
      }

      @Override
      public boolean equals(final Object obj) {
         if (this == obj) {
            return true;
         } else {
            return obj instanceof RandomSequences.DirtyMarkingRandomSource other ? this.random.equals(other.random) : false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(this.random);
      }
   }
}
