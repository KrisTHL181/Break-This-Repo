package net.minecraft.client.server;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DefaultUncaughtExceptionHandler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LanServerDetection {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   private static final Logger LOGGER = LogUtils.getLogger();

   public static class LanServerDetector extends Thread {
      private final LanServerDetection.LanServerList serverList;
      private final InetAddress pingGroup;
      private final MulticastSocket socket;

      public LanServerDetector(final LanServerDetection.LanServerList serverList) throws IOException {
         super("LanServerDetector #" + LanServerDetection.UNIQUE_THREAD_ID.incrementAndGet());
         this.serverList = serverList;
         this.setDaemon(true);
         this.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LanServerDetection.LOGGER));
         this.socket = new MulticastSocket(4445);
         this.pingGroup = InetAddress.getByName("224.0.2.60");
         this.socket.setSoTimeout(5000);
         this.socket.joinGroup(this.pingGroup);
      }

      @Override
      public void run() {
         byte[] buf = new byte[1024];

         while (!this.isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
               this.socket.receive(packet);
            } catch (SocketTimeoutException ignored) {
               continue;
            } catch (IOException e) {
               LanServerDetection.LOGGER.error("Couldn't ping server", e);
               break;
            }

            String received = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
            LanServerDetection.LOGGER.debug("{}: {}", packet.getAddress(), received);
            this.serverList.addServer(received, packet.getAddress());
         }

         try {
            this.socket.leaveGroup(this.pingGroup);
         } catch (IOException var4) {
         }

         this.socket.close();
      }
   }

   public static class LanServerList {
      private final List<LanServer> servers = Lists.newArrayList();
      private boolean isDirty;

      public synchronized @Nullable List<LanServer> takeDirtyServers() {
         if (this.isDirty) {
            List<LanServer> newServers = List.copyOf(this.servers);
            this.isDirty = false;
            return newServers;
         } else {
            return null;
         }
      }

      public synchronized void addServer(final String pingData, final InetAddress socketAddress) {
         String motd = LanServerPinger.parseMotd(pingData);
         String address = LanServerPinger.parseAddress(pingData);
         if (address != null) {
            address = socketAddress.getHostAddress() + ":" + address;
            boolean found = false;

            for (LanServer server : this.servers) {
               if (server.getAddress().equals(address)) {
                  server.updatePingTime();
                  found = true;
                  break;
               }
            }

            if (!found) {
               this.servers.add(new LanServer(motd, address));
               this.isDirty = true;
            }
         }
      }
   }
}
