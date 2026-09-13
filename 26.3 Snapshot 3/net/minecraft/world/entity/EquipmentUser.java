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
   void setItemSlot(final EquipmentSlot slot, final ItemStack stack);

   ItemStack getItemBySlot(final EquipmentSlot slot);

   void setDropChance(final EquipmentSlot slot, final float dropChance);

   default void equip(final EquipmentTable equipment, final LootParams lootParams) {
      this.equip(equipment.lootTable(), lootParams, equipment.slotDropChances());
   }

   default void equip(final ResourceKey<LootTable> lootTable, final LootParams lootParams, final Map<EquipmentSlot, Float> dropChances) {
      this.equip(lootTable, lootParams, 0L, dropChances);
   }

   default void equip(
      final ResourceKey<LootTable> lootTable, final LootParams lootParams, final long optionalLootTableSeed, final Map<EquipmentSlot, Float> dropChances
   ) {
      LootTable table = lootParams.getLevel().getServer().reloadableRegistries().getLootTable(lootTable);
      if (table != LootTable.EMPTY) {
         List<ItemStack> possibleEquipment = table.getRandomItems(lootParams, optionalLootTableSeed);
         List<EquipmentSlot> insertedIntoSlots = new ArrayList<>();

         for (ItemStack toEquip : possibleEquipment) {
            EquipmentSlot slot = this.resolveSlot(toEquip, insertedIntoSlots);
            if (slot != null) {
               ItemStack equipped = slot.limit(toEquip);
               this.setItemSlot(slot, equipped);
               Float dropChance = dropChances.get(slot);
               if (dropChance != null) {
                  this.setDropChance(slot, dropChance);
               }

               insertedIntoSlots.add(slot);
            }
         }
      }
   }

   default @Nullable EquipmentSlot resolveSlot(final ItemStack toEquip, final List<EquipmentSlot> alreadyInsertedIntoSlots) {
      if (toEquip.isEmpty()) {
         return null;
      }

      Equippable equippable = toEquip.get(DataComponents.EQUIPPABLE);
      if (equippable != null) {
         EquipmentSlot slot = equippable.slot();
         if (!alreadyInsertedIntoSlots.contains(slot)) {
            return slot;
         }
      } else if (!alreadyInsertedIntoSlots.contains(EquipmentSlot.MAINHAND)) {
         return EquipmentSlot.MAINHAND;
      }

      return null;
   }
}
