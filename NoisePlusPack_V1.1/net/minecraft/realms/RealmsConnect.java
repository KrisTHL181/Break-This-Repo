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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConnect {
   static final Logger LOGGER = LogUtils.getLogger();
   final Screen onlineScreen;
   volatile boolean aborted;
   @Nullable Connection connection;

   public RealmsConnect(Screen p_120693_) {
      this.onlineScreen = p_120693_;
   }

   public void connect(final RealmsServer p_175032_, ServerAddress p_175033_) {
      final Minecraft minecraft = Minecraft.getInstance();
      minecraft.prepareForMultiplayer();
      minecraft.getNarrator().saySystemNow(Component.translatable("mco.connect.success"));
      final String s = p_175033_.getHost();
      final int i = p_175033_.getPort();
      (new Thread("Realms-connect-task") {
            @Override
            public void run() {
               InetSocketAddress inetsocketaddress = null;

               try {
                  inetsocketaddress = new InetSocketAddress(s, i);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection = Connection.connectToServer(
                     inetsocketaddress, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()), minecraft.getDebugOverlay().getBandwidthLogger()
                  );
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  ClientHandshakePacketListenerImpl clienthandshakepacketlistenerimpl = new ClientHandshakePacketListenerImpl(
                     RealmsConnect.this.connection,
                     minecraft,
                     p_175032_.toServerData(s),
                     RealmsConnect.this.onlineScreen,
                     false,
                     null,
                     p_120726_ -> {},
                     new LevelLoadTracker(),
                     null
                  );
                  if (p_175032_.isMinigameActive()) {
                     clienthandshakepacketlistenerimpl.setMinigameName(p_175032_.minigameName);
                  }

                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.initiateServerboundPlayConnection(s, i, clienthandshakepacketlistenerimpl);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.send(new ServerboundHelloPacket(minecraft.getUser().getName(), minecraft.getUser().getProfileId()));
                  minecraft.updateReportEnvironment(ReportEnvironment.realm(p_175032_));
                  minecraft.quickPlayLog()
                     .setWorldData(QuickPlayLog.Type.REALMS, String.valueOf(p_175032_.id), Objects.requireNonNullElse(p_175032_.name, "unknown"));
                  minecraft.getDownloadedPackSource().configureForServerControl(RealmsConnect.this.connection, ServerPackManager.PackPromptStatus.ALLOWED);
               } catch (Exception exception) {
                  minecraft.getDownloadedPackSource().cleanupAfterDisconnect();
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.LOGGER.error("Couldn't connect to world", exception);
                  String s1 = exception.toString();
                  if (inetsocketaddress != null) {
                     String s2 = inetsocketaddress + ":" + i;
                     s1 = s1.replaceAll(s2, "");
                  }

                  DisconnectedScreen disconnectedscreen = new DisconnectedScreen(
                     RealmsConnect.this.onlineScreen,
                     Component.translatable("mco.connect.failed"),
                     Component.translatable("disconnect.genericReason", s1),
                     CommonComponents.GUI_BACK
                  );
                  minecraft.execute(() -> minecraft.setScreen(disconnectedscreen));
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
