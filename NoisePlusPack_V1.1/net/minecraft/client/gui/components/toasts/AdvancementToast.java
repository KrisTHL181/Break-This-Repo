package net.minecraft.client.gui.components.toasts;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AdvancementToast implements Toast {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
   public static final int DISPLAY_TIME = 5000;
   private final AdvancementHolder advancement;
   private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

   public AdvancementToast(AdvancementHolder p_298724_) {
      this.advancement = p_298724_;
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return this.wantedVisibility;
   }

   @Override
   public void update(ToastManager p_367661_, long p_369792_) {
      DisplayInfo displayinfo = this.advancement.value().display().orElse(null);
      if (displayinfo == null) {
         this.wantedVisibility = Toast.Visibility.HIDE;
      } else {
         this.wantedVisibility = p_369792_ >= 5000.0 * p_367661_.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
      }
   }

   @Override
   public @Nullable SoundEvent getSoundEvent() {
      return this.isChallengeAdvancement() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
   }

   private boolean isChallengeAdvancement() {
      Optional<DisplayInfo> optional = this.advancement.value().display();
      return optional.isPresent() && optional.get().getType().equals(AdvancementType.CHALLENGE);
   }

   @Override
   public void render(GuiGraphics p_281813_, Font p_367310_, long p_282604_) {
      DisplayInfo displayinfo = this.advancement.value().display().orElse(null);
      p_281813_.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
      if (displayinfo != null) {
         List<FormattedCharSequence> list = p_367310_.split(displayinfo.getTitle(), 125);
         int i = displayinfo.getType() == AdvancementType.CHALLENGE ? -30465 : -256;
         if (list.size() == 1) {
            p_281813_.drawString(p_367310_, displayinfo.getType().getDisplayName(), 30, 7, i, false);
            p_281813_.drawString(p_367310_, list.get(0), 30, 18, -1, false);
         } else {
            int j = 1500;
            float f = 300.0F;
            if (p_282604_ < 1500L) {
               int k = Mth.floor(Mth.clamp((float)(1500L - p_282604_) / 300.0F, 0.0F, 1.0F) * 255.0F);
               p_281813_.drawString(p_367310_, displayinfo.getType().getDisplayName(), 30, 11, ARGB.color(k, i), false);
            } else {
               int i1 = Mth.floor(Mth.clamp((float)(p_282604_ - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F);
               int l = this.height() / 2 - list.size() * 9 / 2;

               for (FormattedCharSequence formattedcharsequence : list) {
                  p_281813_.drawString(p_367310_, formattedcharsequence, 30, l, ARGB.white(i1), false);
                  l += 9;
               }
            }
         }

         p_281813_.renderFakeItem(displayinfo.getIcon(), 8, 8);
      }
   }
}
