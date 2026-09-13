package net.minecraft.client.gui.screens.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NautilusInventoryScreen extends AbstractMountInventoryScreen<NautilusInventoryMenu> {
   private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   private static final Identifier NAUTILUS_INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/nautilus.png");

   public NautilusInventoryScreen(NautilusInventoryMenu p_454001_, Inventory p_452034_, AbstractNautilus p_451060_, int p_455777_) {
      super(p_454001_, p_452034_, p_451060_.getDisplayName(), p_455777_, p_451060_);
   }

   @Override
   protected Identifier getBackgroundTextureLocation() {
      return NAUTILUS_INVENTORY_LOCATION;
   }

   @Override
   protected Identifier getSlotSpriteLocation() {
      return SLOT_SPRITE;
   }

   @Override
   protected @Nullable Identifier getChestSlotsSpriteLocation() {
      return null;
   }

   @Override
   protected boolean shouldRenderSaddleSlot() {
      return this.mount.canUseSlot(EquipmentSlot.SADDLE);
   }

   @Override
   protected boolean shouldRenderArmorSlot() {
      return this.mount.canUseSlot(EquipmentSlot.BODY);
   }
}
