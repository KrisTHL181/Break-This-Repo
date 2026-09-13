package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetInstrumentFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetInstrumentFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(RegistryCodecs.homogeneousList(Registries.INSTRUMENT).fieldOf("options").forGetter(f -> f.options))
         .apply(i, SetInstrumentFunction::new)
   );
   private final HolderSet<Instrument> options;

   private SetInstrumentFunction(final List<LootItemCondition> predicates, final HolderSet<Instrument> options) {
      super(predicates);
      this.options = options;
   }

   @Override
   public MapCodec<SetInstrumentFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      this.options
         .getRandomElement(context.getRandom())
         .ifPresent(instrumentHolder -> itemStack.set(DataComponents.INSTRUMENT, new InstrumentComponent((Holder<Instrument>)instrumentHolder)));
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> setInstrumentOptions(final HolderSet<Instrument> options) {
      return simpleBuilder(conditions -> new SetInstrumentFunction(conditions, options));
   }
}
