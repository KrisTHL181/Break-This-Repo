package net.minecraft.data.info;

import com.mojang.brigadier.CommandDispatcher;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class CommandsReport implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public CommandsReport(PackOutput p_256167_, CompletableFuture<HolderLookup.Provider> p_256506_) {
      this.output = p_256167_;
      this.registries = p_256506_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_253721_) {
      Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("commands.json");
      return this.registries
         .thenCompose(
            p_256367_ -> {
               CommandDispatcher<CommandSourceStack> commanddispatcher = new Commands(
                     Commands.CommandSelection.ALL, Commands.createValidationContext(p_256367_)
                  )
                  .getDispatcher();
               return DataProvider.saveStable(p_253721_, ArgumentUtils.serializeNodeToJson(commanddispatcher, commanddispatcher.getRoot()), path);
            }
         );
   }

   @Override
   public final String getName() {
      return "Command Syntax";
   }
}
