package net.minecraft.server.rcon.thread;

import com.mojang.logging.LogUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.PktUtils;
import org.slf4j.Logger;

public class RconClient extends GenericThread {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SERVERDATA_AUTH = 3;
   private static final int SERVERDATA_EXECCOMMAND = 2;
   private static final int SERVERDATA_RESPONSE_VALUE = 0;
   private static final int SERVERDATA_AUTH_RESPONSE = 2;
   private static final int SERVERDATA_AUTH_FAILURE = -1;
   private boolean authed;
   private final Socket client;
   private final byte[] buf = new byte[1460];
   private final String rconPassword;
   private final ServerInterface serverInterface;

   public RconClient(final ServerInterface serverInterface, final String rconPassword, final Socket socket) {
      super("RCON Client " + socket.getInetAddress());
      this.serverInterface = serverInterface;
      this.client = socket;

      try {
         this.client.setSoTimeout(0);
      } catch (Exception ignored) {
         this.running = false;
      }

      this.rconPassword = rconPassword;
   }

   @Override
   public void run() {
      try {
         try {
            while (this.running) {
               BufferedInputStream inputStream = new BufferedInputStream(this.client.getInputStream());
               int read = inputStream.read(this.buf, 0, 1460);
               if (10 > read) {
                  return;
               }

               int offset = 0;
               int pktsize = PktUtils.intFromByteArray(this.buf, 0, read);
               if (pktsize != read - 4) {
                  return;
               }

               offset += 4;
               int requestid = PktUtils.intFromByteArray(this.buf, offset, read);
               offset += 4;
               int cmd = PktUtils.intFromByteArray(this.buf, offset);
               offset += 4;
               switch (cmd) {
                  case 2:
                     if (this.authed) {
                        String command = PktUtils.stringFromByteArray(this.buf, offset, read);

                        try {
                           this.sendCmdResponse(requestid, this.serverInterface.runCommand(command));
                        } catch (Exception e) {
                           this.sendCmdResponse(requestid, "Error executing: " + command + " (" + e.getMessage() + ")");
                        }
                        break;
                     }

                     this.sendAuthFailure();
                     break;
                  case 3:
                     String password = PktUtils.stringFromByteArray(this.buf, offset, read);
                     offset += password.length();
                     if (!password.isEmpty() && password.equals(this.rconPassword)) {
                        this.authed = true;
                        this.send(requestid, 2, "");
                        break;
                     }

                     this.authed = false;
                     this.sendAuthFailure();
                     break;
                  default:
                     this.sendCmdResponse(requestid, String.format(Locale.ROOT, "Unknown request %s", Integer.toHexString(cmd)));
               }
            }

            return;
         } catch (IOException var15) {
         } catch (Exception e) {
            LOGGER.error("Exception whilst parsing RCON input", e);
         }
      } finally {
         this.closeSocket();
         LOGGER.info("Thread {} shutting down", this.name);
         this.running = false;
      }
   }

   private void send(final int requestid, final int cmd, final String str) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1248);
      DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
      byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
      dataOutputStream.writeInt(Integer.reverseBytes(bytes.length + 10));
      dataOutputStream.writeInt(Integer.reverseBytes(requestid));
      dataOutputStream.writeInt(Integer.reverseBytes(cmd));
      dataOutputStream.write(bytes);
      dataOutputStream.write(0);
      dataOutputStream.write(0);
      this.client.getOutputStream().write(outputStream.toByteArray());
   }

   private void sendAuthFailure() throws IOException {
      this.send(-1, 2, "");
   }

   private void sendCmdResponse(final int requestid, String response) throws IOException {
      int len = response.length();

      do {
         int dataLen = 4096 <= len ? 4096 : len;
         this.send(requestid, 0, response.substring(0, dataLen));
         response = response.substring(dataLen);
         len = response.length();
      } while (0 != len);
   }

   @Override
   public void stop() {
      this.running = false;
      this.closeSocket();
      super.stop();
   }

   private void closeSocket() {
      try {
         this.client.close();
      } catch (IOException e) {
         LOGGER.warn("Failed to close socket", e);
      }
   }
}
