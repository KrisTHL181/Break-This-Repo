package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetOminousBottleAmplifierFunction extends LootItemConditionalFunction {
   static final MapCodec<SetOminousBottleAmplifierFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_330680_ -> commonFields(p_330680_)
         .and(NumberProviders.CODEC.fieldOf("amplifier").forGetter(p_328335_ -> p_328335_.amplifierGenerator))
         .apply(p_330680_, SetOminousBottleAmplifierFunction::new)
   );
   private final NumberProvider amplifierGenerator;

   private SetOminousBottleAmplifierFunction(List<LootItemCondition> p_328459_, NumberProvider p_330589_) {
      super(p_328459_);
      this.amplifierGenerator = p_330589_;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.amplifierGenerator.getReferencedContextParams();
   }

   @Override
   public LootItemFunctionType<SetOminousBottleAmplifierFunction> getType() {
      return LootItemFunctions.SET_OMINOUS_BOTTLE_AMPLIFIER;
   }

   @Override
   public ItemStack run(ItemStack p_327902_, LootContext p_335574_) {
      int i = Mth.clamp(this.amplifierGenerator.getInt(p_335574_), 0, 4);
      p_327902_.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(i));
      return p_327902_;
   }

   public NumberProvider amplifier() {
      return this.amplifierGenerator;
   }

   public static LootItemConditionalFunction.Builder<?> setAmplifier(NumberProvider p_329950_) {
      return simpleBuilder(p_334301_ -> new SetOminousBottleAmplifierFunction(p_334301_, p_329950_));
   }
}
