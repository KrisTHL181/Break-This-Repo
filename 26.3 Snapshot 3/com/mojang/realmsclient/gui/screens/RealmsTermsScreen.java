package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class RealmsTermsScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.terms.title");
   private static final Component TERMS_STATIC_TEXT = Component.translatable("mco.terms.sentence.1");
   private static final Component TERMS_LINK_TEXT = CommonComponents.space()
      .append(Component.translatable("mco.terms.sentence.2").withStyle(Style.EMPTY.withUnderlined(true)));
   private final Screen lastScreen;
   private final RealmsServer realmsServer;
   private boolean onLink;

   public RealmsTermsScreen(final Screen lastScreen, final RealmsServer realmsServer) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.realmsServer = realmsServer;
   }

   @Override
   public void init() {
      int columnWidth = this.width / 4 - 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.agree"), button -> this.agreedToTos())
            .bounds(this.width / 4, row(12), columnWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.disagree"), button -> this.minecraft.gui.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 4, row(12), columnWidth, 20)
            .build()
      );
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.isEscape()) {
         this.minecraft.gui.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   private void agreedToTos() {
      RealmsClient client = RealmsClient.getOrCreate();

      try {
         client.agreeToTos();
         this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new GetServerDetailsTask(this.lastScreen, this.realmsServer)));
      } catch (RealmsServiceException e) {
         LOGGER.error("Couldn't agree to TOS", e);
      }
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (this.onLink) {
         this.minecraft.keyboardHandler.setClipboard(CommonLinks.REALMS_TERMS.toString());
         Util.getPlatform().openUri(CommonLinks.REALMS_TERMS);
         return true;
      } else {
         return super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), TERMS_STATIC_TEXT).append(CommonComponents.SPACE).append(TERMS_LINK_TEXT);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
      super.extractRenderState(graphics, xm, ym, a);
      graphics.centeredText(this.font, this.title, this.width / 2, 17, -1);
      graphics.text(this.font, TERMS_STATIC_TEXT, this.width / 2 - 120, row(5), -1);
      int firstPartWidth = this.font.width(TERMS_STATIC_TEXT);
      int x1 = this.width / 2 - 121 + firstPartWidth;
      int y1 = row(5);
      int x2 = x1 + this.font.width(TERMS_LINK_TEXT) + 1;
      int y2 = y1 + 1 + 9;
      this.onLink = x1 <= xm && xm <= x2 && y1 <= ym && ym <= y2;
      graphics.text(this.font, TERMS_LINK_TEXT, this.width / 2 - 120 + firstPartWidth, row(5), this.onLink ? -9670204 : -13408581);
   }
}
