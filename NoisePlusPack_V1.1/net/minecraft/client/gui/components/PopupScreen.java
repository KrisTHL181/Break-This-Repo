package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PopupScreen extends Screen {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("popup/background");
   private static final int SPACING = 12;
   private static final int BG_BORDER_WITH_SPACING = 18;
   private static final int BUTTON_SPACING = 6;
   private static final int IMAGE_SIZE_X = 130;
   private static final int IMAGE_SIZE_Y = 64;
   private static final int POPUP_DEFAULT_WIDTH = 250;
   private final Screen backgroundScreen;
   private final @Nullable Identifier image;
   private final Component message;
   private final List<PopupScreen.ButtonOption> buttons;
   private final @Nullable Runnable onClose;
   private final int contentWidth;
   private final LinearLayout layout = LinearLayout.vertical();

   PopupScreen(
      Screen p_311716_,
      int p_312972_,
      @Nullable Identifier p_457770_,
      Component p_311243_,
      Component p_313078_,
      List<PopupScreen.ButtonOption> p_312924_,
      @Nullable Runnable p_309530_
   ) {
      super(p_311243_);
      this.backgroundScreen = p_311716_;
      this.image = p_457770_;
      this.message = p_313078_;
      this.buttons = p_312924_;
      this.onClose = p_309530_;
      this.contentWidth = p_312972_ - 36;
   }

   @Override
   public void added() {
      super.added();
      this.backgroundScreen.clearFocus();
   }

   @Override
   protected void init() {
      this.backgroundScreen.init(this.width, this.height);
      this.layout.spacing(12).defaultCellSetting().alignHorizontallyCenter();
      this.layout
         .addChild(new MultiLineTextWidget(this.title.copy().withStyle(ChatFormatting.BOLD), this.font).setMaxWidth(this.contentWidth).setCentered(true));
      if (this.image != null) {
         this.layout.addChild(ImageWidget.texture(130, 64, this.image, 130, 64));
      }

      this.layout.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.contentWidth).setCentered(true));
      this.layout.addChild(this.buildButtonRow());
      this.layout.visitWidgets(p_325330_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325330_);
      });
      this.repositionElements();
   }

   private LinearLayout buildButtonRow() {
      int i = 6 * (this.buttons.size() - 1);
      int j = Math.min((this.contentWidth - i) / this.buttons.size(), 150);
      LinearLayout linearlayout = LinearLayout.horizontal();
      linearlayout.spacing(6);

      for (PopupScreen.ButtonOption popupscreen$buttonoption : this.buttons) {
         linearlayout.addChild(Button.builder(popupscreen$buttonoption.message(), p_310515_ -> popupscreen$buttonoption.action().accept(this)).width(j).build());
      }

      return linearlayout;
   }

   @Override
   protected void repositionElements() {
      this.backgroundScreen.resize(this.width, this.height);
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public void renderBackground(GuiGraphics p_312654_, int p_312824_, int p_310533_, float p_313128_) {
      this.backgroundScreen.renderBackground(p_312654_, p_312824_, p_310533_, p_313128_);
      p_312654_.nextStratum();
      this.backgroundScreen.render(p_312654_, -1, -1, p_313128_);
      p_312654_.nextStratum();
      this.renderTransparentBackground(p_312654_);
      p_312654_.blitSprite(
         RenderPipelines.GUI_TEXTURED,
         BACKGROUND_SPRITE,
         this.layout.getX() - 18,
         this.layout.getY() - 18,
         this.layout.getWidth() + 36,
         this.layout.getHeight() + 36
      );
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.title, this.message);
   }

   @Override
   public void onClose() {
      if (this.onClose != null) {
         this.onClose.run();
      }

      this.minecraft.setScreen(this.backgroundScreen);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Screen backgroundScreen;
      private final Component title;
      private Component message = CommonComponents.EMPTY;
      private int width = 250;
      private @Nullable Identifier image;
      private final List<PopupScreen.ButtonOption> buttons = new ArrayList<>();
      private @Nullable Runnable onClose = null;

      public Builder(Screen p_311941_, Component p_309447_) {
         this.backgroundScreen = p_311941_;
         this.title = p_309447_;
      }

      public PopupScreen.Builder setWidth(int p_311856_) {
         this.width = p_311856_;
         return this;
      }

      public PopupScreen.Builder setImage(Identifier p_456171_) {
         this.image = p_456171_;
         return this;
      }

      public PopupScreen.Builder setMessage(Component p_309841_) {
         this.message = p_309841_;
         return this;
      }

      public PopupScreen.Builder addButton(Component p_309455_, Consumer<PopupScreen> p_311142_) {
         this.buttons.add(new PopupScreen.ButtonOption(p_309455_, p_311142_));
         return this;
      }

      public PopupScreen.Builder onClose(Runnable p_311998_) {
         this.onClose = p_311998_;
         return this;
      }

      public PopupScreen build() {
         if (this.buttons.isEmpty()) {
            throw new IllegalStateException("Popup must have at least one button");
         } else {
            return new PopupScreen(this.backgroundScreen, this.width, this.image, this.title, this.message, List.copyOf(this.buttons), this.onClose);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ButtonOption(Component message, Consumer<PopupScreen> action) {
   }
}
