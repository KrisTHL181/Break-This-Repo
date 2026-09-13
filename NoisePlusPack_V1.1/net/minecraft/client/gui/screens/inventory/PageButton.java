package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PageButton extends Button {
   private static final Identifier PAGE_FORWARD_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/page_forward_highlighted");
   private static final Identifier PAGE_FORWARD_SPRITE = Identifier.withDefaultNamespace("widget/page_forward");
   private static final Identifier PAGE_BACKWARD_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/page_backward_highlighted");
   private static final Identifier PAGE_BACKWARD_SPRITE = Identifier.withDefaultNamespace("widget/page_backward");
   private static final Component PAGE_BUTTON_NEXT = Component.translatable("book.page_button.next");
   private static final Component PAGE_BUTTON_PREVIOUS = Component.translatable("book.page_button.previous");
   private final boolean isForward;
   private final boolean playTurnSound;

   public PageButton(int p_99225_, int p_99226_, boolean p_99227_, Button.OnPress p_99228_, boolean p_99229_) {
      super(p_99225_, p_99226_, 23, 13, p_99227_ ? PAGE_BUTTON_NEXT : PAGE_BUTTON_PREVIOUS, p_99228_, DEFAULT_NARRATION);
      this.isForward = p_99227_;
      this.playTurnSound = p_99229_;
   }

   @Override
   public void renderContents(GuiGraphics p_283468_, int p_282922_, int p_283637_, float p_282459_) {
      Identifier identifier;
      if (this.isForward) {
         identifier = this.isHoveredOrFocused() ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE;
      } else {
         identifier = this.isHoveredOrFocused() ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE;
      }

      p_283468_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), 23, 13);
   }

   @Override
   public void playDownSound(SoundManager p_99231_) {
      if (this.playTurnSound) {
         p_99231_.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
      }
   }

   @Override
   public boolean shouldTakeFocusAfterInteraction() {
      return false;
   }
}
