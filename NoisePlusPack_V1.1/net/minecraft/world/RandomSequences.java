package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
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
      p_449375_ -> p_449375_.group(
            Codec.INT.fieldOf("salt").forGetter(RandomSequences::salt),
            Codec.BOOL.optionalFieldOf("include_world_seed", true).forGetter(RandomSequences::includeWorldSeed),
            Codec.BOOL.optionalFieldOf("include_sequence_id", true).forGetter(RandomSequences::includeSequenceId),
            Codec.unboundedMap(Identifier.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(p_390463_ -> p_390463_.sequences)
         )
         .apply(p_449375_, RandomSequences::new)
   );
   public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<>(
      "random_sequences", RandomSequences::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
   );
   private int salt;
   private boolean includeWorldSeed = true;
   private boolean includeSequenceId = true;
   private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public RandomSequences() {
   }

   private RandomSequences(int p_397246_, boolean p_391262_, boolean p_391812_, Map<Identifier, RandomSequence> p_393825_) {
      this.salt = p_397246_;
      this.includeWorldSeed = p_391262_;
      this.includeSequenceId = p_391812_;
      this.sequences.putAll(p_393825_);
   }

   public RandomSource get(Identifier p_457476_, long p_458238_) {
      RandomSource randomsource = this.sequences.computeIfAbsent(p_457476_, p_449377_ -> this.createSequence(p_449377_, p_458238_)).random();
      return new RandomSequences.DirtyMarkingRandomSource(randomsource);
   }

   private RandomSequence createSequence(Identifier p_450713_, long p_454682_) {
      return this.createSequence(p_450713_, p_454682_, this.salt, this.includeWorldSeed, this.includeSequenceId);
   }

   private RandomSequence createSequence(Identifier p_454550_, long p_459663_, int p_299267_, boolean p_300525_, boolean p_297272_) {
      long i = (p_300525_ ? p_459663_ : 0L) ^ p_299267_;
      return new RandomSequence(i, p_297272_ ? Optional.of(p_454550_) : Optional.empty());
   }

   public void forAllSequences(BiConsumer<Identifier, RandomSequence> p_299883_) {
      this.sequences.forEach(p_299883_);
   }

   public void setSeedDefaults(int p_299968_, boolean p_298395_, boolean p_298518_) {
      this.salt = p_299968_;
      this.includeWorldSeed = p_298395_;
      this.includeSequenceId = p_298518_;
   }

   public int clear() {
      int i = this.sequences.size();
      this.sequences.clear();
      return i;
   }

   public void reset(Identifier p_453516_, long p_458199_) {
      this.sequences.put(p_453516_, this.createSequence(p_453516_, p_458199_));
   }

   public void reset(Identifier p_458303_, long p_459419_, int p_455321_, boolean p_458404_, boolean p_459283_) {
      this.sequences.put(p_458303_, this.createSequence(p_458303_, p_459419_, p_455321_, p_458404_, p_459283_));
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

   class DirtyMarkingRandomSource implements RandomSource {
      private final RandomSource random;

      DirtyMarkingRandomSource(final RandomSource p_299209_) {
         this.random = p_299209_;
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
      public void setSeed(long p_300098_) {
         RandomSequences.this.setDirty();
         this.random.setSeed(p_300098_);
      }

      @Override
      public int nextInt() {
         RandomSequences.this.setDirty();
         return this.random.nextInt();
      }

      @Override
      public int nextInt(int p_301106_) {
         RandomSequences.this.setDirty();
         return this.random.nextInt(p_301106_);
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
      public boolean equals(Object p_299603_) {
         if (this == p_299603_) {
            return true;
         } else {
            return p_299603_ instanceof RandomSequences.DirtyMarkingRandomSource randomsequences$dirtymarkingrandomsource
               ? this.random.equals(randomsequences$dirtymarkingrandomsource.random)
               : false;
         }
      }
   }
}
