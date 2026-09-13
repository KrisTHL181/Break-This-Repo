package net.minecraft.world.timeline;

import java.util.Optional;
import java.util.function.LongSupplier;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.KeyframeTrackSampler;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public class AttributeTrackSampler<Value, Argument> implements EnvironmentAttributeLayer.TimeBased<Value> {
   private final AttributeModifier<Value, Argument> modifier;
   private final KeyframeTrackSampler<Argument> argumentSampler;
   private final LongSupplier dayTimeGetter;
   private int cachedTickId;
   private @Nullable Argument cachedArgument;

   public AttributeTrackSampler(
      Optional<Integer> p_458254_,
      AttributeModifier<Value, Argument> p_457534_,
      KeyframeTrack<Argument> p_453682_,
      LerpFunction<Argument> p_458321_,
      LongSupplier p_457723_
   ) {
      this.modifier = p_457534_;
      this.dayTimeGetter = p_457723_;
      this.argumentSampler = p_453682_.bakeSampler(p_458254_, p_458321_);
   }

   @Override
   public Value applyTimeBased(Value p_453402_, int p_459566_) {
      if (this.cachedArgument == null || p_459566_ != this.cachedTickId) {
         this.cachedTickId = p_459566_;
         this.cachedArgument = this.argumentSampler.sample(this.dayTimeGetter.getAsLong());
      }

      return this.modifier.apply(p_453402_, this.cachedArgument);
   }
}
