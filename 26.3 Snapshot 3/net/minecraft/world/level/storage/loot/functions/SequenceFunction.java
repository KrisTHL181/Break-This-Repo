package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class SequenceFunction implements LootItemFunction {
   public static final MapCodec<SequenceFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LootItemFunctions.DIRECT_CODEC.listOf().fieldOf("functions").forGetter(f -> f.functions)).apply(i, SequenceFunction::new)
   );
   public static final Codec<SequenceFunction> INLINE_CODEC = LootItemFunctions.DIRECT_CODEC.listOf().xmap(SequenceFunction::new, f -> f.functions);
   private final List<LootItemFunction> functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

   private SequenceFunction(final List<LootItemFunction> functions) {
      this.functions = functions;
      this.compositeFunction = LootItemFunctions.compose(functions);
   }

   public static SequenceFunction of(final List<LootItemFunction> functions) {
      return new SequenceFunction(List.copyOf(functions));
   }

   public ItemStack apply(final ItemStack stack, final LootContext context) {
      return this.compositeFunction.apply(stack, context);
   }

   @Override
   public void validate(final ValidationContext output) {
      LootItemFunction.super.validate(output);
      Validatable.validate(output, "functions", this.functions);
   }

   @Override
   public MapCodec<SequenceFunction> codec() {
      return MAP_CODEC;
   }
}
