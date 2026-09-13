package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommonLayouts {
   private static final int LABEL_SPACING = 4;

   private CommonLayouts() {
   }

   public static Layout labeledElement(Font p_300569_, LayoutElement p_299110_, Component p_297847_) {
      return labeledElement(p_300569_, p_299110_, p_297847_, p_297385_ -> {});
   }

   public static Layout labeledElement(Font p_298072_, LayoutElement p_300669_, Component p_298837_, Consumer<LayoutSettings> p_301252_) {
      LinearLayout linearlayout = LinearLayout.vertical().spacing(4);
      linearlayout.addChild(new StringWidget(p_298837_, p_298072_));
      linearlayout.addChild(p_300669_, p_301252_);
      return linearlayout;
   }
}
