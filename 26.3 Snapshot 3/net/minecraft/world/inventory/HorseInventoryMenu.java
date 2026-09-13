package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Inventory;

public class HorseInventoryMenu extends AbstractMountInventoryMenu {
   private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
   private static final Identifier LLAMA_ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/llama_armor");
   private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/horse_armor");

   public HorseInventoryMenu(
      final int containerId, final Inventory playerInventory, final Container horseInventory, final AbstractHorse horse, final int inventoryColumns
   ) {
      super(containerId, playerInventory, horseInventory, horse);
      Container saddleContainer = horse.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
      this.addSlot(new ArmorSlot(saddleContainer, horse, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return horse.canUseSlot(EquipmentSlot.SADDLE) && horse.is(EntityTypeTags.CAN_EQUIP_SADDLE);
         }
      });
      final boolean isLlama = horse instanceof Llama;
      Identifier armorSprite = isLlama ? LLAMA_ARMOR_SLOT_SPRITE : ARMOR_SLOT_SPRITE;
      Container armorContainer = horse.createEquipmentSlotContainer(EquipmentSlot.BODY);
      this.addSlot(new ArmorSlot(armorContainer, horse, EquipmentSlot.BODY, 0, 8, 36, armorSprite) {
         @Override
         public boolean isActive() {
            return horse.canUseSlot(EquipmentSlot.BODY) && (horse.is(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || isLlama);
         }
      });
      if (inventoryColumns > 0) {
         for (int y = 0; y < 3; y++) {
            for (int x = 0; x < inventoryColumns; x++) {
               this.addSlot(new Slot(horseInventory, x + y * inventoryColumns, 80 + x * 18, 18 + y * 18));
            }
         }
      }

      this.addStandardInventorySlots(playerInventory, 8, 84);
   }

   @Override
   protected boolean hasInventoryChanged(final Container container) {
      return ((AbstractHorse)this.mount).hasInventoryChanged(container);
   }
}
