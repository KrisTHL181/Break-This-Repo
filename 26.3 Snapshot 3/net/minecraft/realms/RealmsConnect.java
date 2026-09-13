package net.minecraft.realms;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsServer;
import java.net.InetSocketAddress;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsConnect {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Screen onlineScreen;
   private volatile boolean aborted;
   private @Nullable Connection connection;

   public RealmsConnect(final Screen onlineScreen) {
      this.onlineScreen = onlineScreen;
   }

   public void connect(final RealmsServer server, final ServerAddress hostAndPort) {
      final Minecraft minecraft = Minecraft.getInstance();
      minecraft.prepareForMultiplayer();
      minecraft.getNarrator().saySystemNow(Component.translatable("mco.connect.success"));
      final String hostname = hostAndPort.getHost();
      final int port = hostAndPort.getPort();
      (new Thread("Realms-connect-task") {
            @Override
            public void run() {
               InetSocketAddress address = null;

               try {
                  address = new InetSocketAddress(hostname, port);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection = Connection.connectToServer(
                     address, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()), minecraft.getDebugOverlay().getBandwidthLogger()
                  );
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  ClientHandshakePacketListenerImpl clientHandshakePacketListener = new ClientHandshakePacketListenerImpl(
                     RealmsConnect.this.connection,
                     minecraft,
                     server.toServerData(hostname),
                     RealmsConnect.this.onlineScreen,
                     false,
                     null,
                     status -> {},
                     new LevelLoadTracker(),
                     null
                  );
                  if (server.isMinigameActive()) {
                     clientHandshakePacketListener.setMinigameName(server.minigameName);
                  }

                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.initiateServerboundPlayConnection(hostname, port, clientHandshakePacketListener);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.send(new ServerboundHelloPacket(minecraft.getUser().getName(), minecraft.getUser().getProfileId()));
                  minecraft.updateReportEnvironment(ReportEnvironment.realm(server));
                  minecraft.quickPlayLog()
                     .setWorldData(QuickPlayLog.Type.REALMS, String.valueOf(server.id), Objects.requireNonNullElse(server.name, "unknown"));
                  minecraft.getDownloadedPackSource().configureForServerControl(RealmsConnect.this.connection, ServerPackManager.PackPromptStatus.ALLOWED);
               } catch (Exception e) {
                  minecraft.getDownloadedPackSource().cleanupAfterDisconnect();
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.LOGGER.error("Couldn't connect to world", e);
                  String message = e.toString();
                  if (address != null) {
                     String filter = address + ":" + port;
                     message = message.replaceAll(filter, "");
                  }

                  DisconnectedScreen screen = new DisconnectedScreen(
                     RealmsConnect.this.onlineScreen,
                     Component.translatable("mco.connect.failed"),
                     Component.translatable("disconnect.genericReason", message),
                     CommonComponents.GUI_BACK
                  );
                  minecraft.execute(() -> minecraft.gui.setScreen(screen));
               }
            }
         })
         .start();
   }

   public void abort() {
      this.aborted = true;
      if (this.connection != null && this.connection.isConnected()) {
         this.connection.disconnect(Component.translatable("disconnect.genericReason"));
         this.connection.handleDisconnection();
      }
   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }
}
