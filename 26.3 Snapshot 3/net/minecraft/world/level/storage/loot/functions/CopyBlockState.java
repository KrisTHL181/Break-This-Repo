package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState extends LootItemConditionalFunction {
   public static final MapCodec<CopyBlockState> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(f -> f.block),
               Codec.STRING.listOf().fieldOf("properties").forGetter(f -> f.properties.stream().map(Property::getName).toList())
            )
         )
         .apply(i, CopyBlockState::new)
   );
   private final Holder<Block> block;
   private final Set<Property<?>> properties;

   private CopyBlockState(final List<LootItemCondition> predicates, final Holder<Block> block, final Set<Property<?>> properties) {
      super(predicates);
      this.block = block;
      this.properties = properties;
   }

   private CopyBlockState(final List<LootItemCondition> predicates, final Holder<Block> block, final List<String> propertyNames) {
      this(predicates, block, propertyNames.stream().map(block.value().getStateDefinition()::getProperty).filter(Objects::nonNull).collect(Collectors.toSet()));
   }

   @Override
   public MapCodec<CopyBlockState> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.BLOCK_STATE);
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
      if (state != null) {
         itemStack.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY, itemState -> {
            for (Property<?> property : this.properties) {
               if (state.hasProperty(property)) {
                  itemState = itemState.with(property, state);
               }
            }

            return itemState;
         });
      }

      return itemStack;
   }

   public static CopyBlockState.Builder copyState(final Block block) {
      return new CopyBlockState.Builder(block);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
      private final Holder<Block> block;
      private final com.google.common.collect.ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

      private Builder(final Block block) {
         this.block = block.builtInRegistryHolder();
      }

      public CopyBlockState.Builder copy(final Property<?> property) {
         if (!this.block.value().getStateDefinition().getProperties().contains(property)) {
            throw new IllegalStateException("Property " + property + " is not present on block " + this.block);
         }

         this.properties.add(property);
         return this;
      }

      protected CopyBlockState.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyBlockState(this.getConditions(), this.block, this.properties.build());
      }
   }
}
