package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public record TimeCheck(Holder<WorldClock> clock, Optional<Long> period, IntRange value) implements LootItemCondition {
    public static final MapCodec<TimeCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                WorldClock.CODEC.fieldOf("clock").forGetter(TimeCheck::clock),
                Codec.LONG.optionalFieldOf("period").forGetter(TimeCheck::period),
                IntRange.CODEC.fieldOf("value").forGetter(TimeCheck::value)
            )
            .apply(i, TimeCheck::new)
    );

    @Override
    public MapCodec<TimeCheck> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(final ValidationContext context) {
        LootItemCondition.super.validate(context);
        Validatable.validate(context, "value", this.value);
    }

    public boolean test(final LootContext context) {
        ServerLevel level = context.getLevel();
        long time = level.clockManager().getTotalTicks(this.clock);
        if (this.period.isPresent()) {
            time %= this.period.get();
        }

        return this.value.test(context, (int)time);
    }

    public static TimeCheck.Builder time(final Holder<WorldClock> clock, final IntRange value) {
        return new TimeCheck.Builder(clock, value);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Holder<WorldClock> clock;
        private Optional<Long> period = Optional.empty();
        private final IntRange value;

        public Builder(final Holder<WorldClock> clock, final IntRange value) {
            this.clock = clock;
            this.value = value;
        }

        public TimeCheck.Builder setPeriod(final long period) {
            this.period = Optional.of(period);
            return this;
        }

        public TimeCheck build() {
            return new TimeCheck(this.clock, this.period, this.value);
        }
    }
}