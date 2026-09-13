package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jspecify.annotations.Nullable;

public class IntRange {
   private static final Codec<IntRange> RECORD_CODEC = RecordCodecBuilder.create(
      p_327547_ -> p_327547_.group(
            NumberProviders.CODEC.optionalFieldOf("min").forGetter(p_296994_ -> Optional.ofNullable(p_296994_.min)),
            NumberProviders.CODEC.optionalFieldOf("max").forGetter(p_296996_ -> Optional.ofNullable(p_296996_.max))
         )
         .apply(p_327547_, IntRange::new)
   );
   public static final Codec<IntRange> CODEC = Codec.either(Codec.INT, RECORD_CODEC)
      .xmap(p_296998_ -> (IntRange)p_296998_.map(IntRange::exact, Function.identity()), p_296997_ -> {
         OptionalInt optionalint = p_296997_.unpackExact();
         return optionalint.isPresent() ? Either.left(optionalint.getAsInt()) : Either.right(p_296997_);
      });
   private final @Nullable NumberProvider min;
   private final @Nullable NumberProvider max;
   private final IntRange.IntLimiter limiter;
   private final IntRange.IntChecker predicate;

   public Set<ContextKey<?>> getReferencedContextParams() {
      Builder<ContextKey<?>> builder = ImmutableSet.builder();
      if (this.min != null) {
         builder.addAll(this.min.getReferencedContextParams());
      }

      if (this.max != null) {
         builder.addAll(this.max.getReferencedContextParams());
      }

      return builder.build();
   }

   private IntRange(Optional<NumberProvider> p_300812_, Optional<NumberProvider> p_298905_) {
      this(p_300812_.orElse(null), p_298905_.orElse(null));
   }

   private IntRange(@Nullable NumberProvider p_165006_, @Nullable NumberProvider p_165007_) {
      this.min = p_165006_;
      this.max = p_165007_;
      if (p_165006_ == null) {
         if (p_165007_ == null) {
            this.limiter = (p_165050_, p_165051_) -> p_165051_;
            this.predicate = (p_165043_, p_165044_) -> true;
         } else {
            this.limiter = (p_165054_, p_165055_) -> Math.min(p_165007_.getInt(p_165054_), p_165055_);
            this.predicate = (p_165047_, p_165048_) -> p_165048_ <= p_165007_.getInt(p_165047_);
         }
      } else if (p_165007_ == null) {
         this.limiter = (p_165033_, p_165034_) -> Math.max(p_165006_.getInt(p_165033_), p_165034_);
         this.predicate = (p_165019_, p_165020_) -> p_165020_ >= p_165006_.getInt(p_165019_);
      } else {
         this.limiter = (p_165038_, p_165039_) -> Mth.clamp(p_165039_, p_165006_.getInt(p_165038_), p_165007_.getInt(p_165038_));
         this.predicate = (p_165024_, p_165025_) -> p_165025_ >= p_165006_.getInt(p_165024_) && p_165025_ <= p_165007_.getInt(p_165024_);
      }
   }

   public static IntRange exact(int p_165010_) {
      ConstantValue constantvalue = ConstantValue.exactly(p_165010_);
      return new IntRange(Optional.of(constantvalue), Optional.of(constantvalue));
   }

   public static IntRange range(int p_165012_, int p_165013_) {
      return new IntRange(Optional.of(ConstantValue.exactly(p_165012_)), Optional.of(ConstantValue.exactly(p_165013_)));
   }

   public static IntRange lowerBound(int p_165027_) {
      return new IntRange(Optional.of(ConstantValue.exactly(p_165027_)), Optional.empty());
   }

   public static IntRange upperBound(int p_165041_) {
      return new IntRange(Optional.empty(), Optional.of(ConstantValue.exactly(p_165041_)));
   }

   public int clamp(LootContext p_165015_, int p_165016_) {
      return this.limiter.apply(p_165015_, p_165016_);
   }

   public boolean test(LootContext p_165029_, int p_165030_) {
      return this.predicate.test(p_165029_, p_165030_);
   }

   private OptionalInt unpackExact() {
      return Objects.equals(this.min, this.max)
            && this.min instanceof ConstantValue constantvalue
            && Math.floor(constantvalue.value()) == constantvalue.value()
         ? OptionalInt.of((int)constantvalue.value())
         : OptionalInt.empty();
   }

   @FunctionalInterface
   interface IntChecker {
      boolean test(LootContext var1, int var2);
   }

   @FunctionalInterface
   interface IntLimiter {
      int apply(LootContext var1, int var2);
   }
}
