package com.mojang.text2speech;

import ca.weblite.objc.Client;
import ca.weblite.objc.NSObject;
import ca.weblite.objc.Proxy;
import ca.weblite.objc.RuntimeUtils;
import ca.weblite.objc.annotations.Msg;
import com.google.common.collect.Queues;
import com.sun.jna.Pointer;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NarratorMac extends NSObject implements Narrator {
   private static final Logger LOGGER = LoggerFactory.getLogger(NarratorMac.class);
   private final Proxy synth = Client.getInstance().sendProxy("NSSpeechSynthesizer", "alloc", new Object[0]);
   private final Queue<String> queue = Queues.newConcurrentLinkedQueue();
   private boolean speaking;
   private boolean crashed;

   public NarratorMac() {
      super("NSObject");
      if (Pointer.nativeValue(this.synth.getPeer()) == 0L) {
         throw new Narrator.FatalException("Failed to create `NSSpeechSynthesizer`");
      }

      if (Pointer.nativeValue(this.getPeer()) == 0L) {
         throw new Narrator.FatalException("Failed to create `NSSpeechSynthesizerDelegate`");
      }

      this.init();
      this.setDelegate();
   }

   private void init() {
      Pointer init = RuntimeUtils.sel("init");
      if (Pointer.nativeValue(init) == 0L) {
         throw new Narrator.FatalException("Failed to find `init` selector");
      }

      RuntimeUtils.msg(this.synth.getPeer(), init, new Object[0]);
   }

   private void setDelegate() {
      Pointer setDelegate = RuntimeUtils.sel("setDelegate:");
      if (Pointer.nativeValue(setDelegate) == 0L) {
         throw new Narrator.FatalException("Failed to find `setDelegate:` selector");
      }

      RuntimeUtils.msg(this.synth.getPeer(), setDelegate, new Object[]{this.getPeer()});
   }

   private void startSpeaking(String message) {
      this.synth.send("startSpeakingString:", new Object[]{message});
   }

   @Msg(selector = "speechSynthesizer:didFinishSpeaking:", signature = "v@:B")
   public void didFinishSpeaking(boolean naturally) {
      if (this.queue.isEmpty()) {
         this.speaking = false;
      } else {
         this.startSpeaking(this.queue.poll());
      }
   }

   @Override
   public void say(String msg, boolean interrupt, float volume) {
      if (!this.crashed) {
         try {
            this.synth.send("setVolume:", new Object[]{volume});
            if (interrupt) {
               this.synth.send("stopSpeaking", new Object[0]);
            }

            if (this.speaking) {
               this.queue.offer(msg);
            } else {
               this.speaking = true;
               this.startSpeaking(msg);
            }
         } catch (Throwable e) {
            this.crashed = true;
            LOGGER.error("Narrator crashed", e);
         }
      }
   }

   @Override
   public void clear() {
      this.queue.clear();
      this.synth.send("stopSpeaking", new Object[0]);
   }

   @Override
   public void destroy() {
   }
}
