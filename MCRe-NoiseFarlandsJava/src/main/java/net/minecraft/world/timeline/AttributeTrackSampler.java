package net.minecraft.world.timeline;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyframeTrack;
import net.minecraft.util.KeyframeTrackSampler;
import net.minecraft.world.attribute.EnvironmentAttributeLayer;
import net.minecraft.world.attribute.LerpFunction;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;
import org.jspecify.annotations.Nullable;

public class AttributeTrackSampler<Value, Argument> implements EnvironmentAttributeLayer.TimeBased<Value> {
    private final Holder<WorldClock> clock;
    private final AttributeModifier<Value, Argument> modifier;
    private final KeyframeTrackSampler<Argument> argumentSampler;
    private final ClockManager clockManager;
    private int cachedTickId;
    private @Nullable Argument cachedArgument;

    public AttributeTrackSampler(
        final Holder<WorldClock> clock,
        final Optional<Integer> periodTicks,
        final AttributeModifier<Value, Argument> modifier,
        final KeyframeTrack<Argument> argumentTrack,
        final LerpFunction<Argument> argumentLerp,
        final ClockManager clockManager
    ) {
        this.clock = clock;
        this.modifier = modifier;
        this.clockManager = clockManager;
        this.argumentSampler = argumentTrack.bakeSampler(periodTicks, argumentLerp);
    }

    @Override
    public Value applyTimeBased(final Value baseValue, final int cacheTickId) {
        if (this.cachedArgument == null || cacheTickId != this.cachedTickId) {
            this.cachedTickId = cacheTickId;
            this.cachedArgument = this.argumentSampler.sample(this.clockManager.getTotalTicks(this.clock));
        }

        return this.modifier.apply(baseValue, this.cachedArgument);
    }
}