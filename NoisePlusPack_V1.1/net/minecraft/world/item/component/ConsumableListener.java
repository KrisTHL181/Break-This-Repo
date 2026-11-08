package net.minecraft.world.item.component;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ConsumableListener {
   void onConsume(Level var1, LivingEntity var2, ItemStack var3, Consumable var4);
}
