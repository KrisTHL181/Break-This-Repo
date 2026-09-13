package com.mojang.realmsclient.gui.screens;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsPopups {
   private static final int COLOR_INFO = 8226750;
   private static final Component INFO = Component.translatable("mco.info").withColor(8226750);
   private static final Component WARNING = Component.translatable("mco.warning").withColor(-65536);

   public static PopupScreen customPopupScreen(Screen p_408023_, Component p_410095_, Component p_405827_, Consumer<PopupScreen> p_407149_) {
      return new PopupScreen.Builder(p_408023_, p_410095_)
         .setMessage(p_405827_)
         .addButton(CommonComponents.GUI_CONTINUE, p_407149_)
         .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
         .build();
   }

   public static PopupScreen infoPopupScreen(Screen p_344328_, Component p_343794_, Consumer<PopupScreen> p_343122_) {
      return new PopupScreen.Builder(p_344328_, INFO)
         .setMessage(p_343794_)
         .addButton(CommonComponents.GUI_CONTINUE, p_343122_)
         .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
         .build();
   }

   public static PopupScreen warningPopupScreen(Screen p_343951_, Component p_343090_, Consumer<PopupScreen> p_343699_) {
      return new PopupScreen.Builder(p_343951_, WARNING)
         .setMessage(p_343090_)
         .addButton(CommonComponents.GUI_CONTINUE, p_343699_)
         .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
         .build();
   }

   public static PopupScreen warningAcknowledgePopupScreen(Screen p_343667_, Component p_342241_, Consumer<PopupScreen> p_343225_) {
      return new PopupScreen.Builder(p_343667_, WARNING).setMessage(p_342241_).addButton(CommonComponents.GUI_OK, p_343225_).build();
   }
}
