package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetComponentsFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetComponentsFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_327903_ -> commonFields(p_327903_)
         .and(DataComponentPatch.CODEC.fieldOf("components").forGetter(p_331439_ -> p_331439_.components))
         .apply(p_327903_, SetComponentsFunction::new)
   );
   private final DataComponentPatch components;

   private SetComponentsFunction(List<LootItemCondition> p_334087_, DataComponentPatch p_331768_) {
      super(p_334087_);
      this.components = p_331768_;
   }

   @Override
   public LootItemFunctionType<SetComponentsFunction> getType() {
      return LootItemFunctions.SET_COMPONENTS;
   }

   @Override
   public ItemStack run(ItemStack p_336175_, LootContext p_333804_) {
      p_336175_.applyComponentsAndValidate(this.components);
      return p_336175_;
   }

   public static <T> LootItemConditionalFunction.Builder<?> setComponent(DataComponentType<T> p_334396_, T p_330070_) {
      return simpleBuilder(p_328648_ -> new SetComponentsFunction(p_328648_, DataComponentPatch.builder().set(p_334396_, p_330070_).build()));
   }
}
