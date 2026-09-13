package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class SequenceFunction implements LootItemFunction {
   public static final MapCodec<SequenceFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_327578_ -> p_327578_.group(LootItemFunctions.TYPED_CODEC.listOf().fieldOf("functions").forGetter(p_298675_ -> p_298675_.functions))
         .apply(p_327578_, SequenceFunction::new)
   );
   public static final Codec<SequenceFunction> INLINE_CODEC = LootItemFunctions.TYPED_CODEC
      .listOf()
      .xmap(SequenceFunction::new, p_298151_ -> p_298151_.functions);
   private final List<LootItemFunction> functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

   private SequenceFunction(List<LootItemFunction> p_297875_) {
      this.functions = p_297875_;
      this.compositeFunction = LootItemFunctions.compose(p_297875_);
   }

   public static SequenceFunction of(List<LootItemFunction> p_299752_) {
      return new SequenceFunction(List.copyOf(p_299752_));
   }

   public ItemStack apply(ItemStack p_300658_, LootContext p_298148_) {
      return this.compositeFunction.apply(p_300658_, p_298148_);
   }

   @Override
   public void validate(ValidationContext p_297477_) {
      LootItemFunction.super.validate(p_297477_);

      for (int i = 0; i < this.functions.size(); i++) {
         this.functions.get(i).validate(p_297477_.forChild(new ProblemReporter.IndexedFieldPathElement("functions", i)));
      }
   }

   @Override
   public LootItemFunctionType<SequenceFunction> getType() {
      return LootItemFunctions.SEQUENCE;
   }
}
