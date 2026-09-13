package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetCustomDataFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetCustomDataFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(TagParser.LENIENT_CODEC.fieldOf("tag").forGetter(f -> f.tag)).apply(i, SetCustomDataFunction::new)
   );
   private final CompoundTag tag;

   private SetCustomDataFunction(final List<LootItemCondition> predicates, final CompoundTag tag) {
      super(predicates);
      this.tag = tag;
   }

   @Override
   public MapCodec<SetCustomDataFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> tag.merge(this.tag));
      return itemStack;
   }

   @Deprecated
   public static LootItemConditionalFunction.Builder<?> setCustomData(final CompoundTag value) {
      return simpleBuilder(conditions -> new SetCustomDataFunction(conditions, value));
   }
}
