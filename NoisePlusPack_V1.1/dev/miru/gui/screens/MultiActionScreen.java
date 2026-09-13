package dev.miru.gui.screens;

import dev.miru.helper.ComponentActionPair;
import dev.miru.helper.KitUtil;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class MultiActionScreen extends Screen {
   private Screen parent;
   private List<ComponentActionPair> componentActionPairs;
   private int yPadding;
   private int yStart;
   private int btnWidth;
   private int btnHeight;

   public MultiActionScreen(Screen parent, List<ComponentActionPair> componentActionPairs, int yPadding, int yStart, int btnWidth, int btnHeight) {
      super(Component.empty());
      this.parent = parent;
      this.componentActionPairs = componentActionPairs;
      this.yPadding = yPadding;
      this.yStart = yStart;
      this.btnWidth = btnWidth;
      this.btnHeight = btnHeight;
   }

   @Override
   public void init() {
      int index = 0;

      for (ComponentActionPair pair : this.componentActionPairs) {
         Button button = KitUtil.button(
            pair.component(),
            Component.empty(),
            btn -> pair.runnable().run(),
            this.btnWidth,
            this.btnHeight,
            this.width / 2 - this.btnWidth / 2,
            this.yStart + (this.yPadding + this.btnHeight) * index
         );
         index++;
         this.addRenderableWidget(button);
      }

      this.addRenderableWidget(
         KitUtil.button(
            CommonComponents.GUI_BACK,
            Component.empty(),
            btn -> this.minecraft.setScreen(this.parent),
            this.btnWidth,
            this.btnHeight,
            this.width / 2 - this.btnWidth / 2,
            this.height - (this.btnHeight + this.yPadding)
         )
      );
   }
}
