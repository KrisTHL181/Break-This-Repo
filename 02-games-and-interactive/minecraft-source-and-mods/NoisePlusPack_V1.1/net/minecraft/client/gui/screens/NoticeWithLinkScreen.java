package net.minecraft.client.gui.screens;

import java.net.URI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoticeWithLinkScreen extends Screen {
   private static final Component SYMLINK_WORLD_TITLE = Component.translatable("symlink_warning.title.world").withStyle(ChatFormatting.BOLD);
   private static final Component SYMLINK_WORLD_MESSAGE_TEXT = Component.translatable(
      "symlink_warning.message.world", Component.translationArg(CommonLinks.SYMLINK_HELP)
   );
   private static final Component SYMLINK_PACK_TITLE = Component.translatable("symlink_warning.title.pack").withStyle(ChatFormatting.BOLD);
   private static final Component SYMLINK_PACK_MESSAGE_TEXT = Component.translatable(
      "symlink_warning.message.pack", Component.translationArg(CommonLinks.SYMLINK_HELP)
   );
   private final Component message;
   private final URI uri;
   private final Runnable onClose;
   private final GridLayout layout = new GridLayout().rowSpacing(10);

   public NoticeWithLinkScreen(Component p_300556_, Component p_297438_, URI p_342616_, Runnable p_311369_) {
      super(p_300556_);
      this.message = p_297438_;
      this.uri = p_342616_;
      this.onClose = p_311369_;
   }

   public static Screen createWorldSymlinkWarningScreen(Runnable p_309390_) {
      return new NoticeWithLinkScreen(SYMLINK_WORLD_TITLE, SYMLINK_WORLD_MESSAGE_TEXT, CommonLinks.SYMLINK_HELP, p_309390_);
   }

   public static Screen createPackSymlinkWarningScreen(Runnable p_310056_) {
      return new NoticeWithLinkScreen(SYMLINK_PACK_TITLE, SYMLINK_PACK_MESSAGE_TEXT, CommonLinks.SYMLINK_HELP, p_310056_);
   }

   @Override
   protected void init() {
      super.init();
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper gridlayout$rowhelper = this.layout.createRowHelper(1);
      gridlayout$rowhelper.addChild(new StringWidget(this.title, this.font));
      gridlayout$rowhelper.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setCentered(true));
      int i = 120;
      GridLayout gridlayout = new GridLayout().columnSpacing(5);
      GridLayout.RowHelper gridlayout$rowhelper1 = gridlayout.createRowHelper(3);
      gridlayout$rowhelper1.addChild(
         Button.builder(CommonComponents.GUI_OPEN_IN_BROWSER, p_448020_ -> Util.getPlatform().openUri(this.uri)).size(120, 20).build()
      );
      gridlayout$rowhelper1.addChild(
         Button.builder(CommonComponents.GUI_COPY_LINK_TO_CLIPBOARD, p_340803_ -> this.minecraft.keyboardHandler.setClipboard(this.uri.toString()))
            .size(120, 20)
            .build()
      );
      gridlayout$rowhelper1.addChild(Button.builder(CommonComponents.GUI_BACK, p_300975_ -> this.onClose()).size(120, 20).build());
      gridlayout$rowhelper.addChild(gridlayout);
      this.repositionElements();
      this.layout.visitWidgets(this::addRenderableWidget);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   @Override
   public void onClose() {
      this.onClose.run();
   }
}
