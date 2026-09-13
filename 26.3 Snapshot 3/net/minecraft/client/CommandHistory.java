package net.minecraft.client;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import net.minecraft.util.ArrayListDeque;
import org.slf4j.Logger;

public class CommandHistory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_PERSISTED_COMMAND_HISTORY = 50;
   private static final String PERSISTED_COMMANDS_FILE_NAME = "command_history.txt";
   private final Path commandsPath;
   private final ArrayListDeque<String> lastCommands = new ArrayListDeque<>(50);

   public CommandHistory(final Path gameFolder) {
      this.commandsPath = gameFolder.resolve("command_history.txt");
      if (Files.exists(this.commandsPath)) {
         try (BufferedReader reader = Files.newBufferedReader(this.commandsPath, StandardCharsets.UTF_8)) {
            this.lastCommands.addAll(reader.lines().toList());
         } catch (Exception exception) {
            LOGGER.error("Failed to read {}, command history will be missing", "command_history.txt", exception);
         }
      }
   }

   public void addCommand(final String command) {
      if (!command.equals(this.lastCommands.peekLast())) {
         if (this.lastCommands.size() >= 50) {
            this.lastCommands.removeFirst();
         }

         this.lastCommands.addLast(command);
         this.save();
      }
   }

   private void save() {
      try (BufferedWriter writer = Files.newBufferedWriter(this.commandsPath, StandardCharsets.UTF_8)) {
         for (String command : this.lastCommands) {
            writer.write(command);
            writer.newLine();
         }
      } catch (IOException exception) {
         LOGGER.error("Failed to write {}, command history will be missing", "command_history.txt", exception);
      }
   }

   public Collection<String> history() {
      return this.lastCommands;
   }
}
