package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ButtonListDialogScreen<T extends ButtonListDialog> extends DialogScreen<T> {
   public static final int FOOTER_MARGIN = 5;

   public ButtonListDialogScreen(@Nullable Screen p_409584_, T p_408340_, DialogConnectionAccess p_409908_) {
      super(p_409584_, p_408340_, p_409908_);
   }

   protected void populateBodyElements(LinearLayout p_409222_, DialogControlSet p_406243_, T p_410633_, DialogConnectionAccess p_410375_) {
      super.populateBodyElements(p_409222_, p_406243_, p_410633_, p_410375_);
      List<Button> list = this.createListActions(p_410633_, p_410375_).map(p_409395_ -> p_406243_.createActionButton(p_409395_).build()).toList();
      p_409222_.addChild(packControlsIntoColumns(list, p_410633_.columns()));
   }

   protected abstract Stream<ActionButton> createListActions(T var1, DialogConnectionAccess var2);

   protected void updateHeaderAndFooter(HeaderAndFooterLayout p_407882_, DialogControlSet p_406703_, T p_406170_, DialogConnectionAccess p_409416_) {
      super.updateHeaderAndFooter(p_407882_, p_406703_, p_406170_, p_409416_);
      p_406170_.exitAction()
         .ifPresentOrElse(p_409054_ -> p_407882_.addToFooter(p_406703_.createActionButton(p_409054_).build()), () -> p_407882_.setFooterHeight(5));
   }
}
