package net.minecraft.client.gui.screens.dialog;

import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MultiButtonDialogScreen extends ButtonListDialogScreen<MultiActionDialog> {
   public MultiButtonDialogScreen(@Nullable Screen p_406213_, MultiActionDialog p_407049_, DialogConnectionAccess p_408065_) {
      super(p_406213_, p_407049_, p_408065_);
   }

   protected Stream<ActionButton> createListActions(MultiActionDialog p_408087_, DialogConnectionAccess p_406419_) {
      return p_408087_.actions().stream();
   }
}
