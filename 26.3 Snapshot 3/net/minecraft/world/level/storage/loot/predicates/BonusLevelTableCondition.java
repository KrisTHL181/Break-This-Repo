package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record BonusLevelTableCondition(Holder<Enchantment> enchantment, List<Float> values) implements LootItemCondition {
   public static final MapCodec<BonusLevelTableCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Enchantment.CODEC.fieldOf("enchantment").forGetter(BonusLevelTableCondition::enchantment),
            ExtraCodecs.nonEmptyList(Codec.FLOAT.listOf()).fieldOf("chances").forGetter(BonusLevelTableCondition::values)
         )
         .apply(i, BonusLevelTableCondition::new)
   );

   @Override
   public MapCodec<BonusLevelTableCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.TOOL);
   }

   public boolean test(final LootContext context) {
      ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
      int level = tool != null ? EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, tool) : 0;
      float chance = this.values.get(Math.min(level, this.values.size() - 1));
      return context.getRandom().nextFloat() < chance;
   }

   public static LootItemCondition.Builder bonusLevelFlatChance(final Holder<Enchantment> enchantment, final float... chances) {
      List<Float> chancesList = new ArrayList<>(chances.length);

      for (float chance : chances) {
         chancesList.add(chance);
      }

      return () -> new BonusLevelTableCondition(enchantment, chancesList);
   }
}
