package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;

public class NautilusInventoryMenu extends AbstractMountInventoryMenu {
   private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
   private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/nautilus_armor_inventory");

   public NautilusInventoryMenu(int p_453942_, Inventory p_453756_, Container p_459397_, final AbstractNautilus p_459797_, int p_450490_) {
      super(p_453942_, p_453756_, p_459397_, p_459797_);
      Container container = p_459797_.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
      this.addSlot(new ArmorSlot(container, p_459797_, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return p_459797_.canUseSlot(EquipmentSlot.SADDLE);
         }
      });
      Container container1 = p_459797_.createEquipmentSlotContainer(EquipmentSlot.BODY);
      this.addSlot(new ArmorSlot(container1, p_459797_, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return p_459797_.canUseSlot(EquipmentSlot.BODY);
         }
      });
      this.addStandardInventorySlots(p_453756_, 8, 84);
   }

   @Override
   protected boolean hasInventoryChanged(Container p_453582_) {
      return ((AbstractNautilus)this.mount).hasInventoryChanged(p_453582_);
   }
}
