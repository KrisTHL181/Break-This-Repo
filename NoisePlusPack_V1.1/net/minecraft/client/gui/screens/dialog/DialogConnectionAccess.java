package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface DialogConnectionAccess {
   void disconnect(Component var1);

   void runCommand(String var1, @Nullable Screen var2);

   void openDialog(Holder<Dialog> var1, @Nullable Screen var2);

   void sendCustomAction(Identifier var1, Optional<Tag> var2);

   ServerLinks serverLinks();
}
