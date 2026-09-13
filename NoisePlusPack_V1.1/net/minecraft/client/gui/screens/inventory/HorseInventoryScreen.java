package net.minecraft.client.gui.screens.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class HorseInventoryScreen extends AbstractMountInventoryScreen<HorseInventoryMenu> {
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final Identifier CHEST_SLOTS_SPRITE = Identifier.withDefaultNamespace("container/horse/chest_slots");
   private static final Identifier HORSE_INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/horse.png");

   public HorseInventoryScreen(HorseInventoryMenu p_98817_, Inventory p_98818_, AbstractHorse p_450698_, int p_342509_) {
      super(p_98817_, p_98818_, p_450698_.getDisplayName(), p_342509_, p_450698_);
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
      return this.mount.canUseSlot(EquipmentSlot.SADDLE) && this.mount.getType().is(EntityTypeTags.CAN_EQUIP_SADDLE);
   }

   @Override
   protected boolean shouldRenderArmorSlot() {
      return this.mount.canUseSlot(EquipmentSlot.BODY) && (this.mount.getType().is(EntityTypeTags.CAN_WEAR_HORSE_ARMOR) || this.mount instanceof Llama);
   }
}
