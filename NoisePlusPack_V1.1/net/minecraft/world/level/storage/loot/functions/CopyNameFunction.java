package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyNameFunction extends LootItemConditionalFunction {
   public static final MapCodec<CopyNameFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_450084_ -> commonFields(p_450084_)
         .and(LootContextArg.ENTITY_OR_BLOCK.fieldOf("source").forGetter(p_450083_ -> p_450083_.source))
         .apply(p_450084_, CopyNameFunction::new)
   );
   private final LootContextArg<Object> source;

   private CopyNameFunction(List<LootItemCondition> p_300985_, LootContextArg<?> p_451737_) {
      super(p_300985_);
      this.source = LootContextArg.cast((LootContextArg<? extends Object>)p_451737_);
   }

   @Override
   public LootItemFunctionType<CopyNameFunction> getType() {
      return LootItemFunctions.COPY_NAME;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   @Override
   public ItemStack run(ItemStack p_80185_, LootContext p_80186_) {
      if (this.source.get(p_80186_) instanceof Nameable nameable) {
         p_80185_.set(DataComponents.CUSTOM_NAME, nameable.getCustomName());
      }

      return p_80185_;
   }

   public static LootItemConditionalFunction.Builder<?> copyName(LootContextArg<?> p_456670_) {
      return simpleBuilder(p_450086_ -> new CopyNameFunction(p_450086_, p_456670_));
   }
}
