package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public record UniformGenerator(NumberProvider min, NumberProvider max) implements NumberProvider {
    public static final MapCodec<UniformGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                NumberProviders.CODEC.fieldOf("min").forGetter(UniformGenerator::min), NumberProviders.CODEC.fieldOf("max").forGetter(UniformGenerator::max)
            )
            .apply(i, UniformGenerator::new)
    );

    @Override
    public MapCodec<UniformGenerator> codec() {
        return MAP_CODEC;
    }

    public static UniformGenerator between(final float min, final float max) {
        return new UniformGenerator(ConstantValue.exactly(min), ConstantValue.exactly(max));
    }

    @Override
    public int getInt(final LootContext context) {
        return Mth.nextInt(context.getRandom(), this.min.getInt(context), this.max.getInt(context));
    }

    @Override
    public float getFloat(final LootContext context) {
        return Mth.nextFloat(context.getRandom(), this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public void validate(final ValidationContext context) {
        NumberProvider.super.validate(context);
        Validatable.validate(context, "min", this.min);
        Validatable.validate(context, "max", this.max);
    }
}