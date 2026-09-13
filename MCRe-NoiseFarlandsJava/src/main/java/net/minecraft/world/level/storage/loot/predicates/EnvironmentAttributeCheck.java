package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record EnvironmentAttributeCheck<Value>(EnvironmentAttribute<Value> attribute, Value value) implements LootItemCondition {
    public static final MapCodec<EnvironmentAttributeCheck<?>> MAP_CODEC = EnvironmentAttributes.CODEC
        .dispatchMap("attribute", EnvironmentAttributeCheck::attribute, EnvironmentAttributeCheck::createCodec);

    private static <Value> MapCodec<EnvironmentAttributeCheck<Value>> createCodec(final EnvironmentAttribute<Value> attribute) {
        return attribute.valueCodec()
            .fieldOf("value")
            .xmap(value -> new EnvironmentAttributeCheck<>(attribute, (Value)value), EnvironmentAttributeCheck::value);
    }

    @Override
    public MapCodec<EnvironmentAttributeCheck<Value>> codec() {
        return (MapCodec<EnvironmentAttributeCheck<Value>>)(MapCodec)MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.attribute.isPositional() ? Set.of(LootContextParams.ORIGIN) : Set.of();
    }

    public boolean test(final LootContext context) {
        Value actualValue = context.getLevel().environmentAttributes().getValue(context, this.attribute);
        return this.value.equals(actualValue);
    }

    public static <Value> LootItemCondition.Builder environmentAttribute(final EnvironmentAttribute<Value> attribute, final Value value) {
        return () -> new EnvironmentAttributeCheck<>(attribute, value);
    }
}
