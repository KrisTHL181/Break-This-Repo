package net.minecraft.client.gui.screens.dialog.body;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.server.dialog.body.DialogBody;

public interface DialogBodyHandler<T extends DialogBody> {
   LayoutElement createControls(DialogScreen<?> parent, T body);
}
