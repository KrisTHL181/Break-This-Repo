package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record LootItemBlockStatePropertyCondition(Holder<Block> block, Optional<StatePropertiesPredicate> properties) implements LootItemCondition {
    public static final MapCodec<LootItemBlockStatePropertyCondition> MAP_CODEC = RecordCodecBuilder.<LootItemBlockStatePropertyCondition>mapCodec(
            i -> i.group(
                    BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(LootItemBlockStatePropertyCondition::block),
                    StatePropertiesPredicate.CODEC.optionalFieldOf("properties").forGetter(LootItemBlockStatePropertyCondition::properties)
                )
                .apply(i, LootItemBlockStatePropertyCondition::new)
        )
        .validate(LootItemBlockStatePropertyCondition::validate);

    private static DataResult<LootItemBlockStatePropertyCondition> validate(final LootItemBlockStatePropertyCondition condition) {
        return condition.properties()
            .flatMap(properties -> properties.checkState(condition.block().value().getStateDefinition()))
            .map(name -> DataResult.<LootItemBlockStatePropertyCondition>error(() -> "Block " + condition.block() + " has no property" + name))
            .orElse(DataResult.success(condition));
    }

    @Override
    public MapCodec<LootItemBlockStatePropertyCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_STATE);
    }

    public boolean test(final LootContext context) {
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        return state != null && state.is(this.block) && (this.properties.isEmpty() || this.properties.get().matches(state));
    }

    public static LootItemBlockStatePropertyCondition.Builder hasBlockStateProperties(final Block block) {
        return new LootItemBlockStatePropertyCondition.Builder(block);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Holder<Block> block;
        private Optional<StatePropertiesPredicate> properties = Optional.empty();

        public Builder(final Block block) {
            this.block = block.builtInRegistryHolder();
        }

        public LootItemBlockStatePropertyCondition.Builder setProperties(final StatePropertiesPredicate.Builder properties) {
            this.properties = properties.build();
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new LootItemBlockStatePropertyCondition(this.block, this.properties);
        }
    }
}
