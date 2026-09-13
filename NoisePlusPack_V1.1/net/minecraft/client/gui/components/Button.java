package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class Button extends AbstractButton {
   public static final int SMALL_WIDTH = 120;
   public static final int DEFAULT_WIDTH = 150;
   public static final int BIG_WIDTH = 200;
   public static final int DEFAULT_HEIGHT = 20;
   public static final int DEFAULT_SPACING = 8;
   protected static final Button.CreateNarration DEFAULT_NARRATION = p_253298_ -> p_253298_.get();
   protected final Button.OnPress onPress;
   protected final Button.CreateNarration createNarration;

   public static Button.Builder builder(Component p_254439_, Button.OnPress p_254567_) {
      return new Button.Builder(p_254439_, p_254567_);
   }

   protected Button(int p_259075_, int p_259271_, int p_260232_, int p_260028_, Component p_259351_, Button.OnPress p_260152_, Button.CreateNarration p_259552_) {
      super(p_259075_, p_259271_, p_260232_, p_260028_, p_259351_);
      this.onPress = p_260152_;
      this.createNarration = p_259552_;
   }

   @Override
   public void onPress(InputWithModifiers p_424878_) {
      this.onPress.onPress(this);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_259196_) {
      this.defaultButtonNarrationText(p_259196_);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Button.OnPress onPress;
      private @Nullable Tooltip tooltip;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

      public Builder(Component p_254097_, Button.OnPress p_253761_) {
         this.message = p_254097_;
         this.onPress = p_253761_;
      }

      public Button.Builder pos(int p_254538_, int p_254216_) {
         this.x = p_254538_;
         this.y = p_254216_;
         return this;
      }

      public Button.Builder width(int p_254259_) {
         this.width = p_254259_;
         return this;
      }

      public Button.Builder size(int p_253727_, int p_254457_) {
         this.width = p_253727_;
         this.height = p_254457_;
         return this;
      }

      public Button.Builder bounds(int p_254166_, int p_253872_, int p_254522_, int p_253985_) {
         return this.pos(p_254166_, p_253872_).size(p_254522_, p_253985_);
      }

      public Button.Builder tooltip(@Nullable Tooltip p_259609_) {
         this.tooltip = p_259609_;
         return this;
      }

      public Button.Builder createNarration(Button.CreateNarration p_253638_) {
         this.createNarration = p_253638_;
         return this;
      }

      public Button build() {
         Button button = new Button.Plain(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration);
         button.setTooltip(this.tooltip);
         return button;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface CreateNarration {
      MutableComponent createNarrationMessage(Supplier<MutableComponent> var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface OnPress {
      void onPress(Button var1);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Plain extends Button {
      protected Plain(
         int p_455297_, int p_455388_, int p_459653_, int p_452847_, Component p_459347_, Button.OnPress p_453378_, Button.CreateNarration p_456163_
      ) {
         super(p_455297_, p_455388_, p_459653_, p_452847_, p_459347_, p_453378_, p_456163_);
      }

      @Override
      protected void renderContents(GuiGraphics p_460253_, int p_454889_, int p_455896_, float p_458060_) {
         this.renderDefaultSprite(p_460253_);
         this.renderDefaultLabel(p_460253_.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
      }
   }
}
