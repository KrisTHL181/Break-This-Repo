package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyComponentsFunction extends LootItemConditionalFunction {
   private static final Codec<LootContextArg<DataComponentGetter>> GETTER_CODEC = LootContextArg.createArgCodec(
      p_450080_ -> p_450080_.anyEntity(CopyComponentsFunction.DirectSource::new)
         .anyBlockEntity(CopyComponentsFunction.BlockEntitySource::new)
         .anyItemStack(CopyComponentsFunction.DirectSource::new)
   );
   public static final MapCodec<CopyComponentsFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_450079_ -> commonFields(p_450079_)
         .and(
            p_450079_.group(
               GETTER_CODEC.fieldOf("source").forGetter(p_450078_ -> p_450078_.source),
               DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(p_330902_ -> p_330902_.include),
               DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(p_331318_ -> p_331318_.exclude)
            )
         )
         .apply(p_450079_, CopyComponentsFunction::new)
   );
   private final LootContextArg<DataComponentGetter> source;
   private final Optional<List<DataComponentType<?>>> include;
   private final Optional<List<DataComponentType<?>>> exclude;
   private final Predicate<DataComponentType<?>> bakedPredicate;

   CopyComponentsFunction(
      List<LootItemCondition> p_332739_,
      LootContextArg<DataComponentGetter> p_459788_,
      Optional<List<DataComponentType<?>>> p_332029_,
      Optional<List<DataComponentType<?>>> p_329656_
   ) {
      super(p_332739_);
      this.source = p_459788_;
      this.include = p_332029_.map(List::copyOf);
      this.exclude = p_329656_.map(List::copyOf);
      List<Predicate<DataComponentType<?>>> list = new ArrayList<>(2);
      p_329656_.ifPresent(p_329848_ -> list.add(p_331276_ -> !p_329848_.contains(p_331276_)));
      p_332029_.ifPresent(p_331486_ -> list.add(p_331486_::contains));
      this.bakedPredicate = Util.allOf(list);
   }

   @Override
   public LootItemFunctionType<CopyComponentsFunction> getType() {
      return LootItemFunctions.COPY_COMPONENTS;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   @Override
   public ItemStack run(ItemStack p_329465_, LootContext p_328771_) {
      DataComponentGetter datacomponentgetter = this.source.get(p_328771_);
      if (datacomponentgetter != null) {
         if (datacomponentgetter instanceof DataComponentMap datacomponentmap) {
            p_329465_.applyComponents(datacomponentmap.filter(this.bakedPredicate));
         } else {
            Collection<DataComponentType<?>> collection = this.exclude.orElse(List.of());
            this.include.map(Collection::stream).orElse(BuiltInRegistries.DATA_COMPONENT_TYPE.listElements().map(Holder::value)).forEach(p_422267_ -> {
               if (!collection.contains(p_422267_)) {
                  TypedDataComponent<?> typeddatacomponent = datacomponentgetter.getTyped(p_422267_);
                  if (typeddatacomponent != null) {
                     p_329465_.set(typeddatacomponent);
                  }
               }
            });
         }
      }

      return p_329465_;
   }

   public static CopyComponentsFunction.Builder copyComponentsFromEntity(ContextKey<? extends Entity> p_422628_) {
      return new CopyComponentsFunction.Builder(new CopyComponentsFunction.DirectSource<>(p_422628_));
   }

   public static CopyComponentsFunction.Builder copyComponentsFromBlockEntity(ContextKey<? extends BlockEntity> p_429680_) {
      return new CopyComponentsFunction.Builder(new CopyComponentsFunction.BlockEntitySource(p_429680_));
   }

   record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements LootContextArg.Getter<BlockEntity, DataComponentGetter> {
      public DataComponentGetter get(BlockEntity p_425299_) {
         return p_425299_.collectComponents();
      }
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyComponentsFunction.Builder> {
      private final LootContextArg<DataComponentGetter> source;
      private Optional<com.google.common.collect.ImmutableList.Builder<DataComponentType<?>>> include = Optional.empty();
      private Optional<com.google.common.collect.ImmutableList.Builder<DataComponentType<?>>> exclude = Optional.empty();

      Builder(LootContextArg<DataComponentGetter> p_452814_) {
         this.source = p_452814_;
      }

      public CopyComponentsFunction.Builder include(DataComponentType<?> p_329871_) {
         if (this.include.isEmpty()) {
            this.include = Optional.of(ImmutableList.builder());
         }

         this.include.get().add(p_329871_);
         return this;
      }

      public CopyComponentsFunction.Builder exclude(DataComponentType<?> p_332922_) {
         if (this.exclude.isEmpty()) {
            this.exclude = Optional.of(ImmutableList.builder());
         }

         this.exclude.get().add(p_332922_);
         return this;
      }

      protected CopyComponentsFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new CopyComponentsFunction(
            this.getConditions(),
            this.source,
            this.include.map(com.google.common.collect.ImmutableList.Builder::build),
            this.exclude.map(com.google.common.collect.ImmutableList.Builder::build)
         );
      }
   }

   record DirectSource<T extends DataComponentGetter>(ContextKey<? extends T> contextParam) implements LootContextArg.Getter<T, DataComponentGetter> {
      public DataComponentGetter get(T p_454339_) {
         return p_454339_;
      }
   }
}
