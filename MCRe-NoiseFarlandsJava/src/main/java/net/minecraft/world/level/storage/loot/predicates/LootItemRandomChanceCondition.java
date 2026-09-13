package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record LootItemRandomChanceCondition(NumberProvider chance) implements LootItemCondition {
    public static final MapCodec<LootItemRandomChanceCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(NumberProviders.CODEC.fieldOf("chance").forGetter(LootItemRandomChanceCondition::chance)).apply(i, LootItemRandomChanceCondition::new)
    );

    @Override
    public MapCodec<LootItemRandomChanceCondition> codec() {
        return MAP_CODEC;
    }

    public boolean test(final LootContext context) {
        float probability = this.chance.getFloat(context);
        return context.getRandom().nextFloat() < probability;
    }

    public static LootItemCondition.Builder randomChance(final float probability) {
        return () -> new LootItemRandomChanceCondition(ConstantValue.exactly(probability));
    }

    public static LootItemCondition.Builder randomChance(final NumberProvider probability) {
        return () -> new LootItemRandomChanceCondition(probability);
    }
}