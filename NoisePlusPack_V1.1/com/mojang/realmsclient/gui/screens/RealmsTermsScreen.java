package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsTermsScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.terms.title");
   private static final Component TERMS_STATIC_TEXT = Component.translatable("mco.terms.sentence.1");
   private static final Component TERMS_LINK_TEXT = CommonComponents.space()
      .append(Component.translatable("mco.terms.sentence.2").withStyle(Style.EMPTY.withUnderlined(true)));
   private final Screen lastScreen;
   private final RealmsServer realmsServer;
   private boolean onLink;

   public RealmsTermsScreen(Screen p_90033_, RealmsServer p_90035_) {
      super(TITLE);
      this.lastScreen = p_90033_;
      this.realmsServer = p_90035_;
   }

   @Override
   public void init() {
      int i = this.width / 4 - 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.agree"), p_90054_ -> this.agreedToTos()).bounds(this.width / 4, row(12), i, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("mco.terms.buttons.disagree"), p_280762_ -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 + 4, row(12), i, 20)
            .build()
      );
   }

   @Override
   public boolean keyPressed(KeyEvent p_427443_) {
      if (p_427443_.key() == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(p_427443_);
      }
   }

   private void agreedToTos() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         realmsclient.agreeToTos();
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new GetServerDetailsTask(this.lastScreen, this.realmsServer)));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't agree to TOS", realmsserviceexception);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_424280_, boolean p_430282_) {
      if (this.onLink) {
         this.minecraft.keyboardHandler.setClipboard(CommonLinks.REALMS_TERMS.toString());
         Util.getPlatform().openUri(CommonLinks.REALMS_TERMS);
         return true;
      } else {
         return super.mouseClicked(p_424280_, p_430282_);
      }
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), TERMS_STATIC_TEXT).append(CommonComponents.SPACE).append(TERMS_LINK_TEXT);
   }

   @Override
   public void render(GuiGraphics p_281619_, int p_283526_, int p_282002_, float p_282536_) {
      super.render(p_281619_, p_283526_, p_282002_, p_282536_);
      p_281619_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      p_281619_.drawString(this.font, TERMS_STATIC_TEXT, this.width / 2 - 120, row(5), -1);
      int i = this.font.width(TERMS_STATIC_TEXT);
      int j = this.width / 2 - 121 + i;
      int k = row(5);
      int l = j + this.font.width(TERMS_LINK_TEXT) + 1;
      int i1 = k + 1 + 9;
      this.onLink = j <= p_283526_ && p_283526_ <= l && k <= p_282002_ && p_282002_ <= i1;
      p_281619_.drawString(this.font, TERMS_LINK_TEXT, this.width / 2 - 120 + i, row(5), this.onLink ? -9670204 : -13408581);
   }
}
