package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import org.jspecify.annotations.Nullable;

public abstract class ButtonListDialogScreen<T extends ButtonListDialog> extends DialogScreen<T> {
   public static final int FOOTER_MARGIN = 5;

   public ButtonListDialogScreen(final @Nullable Screen previousScreen, final T dialog, final DialogConnectionAccess connectionAccess) {
      super(previousScreen, dialog, connectionAccess);
   }

   protected void populateBodyElements(
      final LinearLayout layout, final DialogControlSet controlSet, final T dialog, final DialogConnectionAccess connectionAccess
   ) {
      super.populateBodyElements(layout, controlSet, dialog, connectionAccess);
      List<Button> buttons = this.createListActions(dialog, connectionAccess).map(d -> controlSet.createActionButton(d).build()).toList();
      layout.addChild(packControlsIntoColumns(buttons, dialog.columns()));
   }

   protected abstract Stream<ActionButton> createListActions(T dialog, DialogConnectionAccess connectionAccess);

   protected void updateHeaderAndFooter(
      final HeaderAndFooterLayout layout, final DialogControlSet controlSet, final T dialog, final DialogConnectionAccess connectionAccess
   ) {
      super.updateHeaderAndFooter(layout, controlSet, dialog, connectionAccess);
      dialog.exitAction().ifPresentOrElse(exitButton -> layout.addToFooter(controlSet.createActionButton(exitButton).build()), () -> layout.setFooterHeight(5));
   }
}
