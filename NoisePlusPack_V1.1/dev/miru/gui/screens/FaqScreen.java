package dev.miru.gui.screens;

import dev.miru.helper.FaqItem;
import dev.miru.helper.KitUtil;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class FaqScreen extends Screen {
   private Screen parent;
   private List<FaqItem> faqItems;
   private int startY;
   private int yPadding;
   private int btnw;
   private int btnh;
   private boolean allowJump;

   public FaqScreen(Screen parent, List<FaqItem> faqItems, int startY, int yPadding, int btnw, int btnh, boolean allowJump) {
      super(Component.empty());
      this.parent = parent;
      this.faqItems = faqItems;
      this.startY = startY;
      this.yPadding = yPadding;
      this.btnw = btnw;
      this.btnh = btnh;
      this.allowJump = allowJump;
   }

   @Override
   public void init() {
      int index = 0;

      for (FaqItem faqItem : this.faqItems) {
         Button button = KitUtil.button(faqItem.getTitle(), faqItem.getDesc(), btn -> {
            if (this.allowJump) {
               this.minecraft.setScreen(new MessageScreen("[%s]%s".formatted(faqItem.getTitle(), faqItem.getDesc()), this));
            }
         }, this.btnw, this.btnh, this.width / 2 - this.btnw / 2, this.startY + (this.btnh + this.yPadding) * index);
         this.addRenderableWidget(button);
         index++;
      }

      this.addRenderableWidget(
         KitUtil.button(
            CommonComponents.GUI_BACK,
            Component.empty(),
            btn -> this.minecraft.setScreen(this.parent),
            this.btnw,
            this.btnh,
            this.width / 2 - this.btnw / 2,
            this.height - (this.btnh + this.yPadding)
         )
      );
   }
}
