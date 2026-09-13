package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class InBedChatScreen extends ChatScreen {
   private Button leaveBedButton;

   public InBedChatScreen(final String initial, final boolean isDraft) {
      super(initial, isDraft, false);
   }

   @Override
   protected void init() {
      super.init();
      this.leaveBedButton = Button.builder(Component.translatable("multiplayer.stopSleeping"), button -> this.sendWakeUp())
         .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
         .build();
      this.addRenderableWidget(this.leaveBedButton);
   }

   @Override
   public void onClose() {
      this.sendWakeUp();
   }

   private void sendWakeUp() {
      ClientPacketListener connection = this.minecraft.player.connection;
      connection.send(new ServerboundPlayerCommandPacket(this.minecraft.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
   }

   public void onPlayerWokeUp() {
      String text = this.input.getValue();
      if (!this.isDraft && !text.isEmpty()) {
         this.exitReason = ChatScreen.ExitReason.DONE;
         this.minecraft.gui.setScreen(new ChatScreen(text, false));
      } else {
         this.exitReason = ChatScreen.ExitReason.INTERRUPTED;
         this.minecraft.gui.setScreen(null);
      }
   }
}
