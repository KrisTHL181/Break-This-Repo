package net.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NestedLootTable extends LootPoolSingletonContainer {
   public static final MapCodec<NestedLootTable> CODEC = RecordCodecBuilder.mapCodec(
      p_391132_ -> p_391132_.group(Codec.either(LootTable.KEY_CODEC, LootTable.DIRECT_CODEC).fieldOf("value").forGetter(p_331624_ -> p_331624_.contents))
         .and(singletonFields(p_391132_))
         .apply(p_391132_, NestedLootTable::new)
   );
   public static final ProblemReporter.PathElement INLINE_LOOT_TABLE_PATH_ELEMENT = new ProblemReporter.PathElement() {
      @Override
      public String get() {
         return "->{inline}";
      }
   };
   private final Either<ResourceKey<LootTable>, LootTable> contents;

   private NestedLootTable(
      Either<ResourceKey<LootTable>, LootTable> p_335218_, int p_332597_, int p_330218_, List<LootItemCondition> p_335913_, List<LootItemFunction> p_331388_
   ) {
      super(p_332597_, p_330218_, p_335913_, p_331388_);
      this.contents = p_335218_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.LOOT_TABLE;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_329435_, LootContext p_332786_) {
      ((LootTable)this.contents.map(p_360669_ -> p_332786_.getResolver().get(p_360669_).map(Holder::value).orElse(LootTable.EMPTY), p_328175_ -> p_328175_))
         .getRandomItemsRaw(p_332786_, p_329435_);
   }

   @Override
   public void validate(ValidationContext p_331194_) {
      Optional<ResourceKey<LootTable>> optional = this.contents.left();
      if (optional.isPresent()) {
         ResourceKey<LootTable> resourcekey = optional.get();
         if (!p_331194_.allowsReferences()) {
            p_331194_.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(resourcekey));
            return;
         }

         if (p_331194_.hasVisitedElement(resourcekey)) {
            p_331194_.reportProblem(new ValidationContext.RecursiveReferenceProblem(resourcekey));
            return;
         }
      }

      super.validate(p_331194_);
      this.contents
         .ifLeft(
            p_360667_ -> p_331194_.resolver()
               .get(p_360667_)
               .ifPresentOrElse(
                  p_405790_ -> ((LootTable)p_405790_.value())
                     .validate(p_331194_.enterElement(new ProblemReporter.ElementReferencePathElement(p_360667_), p_360667_)),
                  () -> p_331194_.reportProblem(new ValidationContext.MissingReferenceProblem(p_360667_))
               )
         )
         .ifRight(p_405787_ -> p_405787_.validate(p_331194_.forChild(INLINE_LOOT_TABLE_PATH_ELEMENT)));
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceKey<LootTable> p_332425_) {
      return simpleBuilder(
         (p_331287_, p_328654_, p_335079_, p_330542_) -> new NestedLootTable(Either.left(p_332425_), p_331287_, p_328654_, p_335079_, p_330542_)
      );
   }

   public static LootPoolSingletonContainer.Builder<?> inlineLootTable(LootTable p_336216_) {
      return simpleBuilder(
         (p_327921_, p_332453_, p_332156_, p_328257_) -> new NestedLootTable(Either.right(p_336216_), p_327921_, p_332453_, p_332156_, p_328257_)
      );
   }
}
