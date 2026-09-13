package net.minecraft.client.gui.screens.multiplayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SafetyScreen extends WarningScreen {
   private static final Component TITLE = Component.translatable("multiplayerWarning.header").withStyle(ChatFormatting.BOLD);
   private static final Component CONTENT = Component.translatable("multiplayerWarning.message");
   private static final Component CHECK = Component.translatable("multiplayerWarning.check").withColor(-2039584);
   private static final Component NARRATION = TITLE.copy().append("\n").append(CONTENT);
   private final Screen previous;

   public SafetyScreen(Screen p_99743_) {
      super(TITLE, CONTENT, CHECK, NARRATION);
      this.previous = p_99743_;
   }

   @Override
   protected Layout addFooterButtons() {
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);
      linearlayout.addChild(Button.builder(CommonComponents.GUI_PROCEED, p_280872_ -> {
         if (this.stopShowing.selected()) {
            this.minecraft.options.skipMultiplayerWarning = true;
            this.minecraft.options.save();
         }

         this.minecraft.setScreen(new JoinMultiplayerScreen(this.previous));
      }).build());
      linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_325385_ -> this.onClose()).build());
      return linearlayout;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.previous);
   }
}
