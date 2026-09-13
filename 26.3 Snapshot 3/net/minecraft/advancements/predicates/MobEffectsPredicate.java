package net.minecraft.advancements.predicates;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public record MobEffectsPredicate(Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap) implements Predicate<MobEffectInstance> {
   public static final Codec<MobEffectsPredicate> CODEC = Codec.unboundedMap(MobEffect.CODEC, MobEffectsPredicate.MobEffectInstancePredicate.CODEC)
      .xmap(MobEffectsPredicate::new, MobEffectsPredicate::effectMap);
   public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectsPredicate> STREAM_CODEC = MobEffectsPredicate.MobEffectInstancePredicate.MAP_STREAM_CODEC
      .map(MobEffectsPredicate::new, MobEffectsPredicate::effectMap);

   public boolean matches(final Entity entity) {
      return entity instanceof LivingEntity living && this.matches(living.getActiveEffectsMap());
   }

   public boolean matches(final LivingEntity entity) {
      return this.matches(entity.getActiveEffectsMap());
   }

   public boolean matches(final Map<Holder<MobEffect>, MobEffectInstance> effects) {
      for (Entry<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> entry : this.effectMap.entrySet()) {
         MobEffectInstance instance = effects.get(entry.getKey());
         if (!entry.getValue().matches(instance)) {
            return false;
         }
      }

      return true;
   }

   public boolean test(final MobEffectInstance mobEffect) {
      MobEffectsPredicate.MobEffectInstancePredicate predicate = this.effectMap.get(mobEffect.getEffect());
      return predicate != null && predicate.matches(mobEffect);
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate> effectMap = ImmutableMap.builder();

      public static MobEffectsPredicate.Builder effects() {
         return new MobEffectsPredicate.Builder();
      }

      public MobEffectsPredicate.Builder and(final Holder<MobEffect> effect) {
         this.effectMap.put(effect, new MobEffectsPredicate.MobEffectInstancePredicate());
         return this;
      }

      public MobEffectsPredicate.Builder and(final Holder<MobEffect> effect, final MobEffectsPredicate.MobEffectInstancePredicate predicate) {
         this.effectMap.put(effect, predicate);
         return this;
      }

      public MobEffectsPredicate build() {
         return new MobEffectsPredicate(this.effectMap.build());
      }
   }

   public record MobEffectInstancePredicate(MinMaxBounds.Ints amplifier, MinMaxBounds.Ints duration, Optional<Boolean> ambient, Optional<Boolean> visible) {
      public static final Codec<MobEffectsPredicate.MobEffectInstancePredicate> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               MinMaxBounds.Ints.CODEC.optionalFieldOf("amplifier", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::amplifier),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("duration", MinMaxBounds.Ints.ANY).forGetter(MobEffectsPredicate.MobEffectInstancePredicate::duration),
               Codec.BOOL.optionalFieldOf("ambient").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::ambient),
               Codec.BOOL.optionalFieldOf("visible").forGetter(MobEffectsPredicate.MobEffectInstancePredicate::visible)
            )
            .apply(i, MobEffectsPredicate.MobEffectInstancePredicate::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectsPredicate.MobEffectInstancePredicate> STREAM_CODEC = StreamCodec.composite(
         MinMaxBounds.Ints.STREAM_CODEC,
         MobEffectsPredicate.MobEffectInstancePredicate::amplifier,
         MinMaxBounds.Ints.STREAM_CODEC,
         MobEffectsPredicate.MobEffectInstancePredicate::duration,
         ByteBufCodecs.optional(ByteBufCodecs.BOOL),
         MobEffectsPredicate.MobEffectInstancePredicate::ambient,
         ByteBufCodecs.optional(ByteBufCodecs.BOOL),
         MobEffectsPredicate.MobEffectInstancePredicate::visible,
         MobEffectsPredicate.MobEffectInstancePredicate::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, Map<Holder<MobEffect>, MobEffectsPredicate.MobEffectInstancePredicate>> MAP_STREAM_CODEC = ByteBufCodecs.map(
         HashMap::new, MobEffect.STREAM_CODEC, STREAM_CODEC
      );

      public MobEffectInstancePredicate() {
         this(MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, Optional.empty(), Optional.empty());
      }

      public boolean matches(final @Nullable MobEffectInstance instance) {
         if (instance == null) {
            return false;
         } else if (!this.amplifier.matches(instance.getAmplifier())) {
            return false;
         } else if (!this.duration.matches(instance.getDuration())) {
            return false;
         } else {
            return this.ambient.isPresent() && this.ambient.get() != instance.isAmbient()
               ? false
               : !this.visible.isPresent() || this.visible.get() == instance.isVisible();
         }
      }
   }
}
