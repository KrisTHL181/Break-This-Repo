package net.minecraft.client.gui.screens.multiplayer;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class WarningScreen extends Screen {
   private static final int MESSAGE_PADDING = 100;
   private final Component message;
   private final @Nullable Component check;
   private final Component narration;
   protected @Nullable Checkbox stopShowing;
   private @Nullable FittingMultiLineTextWidget messageWidget;
   private final FrameLayout layout;

   protected WarningScreen(final Component title, final Component message, final Component narration) {
      this(title, message, null, narration);
   }

   protected WarningScreen(final Component title, final Component message, final @Nullable Component check, final Component narration) {
      super(title);
      this.message = message;
      this.check = check;
      this.narration = narration;
      this.layout = new FrameLayout(0, 0, this.width, this.height);
   }

   protected abstract Layout addFooterButtons();

   @Override
   protected void init() {
      LinearLayout content = this.layout.addChild(LinearLayout.vertical().spacing(8));
      content.defaultCellSetting().alignHorizontallyCenter();
      content.addChild(new StringWidget(this.getTitle(), this.font));
      this.messageWidget = content.addChild(
         new FittingMultiLineTextWidget(0, 0, this.width - 100, this.height - 100, this.message, this.font), s -> s.padding(12)
      );
      LinearLayout footer = content.addChild(LinearLayout.vertical().spacing(8));
      footer.defaultCellSetting().alignHorizontallyCenter();
      if (this.check != null) {
         this.stopShowing = footer.addChild(Checkbox.builder(this.check, this.font).build());
      }

      footer.addChild(this.addFooterButtons());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      if (this.messageWidget != null) {
         this.messageWidget.setWidth(this.width - 100);
         this.messageWidget.setHeight(this.height - 100);
         this.messageWidget.minimizeHeight();
      }

      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return this.narration;
   }
}
