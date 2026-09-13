package net.minecraft.client.gui.components.toasts;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class AdvancementToast implements Toast {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
   public static final int DISPLAY_TIME = 5000;
   private final AdvancementHolder advancement;
   private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
   private final ItemStack iconItem;

   public AdvancementToast(final AdvancementHolder advancement) {
      this.advancement = advancement;
      this.iconItem = advancement.value().display().map(d -> d.getIcon().create()).orElse(ItemStack.EMPTY);
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.wantedVisibility;
   }

   @Override
   public void update(final ToastManager manager, final long fullyVisibleForMs) {
      DisplayInfo display = this.advancement.value().display().orElse(null);
      if (display == null) {
         this.wantedVisibility = Toast.Visibility.HIDE;
      } else {
         this.wantedVisibility = fullyVisibleForMs >= 5000.0 * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
      }
   }

   @Override
   public @Nullable SoundEvent getSoundEvent() {
      return this.isChallengeAdvancement() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
   }

   private boolean isChallengeAdvancement() {
      Optional<DisplayInfo> displayInfo = this.advancement.value().display();
      return displayInfo.isPresent() && displayInfo.get().getType().equals(AdvancementType.CHALLENGE);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final Font font, final long fullyVisibleForMs) {
      DisplayInfo display = this.advancement.value().display().orElse(null);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
      if (display != null) {
         List<FormattedCharSequence> lines = font.split(display.getTitle(), 125);
         int titleColor = display.getType() == AdvancementType.CHALLENGE ? -30465 : -256;
         if (lines.size() == 1) {
            graphics.text(font, display.getType().getDisplayName(), 30, 7, titleColor, false);
            graphics.text(font, lines.get(0), 30, 18, -1, false);
         } else {
            int unlockTextTime = 1500;
            float unlockFadeTime = 300.0F;
            if (fullyVisibleForMs < 1500L) {
               int alpha = Mth.floor(Mth.clamp((float)(1500L - fullyVisibleForMs) / 300.0F, 0.0F, 1.0F) * 255.0F);
               graphics.text(font, display.getType().getDisplayName(), 30, 11, ARGB.color(alpha, titleColor), false);
            } else {
               int alpha = Mth.floor(Mth.clamp((float)(fullyVisibleForMs - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F);
               int y = this.height() / 2 - lines.size() * 9 / 2;

               for (FormattedCharSequence line : lines) {
                  graphics.text(font, line, 30, y, ARGB.white(alpha), false);
                  y += 9;
               }
            }
         }

         graphics.fakeItem(this.iconItem, 8, 8);
      }
   }
}
