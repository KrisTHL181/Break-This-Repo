package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DynamicLoot extends LootPoolSingletonContainer {
   public static final MapCodec<DynamicLoot> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Identifier.CODEC.fieldOf("name").forGetter(e -> e.name)).and(singletonFields(i)).apply(i, DynamicLoot::new)
   );
   private final Identifier name;

   private DynamicLoot(
      final Identifier name, final int weight, final int quality, final List<LootItemCondition> conditions, final List<LootItemFunction> functions
   ) {
      super(weight, quality, conditions, functions);
      this.name = name;
   }

   @Override
   public MapCodec<DynamicLoot> codec() {
      return MAP_CODEC;
   }

   @Override
   public void createItemStack(final Consumer<ItemStack> output, final LootContext context) {
      context.addDynamicDrops(this.name, output);
   }

   public static LootPoolSingletonContainer.Builder<?> dynamicEntry(final Identifier name) {
      return simpleBuilder((weight, quality, conditions, functions) -> new DynamicLoot(name, weight, quality, conditions, functions));
   }
}
