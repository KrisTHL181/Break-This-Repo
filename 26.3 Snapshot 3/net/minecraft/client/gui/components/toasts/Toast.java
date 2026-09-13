package net.minecraft.client.gui.components.toasts;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public interface Toast {
   Object NO_TOKEN = new Object();
   int DEFAULT_WIDTH = 160;
   int SLOT_HEIGHT = 32;

   Toast.Visibility getWantedVisibility();

   void update(final ToastManager manager, final long fullyVisibleForMs);

   default @Nullable SoundEvent getSoundEvent() {
      return null;
   }

   void extractRenderState(final GuiGraphicsExtractor graphics, final Font font, final long fullyVisibleForMs);

   default Object getToken() {
      return NO_TOKEN;
   }

   default float xPos(final int screenWidth, final float visiblePortion) {
      return screenWidth - this.width() * visiblePortion;
   }

   default float yPos(final int firstSlotIndex) {
      return firstSlotIndex * this.height();
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

   enum Visibility {
      SHOW(SoundEvents.UI_TOAST_IN),
      HIDE(SoundEvents.UI_TOAST_OUT);

      private final SoundEvent soundEvent;

      Visibility(final SoundEvent soundEvent) {
         this.soundEvent = soundEvent;
      }

      public void playSound(final SoundManager manager) {
         manager.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0F, 1.0F));
      }
   }
}
