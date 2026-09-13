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
   public static final MapCodec<DynamicLoot> CODEC = RecordCodecBuilder.mapCodec(
      p_450069_ -> p_450069_.group(Identifier.CODEC.fieldOf("name").forGetter(p_450075_ -> p_450075_.name))
         .and(singletonFields(p_450069_))
         .apply(p_450069_, DynamicLoot::new)
   );
   private final Identifier name;

   private DynamicLoot(Identifier p_451015_, int p_79466_, int p_79467_, List<LootItemCondition> p_297929_, List<LootItemFunction> p_299695_) {
      super(p_79466_, p_79467_, p_297929_, p_299695_);
      this.name = p_451015_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.DYNAMIC;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_79481_, LootContext p_79482_) {
      p_79482_.addDynamicDrops(this.name, p_79481_);
   }

   public static LootPoolSingletonContainer.Builder<?> dynamicEntry(Identifier p_452776_) {
      return simpleBuilder((p_450071_, p_450072_, p_450073_, p_450074_) -> new DynamicLoot(p_452776_, p_450071_, p_450072_, p_450073_, p_450074_));
   }
}
