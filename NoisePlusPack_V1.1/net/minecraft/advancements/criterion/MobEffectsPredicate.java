package net.minecraft.advancements.criterion;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public record MobEffectsPredicate(Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap) {
   public static final Codec<MobEffectsPredicate> CODEC = Codec.unboundedMap(MobEffect.CODEC, MobEffectsPredicate.MobEffectInstancePredicate.CODEC)
      .xmap(MobEffectsPredicate::new, MobEffectsPredicate::effectMap);

   public boolean matches(Entity p_458524_) {
      return p_458524_ instanceof LivingEntity livingentity && this.matches(livingentity.getActiveEffectsMap());
   }

   public boolean matches(LivingEntity p_453645_) {
      return this.matches(p_453645_.getActiveEffectsMap());
   }

   public boolean matches(Map<Holder<MobEffect>, MobEffectInstance> p_455961_) {
      for (Entry<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effectMap.entrySet()) {
         MobEffectInstance mobeffectinstance = p_455961_.get(entry.getKey());
         if (!entry.getValue().matches(mobeffectinstance)) {
            return false;
         }
      }

      return true;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap = ImmutableMap.builder();

      public static MobEffectsPredicate.Builder effects() {
         return new MobEffectsPredicate.Builder();
      }

      public MobEffectsPredicate.Builder and(Holder<MobEffect> p_459841_) {
         this.effectMap.put(p_459841_, new MobEffectsPredicate.MobEffectInstancePredicate());
         return this;
      }

      public MobEffectsPredicate.Builder and(Holder<MobEffect> p_451697_, MobEffectsPredicate.MobEffectInstancePredicate p_455490_) {
         this.effectMap.put(p_451697_, p_455490_);
         return this;
      }

      public Optional<MobEffectsPredicate> build() {
         return Optional.of(new MobEffectsPredicate(this.effectMap.build()));
      }
   }

   public record MobEffectInstancePredicate(MinMaxBounds.Ints amplifier, MinMaxBounds.Ints duration, Optional<Boolean> ambient, Optional<Boolean> visible) {
      public static final Codec<MobEffectsPredicate.MobEffectInstancePredicate> CODEC = RecordCodecBuilder.create(
         p_453857_ -> p_453857_.group(
               MinMaxBounds.Ints.CODEC.optionalFieldOf("amplifier", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::amplifier),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("duration", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::duration),
               Codec.BOOL.optionalFieldOf("ambient").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::ambient),
               Codec.BOOL.optionalFieldOf("visible").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::visible)
            )
            .apply(p_453857_, MobEffectsPredicate.MobEffectInstancePredicate::new)
      );

      public MobEffectInstancePredicate() {
         this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, Optional.empty(), Optional.empty());
      }

      public boolean matches(@Nullable MobEffectInstance p_454845_) {
         if (p_454845_ == null) {
            return false;
         } else if (!this.amplifier.matches(p_454845_.getAmplifier())) {
            return false;
         } else if (!this.duration.matches(p_454845_.getDuration())) {
            return false;
         } else {
            return this.ambient.isPresent() && this.ambient.get() != p_454845_.isAmbient()
               ? false
               : !this.visible.isPresent() || this.visible.get() == p_454845_.isVisible();
         }
      }
   }
}
