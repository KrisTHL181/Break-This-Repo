package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

public class ArmorSlot extends Slot {
   private final LivingEntity owner;
   private final EquipmentSlot slot;
   private final @Nullable Identifier emptyIcon;

   public ArmorSlot(
      final Container inventory,
      final LivingEntity owner,
      final EquipmentSlot slot,
      final int slotIndex,
      final int x,
      final int y,
      final @Nullable Identifier emptyIcon
   ) {
      super(inventory, slotIndex, x, y);
      this.owner = owner;
      this.slot = slot;
      this.emptyIcon = emptyIcon;
   }

   @Override
   public void setByPlayer(final ItemStack itemStack, final ItemStack previous) {
      this.owner.onEquipItem(this.slot, previous, itemStack);
      super.setByPlayer(itemStack, previous);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean mayPlace(final ItemStack itemStack) {
      return this.owner.isEquippableInSlot(itemStack, this.slot);
   }

   @Override
   public boolean isActive() {
      return this.owner.canUseSlot(this.slot);
   }

   @Override
   public boolean mayPickup(final Player player) {
      ItemStack itemStack = this.getItem();
      return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
         ? false
         : super.mayPickup(player);
   }

   @Override
   public @Nullable Identifier getNoItemIcon() {
      return this.emptyIcon;
   }
}
