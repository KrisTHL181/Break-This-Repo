package net.minecraft.client.gui.screens.dialog;

import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.SimpleDialog;
import org.jspecify.annotations.Nullable;

public class SimpleDialogScreen<T extends SimpleDialog> extends DialogScreen<T> {
   public SimpleDialogScreen(final @Nullable Screen previousScreen, final T dialog, final DialogConnectionAccess connectionAccess) {
      super(previousScreen, dialog, connectionAccess);
   }

   protected void updateHeaderAndFooter(
      final HeaderAndFooterLayout layout, final DialogControlSet controlSet, final T dialog, final DialogConnectionAccess connectionAccess
   ) {
      super.updateHeaderAndFooter(layout, controlSet, dialog, connectionAccess);
      LinearLayout buttonLayout = LinearLayout.horizontal().spacing(8);

      for (ActionButton action : dialog.mainActions()) {
         buttonLayout.addChild(controlSet.createActionButton(action).build());
      }

      layout.addToFooter(buttonLayout);
   }
}
