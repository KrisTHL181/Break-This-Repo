package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConfirmScreen extends Screen {
   private final Component message;
   protected LinearLayout layout = LinearLayout.vertical().spacing(8);
   protected Component yesButtonComponent;
   protected Component noButtonComponent;
   protected @Nullable Button yesButton;
   protected @Nullable Button noButton;
   private int delayTicker;
   protected final BooleanConsumer callback;

   public ConfirmScreen(BooleanConsumer p_95654_, Component p_95655_, Component p_95656_) {
      this(p_95654_, p_95655_, p_95656_, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
   }

   public ConfirmScreen(BooleanConsumer p_95658_, Component p_95659_, Component p_95660_, Component p_95661_, Component p_95662_) {
      super(p_95659_);
      this.callback = p_95658_;
      this.message = p_95660_;
      this.yesButtonComponent = p_95661_;
      this.noButtonComponent = p_95662_;
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   @Override
   protected void init() {
      super.init();
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(new StringWidget(this.title, this.font));
      this.layout.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setMaxRows(15).setCentered(true));
      this.addAdditionalText();
      LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(4));
      linearlayout.defaultCellSetting().paddingTop(16);
      this.addButtons(linearlayout);
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void addAdditionalText() {
   }

   protected void addButtons(LinearLayout p_406110_) {
      this.yesButton = p_406110_.addChild(Button.builder(this.yesButtonComponent, p_169259_ -> this.callback.accept(true)).build());
      this.noButton = p_406110_.addChild(Button.builder(this.noButtonComponent, p_169257_ -> this.callback.accept(false)).build());
   }

   public void setDelay(int p_95664_) {
      this.delayTicker = p_95664_;
      this.yesButton.active = false;
      this.noButton.active = false;
   }

   @Override
   public void tick() {
      super.tick();
      if (--this.delayTicker == 0) {
         this.yesButton.active = true;
         this.noButton.active = true;
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean keyPressed(KeyEvent p_427088_) {
      if (this.delayTicker <= 0 && p_427088_.key() == 256) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(p_427088_);
      }
   }
}
