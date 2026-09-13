package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NonInteractiveResultSlot extends Slot {
   public NonInteractiveResultSlot(Container p_311408_, int p_312001_, int p_311632_, int p_309399_) {
      super(p_311408_, p_312001_, p_311632_, p_309399_);
   }

   @Override
   public void onQuickCraft(ItemStack p_312884_, ItemStack p_313225_) {
   }

   @Override
   public boolean mayPickup(Player p_311019_) {
      return false;
   }

   @Override
   public Optional<ItemStack> tryRemove(int p_310666_, int p_311310_, Player p_311612_) {
      return Optional.empty();
   }

   @Override
   public ItemStack safeTake(int p_313087_, int p_310389_, Player p_309608_) {
      return ItemStack.EMPTY;
   }

   @Override
   public ItemStack safeInsert(ItemStack p_309950_) {
      return p_309950_;
   }

   @Override
   public ItemStack safeInsert(ItemStack p_311478_, int p_311938_) {
      return this.safeInsert(p_311478_);
   }

   @Override
   public boolean allowModification(Player p_309707_) {
      return false;
   }

   @Override
   public boolean mayPlace(ItemStack p_310756_) {
      return false;
   }

   @Override
   public ItemStack remove(int p_310438_) {
      return ItemStack.EMPTY;
   }

   @Override
   public void onTake(Player p_312646_, ItemStack p_313015_) {
   }

   @Override
   public boolean isHighlightable() {
      return false;
   }

   @Override
   public boolean isFake() {
      return true;
   }
}
