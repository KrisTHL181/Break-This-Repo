package net.minecraft.client.gui.screens.dialog;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class WaitingForResponseScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.waitingForResponse.title");
   private static final Component[] BUTTON_LABELS = new Component[]{
      Component.empty(),
      Component.translatable("gui.waitingForResponse.button.inactive", 4),
      Component.translatable("gui.waitingForResponse.button.inactive", 3),
      Component.translatable("gui.waitingForResponse.button.inactive", 2),
      Component.translatable("gui.waitingForResponse.button.inactive", 1),
      CommonComponents.GUI_BACK
   };
   private static final int BUTTON_VISIBLE_AFTER = 1;
   private static final int BUTTON_ACTIVE_AFTER = 5;
   private final @Nullable Screen previousScreen;
   private final HeaderAndFooterLayout layout;
   private final Button closeButton;
   private int ticks;

   public WaitingForResponseScreen(final @Nullable Screen nextScreen) {
      super(TITLE);
      this.previousScreen = nextScreen;
      this.layout = new HeaderAndFooterLayout(this, 33, 0);
      this.closeButton = Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(200).build();
   }

   @Override
   protected void init() {
      super.init();
      this.layout.addTitleHeader(TITLE, this.font);
      this.layout.addToContents(this.closeButton);
      this.closeButton.visible = false;
      this.closeButton.active = false;
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.closeButton.active) {
         int secondsVisible = this.ticks++ / 20;
         this.closeButton.visible = secondsVisible >= 1;
         this.closeButton.setMessage(BUTTON_LABELS[secondsVisible]);
         if (secondsVisible == 5) {
            this.closeButton.active = true;
            this.triggerImmediateNarration(true);
         }
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.closeButton.active;
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.previousScreen);
   }

   public @Nullable Screen previousScreen() {
      return this.previousScreen;
   }
}
