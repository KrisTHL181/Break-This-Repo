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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CommandHistory {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_PERSISTED_COMMAND_HISTORY = 50;
   private static final String PERSISTED_COMMANDS_FILE_NAME = "command_history.txt";
   private final Path commandsPath;
   private final ArrayListDeque<String> lastCommands = new ArrayListDeque<>(50);

   public CommandHistory(Path p_298502_) {
      this.commandsPath = p_298502_.resolve("command_history.txt");
      if (Files.exists(this.commandsPath)) {
         try (BufferedReader bufferedreader = Files.newBufferedReader(this.commandsPath, StandardCharsets.UTF_8)) {
            this.lastCommands.addAll(bufferedreader.lines().toList());
         } catch (Exception exception) {
            LOGGER.error("Failed to read {}, command history will be missing", "command_history.txt", exception);
         }
      }
   }

   public void addCommand(String p_298813_) {
      if (!p_298813_.equals(this.lastCommands.peekLast())) {
         if (this.lastCommands.size() >= 50) {
            this.lastCommands.removeFirst();
         }

         this.lastCommands.addLast(p_298813_);
         this.save();
      }
   }

   private void save() {
      try (BufferedWriter bufferedwriter = Files.newBufferedWriter(this.commandsPath, StandardCharsets.UTF_8)) {
         for (String s : this.lastCommands) {
            bufferedwriter.write(s);
            bufferedwriter.newLine();
         }
      } catch (IOException ioexception) {
         LOGGER.error("Failed to write {}, command history will be missing", "command_history.txt", ioexception);
      }
   }

   public Collection<String> history() {
      return this.lastCommands;
   }
}
