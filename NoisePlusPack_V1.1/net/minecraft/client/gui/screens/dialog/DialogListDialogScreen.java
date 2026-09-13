package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DialogListDialogScreen extends ButtonListDialogScreen<DialogListDialog> {
   public DialogListDialogScreen(@Nullable Screen p_408049_, DialogListDialog p_409827_, DialogConnectionAccess p_405973_) {
      super(p_408049_, p_409827_, p_405973_);
   }

   protected Stream<ActionButton> createListActions(DialogListDialog p_409917_, DialogConnectionAccess p_409822_) {
      return p_409917_.dialogs().stream().map(p_408364_ -> createDialogClickAction(p_409917_, (Holder<Dialog>)p_408364_));
   }

   private static ActionButton createDialogClickAction(DialogListDialog p_406400_, Holder<Dialog> p_406387_) {
      return new ActionButton(
         new CommonButtonData(p_406387_.value().common().computeExternalTitle(), p_406400_.buttonWidth()),
         Optional.of(new StaticAction(new ClickEvent.ShowDialog(p_406387_)))
      );
   }
}
