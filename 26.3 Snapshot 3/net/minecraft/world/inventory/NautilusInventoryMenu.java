package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;

public class NautilusInventoryMenu extends AbstractMountInventoryMenu {
   private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
   private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/nautilus_armor_inventory");

   public NautilusInventoryMenu(
      final int containerId, final Inventory playerInventory, final Container nautilusInventory, final AbstractNautilus nautilus, final int inventoryColumns
   ) {
      super(containerId, playerInventory, nautilusInventory, nautilus);
      Container saddleContainer = nautilus.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
      this.addSlot(new ArmorSlot(saddleContainer, nautilus, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return nautilus.canUseSlot(EquipmentSlot.SADDLE);
         }
      });
      Container armorContainer = nautilus.createEquipmentSlotContainer(EquipmentSlot.BODY);
      this.addSlot(new ArmorSlot(armorContainer, nautilus, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return nautilus.canUseSlot(EquipmentSlot.BODY);
         }
      });
      this.addStandardInventorySlots(playerInventory, 8, 84);
   }

   @Override
   protected boolean hasInventoryChanged(final Container container) {
      return ((AbstractNautilus)this.mount).hasInventoryChanged(container);
   }
}
