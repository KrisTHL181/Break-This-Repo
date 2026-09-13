package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import dev.miru.commands.TppModCommands;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.server.commands.AttributeCommand;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.ChaseCommand;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.DamageCommand;
import net.minecraft.server.commands.DataPackCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.DebugCommand;
import net.minecraft.server.commands.DebugConfigCommand;
import net.minecraft.server.commands.DebugMobSpawningCommand;
import net.minecraft.server.commands.DebugPathCommand;
import net.minecraft.server.commands.DefaultGameModeCommands;
import net.minecraft.server.commands.DialogCommand;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.server.commands.FetchProfileCommand;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.commands.HelpCommand;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.JfrCommand;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.RaidCommand;
import net.minecraft.server.commands.RandomCommand;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.commands.ReturnCommand;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.server.commands.RotateCommand;
import net.minecraft.server.commands.SaveAllCommand;
import net.minecraft.server.commands.SaveOffCommand;
import net.minecraft.server.commands.SaveOnCommand;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.commands.ScheduleCommand;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.commands.SeedCommand;
import net.minecraft.server.commands.ServerPackCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.SetPlayerIdleTimeoutCommand;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.commands.SetWorldSpawnCommand;
import net.minecraft.server.commands.SpawnArmorTrimsCommand;
import net.minecraft.server.commands.SpectateCommand;
import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.commands.StopCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.server.commands.StopwatchCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.TagCommand;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.commands.TickCommand;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.commands.TransferCommand;
import net.minecraft.server.commands.TriggerCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.server.commands.WardenSpawnTrackerCommand;
import net.minecraft.server.commands.WaypointCommand;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Commands {
   public static final String COMMAND_PREFIX = "/";
   private static final ThreadLocal<@Nullable ExecutionContext<CommandSourceStack>> CURRENT_EXECUTION_CONTEXT = new ThreadLocal<>();
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final PermissionCheck LEVEL_ALL = PermissionCheck.AlwaysPass.INSTANCE;
   public static final PermissionCheck LEVEL_MODERATORS = new PermissionCheck.Require(Permissions.COMMANDS_MODERATOR);
   public static final PermissionCheck LEVEL_GAMEMASTERS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
   public static final PermissionCheck LEVEL_ADMINS = new PermissionCheck.Require(Permissions.COMMANDS_ADMIN);
   public static final PermissionCheck LEVEL_OWNERS = new PermissionCheck.Require(Permissions.COMMANDS_OWNER);
   private static final ClientboundCommandsPacket.NodeInspector<CommandSourceStack> COMMAND_NODE_INSPECTOR = new ClientboundCommandsPacket.NodeInspector<CommandSourceStack>() {
      private final CommandSourceStack noPermissionSource = Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS);

      @Override
      public @Nullable Identifier suggestionId(ArgumentCommandNode<CommandSourceStack, ?> p_406913_) {
         SuggestionProvider<CommandSourceStack> suggestionprovider = p_406913_.getCustomSuggestions();
         return suggestionprovider != null ? SuggestionProviders.getName(suggestionprovider) : null;
      }

      @Override
      public boolean isExecutable(CommandNode<CommandSourceStack> p_406408_) {
         return p_406408_.getCommand() != null;
      }

      @Override
      public boolean isRestricted(CommandNode<CommandSourceStack> p_406438_) {
         Predicate<CommandSourceStack> predicate = p_406438_.getRequirement();
         return !predicate.test(this.noPermissionSource);
      }
   };
   private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher();

   public Commands(Commands.CommandSelection p_230943_, CommandBuildContext p_230944_) {
      AdvancementCommands.register(this.dispatcher);
      AttributeCommand.register(this.dispatcher, p_230944_);
      ExecuteCommand.register(this.dispatcher, p_230944_);
      BossBarCommands.register(this.dispatcher, p_230944_);
      ClearInventoryCommands.register(this.dispatcher, p_230944_);
      CloneCommands.register(this.dispatcher, p_230944_);
      DamageCommand.register(this.dispatcher, p_230944_);
      DataCommands.register(this.dispatcher);
      DataPackCommand.register(this.dispatcher, p_230944_);
      DebugCommand.register(this.dispatcher);
      DefaultGameModeCommands.register(this.dispatcher);
      DialogCommand.register(this.dispatcher, p_230944_);
      DifficultyCommand.register(this.dispatcher);
      EffectCommands.register(this.dispatcher, p_230944_);
      EmoteCommands.register(this.dispatcher);
      EnchantCommand.register(this.dispatcher, p_230944_);
      ExperienceCommand.register(this.dispatcher);
      FillCommand.register(this.dispatcher, p_230944_);
      FillBiomeCommand.register(this.dispatcher, p_230944_);
      ForceLoadCommand.register(this.dispatcher);
      FunctionCommand.register(this.dispatcher);
      GameModeCommand.register(this.dispatcher);
      GameRuleCommand.register(this.dispatcher, p_230944_);
      GiveCommand.register(this.dispatcher, p_230944_);
      HelpCommand.register(this.dispatcher);
      ItemCommands.register(this.dispatcher, p_230944_);
      KickCommand.register(this.dispatcher);
      KillCommand.register(this.dispatcher);
      ListPlayersCommand.register(this.dispatcher);
      LocateCommand.register(this.dispatcher, p_230944_);
      LootCommand.register(this.dispatcher, p_230944_);
      MsgCommand.register(this.dispatcher);
      ParticleCommand.register(this.dispatcher, p_230944_);
      PlaceCommand.register(this.dispatcher);
      PlaySoundCommand.register(this.dispatcher);
      RandomCommand.register(this.dispatcher);
      ReloadCommand.register(this.dispatcher);
      RecipeCommand.register(this.dispatcher);
      FetchProfileCommand.register(this.dispatcher);
      ReturnCommand.register(this.dispatcher);
      RideCommand.register(this.dispatcher);
      RotateCommand.register(this.dispatcher);
      SayCommand.register(this.dispatcher);
      ScheduleCommand.register(this.dispatcher);
      ScoreboardCommand.register(this.dispatcher, p_230944_);
      SeedCommand.register(this.dispatcher, p_230943_ != Commands.CommandSelection.INTEGRATED);
      VersionCommand.register(this.dispatcher, p_230943_ != Commands.CommandSelection.INTEGRATED);
      SetBlockCommand.register(this.dispatcher, p_230944_);
      SetSpawnCommand.register(this.dispatcher);
      SetWorldSpawnCommand.register(this.dispatcher);
      SpectateCommand.register(this.dispatcher);
      SpreadPlayersCommand.register(this.dispatcher);
      StopSoundCommand.register(this.dispatcher);
      StopwatchCommand.register(this.dispatcher);
      SummonCommand.register(this.dispatcher, p_230944_);
      TagCommand.register(this.dispatcher);
      TeamCommand.register(this.dispatcher, p_230944_);
      TeamMsgCommand.register(this.dispatcher);
      TeleportCommand.register(this.dispatcher);
      TellRawCommand.register(this.dispatcher, p_230944_);
      TestCommand.register(this.dispatcher, p_230944_);
      TickCommand.register(this.dispatcher);
      TimeCommand.register(this.dispatcher);
      TitleCommand.register(this.dispatcher, p_230944_);
      TriggerCommand.register(this.dispatcher);
      WaypointCommand.register(this.dispatcher, p_230944_);
      WeatherCommand.register(this.dispatcher);
      WorldBorderCommand.register(this.dispatcher);
      JvmProfiler.INSTANCE.isAvailable();
      JfrCommand.register(this.dispatcher);
      ChaseCommand.register(this.dispatcher);
      RaidCommand.register(this.dispatcher, p_230944_);
      DebugPathCommand.register(this.dispatcher);
      DebugMobSpawningCommand.register(this.dispatcher);
      WardenSpawnTrackerCommand.register(this.dispatcher);
      SpawnArmorTrimsCommand.register(this.dispatcher);
      ServerPackCommand.register(this.dispatcher);
      DebugConfigCommand.register(this.dispatcher, p_230944_);
      BanIpCommands.register(this.dispatcher);
      BanListCommands.register(this.dispatcher);
      BanPlayerCommands.register(this.dispatcher);
      DeOpCommands.register(this.dispatcher);
      OpCommand.register(this.dispatcher);
      PardonCommand.register(this.dispatcher);
      PardonIpCommand.register(this.dispatcher);
      PerfCommand.register(this.dispatcher);
      SaveAllCommand.register(this.dispatcher);
      SaveOffCommand.register(this.dispatcher);
      SaveOnCommand.register(this.dispatcher);
      SetPlayerIdleTimeoutCommand.register(this.dispatcher);
      StopCommand.register(this.dispatcher);
      TransferCommand.register(this.dispatcher);
      WhitelistCommand.register(this.dispatcher);
      PublishCommand.register(this.dispatcher);
      TppModCommands.register(this.dispatcher);
      this.dispatcher.setConsumer(ExecutionCommandSource.resultConsumer());
   }

   public static <S> ParseResults<S> mapSource(ParseResults<S> p_242928_, UnaryOperator<S> p_242890_) {
      CommandContextBuilder<S> commandcontextbuilder = p_242928_.getContext();
      CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder.withSource(p_242890_.apply((S)commandcontextbuilder.getSource()));
      return new ParseResults(commandcontextbuilder1, p_242928_.getReader(), p_242928_.getExceptions());
   }

   public void performPrefixedCommand(CommandSourceStack p_230958_, String p_230959_) {
      p_230959_ = trimOptionalPrefix(p_230959_);
      this.performCommand(this.dispatcher.parse(p_230959_, p_230958_), p_230959_);
   }

   public static String trimOptionalPrefix(String p_410619_) {
      return p_410619_.startsWith("/") ? p_410619_.substring(1) : p_410619_;
   }

   public void performCommand(ParseResults<CommandSourceStack> p_242844_, String p_242841_) {
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_242844_.getContext().getSource();
      Profiler.get().push(() -> "/" + p_242841_);
      ContextChain<CommandSourceStack> contextchain = finishParsing(p_242844_, p_242841_, commandsourcestack);

      try {
         if (contextchain != null) {
            executeCommandInContext(
               commandsourcestack,
               p_308343_ -> ExecutionContext.queueInitialCommandExecution(p_308343_, p_242841_, contextchain, commandsourcestack, CommandResultCallback.EMPTY)
            );
         }
      } catch (Exception exception) {
         MutableComponent mutablecomponent = Component.literal(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
         if (LOGGER.isDebugEnabled()) {
            LOGGER.error("Command exception: /{}", p_242841_, exception);
            StackTraceElement[] astacktraceelement = exception.getStackTrace();

            for (int i = 0; i < Math.min(astacktraceelement.length, 3); i++) {
               mutablecomponent.append("\n\n")
                  .append(astacktraceelement[i].getMethodName())
                  .append("\n ")
                  .append(astacktraceelement[i].getFileName())
                  .append(":")
                  .append(String.valueOf(astacktraceelement[i].getLineNumber()));
            }
         }

         commandsourcestack.sendFailure(
            Component.translatable("command.failed").withStyle(p_389638_ -> p_389638_.withHoverEvent(new HoverEvent.ShowText(mutablecomponent)))
         );
         if (SharedConstants.DEBUG_VERBOSE_COMMAND_ERRORS || SharedConstants.IS_RUNNING_IN_IDE) {
            commandsourcestack.sendFailure(Component.literal(Util.describeError(exception)));
            LOGGER.error("'/{}' threw an exception", p_242841_, exception);
         }
      } finally {
         Profiler.get().pop();
      }
   }

   private static @Nullable ContextChain<CommandSourceStack> finishParsing(
      ParseResults<CommandSourceStack> p_311671_, String p_312044_, CommandSourceStack p_310074_
   ) {
      try {
         validateParseResults(p_311671_);
         return (ContextChain<CommandSourceStack>)ContextChain.tryFlatten(p_311671_.getContext().build(p_312044_))
            .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(p_311671_.getReader()));
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_310074_.sendFailure(ComponentUtils.fromMessage(commandsyntaxexception.getRawMessage()));
         if (commandsyntaxexception.getInput() != null && commandsyntaxexception.getCursor() >= 0) {
            int i = Math.min(commandsyntaxexception.getInput().length(), commandsyntaxexception.getCursor());
            MutableComponent mutablecomponent = Component.empty()
               .withStyle(ChatFormatting.GRAY)
               .withStyle(p_389640_ -> p_389640_.withClickEvent(new ClickEvent.SuggestCommand("/" + p_312044_)));
            if (i > 10) {
               mutablecomponent.append(CommonComponents.ELLIPSIS);
            }

            mutablecomponent.append(commandsyntaxexception.getInput().substring(Math.max(0, i - 10), i));
            if (i < commandsyntaxexception.getInput().length()) {
               Component component = Component.literal(commandsyntaxexception.getInput().substring(i)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
               mutablecomponent.append(component);
            }

            mutablecomponent.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            p_310074_.sendFailure(mutablecomponent);
         }

         return null;
      }
   }

   public static void executeCommandInContext(CommandSourceStack p_312477_, Consumer<ExecutionContext<CommandSourceStack>> p_313031_) {
      ExecutionContext<CommandSourceStack> executioncontext = CURRENT_EXECUTION_CONTEXT.get();
      boolean flag = executioncontext == null;
      if (flag) {
         GameRules gamerules = p_312477_.getLevel().getGameRules();
         int i = Math.max(1, gamerules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH));
         int j = gamerules.get(GameRules.MAX_COMMAND_FORKS);

         try (ExecutionContext<CommandSourceStack> executioncontext1 = new ExecutionContext<>(i, j, Profiler.get())) {
            CURRENT_EXECUTION_CONTEXT.set(executioncontext1);
            p_313031_.accept(executioncontext1);
            executioncontext1.runCommandQueue();
         } finally {
            CURRENT_EXECUTION_CONTEXT.set(null);
         }
      } else {
         p_313031_.accept(executioncontext);
      }
   }

   public void sendCommands(ServerPlayer p_82096_) {
      Map<CommandNode<CommandSourceStack>, CommandNode<CommandSourceStack>> map = new HashMap<>();
      RootCommandNode<CommandSourceStack> rootcommandnode = new RootCommandNode();
      map.put(this.dispatcher.getRoot(), rootcommandnode);
      fillUsableCommands(this.dispatcher.getRoot(), rootcommandnode, p_82096_.createCommandSourceStack(), map);
      p_82096_.connection.send(new ClientboundCommandsPacket(rootcommandnode, COMMAND_NODE_INSPECTOR));
   }

   private static <S> void fillUsableCommands(CommandNode<S> p_82113_, CommandNode<S> p_82114_, S p_408190_, Map<CommandNode<S>, CommandNode<S>> p_82116_) {
      for (CommandNode<S> commandnode : p_82113_.getChildren()) {
         if (commandnode.canUse(p_408190_)) {
            ArgumentBuilder<S, ?> argumentbuilder = commandnode.createBuilder();
            if (argumentbuilder.getRedirect() != null) {
               argumentbuilder.redirect(p_82116_.get(argumentbuilder.getRedirect()));
            }

            CommandNode<S> commandnode1 = argumentbuilder.build();
            p_82116_.put(commandnode, commandnode1);
            p_82114_.addChild(commandnode1);
            if (!commandnode.getChildren().isEmpty()) {
               fillUsableCommands(commandnode, commandnode1, p_408190_, p_82116_);
            }
         }
      }
   }

   public static LiteralArgumentBuilder<CommandSourceStack> literal(String p_82128_) {
      return LiteralArgumentBuilder.literal(p_82128_);
   }

   public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String p_82130_, ArgumentType<T> p_82131_) {
      return RequiredArgumentBuilder.argument(p_82130_, p_82131_);
   }

   public static Predicate<String> createValidator(Commands.ParseFunction p_82121_) {
      return p_82124_ -> {
         try {
            p_82121_.parse(new StringReader(p_82124_));
            return true;
         } catch (CommandSyntaxException commandsyntaxexception) {
            return false;
         }
      };
   }

   public CommandDispatcher<CommandSourceStack> getDispatcher() {
      return this.dispatcher;
   }

   public static <S> void validateParseResults(ParseResults<S> p_311833_) throws CommandSyntaxException {
      CommandSyntaxException commandsyntaxexception = getParseException(p_311833_);
      if (commandsyntaxexception != null) {
         throw commandsyntaxexception;
      }
   }

   public static <S> @Nullable CommandSyntaxException getParseException(ParseResults<S> p_82098_) {
      if (!p_82098_.getReader().canRead()) {
         return null;
      } else if (p_82098_.getExceptions().size() == 1) {
         return (CommandSyntaxException)p_82098_.getExceptions().values().iterator().next();
      } else {
         return p_82098_.getContext().getRange().isEmpty()
            ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(p_82098_.getReader())
            : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(p_82098_.getReader());
      }
   }

   public static CommandBuildContext createValidationContext(final HolderLookup.Provider p_256243_) {
      return new CommandBuildContext() {
         @Override
         public FeatureFlagSet enabledFeatures() {
            return FeatureFlags.REGISTRY.allFlags();
         }

         @Override
         public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
            return p_256243_.listRegistryKeys();
         }

         @Override
         public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_407321_) {
            return p_256243_.lookup(p_407321_).map(this::createLookup);
         }

         private <T> HolderLookup.RegistryLookup.Delegate<T> createLookup(final HolderLookup.RegistryLookup<T> p_407362_) {
            return new HolderLookup.RegistryLookup.Delegate<T>() {
               @Override
               public HolderLookup.RegistryLookup<T> parent() {
                  return p_407362_;
               }

               @Override
               public Optional<HolderSet.Named<T>> get(TagKey<T> p_406208_) {
                  return Optional.of(this.getOrThrow(p_406208_));
               }

               @Override
               public HolderSet.Named<T> getOrThrow(TagKey<T> p_410402_) {
                  Optional<HolderSet.Named<T>> optional = this.parent().get(p_410402_);
                  return optional.orElseGet(() -> HolderSet.emptyNamed(this.parent(), p_410402_));
               }
            };
         }
      };
   }

   public static void validate() {
      CommandBuildContext commandbuildcontext = createValidationContext(VanillaRegistries.createLookup());
      CommandDispatcher<CommandSourceStack> commanddispatcher = new Commands(Commands.CommandSelection.ALL, commandbuildcontext).getDispatcher();
      RootCommandNode<CommandSourceStack> rootcommandnode = commanddispatcher.getRoot();
      commanddispatcher.findAmbiguities(
         (p_230947_, p_230948_, p_230949_, p_230950_) -> LOGGER.warn(
            "Ambiguity between arguments {} and {} with inputs: {}",
            new Object[]{commanddispatcher.getPath(p_230948_), commanddispatcher.getPath(p_230949_), p_230950_}
         )
      );
      Set<ArgumentType<?>> set = ArgumentUtils.findUsedArgumentTypes(rootcommandnode);
      Set<ArgumentType<?>> set1 = set.stream().filter(p_325582_ -> !ArgumentTypeInfos.isClassRecognized(p_325582_.getClass())).collect(Collectors.toSet());
      if (!set1.isEmpty()) {
         LOGGER.warn(
            "Missing type registration for following arguments:\n {}", set1.stream().map(p_325583_ -> "\t" + p_325583_).collect(Collectors.joining(",\n"))
         );
         throw new IllegalStateException("Unregistered argument types");
      }
   }

   public static <T extends PermissionSetSupplier> PermissionProviderCheck<T> hasPermission(PermissionCheck p_459730_) {
      return new PermissionProviderCheck<>(p_459730_);
   }

   public static CommandSourceStack createCompilationContext(PermissionSet p_452426_) {
      return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, p_452426_, "", CommonComponents.EMPTY, null, null);
   }

   public enum CommandSelection {
      ALL(true, true),
      DEDICATED(false, true),
      INTEGRATED(true, false);

      final boolean includeIntegrated;
      final boolean includeDedicated;

      CommandSelection(final boolean p_82151_, final boolean p_82152_) {
         this.includeIntegrated = p_82151_;
         this.includeDedicated = p_82152_;
      }
   }

   @FunctionalInterface
   public interface ParseFunction {
      void parse(StringReader var1) throws CommandSyntaxException;
   }
}
