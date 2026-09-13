package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LockIconButton extends Button {
   private boolean locked;

   public LockIconButton(int p_94299_, int p_94300_, Button.OnPress p_94301_) {
      super(p_94299_, p_94300_, 20, 20, Component.translatable("narrator.button.difficulty_lock"), p_94301_, DEFAULT_NARRATION);
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

   public void setLocked(boolean p_94310_) {
      this.locked = p_94310_;
   }

   @Override
   public void renderContents(GuiGraphics p_282701_, int p_282638_, int p_283565_, float p_282549_) {
      LockIconButton.Icon lockiconbutton$icon;
      if (!this.active) {
         lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED_DISABLED : LockIconButton.Icon.UNLOCKED_DISABLED;
      } else if (this.isHoveredOrFocused()) {
         lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED_HOVER : LockIconButton.Icon.UNLOCKED_HOVER;
      } else {
         lockiconbutton$icon = this.locked ? LockIconButton.Icon.LOCKED : LockIconButton.Icon.UNLOCKED;
      }

      p_282701_.blitSprite(RenderPipelines.GUI_TEXTURED, lockiconbutton$icon.sprite, this.getX(), this.getY(), this.width, this.height);
   }

   @OnlyIn(Dist.CLIENT)
   enum Icon {
      LOCKED(Identifier.withDefaultNamespace("widget/locked_button")),
      LOCKED_HOVER(Identifier.withDefaultNamespace("widget/locked_button_highlighted")),
      LOCKED_DISABLED(Identifier.withDefaultNamespace("widget/locked_button_disabled")),
      UNLOCKED(Identifier.withDefaultNamespace("widget/unlocked_button")),
      UNLOCKED_HOVER(Identifier.withDefaultNamespace("widget/unlocked_button_highlighted")),
      UNLOCKED_DISABLED(Identifier.withDefaultNamespace("widget/unlocked_button_disabled"));

      final Identifier sprite;

      Icon(final Identifier p_459486_) {
         this.sprite = p_459486_;
      }
   }
}
