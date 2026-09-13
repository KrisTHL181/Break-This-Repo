package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.modifier.AttributeModifier;

public record AttributeTrack<Value, Argument>(AttributeModifier<Value, Argument> modifier, KeyframeTrack<Argument> argumentTrack) {
   public static <Value> Codec<AttributeTrack<Value, ?>> createCodec(EnvironmentAttribute<Value> p_451460_) {
      MapCodec<AttributeModifier<Value, ?>> mapcodec = p_451460_.type().modifierCodec().optionalFieldOf("modifier", AttributeModifier.override());
      return mapcodec.dispatch(AttributeTrack::modifier, Util.memoize(p_452671_ -> createCodecWithModifier(p_451460_, p_452671_)));
   }

   private static <Value, Argument> MapCodec<AttributeTrack<Value, Argument>> createCodecWithModifier(
      EnvironmentAttribute<Value> p_452031_, AttributeModifier<Value, Argument> p_455596_
   ) {
      return KeyframeTrack.mapCodec(p_455596_.argumentCodec(p_452031_))
         .xmap(p_450400_ -> new AttributeTrack<>(p_455596_, p_450400_), AttributeTrack::argumentTrack);
   }

   public AttributeTrackSampler<Value, Argument> bakeSampler(EnvironmentAttribute<Value> p_457066_, Optional<Integer> p_455593_, LongSupplier p_458090_) {
      return new AttributeTrackSampler<>(p_455593_, this.modifier, this.argumentTrack, this.modifier.argumentKeyframeLerp(p_457066_), p_458090_);
   }

   public static DataResult<AttributeTrack<?, ?>> validatePeriod(AttributeTrack<?, ?> p_459868_, int p_457041_) {
      return KeyframeTrack.validatePeriod(p_459868_.argumentTrack(), p_457041_).map(p_455588_ -> p_459868_);
   }
}
