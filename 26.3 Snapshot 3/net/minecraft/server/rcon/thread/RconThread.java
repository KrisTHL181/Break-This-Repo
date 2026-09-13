package net.minecraft.server.rcon.thread;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RconThread extends GenericThread {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerSocket socket;
   private final String rconPassword;
   private final List<RconClient> clients = Lists.newArrayList();
   private final ServerInterface serverInterface;

   private RconThread(final ServerInterface serverInterface, final ServerSocket socket, final String rconPassword) {
      super("RCON Listener");
      this.serverInterface = serverInterface;
      this.socket = socket;
      this.rconPassword = rconPassword;
   }

   private void clearClients() {
      this.clients.removeIf(client -> !client.isRunning());
   }

   @Override
   public void run() {
      try {
         while (this.running) {
            try {
               Socket client = this.socket.accept();
               RconClient rconClient = new RconClient(this.serverInterface, this.rconPassword, client);
               rconClient.start();
               this.clients.add(rconClient);
               this.clearClients();
            } catch (SocketTimeoutException ignored) {
               this.clearClients();
            } catch (IOException e) {
               if (this.running) {
                  LOGGER.info("IO exception: ", e);
               }
            }
         }
      } finally {
         this.closeSocket(this.socket);
      }
   }

   public static @Nullable RconThread create(final ServerInterface serverInterface) {
      DedicatedServerProperties settings = serverInterface.getProperties();
      String serverIp = serverInterface.getServerIp();
      if (serverIp.isEmpty()) {
         serverIp = "0.0.0.0";
      }

      int port = settings.rconPort;
      if (0 < port && 65535 >= port) {
         String password = settings.rconPassword;
         if (password.isEmpty()) {
            LOGGER.warn("No rcon password set in server.properties, rcon disabled!");
            return null;
         }

         try {
            ServerSocket socket = new ServerSocket(port, 0, InetAddress.getByName(serverIp));
            socket.setSoTimeout(500);
            RconThread result = new RconThread(serverInterface, socket, password);
            if (!result.start()) {
               return null;
            }

            LOGGER.info("RCON running on {}:{}", serverIp, port);
            return result;
         } catch (IOException e) {
            LOGGER.warn("Unable to initialise RCON on {}:{}", new Object[]{serverIp, port, e});
            return null;
         }
      } else {
         LOGGER.warn("Invalid rcon port {} found in server.properties, rcon disabled!", port);
         return null;
      }
   }

   @Override
   public void stop() {
      this.running = false;
      this.closeSocket(this.socket);
      super.stop();

      for (RconClient rconClient : this.clients) {
         if (rconClient.isRunning()) {
            rconClient.stop();
         }
      }

      this.clients.clear();
   }

   private void closeSocket(final ServerSocket socket) {
      LOGGER.debug("closeSocket: {}", socket);

      try {
         socket.close();
      } catch (IOException e) {
         LOGGER.warn("Failed to close socket", e);
      }
   }
}
