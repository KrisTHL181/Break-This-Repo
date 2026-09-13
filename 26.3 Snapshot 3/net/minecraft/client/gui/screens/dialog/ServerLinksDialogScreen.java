package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraft.server.dialog.action.StaticAction;
import org.jspecify.annotations.Nullable;

public class ServerLinksDialogScreen extends ButtonListDialogScreen<ServerLinksDialog> {
   public ServerLinksDialogScreen(final @Nullable Screen previousScreen, final ServerLinksDialog dialog, final DialogConnectionAccess connectionAccess) {
      super(previousScreen, dialog, connectionAccess);
   }

   protected Stream<ActionButton> createListActions(final ServerLinksDialog dialog, final DialogConnectionAccess connectionAccess) {
      return connectionAccess.serverLinks().entries().stream().map(entry -> createDialogClickAction(dialog, entry));
   }

   private static ActionButton createDialogClickAction(final ServerLinksDialog data, final ServerLinks.Entry entry) {
      return new ActionButton(
         new CommonButtonData(entry.displayName(), data.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.OpenUrl(entry.link())))
      );
   }
}
