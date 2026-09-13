package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class LockIconButton extends Button {
   private boolean locked;

   public LockIconButton(final int x, final int y, final Button.OnPress onPress) {
      super(x, y, 20, 20, Component.translatable("narrator.button.difficulty_lock"), onPress, DEFAULT_NARRATION);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      return CommonComponents.joinForNarration(
         super.createNarrationMessage(),
         this.isLocked()
            ? Component.translatable("narrator.button.difficulty_lock.locked")
            : Component.translatable("narrator.button.difficulty_lock.unlocked")
      );
   }

   public boolean isLocked() {
      return this.locked;
   }

   public void setLocked(final boolean locked) {
      this.locked = locked;
   }

   @Override
   public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      LockIconButton.Icon icon;
      if (!this.active) {
         icon = this.locked ? LockIconButton.Icon.LOCKED_DISABLED : LockIconButton.Icon.UNLOCKED_DISABLED;
      } else if (this.isHoveredOrFocused()) {
         icon = this.locked ? LockIconButton.Icon.LOCKED_HOVER : LockIconButton.Icon.UNLOCKED_HOVER;
      } else {
         icon = this.locked ? LockIconButton.Icon.LOCKED : LockIconButton.Icon.UNLOCKED;
      }

      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.sprite, this.getX(), this.getY(), this.width, this.height);
   }

   private enum Icon {
      LOCKED(Identifier.withDefaultNamespace("widget/locked_button")),
      LOCKED_HOVER(Identifier.withDefaultNamespace("widget/locked_button_highlighted")),
      LOCKED_DISABLED(Identifier.withDefaultNamespace("widget/locked_button_disabled")),
      UNLOCKED(Identifier.withDefaultNamespace("widget/unlocked_button")),
      UNLOCKED_HOVER(Identifier.withDefaultNamespace("widget/unlocked_button_highlighted")),
      UNLOCKED_DISABLED(Identifier.withDefaultNamespace("widget/unlocked_button_disabled"));

      private final Identifier sprite;

      Icon(final Identifier sprite) {
         this.sprite = sprite;
      }
   }
}
