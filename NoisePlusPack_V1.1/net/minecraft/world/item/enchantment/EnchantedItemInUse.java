package net.minecraft.world.item.enchantment;

import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EnchantedItemInUse(ItemStack itemStack, @Nullable EquipmentSlot inSlot, @Nullable LivingEntity owner, Consumer<Item> onBreak) {
   public EnchantedItemInUse(ItemStack p_343725_, EquipmentSlot p_342093_, LivingEntity p_344478_) {
      this(p_343725_, p_342093_, p_344478_, p_345140_ -> p_344478_.onEquippedItemBroken(p_345140_, p_342093_));
   }
}
