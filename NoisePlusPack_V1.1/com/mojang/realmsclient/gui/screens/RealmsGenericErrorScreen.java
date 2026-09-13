package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsGenericErrorScreen extends RealmsScreen {
   private static final Component GENERIC_TITLE = Component.translatable("mco.errorMessage.generic");
   private final Screen nextScreen;
   private final Component detail;
   private MultiLineLabel splitDetail = MultiLineLabel.EMPTY;

   public RealmsGenericErrorScreen(RealmsServiceException p_88669_, Screen p_88670_) {
      this(RealmsGenericErrorScreen.ErrorMessage.forServiceError(p_88669_), p_88670_);
   }

   public RealmsGenericErrorScreen(Component p_88672_, Screen p_88673_) {
      this(new RealmsGenericErrorScreen.ErrorMessage(GENERIC_TITLE, p_88672_), p_88673_);
   }

   public RealmsGenericErrorScreen(Component p_88675_, Component p_88676_, Screen p_88677_) {
      this(new RealmsGenericErrorScreen.ErrorMessage(p_88675_, p_88676_), p_88677_);
   }

   private RealmsGenericErrorScreen(RealmsGenericErrorScreen.ErrorMessage p_451864_, Screen p_458976_) {
      super(p_451864_.title);
      this.nextScreen = p_458976_;
      this.detail = ComponentUtils.mergeStyles(p_451864_.detail, Style.EMPTY.withColor(-2142128));
   }

   @Override
   public void init() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_OK, p_325126_ -> this.onClose()).bounds(this.width / 2 - 100, this.height - 52, 200, 20).build()
      );
      this.splitDetail = MultiLineLabel.create(this.font, this.detail, this.width * 3 / 4);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.nextScreen);
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.detail);
   }

   @Override
   public void render(GuiGraphics p_283497_, int p_88680_, int p_88681_, float p_88682_) {
      super.render(p_283497_, p_88680_, p_88681_, p_88682_);
      p_283497_.drawCenteredString(this.font, this.title, this.width / 2, 80, -1);
      ActiveTextCollector activetextcollector = p_283497_.textRenderer();
      this.splitDetail.visitLines(TextAlignment.CENTER, this.width / 2, 100, 9, activetextcollector);
   }

   @OnlyIn(Dist.CLIENT)
   record ErrorMessage(Component title, Component detail) {
      static RealmsGenericErrorScreen.ErrorMessage forServiceError(RealmsServiceException p_455496_) {
         RealmsError realmserror = p_455496_.realmsError;
         return new RealmsGenericErrorScreen.ErrorMessage(
            Component.translatable("mco.errorMessage.realmsService.realmsError", realmserror.errorCode()), realmserror.errorMessage()
         );
      }
   }
}
