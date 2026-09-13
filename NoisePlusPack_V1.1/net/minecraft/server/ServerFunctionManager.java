package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ServerFunctionManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier TICK_FUNCTION_TAG = Identifier.withDefaultNamespace("tick");
   private static final Identifier LOAD_FUNCTION_TAG = Identifier.withDefaultNamespace("load");
   private final MinecraftServer server;
   private List<CommandFunction<CommandSourceStack>> ticking = ImmutableList.of();
   private boolean postReload;
   private ServerFunctionLibrary library;

   public ServerFunctionManager(MinecraftServer p_136110_, ServerFunctionLibrary p_136111_) {
      this.server = p_136110_;
      this.library = p_136111_;
      this.postReload(p_136111_);
   }

   public CommandDispatcher<CommandSourceStack> getDispatcher() {
      return this.server.getCommands().getDispatcher();
   }

   public void tick() {
      if (this.server.tickRateManager().runsNormally()) {
         if (this.postReload) {
            this.postReload = false;
            Collection<CommandFunction<CommandSourceStack>> collection = this.library.getTag(LOAD_FUNCTION_TAG);
            this.executeTagFunctions(collection, LOAD_FUNCTION_TAG);
         }

         this.executeTagFunctions(this.ticking, TICK_FUNCTION_TAG);
      }
   }

   private void executeTagFunctions(Collection<CommandFunction<CommandSourceStack>> p_136116_, Identifier p_451194_) {
      Profiler.get().push(p_451194_::toString);

      for (CommandFunction<CommandSourceStack> commandfunction : p_136116_) {
         this.execute(commandfunction, this.getGameLoopSender());
      }

      Profiler.get().pop();
   }

   public void execute(CommandFunction<CommandSourceStack> p_311911_, CommandSourceStack p_136114_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push(() -> "function " + p_311911_.id());

      try {
         InstantiatedFunction<CommandSourceStack> instantiatedfunction = p_311911_.instantiate(null, this.getDispatcher());
         Commands.executeCommandInContext(
            p_136114_, p_311172_ -> ExecutionContext.queueInitialFunctionCall(p_311172_, instantiatedfunction, p_136114_, CommandResultCallback.EMPTY)
         );
      } catch (FunctionInstantiationException var9) {
      } catch (Exception exception) {
         LOGGER.warn("Failed to execute function {}", p_311911_.id(), exception);
      } finally {
         profilerfiller.pop();
      }
   }

   public void replaceLibrary(ServerFunctionLibrary p_136121_) {
      this.library = p_136121_;
      this.postReload(p_136121_);
   }

   private void postReload(ServerFunctionLibrary p_136126_) {
      this.ticking = List.copyOf(p_136126_.getTag(TICK_FUNCTION_TAG));
      this.postReload = true;
   }

   public CommandSourceStack getGameLoopSender() {
      return this.server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
   }

   public Optional<CommandFunction<CommandSourceStack>> get(Identifier p_452841_) {
      return this.library.getFunction(p_452841_);
   }

   public List<CommandFunction<CommandSourceStack>> getTag(Identifier p_457118_) {
      return this.library.getTag(p_457118_);
   }

   public Iterable<Identifier> getFunctionNames() {
      return this.library.getFunctions().keySet();
   }

   public Iterable<Identifier> getTagNames() {
      return this.library.getAvailableTags();
   }
}
