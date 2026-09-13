package net.minecraft.world.item.enchantment;

import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EnchantedItemInUse(ItemStack itemStack, @Nullable EquipmentSlot inSlot, @Nullable LivingEntity owner, Consumer<Item> onBreak) {
   public EnchantedItemInUse(final ItemStack itemStack, final EquipmentSlot inSlot, final LivingEntity owner) {
      this(itemStack, inSlot, owner, item -> owner.onEquippedItemBroken(item, inSlot));
   }
}
