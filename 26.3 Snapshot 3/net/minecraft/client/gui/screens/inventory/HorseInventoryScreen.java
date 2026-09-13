package net.minecraft.client.gui.screens.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.jspecify.annotations.Nullable;

public class HorseInventoryScreen extends AbstractMountInventoryScreen<HorseInventoryMenu> {
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final Identifier CHEST_SLOTS_SPRITE = Identifier.withDefaultNamespace("container/horse/chest_slots");
   private static final Identifier HORSE_INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/horse.png");

   public HorseInventoryScreen(final HorseInventoryMenu menu, final Inventory inventory, final AbstractHorse horse, final int inventoryColumns) {
      super(menu, inventory, horse.getDisplayName(), inventoryColumns, horse);
   }

   @Override
   protected Identifier getBackgroundTextureLocation() {
      return HORSE_INVENTORY_LOCATION;
   }

   @Override
   protected Identifier getSlotSpriteLocation() {
      return SLOT_SPRITE;
   }

   @Override
   protected @Nullable Identifier getChestSlotsSpriteLocation() {
      return CHEST_SLOTS_SPRITE;
   }

   @Override
   protected boolean shouldRenderSaddleSlot() {
      return this.mount.canUseSlot(EquipmentSlot.SADDLE) && this.mount.is(EntityTypeTags.CAN_EQUIP_SADDLE);
   }

   @Override
   protected boolean shouldRenderArmorSlot() {
      return this.mount.canUseSlot(EquipmentSlot.BODY) && (this.mount.is(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || this.mount instanceof Llama);
   }
}
