package net.minecraft.client;

import com.mojang.blaze3d.platform.MessageBox;
import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

public class GameNarrator {
   public static final Component NO_TITLE = CommonComponents.EMPTY;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final Narrator narrator = Narrator.getNarrator();

   public GameNarrator(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void sayChatQueued(final Component message) {
      if (this.getStatus().shouldNarrateChat()) {
         this.narrateNotInterruptingMessage(message);
      }
   }

   public void saySystemChatQueued(final Component message) {
      if (this.getStatus().shouldNarrateSystemOrChat()) {
         this.narrateNotInterruptingMessage(message);
      }
   }

   public void saySystemQueued(final Component message) {
      if (this.getStatus().shouldNarrateSystem()) {
         this.narrateNotInterruptingMessage(message);
      }
   }

   private void narrateNotInterruptingMessage(final Component message) {
      String messageString = message.getString();
      if (!messageString.isEmpty()) {
         this.logNarratedMessage(messageString);
         this.narrateMessage(messageString, false);
      }
   }

   public void saySystemNow(final Component message) {
      this.saySystemNow(message.getString());
   }

   public void saySystemNow(final String message) {
      if (this.getStatus().shouldNarrateSystem() && !message.isEmpty()) {
         this.logNarratedMessage(message);
         if (this.narrator.active()) {
            this.narrator.clear();
            this.narrateMessage(message, true);
         }
      }
   }

   private void narrateMessage(final String message, final boolean interrupt) {
      this.narrator.say(message, interrupt, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
   }

   private NarratorStatus getStatus() {
      return this.minecraft.options.narrator().get();
   }

   private void logNarratedMessage(final String message) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.debug("Narrating: {}", message.replaceAll("\n", "\\\\n"));
      }
   }

   public void updateNarratorStatus(final NarratorStatus status) {
      this.clear();
      this.narrateMessage(Component.translatable("options.narrator").append(" : ").append(status.getName()).getString(), true);
      ToastManager toastManager = Minecraft.getInstance().gui.toastManager();
      if (this.narrator.active()) {
         if (status == NarratorStatus.OFF) {
            SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.disabled"), null);
         } else {
            SystemToast.addOrUpdate(toastManager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.enabled"), status.getName());
         }
      } else {
         SystemToast.addOrUpdate(
            toastManager,
            SystemToast.SystemToastId.NARRATOR_TOGGLE,
            Component.translatable("narrator.toast.disabled"),
            Component.translatable("options.narrator.notavailable")
         );
      }
   }

   public boolean isActive() {
      return this.narrator.active();
   }

   public void clear() {
      if (this.getStatus() != NarratorStatus.OFF && this.narrator.active()) {
         this.narrator.clear();
      }
   }

   public void destroy() {
      this.narrator.destroy();
   }

   public void checkStatus(final boolean requiredActive) {
      if (requiredActive
         && !this.isActive()
         && !MessageBox.errorWithContinue(
            "Failed to initialize text-to-speech library. Do you want to continue?\nIf this problem persists, please report it at bugs.mojang.com"
         )) {
         throw new GameNarrator.NarratorInitException("Narrator library is not active");
      }
   }

   public static class NarratorInitException extends SilentInitException {
      public NarratorInitException(final String message) {
         super(message);
      }
   }
}
