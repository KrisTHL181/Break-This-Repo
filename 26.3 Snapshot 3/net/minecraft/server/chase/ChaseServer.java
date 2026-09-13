package net.minecraft.server.chase;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChaseServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String serverBindAddress;
   private final int serverPort;
   private final PlayerList playerList;
   private final int broadcastIntervalMs;
   private volatile boolean wantsToRun;
   private @Nullable ServerSocket serverSocket;
   private final CopyOnWriteArrayList<Socket> clientSockets = new CopyOnWriteArrayList<>();

   public ChaseServer(final String serverBindAddress, final int serverPort, final PlayerList playerList, final int broadcastIntervalMs) {
      this.serverBindAddress = serverBindAddress;
      this.serverPort = serverPort;
      this.playerList = playerList;
      this.broadcastIntervalMs = broadcastIntervalMs;
   }

   public void start() throws IOException {
      if (this.serverSocket != null && !this.serverSocket.isClosed()) {
         LOGGER.warn("Remote control server was asked to start, but it is already running. Will ignore.");
      } else {
         this.wantsToRun = true;
         this.serverSocket = new ServerSocket(this.serverPort, 50, InetAddress.getByName(this.serverBindAddress));
         Thread acceptor = new Thread(this::runAcceptor, "chase-server-acceptor");
         acceptor.setDaemon(true);
         acceptor.start();
         Thread sender = new Thread(this::runSender, "chase-server-sender");
         sender.setDaemon(true);
         sender.start();
      }
   }

   private void runSender() {
      ChaseServer.PlayerPosition oldPlayerPosition = null;

      while (this.wantsToRun) {
         if (!this.clientSockets.isEmpty()) {
            ChaseServer.PlayerPosition playerPosition = this.getPlayerPosition();
            if (playerPosition != null && !playerPosition.equals(oldPlayerPosition)) {
               oldPlayerPosition = playerPosition;
               byte[] messageBytes = playerPosition.format().getBytes(StandardCharsets.US_ASCII);

               for (Socket clientSocket : this.clientSockets) {
                  if (!clientSocket.isClosed()) {
                     Util.ioPool().execute(() -> {
                        try {
                           OutputStream output = clientSocket.getOutputStream();
                           output.write(messageBytes);
                           output.flush();
                        } catch (IOException e) {
                           LOGGER.info("Remote control client socket got an IO exception and will be closed", e);
                           IOUtils.closeQuietly(clientSocket);
                        }
                     });
                  }
               }
            }

            List<Socket> closed = this.clientSockets.stream().filter(Socket::isClosed).collect(Collectors.toList());
            this.clientSockets.removeAll(closed);
         }

         if (this.wantsToRun) {
            try {
               Thread.sleep(this.broadcastIntervalMs);
            } catch (InterruptedException var6) {
            }
         }
      }
   }

   public void stop() {
      this.wantsToRun = false;
      IOUtils.closeQuietly(this.serverSocket);
      this.serverSocket = null;
   }

   private void runAcceptor() {
      try {
         while (this.wantsToRun) {
            if (this.serverSocket != null) {
               LOGGER.info("Remote control server is listening for connections on port {}", this.serverPort);
               Socket clientSocket = this.serverSocket.accept();
               LOGGER.info("Remote control server received client connection on port {}", clientSocket.getPort());
               this.clientSockets.add(clientSocket);
            }
         }
      } catch (ClosedByInterruptException e) {
         if (this.wantsToRun) {
            LOGGER.info("Remote control server closed by interrupt");
         }
      } catch (IOException e) {
         if (this.wantsToRun) {
            LOGGER.error("Remote control server closed because of an IO exception", e);
         }
      } finally {
         IOUtils.closeQuietly(this.serverSocket);
      }

      LOGGER.info("Remote control server is now stopped");
      this.wantsToRun = false;
   }

   private ChaseServer.@Nullable PlayerPosition getPlayerPosition() {
      List<ServerPlayer> players = this.playerList.getPlayers();
      if (players.isEmpty()) {
         return null;
      }

      ServerPlayer player = players.get(0);
      String dimensionName = (String)ChaseCommand.DIMENSION_NAMES.inverse().get(player.level().dimension());
      return dimensionName == null
         ? null
         : new ChaseServer.PlayerPosition(dimensionName, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
   }

   private record PlayerPosition(String dimensionName, double x, double y, double z, float yRot, float xRot) {
      private String format() {
         return String.format(Locale.ROOT, "t %s %.2f %.2f %.2f %.2f %.2f\n", this.dimensionName, this.x, this.y, this.z, this.yRot, this.xRot);
      }
   }
}
