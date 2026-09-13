package net.minecraft.server.chase;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChaseClient {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int RECONNECT_INTERVAL_SECONDS = 5;
   private final String serverHost;
   private final int serverPort;
   private final MinecraftServer server;
   private volatile boolean wantsToRun;
   private @Nullable Socket socket;
   private @Nullable Thread thread;

   public ChaseClient(final String serverHost, final int serverPort, final MinecraftServer server) {
      this.serverHost = serverHost;
      this.serverPort = serverPort;
      this.server = server;
   }

   public void start() {
      if (this.thread != null && this.thread.isAlive()) {
         LOGGER.warn("Remote control client was asked to start, but it is already running. Will ignore.");
      }

      this.wantsToRun = true;
      this.thread = new Thread(this::run, "chase-client");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   public void stop() {
      this.wantsToRun = false;
      IOUtils.closeQuietly(this.socket);
      this.socket = null;
      this.thread = null;
   }

   public void run() {
      String serverAddress = this.serverHost + ":" + this.serverPort;

      while (this.wantsToRun) {
         try {
            LOGGER.info("Connecting to remote control server {}", serverAddress);
            this.socket = new Socket(this.serverHost, this.serverPort);
            LOGGER.info("Connected to remote control server! Will continuously execute the command broadcasted by that server.");

            try (BufferedReader input = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.US_ASCII))) {
               while (this.wantsToRun) {
                  String message = input.readLine();
                  if (message == null) {
                     LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", serverAddress, 5);
                     break;
                  }

                  this.handleMessage(message);
               }
            } catch (IOException err) {
               LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", serverAddress, 5);
            }
         } catch (IOException e) {
            LOGGER.warn("Failed to connect to remote control server {}. Will retry in {}s.", serverAddress, 5);
         }

         if (this.wantsToRun) {
            try {
               Thread.sleep(5000L);
            } catch (InterruptedException var5) {
            }
         }
      }
   }

   private void handleMessage(final String message) {
      try (Scanner scanner = new Scanner(new StringReader(message))) {
         scanner.useLocale(Locale.ROOT);
         String head = scanner.next();
         if ("t".equals(head)) {
            this.handleTeleport(scanner);
         } else {
            LOGGER.warn("Unknown message type '{}'", head);
         }
      } catch (NoSuchElementException e) {
         LOGGER.warn("Could not parse message '{}', ignoring", message);
      }
   }

   private void handleTeleport(final Scanner scanner) {
      parseTarget(scanner)
         .ifPresent(
            target -> this.executeCommand(
               String.format(
                  Locale.ROOT,
                  "execute in %s run tp @s %.3f %.3f %.3f %.3f %.3f",
                  target.level.identifier(),
                  target.pos.x,
                  target.pos.y,
                  target.pos.z,
                  target.rot.y,
                  target.rot.x
               )
            )
         );
   }

   private static Optional<ChaseClient.TeleportTarget> parseTarget(final Scanner scanner) {
      ResourceKey<Level> levelType = (ResourceKey<Level>)ChaseCommand.DIMENSION_NAMES.get(scanner.next());
      if (levelType == null) {
         return Optional.empty();
      }

      float x = scanner.nextFloat();
      float y = scanner.nextFloat();
      float z = scanner.nextFloat();
      float yRot = scanner.nextFloat();
      float xRot = scanner.nextFloat();
      return Optional.of(new ChaseClient.TeleportTarget(levelType, new Vec3(x, y, z), new Vec2(xRot, yRot)));
   }

   private void executeCommand(final String command) {
      this.server
         .execute(
            () -> {
               List<ServerPlayer> players = this.server.getPlayerList().getPlayers();
               if (!players.isEmpty()) {
                  ServerPlayer player = players.get(0);
                  ServerLevel level = this.server.overworld();
                  CommandSourceStack commandSourceStack = new CommandSourceStack(
                     player.commandSource(),
                     Vec3.atLowerCornerOf(level.getRespawnData().pos()),
                     Vec2.ZERO,
                     level,
                     LevelBasedPermissionSet.OWNER,
                     "",
                     CommonComponents.EMPTY,
                     this.server,
                     player
                  );
                  Commands commands = this.server.getCommands();
                  commands.performPrefixedCommand(commandSourceStack, command);
               }
            }
         );
   }

   private record TeleportTarget(ResourceKey<Level> level, Vec3 pos, Vec2 rot) {
   }
}
