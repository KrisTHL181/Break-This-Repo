package net.minecraft.world.timeline;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.level.Level;

public class Timeline {
   public static final Codec<Holder<Timeline>> CODEC = RegistryFixedCodec.create(Registries.TIMELINE);
   private static final Codec<Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>>> TRACKS_CODEC = Codec.dispatchedMap(
      EnvironmentAttributes.CODEC, Util.memoize(AttributeTrack::createCodec)
   );
   public static final Codec<Timeline> DIRECT_CODEC = RecordCodecBuilder.create(
         p_451719_ -> p_451719_.group(
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("period_ticks").forGetter(p_453317_ -> p_453317_.periodTicks),
               TRACKS_CODEC.optionalFieldOf("tracks", Map.of()).forGetter(p_458374_ -> p_458374_.tracks)
            )
            .apply(p_451719_, Timeline::new)
      )
      .validate(Timeline::validateInternal);
   public static final Codec<Timeline> NETWORK_CODEC = DIRECT_CODEC.xmap(Timeline::filterSyncableTracks, Timeline::filterSyncableTracks);
   private final Optional<Integer> periodTicks;
   private final Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks;

   private static Timeline filterSyncableTracks(Timeline p_455349_) {
      Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> map = Map.copyOf(Maps.filterKeys(p_455349_.tracks, EnvironmentAttribute::isSyncable));
      return new Timeline(p_455349_.periodTicks, map);
   }

   Timeline(Optional<Integer> p_460367_, Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> p_456030_) {
      this.periodTicks = p_460367_;
      this.tracks = p_456030_;
   }

   private static DataResult<Timeline> validateInternal(Timeline p_455741_) {
      if (p_455741_.periodTicks.isEmpty()) {
         return DataResult.success(p_455741_);
      }

      int i = p_455741_.periodTicks.get();
      DataResult<Timeline> dataresult = DataResult.success(p_455741_);

      for (AttributeTrack<?, ?> attributetrack : p_455741_.tracks.values()) {
         dataresult = dataresult.apply2stable((p_459726_, p_461064_) -> p_459726_, AttributeTrack.validatePeriod(attributetrack, i));
      }

      return dataresult;
   }

   public static Timeline.Builder builder() {
      return new Timeline.Builder();
   }

   public long getCurrentTicks(Level p_453089_) {
      long i = this.getTotalTicks(p_453089_);
      return this.periodTicks.isEmpty() ? i : i % this.periodTicks.get().intValue();
   }

   public long getTotalTicks(Level p_457040_) {
      return p_457040_.getDayTime();
   }

   public Optional<Integer> periodTicks() {
      return this.periodTicks;
   }

   public Set<EnvironmentAttribute<?>> attributes() {
      return this.tracks.keySet();
   }

   public <Value> AttributeTrackSampler<Value, ?> createTrackSampler(EnvironmentAttribute<Value> p_459847_, LongSupplier p_457004_) {
      AttributeTrack<Value, ?> attributetrack = (AttributeTrack<Value, ?>)this.tracks.get(p_459847_);
      if (attributetrack == null) {
         throw new IllegalStateException("Timeline has no track for " + p_459847_);
      } else {
         return attributetrack.bakeSampler(p_459847_, this.periodTicks, p_457004_);
      }
   }

   public static class Builder {
      private Optional<Integer> periodTicks = Optional.empty();
      private final com.google.common.collect.ImmutableMap.Builder<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks = ImmutableMap.builder();

      Builder() {
      }

      public Timeline.Builder setPeriodTicks(int p_453271_) {
         this.periodTicks = Optional.of(p_453271_);
         return this;
      }

      public <Value, Argument> Timeline.Builder addModifierTrack(
         EnvironmentAttribute<Value> p_455532_, AttributeModifier<Value, Argument> p_460873_, Consumer<KeyframeTrack.Builder<Argument>> p_450715_
      ) {
         p_455532_.type().checkAllowedModifier(p_460873_);
         KeyframeTrack.Builder<Argument> builder = new KeyframeTrack.Builder<>();
         p_450715_.accept(builder);
         this.tracks.put(p_455532_, new AttributeTrack<>(p_460873_, builder.build()));
         return this;
      }

      public <Value> Timeline.Builder addTrack(EnvironmentAttribute<Value> p_460826_, Consumer<KeyframeTrack.Builder<Value>> p_457805_) {
         return this.addModifierTrack(p_460826_, AttributeModifier.override(), p_457805_);
      }

      public Timeline build() {
         return new Timeline(this.periodTicks, this.tracks.build());
      }
   }
}
