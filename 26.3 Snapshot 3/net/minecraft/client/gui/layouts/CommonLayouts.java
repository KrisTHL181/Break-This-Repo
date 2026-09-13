package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

public class CommonLayouts {
   private static final int LABEL_SPACING = 4;

   private CommonLayouts() {
   }

   public static Layout labeledElement(final Font font, final LayoutElement element, final Component label) {
      return labeledElement(font, element, label, s -> {});
   }

   public static Layout labeledElement(final Font font, final LayoutElement element, final Component label, final Consumer<LayoutSettings> settings) {
      LinearLayout layout = LinearLayout.vertical().spacing(4);
      layout.addChild(new StringWidget(label, font));
      layout.addChild(element, settings);
      return layout;
   }
}
