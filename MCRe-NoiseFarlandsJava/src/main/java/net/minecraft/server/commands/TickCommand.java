package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
    private static final float MAX_TICKRATE = 10000.0F;
    private static final String DEFAULT_TICKRATE = String.valueOf(20);

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tick")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("query").executes(c -> tickQuery(c.getSource())))
                .then(
                    Commands.literal("rate")
                        .then(
                            Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, b))
                                .executes(c -> setTickingRate(c.getSource(), FloatArgumentType.getFloat(c, "rate")))
                        )
                )
                .then(
                    Commands.literal("step")
                        .executes(c -> step(c.getSource(), 1))
                        .then(Commands.literal("stop").executes(c -> stopStepping(c.getSource())))
                        .then(
                            Commands.argument("time", TimeArgument.time(1))
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, b))
                                .executes(c -> step(c.getSource(), IntegerArgumentType.getInteger(c, "time")))
                        )
                )
                .then(
                    Commands.literal("sprint")
                        .then(Commands.literal("stop").executes(c -> stopSprinting(c.getSource())))
                        .then(
                            Commands.argument("time", TimeArgument.time(1))
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, b))
                                .executes(c -> sprint(c.getSource(), IntegerArgumentType.getInteger(c, "time")))
                        )
                )
                .then(Commands.literal("unfreeze").executes(c -> setFreeze(c.getSource(), false)))
                .then(Commands.literal("freeze").executes(c -> setFreeze(c.getSource(), true)))
        );
    }

    private static String nanosToMilisString(final long nanos) {
        return String.format(Locale.ROOT, "%.1f", (float)nanos / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND);
    }

    private static int setTickingRate(final CommandSourceStack source, final float rate) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        manager.setTickRate(rate);
        String tickRateString = String.format(Locale.ROOT, "%.1f", rate);
        source.sendSuccess(() -> Component.translatable("commands.tick.rate.success", tickRateString), true);
        return (int)rate;
    }

    private static int tickQuery(final CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        String busyTime = nanosToMilisString(source.getServer().getAverageTickTimeNanos());
        float tickRate = manager.tickrate();
        String tickRateString = String.format(Locale.ROOT, "%.1f", tickRate);
        if (manager.isSprinting()) {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
            source.sendSuccess(() -> Component.translatable("commands.tick.query.rate.sprinting", tickRateString, busyTime), false);
        } else {
            if (manager.isFrozen()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
            } else if (manager.nanosecondsPerTick() < source.getServer().getAverageTickTimeNanos()) {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
            }

            String milliSecondsPerTickTarget = nanosToMilisString(manager.nanosecondsPerTick());
            source.sendSuccess(() -> Component.translatable("commands.tick.query.rate.running", tickRateString, busyTime, milliSecondsPerTickTarget), false);
        }

        long[] samples = Arrays.copyOf(source.getServer().getTickTimesNanos(), source.getServer().getTickTimesNanos().length);
        Arrays.sort(samples);
        String p50 = nanosToMilisString(samples[samples.length / 2]);
        String p95 = nanosToMilisString(samples[(int)(samples.length * 0.95)]);
        String p99 = nanosToMilisString(samples[(int)(samples.length * 0.99)]);
        source.sendSuccess(() -> Component.translatable("commands.tick.query.percentiles", p50, p95, p99, samples.length), false);
        return (int)tickRate;
    }

    private static int sprint(final CommandSourceStack source, final int time) {
        boolean interrupted = source.getServer().tickRateManager().requestGameToSprint(time);
        if (interrupted) {
            source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
        }

        source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
        return 1;
    }

    private static int setFreeze(final CommandSourceStack source, final boolean freeze) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        if (freeze) {
            if (manager.isSprinting()) {
                manager.stopSprinting();
            }

            if (manager.isSteppingForward()) {
                manager.stopStepping();
            }
        }

        manager.setFrozen(freeze);
        if (freeze) {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
        }

        return freeze ? 1 : 0;
    }

    private static int step(final CommandSourceStack source, final int advance) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stepGameIfPaused(advance);
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.step.success", advance), true);
        } else {
            source.sendFailure(Component.translatable("commands.tick.step.fail"));
        }

        return 1;
    }

    private static int stopStepping(final CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stopStepping();
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
            return 0;
        }
    }

    private static int stopSprinting(final CommandSourceStack source) {
        ServerTickRateManager manager = source.getServer().tickRateManager();
        boolean success = manager.stopSprinting();
        if (success) {
            source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
            return 0;
        }
    }
}