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

   public HorseInventoryMenu(int p_39656_, Inventory p_39657_, Container p_39658_, final AbstractHorse p_451212_, int p_342974_) {
      super(p_39656_, p_39657_, p_39658_, p_451212_);
      Container container = p_451212_.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
      this.addSlot(new ArmorSlot(container, p_451212_, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
         @Override
         public boolean isActive() {
            return p_451212_.canUseSlot(EquipmentSlot.SADDLE) && p_451212_.getType().is(EntityTypeTags.CAN_EQUIP_SADDLE);
         }
      });
      final boolean flag = p_451212_ instanceof Llama;
      Identifier identifier = flag ? LLAMA_ARMOR_SLOT_SPRITE : ARMOR_SLOT_SPRITE;
      Container container1 = p_451212_.createEquipmentSlotContainer(EquipmentSlot.BODY);
      this.addSlot(new ArmorSlot(container1, p_451212_, EquipmentSlot.BODY, 0, 8, 36, identifier) {
         @Override
         public boolean isActive() {
            return p_451212_.canUseSlot(EquipmentSlot.BODY) && (p_451212_.getType().is(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || flag);
         }
      });
      if (p_342974_ > 0) {
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < p_342974_; j++) {
               this.addSlot(new Slot(p_39658_, j + i * p_342974_, 80 + j * 18, 18 + i * 18));
            }
         }
      }

      this.addStandardInventorySlots(p_39657_, 8, 84);
   }

   @Override
   protected boolean hasInventoryChanged(Container p_453848_) {
      return ((AbstractHorse)this.mount).hasInventoryChanged(p_453848_);
   }
}
