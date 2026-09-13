package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public interface EquipmentUser {
   void setItemSlot(EquipmentSlot var1, ItemStack var2);

   ItemStack getItemBySlot(EquipmentSlot var1);

   void setDropChance(EquipmentSlot var1, float var2);

   default void equip(EquipmentTable p_331159_, LootParams p_332346_) {
      this.equip(p_331159_.lootTable(), p_332346_, p_331159_.slotDropChances());
   }

   default void equip(ResourceKey<LootTable> p_329232_, LootParams p_330675_, Map<EquipmentSlot, Float> p_328003_) {
      this.equip(p_329232_, p_330675_, 0L, p_328003_);
   }

   default void equip(ResourceKey<LootTable> p_331471_, LootParams p_333826_, long p_331881_, Map<EquipmentSlot, Float> p_328541_) {
      LootTable loottable = p_333826_.getLevel().getServer().reloadableRegistries().getLootTable(p_331471_);
      if (loottable != LootTable.EMPTY) {
         List<ItemStack> list = loottable.getRandomItems(p_333826_, p_331881_);
         List<EquipmentSlot> list1 = new ArrayList<>();

         for (ItemStack itemstack : list) {
            EquipmentSlot equipmentslot = this.resolveSlot(itemstack, list1);
            if (equipmentslot != null) {
               ItemStack itemstack1 = equipmentslot.limit(itemstack);
               this.setItemSlot(equipmentslot, itemstack1);
               Float f = p_328541_.get(equipmentslot);
               if (f != null) {
                  this.setDropChance(equipmentslot, f);
               }

               list1.add(equipmentslot);
            }
         }
      }
   }

   default @Nullable EquipmentSlot resolveSlot(ItemStack p_329649_, List<EquipmentSlot> p_334449_) {
      if (p_329649_.isEmpty()) {
         return null;
      }

      Equippable equippable = p_329649_.get(DataComponents.EQUIPPABLE);
      if (equippable != null) {
         EquipmentSlot equipmentslot = equippable.slot();
         if (!p_334449_.contains(equipmentslot)) {
            return equipmentslot;
         }
      } else if (!p_334449_.contains(EquipmentSlot.MAINHAND)) {
         return EquipmentSlot.MAINHAND;
      }

      return null;
   }
}
