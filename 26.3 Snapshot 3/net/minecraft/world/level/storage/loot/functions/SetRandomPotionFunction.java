package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetRandomPotionFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetRandomPotionFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(RegistryCodecs.homogeneousList(Registries.POTION).optionalFieldOf("options").forGetter(f -> f.options))
         .apply(i, SetRandomPotionFunction::new)
   );
   private final Optional<HolderSet<Potion>> options;

   private SetRandomPotionFunction(final List<LootItemCondition> predicates, final Optional<HolderSet<Potion>> options) {
      super(predicates);
      this.options = options;
   }

   @Override
   public MapCodec<SetRandomPotionFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      Optional<? extends Holder<Potion>> potion;
      if (this.options.isPresent()) {
         potion = this.options.get().getRandomElement(context.getRandom());
      } else {
         potion = context.getLevel().registryAccess().lookupOrThrow(Registries.POTION).getRandom(context.getRandom());
      }

      if (potion.isPresent()) {
         itemStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, potion.get(), PotionContents::withPotion);
      }

      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> fromTagKey(final HolderSet<Potion> tagKey) {
      return simpleBuilder(conditions -> new SetRandomPotionFunction(conditions, Optional.of(tagKey)));
   }
}
