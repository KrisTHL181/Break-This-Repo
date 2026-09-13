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

class ArmorSlot extends Slot {
   private final LivingEntity owner;
   private final EquipmentSlot slot;
   private final @Nullable Identifier emptyIcon;

   public ArmorSlot(
      Container p_344669_, LivingEntity p_343390_, EquipmentSlot p_343712_, int p_344144_, int p_345211_, int p_342796_, @Nullable Identifier p_454870_
   ) {
      super(p_344669_, p_344144_, p_345211_, p_342796_);
      this.owner = p_343390_;
      this.slot = p_343712_;
      this.emptyIcon = p_454870_;
   }

   @Override
   public void setByPlayer(ItemStack p_342337_, ItemStack p_345204_) {
      this.owner.onEquipItem(this.slot, p_345204_, p_342337_);
      super.setByPlayer(p_342337_, p_345204_);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean mayPlace(ItemStack p_344267_) {
      return this.owner.isEquippableInSlot(p_344267_, this.slot);
   }

   @Override
   public boolean isActive() {
      return this.owner.canUseSlot(this.slot);
   }

   @Override
   public boolean mayPickup(Player p_344552_) {
      ItemStack itemstack = this.getItem();
      return !itemstack.isEmpty() && !p_344552_.isCreative() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
         ? false
         : super.mayPickup(p_344552_);
   }

   @Override
   public @Nullable Identifier getNoItemIcon() {
      return this.emptyIcon;
   }
}
