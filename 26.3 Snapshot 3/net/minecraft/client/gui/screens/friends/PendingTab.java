package net.minecraft.client.gui.screens.friends;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

class PendingTab extends AbstractFriendsTab {
   private static final Component RECEIVED_HEADER = Component.translatable("gui.friends.pending.received")
      .withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
   private static final Component SENT_HEADER = Component.translatable("gui.friends.pending.sent").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
   private static final Component EMPTY_STATE = Component.translatable("gui.friends.pending.empty").withStyle(ChatFormatting.GRAY);
   private final FriendsOverlayScreen screen;
   private final LinearLayout layout;
   private final LinearLayout pendingScrollableContent;
   private final LoadingDotsWidget loadingDotsWidget;
   private final ScrollableLayout scrollableLayout;
   private @Nullable FrameLayout contentFrame;

   PendingTab(final Minecraft minecraft, final LoadingDotsWidget loadingDotsWidget, final FriendsOverlayScreen screen, final int width, final int height) {
      super(width, height);
      this.screen = screen;
      this.layout = LinearLayout.vertical();
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.loadingDotsWidget = loadingDotsWidget;
      this.pendingScrollableContent = LinearLayout.vertical();
      this.scrollableLayout = new ScrollableLayout(minecraft, this.pendingScrollableContent, height, ScrollableLayout.ReserveStrategy.BOTH);
      this.scrollableLayout.setScrollbarSpacing(2);
      this.scrollableLayout.setMaxHeight(height);
      this.layout.addChild(this.scrollableLayout);
      this.rearrangeElements();
   }

   @Override
   void rearrangeElements() {
      this.scrollableLayout.setMinHeight(this.height);
      this.scrollableLayout.setMaxHeight(this.height);
      if (this.contentFrame != null) {
         this.contentFrame.setMinHeight(this.height);
      }
   }

   @Override
   public Component getTabTitle() {
      return Component.translatable("gui.friends.requests_count", 0);
   }

   @Override
   public Component getTabExtraNarration() {
      return Component.empty();
   }

   @Override
   public void visitChildren(final Consumer<AbstractWidget> childrenConsumer) {
      this.layout.visitWidgets(childrenConsumer);
   }

   @Override
   public void doLayout(final ScreenRectangle screenRectangle) {
      this.layout.arrangeElements();
      FrameLayout.alignInRectangle(this.layout, screenRectangle, 0.5F, 0.16666667F);
   }

   @Override
   public Layout getLayout() {
      return this.layout;
   }

   public void showLoading() {
      this.pendingScrollableContent.removeChildren();
      this.contentFrame = this.createCenteredFrame(this.loadingDotsWidget, this.getListContentWidth(), this.height);
      this.pendingScrollableContent.addChild(this.contentFrame);
   }

   public void showError(final Component message) {
      this.pendingScrollableContent.removeChildren();
      int maxWidth = this.getListContentWidth();
      MultiLineTextWidget text = this.createCenteredText(message.copy().withStyle(ChatFormatting.GRAY), this.screen.getFont(), maxWidth);
      this.contentFrame = this.createCenteredFrame(text, maxWidth, this.height);
      this.pendingScrollableContent.addChild(this.contentFrame);
   }

   public void updateEntries(final List<IncomingEntry> incomingEntries, final List<OutgoingEntry> outgoingEntries) {
      this.pendingScrollableContent.removeChildren();
      this.contentFrame = null;
      if (!incomingEntries.isEmpty()) {
         this.pendingScrollableContent
            .addChild(this.createText(RECEIVED_HEADER, this.screen.getFont(), this.getListContentWidth()), LayoutSettings::alignHorizontallyCenter);
         incomingEntries.forEach(this.pendingScrollableContent::addChild);
      }

      if (!outgoingEntries.isEmpty()) {
         this.pendingScrollableContent
            .addChild(this.createText(SENT_HEADER, this.screen.getFont(), this.getListContentWidth()), LayoutSettings::alignHorizontallyCenter);
         outgoingEntries.forEach(this.pendingScrollableContent::addChild);
      }
   }

   @Override
   protected Layout entriesContainer() {
      return this.pendingScrollableContent;
   }

   public void showEmpty() {
      this.pendingScrollableContent.removeChildren();
      LinearLayout content = new LinearLayout(0, 0, LinearLayout.Orientation.VERTICAL).spacing(8);
      content.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
      int maxWidth = this.getListContentWidth();
      content.addChild(this.createCenteredText(EMPTY_STATE, this.screen.getFont(), maxWidth));
      this.contentFrame = this.createCenteredFrame(content, maxWidth, this.height);
      this.pendingScrollableContent.addChild(this.contentFrame);
   }
}
