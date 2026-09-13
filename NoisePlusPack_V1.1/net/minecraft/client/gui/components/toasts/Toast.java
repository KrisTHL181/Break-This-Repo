package net.minecraft.client.gui.components.toasts;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface Toast {
   Object NO_TOKEN = new Object();
   int DEFAULT_WIDTH = 160;
   int SLOT_HEIGHT = 32;

   Toast.Visibility getWantedVisibility();

   void update(ToastManager var1, long var2);

   default @Nullable SoundEvent getSoundEvent() {
      return null;
   }

   void render(GuiGraphics var1, Font var2, long var3);

   default Object getToken() {
      return NO_TOKEN;
   }

   default float xPos(int p_408043_, float p_408621_) {
      return p_408043_ - this.width() * p_408621_;
   }

   default float yPos(int p_406321_) {
      return p_406321_ * this.height();
   }

   default int width() {
      return 160;
   }

   default int height() {
      return 32;
   }

   default int occcupiedSlotCount() {
      return Mth.positiveCeilDiv(this.height(), 32);
   }

   default void onFinishedRendering() {
   }

   @OnlyIn(Dist.CLIENT)
   enum Visibility {
      SHOW(SoundEvents.UI_TOAST_IN),
      HIDE(SoundEvents.UI_TOAST_OUT);

      private final SoundEvent soundEvent;

      Visibility(final SoundEvent p_94908_) {
         this.soundEvent = p_94908_;
      }

      public void playSound(SoundManager p_94910_) {
         p_94910_.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0F, 1.0F));
      }
   }
}
