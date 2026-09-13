package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ToggleTooltips extends LootItemConditionalFunction {
   public static final MapCodec<ToggleTooltips> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(Codec.unboundedMap(DataComponentType.CODEC, Codec.BOOL).fieldOf("toggles").forGetter(e -> e.values))
         .apply(i, ToggleTooltips::new)
   );
   private final Map<DataComponentType<?>, Boolean> values;

   private ToggleTooltips(final List<LootItemCondition> predicates, final Map<DataComponentType<?>, Boolean> values) {
      super(predicates);
      this.values = values;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, display -> {
         for (Entry<DataComponentType<?>, Boolean> entry : this.values.entrySet()) {
            boolean shown = entry.getValue();
            display = display.withHidden(entry.getKey(), !shown);
         }

         return display;
      });
      return itemStack;
   }

   @Override
   public MapCodec<ToggleTooltips> codec() {
      return MAP_CODEC;
   }
}
