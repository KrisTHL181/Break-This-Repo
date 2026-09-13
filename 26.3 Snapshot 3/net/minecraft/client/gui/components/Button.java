package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

public abstract class Button extends AbstractButton {
   public static final int SMALL_WIDTH = 120;
   public static final int DEFAULT_WIDTH = 150;
   public static final int BIG_WIDTH = 200;
   public static final int DEFAULT_HEIGHT = 20;
   public static final int DEFAULT_SPACING = 8;
   protected static final Button.CreateNarration DEFAULT_NARRATION = defaultNarrationSupplier -> defaultNarrationSupplier.get();
   protected final Button.OnPress onPress;
   protected final Button.CreateNarration createNarration;

   public static Button.Builder builder(final Component message, final Button.OnPress onPress) {
      return new Button.Builder(message, onPress);
   }

   protected Button(
      final int x,
      final int y,
      final int width,
      final int height,
      final Component message,
      final Button.OnPress onPress,
      final Button.CreateNarration createNarration
   ) {
      super(x, y, width, height, message);
      this.onPress = onPress;
      this.createNarration = createNarration;
   }

   @Override
   public void onPress(final InputWithModifiers input) {
      this.onPress.onPress(this);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
   }

   @Override
   public void updateWidgetNarration(final NarrationElementOutput output) {
      this.defaultButtonNarrationText(output);
   }

   public static class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      private @Nullable Tooltip tooltip;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

      public Builder(final Component message, final Button.OnPress onPress) {
         this.message = message;
         this.onPress = onPress;
      }

      public Button.Builder pos(final int x, final int y) {
         this.x = x;
         this.y = y;
         return this;
      }

      public Button.Builder width(final int width) {
         this.width = width;
         return this;
      }

      public Button.Builder size(final int width, final int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public Button.Builder bounds(final int x, final int y, final int width, final int height) {
         return this.pos(x, y).size(width, height);
      }

      public Button.Builder tooltip(final @Nullable Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Button.Builder createNarration(final Button.CreateNarration createNarration) {
         this.createNarration = createNarration;
         return this;
      }

      public Button build() {
         Button button = new Button.Plain(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration);
         button.setTooltip(this.tooltip);
         return button;
      }
   }

   public interface CreateNarration {
      MutableComponent createNarrationMessage(Supplier<MutableComponent> defaultNarrationSupplier);
   }

   public interface OnPress {
      void onPress(final Button button);
   }

   public static class Plain extends Button {
      protected Plain(
         final int x,
         final int y,
         final int width,
         final int height,
         final Component message,
         final Button.OnPress onPress,
         final Button.CreateNarration createNarration
      ) {
         super(x, y, width, height, message, onPress, createNarration);
      }

      @Override
      protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
         this.extractDefaultSprite(graphics);
         this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
      }
   }
}
