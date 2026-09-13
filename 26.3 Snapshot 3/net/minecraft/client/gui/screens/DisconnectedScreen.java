package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class DisconnectedScreen extends Screen {
   private static final Component TO_SERVER_LIST = Component.translatable("gui.toMenu");
   private static final Component TO_TITLE = Component.translatable("gui.toTitle");
   private static final Component REPORT_TO_SERVER_TITLE = Component.translatable("gui.report_to_server");
   private static final Component OPEN_REPORT_DIR_TITLE = Component.translatable("gui.open_report_dir");
   private final Screen parent;
   private final DisconnectionDetails details;
   private final Component buttonText;
   private final LinearLayout layout = LinearLayout.vertical();

   public DisconnectedScreen(final Screen parent, final Component title, final Component reason) {
      this(parent, title, new DisconnectionDetails(reason));
   }

   public DisconnectedScreen(final Screen parent, final Component title, final Component reason, final Component buttonText) {
      this(parent, title, new DisconnectionDetails(reason), buttonText);
   }

   public DisconnectedScreen(final Screen parent, final Component title, final DisconnectionDetails details) {
      this(parent, title, details, TO_SERVER_LIST);
   }

   public DisconnectedScreen(final Screen parent, final Component title, final DisconnectionDetails details, final Component buttonText) {
      super(title);
      this.parent = parent;
      this.details = details;
      this.buttonText = buttonText;
   }

   @Override
   protected void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
      this.layout.addChild(new StringWidget(this.title, this.font));
      this.layout.addChild(new MultiLineTextWidget(this.details.reason(), this.font).setMaxWidth(this.width - 50).setCentered(true));
      this.layout.defaultCellSetting().padding(2);
      this.details
         .bugReportLink()
         .ifPresent(
            bugReportLink -> this.layout
               .addChild(Button.builder(REPORT_TO_SERVER_TITLE, ConfirmLinkScreen.confirmLink(this, bugReportLink, false)).width(200).build())
         );
      this.details
         .report()
         .ifPresent(
            report -> this.layout.addChild(Button.builder(OPEN_REPORT_DIR_TITLE, button -> Util.getPlatform().openPath(report.getParent())).width(200).build())
         );
      Button backButton;
      if (this.minecraft.allowsMultiplayer()) {
         backButton = Button.builder(this.buttonText, var1x -> this.minecraft.gui.setScreen(this.parent)).width(200).build();
      } else {
         backButton = Button.builder(TO_TITLE, var1x -> this.minecraft.gui.setScreen(new TitleScreen())).width(200).build();
      }

      this.layout.addChild(backButton);
      this.layout.arrangeElements();
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.title, this.details.reason());
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
