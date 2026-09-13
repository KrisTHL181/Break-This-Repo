package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class SetLoreFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetLoreFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_327612_ -> commonFields(p_327612_)
         .and(
            p_327612_.group(
               ComponentSerialization.CODEC.sizeLimitedListOf(256).fieldOf("lore").forGetter(p_300292_ -> p_300292_.lore),
               ListOperation.codec(256).forGetter(p_327611_ -> p_327611_.mode),
               LootContext.EntityTarget.CODEC.optionalFieldOf("entity").forGetter(p_300757_ -> p_300757_.resolutionContext)
            )
         )
         .apply(p_327612_, SetLoreFunction::new)
   );
   private final List<Component> lore;
   private final ListOperation mode;
   private final Optional<LootContext.EntityTarget> resolutionContext;

   public SetLoreFunction(List<LootItemCondition> p_81085_, List<Component> p_300257_, ListOperation p_333397_, Optional<LootContext.EntityTarget> p_301400_) {
      super(p_81085_);
      this.lore = List.copyOf(p_300257_);
      this.mode = p_333397_;
      this.resolutionContext = p_301400_;
   }

   @Override
   public LootItemFunctionType<SetLoreFunction> getType() {
      return LootItemFunctions.SET_LORE;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.resolutionContext.<Set<ContextKey<?>>>map(p_450100_ -> Set.of(p_450100_.contextParam())).orElseGet(Set::of);
   }

   @Override
   public ItemStack run(ItemStack p_81089_, LootContext p_81090_) {
      p_81089_.update(DataComponents.LORE, ItemLore.EMPTY, p_327614_ -> new ItemLore(this.updateLore(p_327614_, p_81090_)));
      return p_81089_;
   }

   private List<Component> updateLore(@Nullable ItemLore p_329508_, LootContext p_335535_) {
      if (p_329508_ == null && this.lore.isEmpty()) {
         return List.of();
      }

      UnaryOperator<Component> unaryoperator = SetNameFunction.createResolver(p_335535_, this.resolutionContext.orElse(null));
      List<Component> list = this.lore.stream().map(unaryoperator).toList();
      return this.mode.apply(p_329508_.lines(), list, 256);
   }

   public static SetLoreFunction.Builder setLore() {
      return new SetLoreFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetLoreFunction.Builder> {
      private Optional<LootContext.EntityTarget> resolutionContext = Optional.empty();
      private final com.google.common.collect.ImmutableList.Builder<Component> lore = ImmutableList.builder();
      private ListOperation mode = ListOperation.Append.INSTANCE;

      public SetLoreFunction.Builder setMode(ListOperation p_333307_) {
         this.mode = p_333307_;
         return this;
      }

      public SetLoreFunction.Builder setResolutionContext(LootContext.EntityTarget p_165450_) {
         this.resolutionContext = Optional.of(p_165450_);
         return this;
      }

      public SetLoreFunction.Builder addLine(Component p_165452_) {
         this.lore.add(p_165452_);
         return this;
      }

      protected SetLoreFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetLoreFunction(this.getConditions(), this.lore.build(), this.mode, this.resolutionContext);
      }
   }
}
