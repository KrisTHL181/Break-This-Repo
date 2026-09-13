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
   public static final MapCodec<SetCustomDataFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_336302_ -> commonFields(p_336302_)
         .and(TagParser.LENIENT_CODEC.fieldOf("tag").forGetter(p_328670_ -> p_328670_.tag))
         .apply(p_336302_, SetCustomDataFunction::new)
   );
   private final CompoundTag tag;

   private SetCustomDataFunction(List<LootItemCondition> p_334383_, CompoundTag p_334528_) {
      super(p_334383_);
      this.tag = p_334528_;
   }

   @Override
   public LootItemFunctionType<SetCustomDataFunction> getType() {
      return LootItemFunctions.SET_CUSTOM_DATA;
   }

   @Override
   public ItemStack run(ItemStack p_328195_, LootContext p_331034_) {
      CustomData.update(DataComponents.CUSTOM_DATA, p_328195_, p_335000_ -> p_335000_.merge(this.tag));
      return p_328195_;
   }

   @Deprecated
   public static LootItemConditionalFunction.Builder<?> setCustomData(CompoundTag p_328660_) {
      return simpleBuilder(p_332883_ -> new SetCustomDataFunction(p_332883_, p_328660_));
   }
}
