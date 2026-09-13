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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ServerLinksDialogScreen extends ButtonListDialogScreen<ServerLinksDialog> {
   public ServerLinksDialogScreen(@Nullable Screen p_407976_, ServerLinksDialog p_410274_, DialogConnectionAccess p_409229_) {
      super(p_407976_, p_410274_, p_409229_);
   }

   protected Stream<ActionButton> createListActions(ServerLinksDialog p_410724_, DialogConnectionAccess p_406702_) {
      return p_406702_.serverLinks().entries().stream().map(p_409170_ -> createDialogClickAction(p_410724_, p_409170_));
   }

   private static ActionButton createDialogClickAction(ServerLinksDialog p_406827_, ServerLinks.Entry p_407773_) {
      return new ActionButton(
         new CommonButtonData(p_407773_.displayName(), p_406827_.buttonWidth()), Optional.of(new StaticAction(new ClickEvent.OpenUrl(p_407773_.link())))
      );
   }
}
