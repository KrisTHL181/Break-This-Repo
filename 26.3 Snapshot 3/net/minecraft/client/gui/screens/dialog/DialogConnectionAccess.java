package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.Nullable;

public interface DialogConnectionAccess {
   void disconnect(Component message);

   void runCommand(String command, @Nullable Screen activeScreen);

   void openDialog(Holder<Dialog> dialog, @Nullable Screen activeScreen);

   void sendCustomAction(Identifier id, Optional<Tag> payload);

   ServerLinks serverLinks();
}
