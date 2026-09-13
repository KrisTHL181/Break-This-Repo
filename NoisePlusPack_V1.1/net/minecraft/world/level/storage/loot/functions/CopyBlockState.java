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
   public static final MapCodec<CopyBlockState> CODEC = RecordCodecBuilder.mapCodec(
      p_360670_ -> commonFields(p_360670_)
         .and(
            p_360670_.group(
               BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(p_297074_ -> p_297074_.block),
               Codec.STRING.listOf().fieldOf("properties").forGetter(p_297075_ -> p_297075_.properties.stream().map(Property::getName).toList())
            )
         )
         .apply(p_360670_, CopyBlockState::new)
   );
   private final Holder<Block> block;
   private final Set<Property<?>> properties;

   CopyBlockState(List<LootItemCondition> p_301076_, Holder<Block> p_298008_, Set<Property<?>> p_80052_) {
      super(p_301076_);
      this.block = p_298008_;
      this.properties = p_80052_;
   }

   private CopyBlockState(List<LootItemCondition> p_297498_, Holder<Block> p_299449_, List<String> p_298231_) {
      this(
         p_297498_, p_299449_, p_298231_.stream().map(p_299449_.value().getStateDefinition()::getProperty).filter(Objects::nonNull).collect(Collectors.toSet())
      );
   }

   @Override
   public LootItemFunctionType<CopyBlockState> getType() {
      return LootItemFunctions.COPY_STATE;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.BLOCK_STATE);
   }

   @Override
   protected ItemStack run(ItemStack p_80060_, LootContext p_80061_) {
      BlockState blockstate = p_80061_.getOptionalParameter(LootContextParams.BLOCK_STATE);
      if (blockstate != null) {
         p_80060_.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY, p_327562_ -> {
            for (Property<?> property : this.properties) {
               if (blockstate.hasProperty(property)) {
                  p_327562_ = p_327562_.with(property, blockstate);
               }
            }

            return p_327562_;
         });
      }

      return p_80060_;
   }

   public static CopyBlockState.Builder copyState(Block p_80063_) {
      return new CopyBlockState.Builder(p_80063_);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
      private final Holder<Block> block;
      private final com.google.common.collect.ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

      Builder(Block p_80079_) {
         this.block = p_80079_.builtInRegistryHolder();
      }

      public CopyBlockState.Builder copy(Property<?> p_80085_) {
         if (!this.block.value().getStateDefinition().getProperties().contains(p_80085_)) {
            throw new IllegalStateException("Property " + p_80085_ + " is not present on block " + this.block);
         }

         this.properties.add(p_80085_);
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
