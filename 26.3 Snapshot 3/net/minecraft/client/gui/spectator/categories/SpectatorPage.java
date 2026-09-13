package net.minecraft.client.gui.spectator.categories;

import com.google.common.base.MoreObjects;
import java.util.List;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;

public class SpectatorPage {
   public static final int NO_SELECTION = -1;
   private final List<SpectatorMenuItem> items;
   private final int selection;

   public SpectatorPage(final List<SpectatorMenuItem> items, final int selection) {
      this.items = items;
      this.selection = selection;
   }

   public SpectatorMenuItem getItem(final int slot) {
      return slot >= 0 && slot < this.items.size()
         ? (SpectatorMenuItem)MoreObjects.firstNonNull(this.items.get(slot), SpectatorMenu.EMPTY_SLOT)
         : SpectatorMenu.EMPTY_SLOT;
   }

   public int getSelectedSlot() {
      return this.selection;
   }
}
