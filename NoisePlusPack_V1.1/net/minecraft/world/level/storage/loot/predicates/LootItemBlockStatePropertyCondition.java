package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record LootItemBlockStatePropertyCondition(Holder<Block> block, Optional<StatePropertiesPredicate> properties) implements LootItemCondition {
   public static final MapCodec<LootItemBlockStatePropertyCondition> CODEC = RecordCodecBuilder.mapCodec(
         p_450111_ -> p_450111_.group(
               BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(LootItemBlockStatePropertyCondition::block),
               StatePropertiesPredicate.CODEC.optionalFieldOf("properties").forGetter(LootItemBlockStatePropertyCondition::properties)
            )
            .apply(p_450111_, LootItemBlockStatePropertyCondition::new)
      )
      .validate(LootItemBlockStatePropertyCondition::validate);

   private static DataResult<LootItemBlockStatePropertyCondition> validate(LootItemBlockStatePropertyCondition p_298673_) {
      return p_298673_.properties()
         .flatMap(p_450110_ -> p_450110_.checkState(p_298673_.block().value().getStateDefinition()))
         .map(p_298572_ -> DataResult.error(() -> "Block " + p_298673_.block() + " has no property" + p_298572_))
         .orElse(DataResult.success(p_298673_));
   }

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.BLOCK_STATE_PROPERTY;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.BLOCK_STATE);
   }

   public boolean test(LootContext p_81772_) {
      BlockState blockstate = p_81772_.getOptionalParameter(LootContextParams.BLOCK_STATE);
      return blockstate != null && blockstate.is(this.block) && (this.properties.isEmpty() || this.properties.get().matches(blockstate));
   }

   public static LootItemBlockStatePropertyCondition.Builder hasBlockStateProperties(Block p_81770_) {
      return new LootItemBlockStatePropertyCondition.Builder(p_81770_);
   }

   public static class Builder implements LootItemCondition.Builder {
      private final Holder<Block> block;
      private Optional<StatePropertiesPredicate> properties = Optional.empty();

      public Builder(Block p_81783_) {
         this.block = p_81783_.builtInRegistryHolder();
      }

      public LootItemBlockStatePropertyCondition.Builder setProperties(StatePropertiesPredicate.Builder p_457394_) {
         this.properties = p_457394_.build();
         return this;
      }

      @Override
      public LootItemCondition build() {
         return new LootItemBlockStatePropertyCondition(this.block, this.properties);
      }
   }
}
