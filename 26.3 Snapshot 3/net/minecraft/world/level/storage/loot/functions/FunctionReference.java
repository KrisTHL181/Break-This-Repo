package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<FunctionReference> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(ResourceKey.codec(Registries.ITEM_MODIFIER).fieldOf("name").forGetter(f -> f.name)).apply(i, FunctionReference::new)
   );
   private final ResourceKey<LootItemFunction> name;

   private FunctionReference(final List<LootItemCondition> predicates, final ResourceKey<LootItemFunction> name) {
      super(predicates);
      this.name = name;
   }

   @Override
   public MapCodec<FunctionReference> codec() {
      return MAP_CODEC;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validateReference(context, this.name);
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      LootItemFunction function = context.getResolver().get(this.name).map(Holder::value).orElse(null);
      if (function == null) {
         LOGGER.warn("Unknown function: {}", this.name.identifier());
         return itemStack;
      }

      LootContext.VisitedEntry<?> breadcrumb = LootContext.createVisitedEntry(function);
      if (context.pushVisitedElement(breadcrumb)) {
         try {
            return function.apply(itemStack, context);
         } finally {
            context.popVisitedElement(breadcrumb);
         }
      } else {
         LOGGER.warn("Detected infinite loop in loot tables");
         return itemStack;
      }
   }

   public static LootItemConditionalFunction.Builder<?> functionReference(final ResourceKey<LootItemFunction> name) {
      return simpleBuilder(conditions -> new FunctionReference(conditions, name));
   }
}
