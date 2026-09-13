package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;

public class TimeCommand {
   private static final DynamicCommandExceptionType ERROR_NO_DEFAULT_CLOCK = new DynamicCommandExceptionType(
      dimension -> Component.translatableEscape("commands.time.no_default_clock", dimension)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_TIME_MARKER_FOUND = new Dynamic2CommandExceptionType(
      (clock, timeMarker) -> Component.translatableEscape("commands.time.no_time_marker_found", timeMarker, clock)
   );
   private static final Dynamic2CommandExceptionType ERROR_WRONG_TIMELINE_FOR_CLOCK = new Dynamic2CommandExceptionType(
      (clock, timeline) -> Component.translatableEscape("commands.time.wrong_timeline_for_clock", timeline, clock)
   );
   private static final Dynamic2CommandExceptionType ERROR_ALREADY_AT_TIME_MARKER = new Dynamic2CommandExceptionType(
      (clock, marker) -> Component.translatableEscape("commands.time.set.already_at_time_marker", clock, marker)
   );
   private static final Dynamic2CommandExceptionType ERROR_ALREADY_AT_TIME = new Dynamic2CommandExceptionType(
      (clock, time) -> Component.translatableEscape("commands.time.set.already_at_time", clock, time)
   );
   private static final DynamicCommandExceptionType ERROR_ALREADY_PAUSED = new DynamicCommandExceptionType(
      clock -> Component.translatableEscape("commands.time.pause.already_paused", clock)
   );
   private static final DynamicCommandExceptionType ERROR_ALREADY_RUNNING = new DynamicCommandExceptionType(
      clock -> Component.translatableEscape("commands.time.pause.already_running", clock)
   );
   private static final Dynamic2CommandExceptionType ERROR_ALREADY_SAME_RATE = new Dynamic2CommandExceptionType(
      (clock, rate) -> Component.translatableEscape("commands.time.rate.already_same", clock, rate)
   );
   private static final int MAX_CLOCK_RATE = 1000;

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      var baseCommand = (LiteralArgumentBuilder<CommandSourceStack> & ArgumentBuilder)Commands.literal("time")
         .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
      dispatcher.register(addClockNodes(context, baseCommand, c -> getDefaultClock((CommandSourceStack)c.getSource())));
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)baseCommand.then(
               Commands.literal("query").then(Commands.literal("gametime").executes(c -> queryGameTime((CommandSourceStack)c.getSource())))
            ))
            .then(
               Commands.literal("of")
                  .then(
                     addClockNodes(
                        context,
                        Commands.argument("clock", ResourceArgument.resource(context, Registries.WORLD_CLOCK)),
                        c -> ResourceArgument.getClock(c, "clock")
                     )
                  )
            )
      );
   }

   private static <A extends ArgumentBuilder<CommandSourceStack, A>> A addClockNodes(
      final CommandBuildContext context, final A node, final TimeCommand.ClockGetter clockGetter
   ) {
      return (A)node.then(
            ((LiteralArgumentBuilder)Commands.literal("set")
                  .then(
                     Commands.argument("time", TimeArgument.time())
                        .executes(c -> setTotalTicks((CommandSourceStack)c.getSource(), clockGetter.getClock(c), IntegerArgumentType.getInteger(c, "time")))
                  ))
               .then(
                  Commands.argument("timemarker", IdentifierArgument.id())
                     .suggests((c, p) -> suggestTimeMarkers((CommandSourceStack)c.getSource(), p, clockGetter.getClock(c)))
                     .executes(
                        c -> setTimeToTimeMarker(
                           (CommandSourceStack)c.getSource(),
                           clockGetter.getClock(c),
                           ResourceKey.create(ClockTimeMarkers.ROOT_ID, IdentifierArgument.getId(c, "timemarker"))
                        )
                     )
               )
         )
         .then(
            Commands.literal("add")
               .then(
                  Commands.argument("time", TimeArgument.time(Integer.MIN_VALUE))
                     .executes(c -> addTime((CommandSourceStack)c.getSource(), clockGetter.getClock(c), IntegerArgumentType.getInteger(c, "time")))
               )
         )
         .then(Commands.literal("pause").executes(c -> setPaused((CommandSourceStack)c.getSource(), clockGetter.getClock(c), true)))
         .then(Commands.literal("resume").executes(c -> setPaused((CommandSourceStack)c.getSource(), clockGetter.getClock(c), false)))
         .then(
            Commands.literal("rate")
               .then(
                  Commands.argument("rate", FloatArgumentType.floatArg(1.0E-5F, 1000.0F))
                     .executes(c -> setRate((CommandSourceStack)c.getSource(), clockGetter.getClock(c), FloatArgumentType.getFloat(c, "rate")))
               )
         )
         .then(
            ((LiteralArgumentBuilder)Commands.literal("query")
                  .then(Commands.literal("time").executes(c -> queryTime((CommandSourceStack)c.getSource(), clockGetter.getClock(c)))))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("timeline", ResourceArgument.resource(context, Registries.TIMELINE))
                        .suggests((c, p) -> suggestTimelines((CommandSourceStack)c.getSource(), p, clockGetter.getClock(c)))
                        .executes(
                           c -> queryTimelineTicks((CommandSourceStack)c.getSource(), clockGetter.getClock(c), ResourceArgument.getTimeline(c, "timeline"))
                        ))
                     .then(
                        Commands.literal("repetition")
                           .executes(
                              c -> queryTimelineRepetitions(
                                 (CommandSourceStack)c.getSource(), clockGetter.getClock(c), ResourceArgument.getTimeline(c, "timeline")
                              )
                           )
                     )
               )
         );
   }

   private static CompletableFuture<Suggestions> suggestTimeMarkers(
      final CommandSourceStack source, final SuggestionsBuilder builder, final Holder<WorldClock> clock
   ) {
      return SharedSuggestionProvider.suggestResource(source.getServer().clockManager().commandTimeMarkersForClock(clock).map(ResourceKey::identifier), builder);
   }

   private static CompletableFuture<Suggestions> suggestTimelines(
      final CommandSourceStack source, final SuggestionsBuilder builder, final Holder<WorldClock> clock
   ) {
      Stream<ResourceKey<Timeline>> timelines = source.registryAccess()
         .lookupOrThrow(Registries.TIMELINE)
         .listElements()
         .filter(timeline -> timeline.value().clock().equals(clock))
         .map(Holder.Reference::key);
      return SharedSuggestionProvider.suggestResource(timelines.map(ResourceKey::identifier), builder);
   }

   private static int queryGameTime(final CommandSourceStack source) {
      long gameTime = source.getLevel().getGameTime();
      source.sendSuccess(() -> Component.translatable("commands.time.query.gametime", gameTime), false);
      return wrapTime(gameTime);
   }

   private static int queryTime(final CommandSourceStack source, final Holder<WorldClock> clock) {
      ServerClockManager clockManager = source.getServer().clockManager();
      long totalTicks = clockManager.getInstance(clock).totalTicks();
      source.sendSuccess(() -> Component.translatable("commands.time.query.absolute", clock.getRegisteredName(), totalTicks), false);
      return wrapTime(totalTicks);
   }

   private static int queryTimelineTicks(final CommandSourceStack source, final Holder<WorldClock> clock, final Holder<Timeline> timeline) throws CommandSyntaxException {
      if (!clock.equals(timeline.value().clock())) {
         throw ERROR_WRONG_TIMELINE_FOR_CLOCK.create(clock.getRegisteredName(), timeline.getRegisteredName());
      }

      ServerClockManager clockManager = source.getServer().clockManager();
      long currentTicks = timeline.value().getCurrentTicks(clockManager);
      source.sendSuccess(() -> Component.translatable("commands.time.query.timeline", timeline.getRegisteredName(), currentTicks), false);
      return wrapTime(currentTicks);
   }

   private static int queryTimelineRepetitions(final CommandSourceStack source, final Holder<WorldClock> clock, final Holder<Timeline> timeline) throws CommandSyntaxException {
      if (!clock.equals(timeline.value().clock())) {
         throw ERROR_WRONG_TIMELINE_FOR_CLOCK.create(clock.getRegisteredName(), timeline.getRegisteredName());
      }

      ServerClockManager clockManager = source.getServer().clockManager();
      long repetitions = timeline.value().getPeriodCount(clockManager);
      source.sendSuccess(() -> Component.translatable("commands.time.query.timeline.repetitions", timeline.getRegisteredName(), repetitions), false);
      return wrapTime(repetitions);
   }

   private static int setTotalTicks(final CommandSourceStack source, final Holder<WorldClock> clock, final int totalTicks) throws CommandSyntaxException {
      ServerClockManager clockManager = source.getServer().clockManager();
      if (clockManager.getInstance(clock).totalTicks() == totalTicks) {
         throw ERROR_ALREADY_AT_TIME.create(clock.getRegisteredName(), totalTicks);
      }

      clockManager.setTotalTicks(clock, totalTicks);
      source.sendSuccess(() -> Component.translatable("commands.time.set.absolute", clock.getRegisteredName(), totalTicks), true);
      return totalTicks;
   }

   private static int addTime(final CommandSourceStack source, final Holder<WorldClock> clock, final int time) {
      ServerClockManager clockManager = source.getServer().clockManager();
      clockManager.addTicks(clock, time);
      long totalTicks = clockManager.getInstance(clock).totalTicks();
      source.sendSuccess(() -> Component.translatable("commands.time.set.absolute", clock.getRegisteredName(), totalTicks), true);
      return wrapTime(totalTicks);
   }

   private static int setTimeToTimeMarker(final CommandSourceStack source, final Holder<WorldClock> clock, final ResourceKey<ClockTimeMarker> timeMarkerId) throws CommandSyntaxException {
      ServerClockManager clockManager = source.getServer().clockManager();
      ServerClockManager.MoveResult moveResult = clockManager.moveToTimeMarker(clock, timeMarkerId);
      String clockName = clock.getRegisteredName();
      String timeMarkerName = timeMarkerId.identifier().toString();
      switch (moveResult) {
         case NO_TIME_MARKER_FOUND:
            throw ERROR_NO_TIME_MARKER_FOUND.create(clockName, timeMarkerName);
         case NOT_MOVED:
            throw ERROR_ALREADY_AT_TIME_MARKER.create(clockName, timeMarkerName);
         case MOVED:
            source.sendSuccess(() -> Component.translatable("commands.time.set.time_marker", clockName, timeMarkerName), true);
         default:
            return wrapTime(clockManager.getInstance(clock).totalTicks());
      }
   }

   private static int setPaused(final CommandSourceStack source, final Holder<WorldClock> clock, final boolean paused) throws CommandSyntaxException {
      ServerClockManager clockManager = source.getServer().clockManager();
      String clockName = clock.getRegisteredName();
      if (clockManager.getInstance(clock).isPaused() == paused) {
         if (paused) {
            throw ERROR_ALREADY_PAUSED.create(clockName);
         } else {
            throw ERROR_ALREADY_RUNNING.create(clockName);
         }
      } else {
         clockManager.setPaused(clock, paused);
         source.sendSuccess(() -> Component.translatable(paused ? "commands.time.pause" : "commands.time.resume", clockName), true);
         return 1;
      }
   }

   private static int setRate(final CommandSourceStack source, final Holder<WorldClock> clock, final float rate) throws CommandSyntaxException {
      ServerClockManager clockManager = source.getServer().clockManager();
      if (clockManager.getInstance(clock).rate() == rate) {
         throw ERROR_ALREADY_SAME_RATE.create(clock.getRegisteredName(), rate);
      }

      clockManager.setRate(clock, rate);
      source.sendSuccess(() -> Component.translatable("commands.time.rate", clock.getRegisteredName(), rate), true);
      return 1;
   }

   private static int wrapTime(final long ticks) {
      return Math.toIntExact(ticks % 2147483647L);
   }

   private static Holder<WorldClock> getDefaultClock(final CommandSourceStack source) throws CommandSyntaxException {
      Holder<DimensionType> dimensionType = source.getLevel().dimensionTypeRegistration();
      return dimensionType.value().defaultClock().orElseThrow(() -> ERROR_NO_DEFAULT_CLOCK.create(dimensionType.getRegisteredName()));
   }

   private interface ClockGetter {
      Holder<WorldClock> getClock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
   }
}
