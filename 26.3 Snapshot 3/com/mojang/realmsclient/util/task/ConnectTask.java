package com.mojang.realmsclient.util.task;

import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsConnect;

public class ConnectTask extends LongRunningTask {
   private static final Component TITLE = Component.translatable("mco.connect.connecting");
   private final RealmsConnect realmsConnect;
   private final RealmsServer server;
   private final RealmsJoinInformation address;

   public ConnectTask(final Screen lastScreen, final RealmsServer server, final RealmsJoinInformation address) {
      this.server = server;
      this.address = address;
      this.realmsConnect = new RealmsConnect(lastScreen);
   }

   @Override
   public void run() {
      if (this.address.address() != null) {
         this.realmsConnect.connect(this.server, ServerAddress.parseString(this.address.address()));
      } else {
         this.abortTask();
      }
   }

   @Override
   public void abortTask() {
      super.abortTask();
      this.realmsConnect.abort();
      Minecraft.getInstance().getDownloadedPackSource().cleanupAfterDisconnect();
   }

   @Override
   public void tick() {
      this.realmsConnect.tick();
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
