package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.jspecify.annotations.Nullable;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(
      p_308741_ -> Component.translatableEscape("commands.function.error.argument_not_compound", p_308741_)
   );
   static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType(
      p_308742_ -> Component.translatableEscape("commands.function.scheduled.no_functions", p_308742_)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (p_308724_, p_308725_) -> Component.translatableEscape("commands.function.instantiationFailure", p_308724_, p_308725_)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
      ServerFunctionManager serverfunctionmanager = ((CommandSourceStack)p_137719_.getSource()).getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(serverfunctionmanager.getTagNames(), p_137720_, "#");
      return SharedSuggestionProvider.suggestResource(serverfunctionmanager.getFunctionNames(), p_137720_);
   };
   static final FunctionCommand.Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new FunctionCommand.Callbacks<CommandSourceStack>() {
      public void signalResult(CommandSourceStack p_311645_, Identifier p_457378_, int p_313021_) {
         p_311645_.sendSuccess(() -> Component.translatable("commands.function.result", Component.translationArg(p_457378_), p_313021_), true);
      }
   };

   public static void register(CommandDispatcher<CommandSourceStack> p_137715_) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("with");

      for (DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         datacommands$dataprovider.wrap(literalargumentbuilder, p_308740_ -> p_308740_.executes(new FunctionCommand.FunctionCustomExecutor() {
            @Override
            protected CompoundTag arguments(CommandContext<CommandSourceStack> p_309658_) throws CommandSyntaxException {
               return datacommands$dataprovider.access(p_309658_).getData();
            }
         }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes(new FunctionCommand.FunctionCustomExecutor() {
            @Override
            protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310697_) throws CommandSyntaxException {
               return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(p_310697_, "path"), datacommands$dataprovider.access(p_310697_));
            }
         })));
      }

      p_137715_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("function").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", FunctionArgument.functions())
                        .suggests(SUGGEST_FUNCTION)
                        .executes(new FunctionCommand.FunctionCustomExecutor() {
                           @Override
                           protected @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> p_310275_) {
                              return null;
                           }
                        }))
                     .then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes(new FunctionCommand.FunctionCustomExecutor() {
                        @Override
                        protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310980_) {
                           return CompoundTagArgument.getCompoundTag(p_310980_, "arguments");
                        }
                     })))
                  .then(literalargumentbuilder)
            )
      );
   }

   static CompoundTag getArgumentTag(NbtPathArgument.NbtPath p_298274_, DataAccessor p_301396_) throws CommandSyntaxException {
      Tag tag = DataCommands.getSingleTag(p_298274_, p_301396_);
      if (tag instanceof CompoundTag compoundtag) {
         return compoundtag;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
      }
   }

   public static CommandSourceStack modifySenderForExecution(CommandSourceStack p_309881_) {
      return p_309881_.withSuppressedOutput().withMaximumPermission(LevelBasedPermissionSet.GAMEMASTER);
   }

   public static <T extends ExecutionCommandSource<T>> void queueFunctions(
      Collection<CommandFunction<T>> p_311080_,
      @Nullable CompoundTag p_311435_,
      T p_310141_,
      T p_312402_,
      ExecutionControl<T> p_309669_,
      FunctionCommand.Callbacks<T> p_312300_,
      ChainModifiers p_312226_
   ) throws CommandSyntaxException {
      if (p_312226_.isReturn()) {
         queueFunctionsAsReturn(p_311080_, p_311435_, p_310141_, p_312402_, p_309669_, p_312300_);
      } else {
         queueFunctionsNoReturn(p_311080_, p_311435_, p_310141_, p_312402_, p_309669_, p_312300_);
      }
   }

   private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(
      @Nullable CompoundTag p_312138_,
      ExecutionControl<T> p_309532_,
      CommandDispatcher<T> p_312204_,
      T p_311370_,
      CommandFunction<T> p_310160_,
      Identifier p_455173_,
      CommandResultCallback p_312950_,
      boolean p_312453_
   ) throws CommandSyntaxException {
      try {
         InstantiatedFunction<T> instantiatedfunction = p_310160_.instantiate(p_312138_, p_312204_);
         p_309532_.queueNext(new CallFunction<>(instantiatedfunction, p_312950_, p_312453_).bind(p_311370_));
      } catch (FunctionInstantiationException functioninstantiationexception) {
         throw ERROR_FUNCTION_INSTANTATION_FAILURE.create(p_455173_, functioninstantiationexception.messageComponent());
      }
   }

   private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(
      T p_309693_, FunctionCommand.Callbacks<T> p_309991_, Identifier p_451634_, CommandResultCallback p_312314_
   ) {
      return p_309693_.isSilent() ? p_312314_ : (p_448940_, p_448941_) -> {
         p_309991_.signalResult(p_309693_, p_451634_, p_448941_);
         p_312314_.onResult(p_448940_, p_448941_);
      };
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(
      Collection<CommandFunction<T>> p_309905_,
      @Nullable CompoundTag p_312616_,
      T p_312541_,
      T p_310023_,
      ExecutionControl<T> p_312344_,
      FunctionCommand.Callbacks<T> p_309916_
   ) throws CommandSyntaxException {
      CommandDispatcher<T> commanddispatcher = p_312541_.dispatcher();
      T t = p_310023_.clearCallbacks();
      CommandResultCallback commandresultcallback = CommandResultCallback.chain(p_312541_.callback(), p_312344_.currentFrame().returnValueConsumer());

      for (CommandFunction<T> commandfunction : p_309905_) {
         Identifier identifier = commandfunction.id();
         CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(p_312541_, p_309916_, identifier, commandresultcallback);
         instantiateAndQueueFunctions(p_312616_, p_312344_, commanddispatcher, t, commandfunction, identifier, commandresultcallback1, true);
      }

      p_312344_.queueNext(FallthroughTask.instance());
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(
      Collection<CommandFunction<T>> p_312947_,
      @Nullable CompoundTag p_311961_,
      T p_310755_,
      T p_312089_,
      ExecutionControl<T> p_310294_,
      FunctionCommand.Callbacks<T> p_311742_
   ) throws CommandSyntaxException {
      CommandDispatcher<T> commanddispatcher = p_310755_.dispatcher();
      T t = p_312089_.clearCallbacks();
      CommandResultCallback commandresultcallback = p_310755_.callback();
      if (!p_312947_.isEmpty()) {
         if (p_312947_.size() == 1) {
            CommandFunction<T> commandfunction = p_312947_.iterator().next();
            Identifier identifier = commandfunction.id();
            CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(p_310755_, p_311742_, identifier, commandresultcallback);
            instantiateAndQueueFunctions(p_311961_, p_310294_, commanddispatcher, t, commandfunction, identifier, commandresultcallback1, false);
         } else if (commandresultcallback == CommandResultCallback.EMPTY) {
            for (CommandFunction<T> commandfunction1 : p_312947_) {
               Identifier identifier2 = commandfunction1.id();
               CommandResultCallback commandresultcallback2 = decorateOutputIfNeeded(p_310755_, p_311742_, identifier2, commandresultcallback);
               instantiateAndQueueFunctions(p_311961_, p_310294_, commanddispatcher, t, commandfunction1, identifier2, commandresultcallback2, false);
            }
         } else {
            class Accumulator {
               boolean anyResult;
               int sum;

               public void add(int p_310205_) {
                  this.anyResult = true;
                  this.sum += p_310205_;
               }
            }

            Accumulator functioncommand$1accumulator = new Accumulator();
            CommandResultCallback commandresultcallback4 = (p_308727_, p_308728_) -> functioncommand$1accumulator.add(p_308728_);

            for (CommandFunction<T> commandfunction2 : p_312947_) {
               Identifier identifier1 = commandfunction2.id();
               CommandResultCallback commandresultcallback3 = decorateOutputIfNeeded(p_310755_, p_311742_, identifier1, commandresultcallback4);
               instantiateAndQueueFunctions(p_311961_, p_310294_, commanddispatcher, t, commandfunction2, identifier1, commandresultcallback3, false);
            }

            p_310294_.queueNext((p_308731_, p_308732_) -> {
               if (functioncommand$1accumulator.anyResult) {
                  commandresultcallback.onSuccess(functioncommand$1accumulator.sum);
               }
            });
         }
      }
   }

   public interface Callbacks<T> {
      void signalResult(T var1, Identifier var2, int var3);
   }

   abstract static class FunctionCustomExecutor
      extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
      implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      protected abstract @Nullable CompoundTag arguments(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

      public void runGuarded(
         CommandSourceStack p_310423_, ContextChain<CommandSourceStack> p_311781_, ChainModifiers p_313209_, ExecutionControl<CommandSourceStack> p_312609_
      ) throws CommandSyntaxException {
         CommandContext<CommandSourceStack> commandcontext = p_311781_.getTopContext().copyFor(p_310423_);
         Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> pair = FunctionArgument.getFunctionCollection(commandcontext, "name");
         Collection<CommandFunction<CommandSourceStack>> collection = (Collection<CommandFunction<CommandSourceStack>>)pair.getSecond();
         if (collection.isEmpty()) {
            throw FunctionCommand.ERROR_NO_FUNCTIONS.create(Component.translationArg((Identifier)pair.getFirst()));
         }

         CompoundTag compoundtag = this.arguments(commandcontext);
         CommandSourceStack commandsourcestack = FunctionCommand.modifySenderForExecution(p_310423_);
         if (collection.size() == 1) {
            p_310423_.sendSuccess(
               () -> Component.translatable("commands.function.scheduled.single", Component.translationArg(collection.iterator().next().id())), true
            );
         } else {
            p_310423_.sendSuccess(
               () -> Component.translatable(
                  "commands.function.scheduled.multiple",
                  ComponentUtils.formatList(collection.stream().map(CommandFunction::id).toList(), Component::translationArg)
               ),
               true
            );
         }

         FunctionCommand.queueFunctions(collection, compoundtag, p_310423_, commandsourcestack, p_312609_, FunctionCommand.FULL_CONTEXT_CALLBACKS, p_313209_);
      }
   }
}
