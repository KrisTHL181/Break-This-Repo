package net.minecraft.client;

import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GameNarrator {
   public static final Component NO_TITLE = CommonComponents.EMPTY;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final Narrator narrator = Narrator.getNarrator();

   public GameNarrator(Minecraft p_240577_) {
      this.minecraft = p_240577_;
   }

   public void sayChatQueued(Component p_408728_) {
      if (this.getStatus().shouldNarrateChat()) {
         this.narrateNotInterruptingMessage(p_408728_);
      }
   }

   public void saySystemChatQueued(Component p_409591_) {
      if (this.getStatus().shouldNarrateSystemOrChat()) {
         this.narrateNotInterruptingMessage(p_409591_);
      }
   }

   public void saySystemQueued(Component p_407712_) {
      if (this.getStatus().shouldNarrateSystem()) {
         this.narrateNotInterruptingMessage(p_407712_);
      }
   }

   private void narrateNotInterruptingMessage(Component p_406106_) {
      String s = p_406106_.getString();
      if (!s.isEmpty()) {
         this.logNarratedMessage(s);
         this.narrateMessage(s, false);
      }
   }

   public void saySystemNow(Component p_406079_) {
      this.saySystemNow(p_406079_.getString());
   }

   public void saySystemNow(String p_93320_) {
      if (this.getStatus().shouldNarrateSystem() && !p_93320_.isEmpty()) {
         this.logNarratedMessage(p_93320_);
         if (this.narrator.active()) {
            this.narrator.clear();
            this.narrateMessage(p_93320_, true);
         }
      }
   }

   private void narrateMessage(String p_391325_, boolean p_392353_) {
      this.narrator.say(p_391325_, p_392353_, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
   }

   private NarratorStatus getStatus() {
      return this.minecraft.options.narrator().get();
   }

   private void logNarratedMessage(String p_168788_) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.debug("Narrating: {}", p_168788_.replaceAll("\n", "\\\\n"));
      }
   }

   public void updateNarratorStatus(NarratorStatus p_93318_) {
      this.clear();
      this.narrateMessage(Component.translatable("options.narrator").append(" : ").append(p_93318_.getName()).getString(), true);
      ToastManager toastmanager = Minecraft.getInstance().getToastManager();
      if (this.narrator.active()) {
         if (p_93318_ == NarratorStatus.OFF) {
            SystemToast.addOrUpdate(toastmanager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.disabled"), null);
         } else {
            SystemToast.addOrUpdate(
               toastmanager, SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.translatable("narrator.toast.enabled"), p_93318_.getName()
            );
         }
      } else {
         SystemToast.addOrUpdate(
            toastmanager,
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

   public void checkStatus(boolean p_289016_) {
      if (p_289016_
         && !this.isActive()
         && !TinyFileDialogs.tinyfd_messageBox(
            "Minecraft",
            "Failed to initialize text-to-speech library. Do you want to continue?\nIf this problem persists, please report it at bugs.mojang.com",
            "yesno",
            "error",
            true
         )) {
         throw new GameNarrator.NarratorInitException("Narrator library is not active");
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NarratorInitException extends SilentInitException {
      public NarratorInitException(String p_288985_) {
         super(p_288985_);
      }
   }
}
