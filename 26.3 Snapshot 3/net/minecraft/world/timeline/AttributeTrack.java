package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;

public record AttributeTrack<Value, Argument>(AttributeModifier<Value, Argument> modifier, KeyframeTrack<Argument> argumentTrack) {
   public static <Value> Codec<AttributeTrack<Value, ?>> createCodec(final EnvironmentAttribute<Value> attribute) {
      MapCodec<AttributeModifier<Value, ?>> modifierCodec = attribute.type().modifierCodec().optionalFieldOf("modifier", AttributeModifier.override());
      return modifierCodec.dispatch(AttributeTrack::modifier, Util.memoize(modifier -> createCodecWithModifier(attribute, modifier)));
   }

   private static <Value, Argument> MapCodec<AttributeTrack<Value, Argument>> createCodecWithModifier(
      final EnvironmentAttribute<Value> attribute, final AttributeModifier<Value, Argument> modifier
   ) {
      return KeyframeTrack.mapCodec(modifier.argumentCodec(attribute)).xmap(track -> new AttributeTrack<>(modifier, track), AttributeTrack::argumentTrack);
   }

   public AttributeTrackSampler<Value, Argument> bakeSampler(
      final EnvironmentAttribute<Value> attribute, final Holder<WorldClock> clock, final Optional<Integer> periodTicks, final ClockManager clockManager
   ) {
      return new AttributeTrackSampler<>(clock, periodTicks, this.modifier, this.argumentTrack, this.modifier.argumentKeyframeLerp(attribute), clockManager);
   }

   public static DataResult<AttributeTrack<?, ?>> validatePeriod(final AttributeTrack<?, ?> track, final int periodTicks) {
      return KeyframeTrack.validatePeriod(track.argumentTrack(), periodTicks).map(ignored -> track);
   }
}
