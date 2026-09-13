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
   public static final MapCodec<ToggleTooltips> CODEC = RecordCodecBuilder.mapCodec(
      p_391137_ -> commonFields(p_391137_)
         .and(Codec.unboundedMap(DataComponentType.CODEC, Codec.BOOL).fieldOf("toggles").forGetter(p_331447_ -> p_331447_.values))
         .apply(p_391137_, ToggleTooltips::new)
   );
   private final Map<DataComponentType<?>, Boolean> values;

   private ToggleTooltips(List<LootItemCondition> p_330048_, Map<DataComponentType<?>, Boolean> p_332012_) {
      super(p_330048_);
      this.values = p_332012_;
   }

   @Override
   protected ItemStack run(ItemStack p_334443_, LootContext p_331872_) {
      p_334443_.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, p_391136_ -> {
         for (Entry<DataComponentType<?>, Boolean> entry : this.values.entrySet()) {
            boolean flag = entry.getValue();
            p_391136_ = p_391136_.withHidden(entry.getKey(), !flag);
         }

         return p_391136_;
      });
      return p_334443_;
   }

   @Override
   public LootItemFunctionType<ToggleTooltips> getType() {
      return LootItemFunctions.TOGGLE_TOOLTIPS;
   }
}
