package net.minecraft.world.level.storage.loot.entries;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public interface LootPoolEntry {
   int getWeight(float var1);

   void createItemStack(Consumer<ItemStack> var1, LootContext var2);
}
