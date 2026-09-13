package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FilteredFunction extends LootItemConditionalFunction {
   public static final MapCodec<FilteredFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_450091_ -> commonFields(p_450091_)
         .and(
            p_450091_.group(
               ItemPredicate.CODEC.fieldOf("item_filter").forGetter(p_450088_ -> p_450088_.filter),
               LootItemFunctions.ROOT_CODEC.optionalFieldOf("on_pass").forGetter(p_450089_ -> p_450089_.onPass),
               LootItemFunctions.ROOT_CODEC.optionalFieldOf("on_fail").forGetter(p_450090_ -> p_450090_.onFail)
            )
         )
         .apply(p_450091_, FilteredFunction::new)
   );
   private final ItemPredicate filter;
   private final Optional<LootItemFunction> onPass;
   private final Optional<LootItemFunction> onFail;

   FilteredFunction(List<LootItemCondition> p_333409_, ItemPredicate p_454248_, Optional<LootItemFunction> p_451708_, Optional<LootItemFunction> p_452548_) {
      super(p_333409_);
      this.filter = p_454248_;
      this.onPass = p_451708_;
      this.onFail = p_452548_;
   }

   @Override
   public LootItemFunctionType<FilteredFunction> getType() {
      return LootItemFunctions.FILTERED;
   }

   @Override
   public ItemStack run(ItemStack p_330820_, LootContext p_333822_) {
      Optional<LootItemFunction> optional = this.filter.test(p_330820_) ? this.onPass : this.onFail;
      return optional.isPresent() ? optional.get().apply(p_330820_, p_333822_) : p_330820_;
   }

   @Override
   public void validate(ValidationContext p_336040_) {
      super.validate(p_336040_);
      this.onPass.ifPresent(p_450093_ -> p_450093_.validate(p_336040_.forChild(new ProblemReporter.FieldPathElement("on_pass"))));
      this.onFail.ifPresent(p_450095_ -> p_450095_.validate(p_336040_.forChild(new ProblemReporter.FieldPathElement("on_fail"))));
   }

   public static FilteredFunction.Builder filtered(ItemPredicate p_459669_) {
      return new FilteredFunction.Builder(p_459669_);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<FilteredFunction.Builder> {
      private final ItemPredicate itemPredicate;
      private Optional<LootItemFunction> onPass = Optional.empty();
      private Optional<LootItemFunction> onFail = Optional.empty();

      Builder(ItemPredicate p_457221_) {
         this.itemPredicate = p_457221_;
      }

      protected FilteredFunction.Builder getThis() {
         return this;
      }

      public FilteredFunction.Builder onPass(Optional<LootItemFunction> p_454584_) {
         this.onPass = p_454584_;
         return this;
      }

      public FilteredFunction.Builder onFail(Optional<LootItemFunction> p_451739_) {
         this.onFail = p_451739_;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new FilteredFunction(this.getConditions(), this.itemPredicate, this.onPass, this.onFail);
      }
   }
}
