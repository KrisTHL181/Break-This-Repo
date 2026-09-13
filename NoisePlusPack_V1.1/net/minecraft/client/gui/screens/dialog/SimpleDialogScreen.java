package net.minecraft.client.gui.screens.dialog;

import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.SimpleDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SimpleDialogScreen<T extends SimpleDialog> extends DialogScreen<T> {
   public SimpleDialogScreen(@Nullable Screen p_408002_, T p_407544_, DialogConnectionAccess p_408945_) {
      super(p_408002_, p_407544_, p_408945_);
   }

   protected void updateHeaderAndFooter(HeaderAndFooterLayout p_409134_, DialogControlSet p_407525_, T p_408220_, DialogConnectionAccess p_408415_) {
      super.updateHeaderAndFooter(p_409134_, p_407525_, p_408220_, p_408415_);
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);

      for (ActionButton actionbutton : p_408220_.mainActions()) {
         linearlayout.addChild(p_407525_.createActionButton(actionbutton).build());
      }

      p_409134_.addToFooter(linearlayout);
   }
}
