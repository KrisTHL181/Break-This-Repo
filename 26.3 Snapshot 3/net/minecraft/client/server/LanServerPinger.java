package net.minecraft.client.server;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DefaultUncaughtExceptionHandler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LanServerPinger extends Thread {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String MULTICAST_GROUP = "224.0.2.60";
   public static final int PING_PORT = 4445;
   private static final long PING_INTERVAL = 1500L;
   private final String motd;
   private final DatagramSocket socket;
   private boolean isRunning = true;
   private final String serverAddress;

   public LanServerPinger(final String motd, final String serverAddress) throws IOException {
      super("LanServerPinger #" + UNIQUE_THREAD_ID.incrementAndGet());
      this.motd = motd;
      this.serverAddress = serverAddress;
      this.setDaemon(true);
      this.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      this.socket = new DatagramSocket();
   }

   @Override
   public void run() {
      String pingString = createPingString(this.motd, this.serverAddress);
      byte[] ping = pingString.getBytes(StandardCharsets.UTF_8);

      while (!this.isInterrupted() && this.isRunning) {
         try {
            InetAddress group = InetAddress.getByName("224.0.2.60");
            DatagramPacket packet = new DatagramPacket(ping, ping.length, group, 4445);
            this.socket.send(packet);
         } catch (IOException e) {
            LOGGER.warn("LanServerPinger failed to run", e);
            break;
         }

         try {
            sleep(1500L);
         } catch (InterruptedException var5) {
         }
      }
   }

   @Override
   public void interrupt() {
      super.interrupt();
      this.isRunning = false;
   }

   public static String createPingString(final String motd, final String address) {
      return "[MOTD]" + motd + "[/MOTD][AD]" + address + "[/AD]";
   }

   public static String parseMotd(final String pingString) {
      int startIndex = pingString.indexOf("[MOTD]");
      if (startIndex < 0) {
         return "missing no";
      }

      int endIndex = pingString.indexOf("[/MOTD]", startIndex + "[MOTD]".length());
      return endIndex < startIndex ? "missing no" : pingString.substring(startIndex + "[MOTD]".length(), endIndex);
   }

   public static @Nullable String parseAddress(final String pingString) {
      int endMotdIndex = pingString.indexOf("[/MOTD]");
      if (endMotdIndex < 0) {
         return null;
      }

      int secondEndMotdIndex = pingString.indexOf("[/MOTD]", endMotdIndex + "[/MOTD]".length());
      if (secondEndMotdIndex >= 0) {
         return null;
      }

      int startIndex = pingString.indexOf("[AD]", endMotdIndex + "[/MOTD]".length());
      if (startIndex < 0) {
         return null;
      }

      int endIndex = pingString.indexOf("[/AD]", startIndex + "[AD]".length());
      return endIndex < startIndex ? null : pingString.substring(startIndex + "[AD]".length(), endIndex);
   }
}
