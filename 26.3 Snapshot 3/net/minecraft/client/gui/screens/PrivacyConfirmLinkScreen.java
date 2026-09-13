package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class PrivacyConfirmLinkScreen extends ConfirmLinkScreen {
   private static final Component TITLE = Component.translatable("gui.privacy_link.title").withStyle(style -> style.withColor(-256).withUnderlined(true));
   private static final Component MESSAGE = Component.translatable("gui.privacy_link.message");
   private final Component urlComponent;

   public PrivacyConfirmLinkScreen(final BooleanConsumer callback, final String url) {
      super(callback, TITLE, MESSAGE, url, CommonComponents.GUI_CANCEL, true);
      this.urlComponent = Component.literal(url).withStyle(ChatFormatting.WHITE);
   }

   @Override
   protected void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(new MultiLineTextWidget(this.title, this.font).setMaxWidth(this.width - 50).setMaxRows(4).setCentered(true));
      this.layout.addChild(new MultiLineTextWidget(MESSAGE, this.font).setMaxWidth(this.width - 50).setMaxRows(15).setCentered(true));
      this.addAdditionalText();
      LinearLayout buttonLayout = this.layout.addChild(LinearLayout.horizontal().spacing(4));
      buttonLayout.defaultCellSetting().paddingTop(16);
      this.addButtons(buttonLayout);
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void addAdditionalText() {
      this.layout.addChild(new StringWidget(this.urlComponent, this.font));
   }

   public static void confirmLinkNow(final @Nullable Screen parentScreen, final URI uri) {
      confirmLinkNow(parentScreen, uri.toString());
   }

   public static void confirmLinkNow(final @Nullable Screen parentScreen, final String uri) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.gui.setScreen(new PrivacyConfirmLinkScreen(shouldOpen -> {
         if (shouldOpen) {
            Util.getPlatform().openUri(uri);
         }

         minecraft.gui.setScreen(parentScreen);
      }, uri));
   }
}
