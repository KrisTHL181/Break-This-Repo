package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class EffectCommands {
    private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.give.failed"));
    private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.effect.clear.everything.failed")
    );
    private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.effect.clear.specific.failed")
    );

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("effect")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("clear")
                        .executes(c -> clearEffects(c.getSource(), ImmutableList.of(c.getSource().getEntityOrException())))
                        .then(
                            Commands.argument("targets", EntityArgument.entities())
                                .executes(c -> clearEffects(c.getSource(), EntityArgument.getEntities(c, "targets")))
                                .then(
                                    Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT))
                                        .executes(
                                            c -> clearEffect(
                                                c.getSource(), EntityArgument.getEntities(c, "targets"), ResourceArgument.getMobEffect(c, "effect")
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("give")
                        .then(
                            Commands.argument("targets", EntityArgument.entities())
                                .then(
                                    Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT))
                                        .executes(
                                            c -> giveEffect(
                                                c.getSource(),
                                                EntityArgument.getEntities(c, "targets"),
                                                ResourceArgument.getMobEffect(c, "effect"),
                                                null,
                                                0,
                                                true
                                            )
                                        )
                                        .then(
                                            Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                                .executes(
                                                    c -> giveEffect(
                                                        c.getSource(),
                                                        EntityArgument.getEntities(c, "targets"),
                                                        ResourceArgument.getMobEffect(c, "effect"),
                                                        IntegerArgumentType.getInteger(c, "seconds"),
                                                        0,
                                                        true
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                        .executes(
                                                            c -> giveEffect(
                                                                c.getSource(),
                                                                EntityArgument.getEntities(c, "targets"),
                                                                ResourceArgument.getMobEffect(c, "effect"),
                                                                IntegerArgumentType.getInteger(c, "seconds"),
                                                                IntegerArgumentType.getInteger(c, "amplifier"),
                                                                true
                                                            )
                                                        )
                                                        .then(
                                                            Commands.argument("hideParticles", BoolArgumentType.bool())
                                                                .executes(
                                                                    c -> giveEffect(
                                                                        c.getSource(),
                                                                        EntityArgument.getEntities(c, "targets"),
                                                                        ResourceArgument.getMobEffect(c, "effect"),
                                                                        IntegerArgumentType.getInteger(c, "seconds"),
                                                                        IntegerArgumentType.getInteger(c, "amplifier"),
                                                                        !BoolArgumentType.getBool(c, "hideParticles")
                                                                    )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("infinite")
                                                .executes(
                                                    c -> giveEffect(
                                                        c.getSource(),
                                                        EntityArgument.getEntities(c, "targets"),
                                                        ResourceArgument.getMobEffect(c, "effect"),
                                                        -1,
                                                        0,
                                                        true
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                        .executes(
                                                            c -> giveEffect(
                                                                c.getSource(),
                                                                EntityArgument.getEntities(c, "targets"),
                                                                ResourceArgument.getMobEffect(c, "effect"),
                                                                -1,
                                                                IntegerArgumentType.getInteger(c, "amplifier"),
                                                                true
                                                            )
                                                        )
                                                        .then(
                                                            Commands.argument("hideParticles", BoolArgumentType.bool())
                                                                .executes(
                                                                    c -> giveEffect(
                                                                        c.getSource(),
                                                                        EntityArgument.getEntities(c, "targets"),
                                                                        ResourceArgument.getMobEffect(c, "effect"),
                                                                        -1,
                                                                        IntegerArgumentType.getInteger(c, "amplifier"),
                                                                        !BoolArgumentType.getBool(c, "hideParticles")
                                                                    )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int giveEffect(
        final CommandSourceStack source,
        final Collection<? extends Entity> entities,
        final Holder<MobEffect> effectHolder,
        final @Nullable Integer seconds,
        final int amplifier,
        final boolean particles
    ) throws CommandSyntaxException {
        MobEffect effect = effectHolder.value();
        int count = 0;
        int duration;
        if (seconds != null) {
            if (effect.isInstantaneous()) {
                duration = seconds;
            } else if (seconds == -1) {
                duration = -1;
            } else {
                duration = seconds * 20;
            }
        } else if (effect.isInstantaneous()) {
            duration = 1;
        } else {
            duration = 600;
        }

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                MobEffectInstance instance = new MobEffectInstance(effectHolder, duration, amplifier, false, particles);
                if (livingEntity.addEffect(instance, source.getEntity())) {
                    count++;
                }
            }
        }

        if (count == 0) {
            throw ERROR_GIVE_FAILED.create();
        }

        if (entities.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.effect.give.success.single", effect.getDisplayName(), entities.iterator().next().getDisplayName(), duration / 20
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.effect.give.success.multiple", effect.getDisplayName(), entities.size(), duration / 20), true
            );
        }

        return count;
    }

    private static int clearEffects(final CommandSourceStack source, final Collection<? extends Entity> entities) throws CommandSyntaxException {
        int count = 0;

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.removeAllEffects()) {
                count++;
            }
        }

        if (count == 0) {
            throw ERROR_CLEAR_EVERYTHING_FAILED.create();
        }

        if (entities.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable("commands.effect.clear.everything.success.single", entities.iterator().next().getDisplayName()), true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.everything.success.multiple", entities.size()), true);
        }

        return count;
    }

    private static int clearEffect(final CommandSourceStack source, final Collection<? extends Entity> entities, final Holder<MobEffect> effectHolder) throws CommandSyntaxException {
        MobEffect effect = effectHolder.value();
        int count = 0;

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.removeEffect(effectHolder)) {
                count++;
            }
        }

        if (count == 0) {
            throw ERROR_CLEAR_SPECIFIC_FAILED.create();
        }

        if (entities.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.effect.clear.specific.success.single", effect.getDisplayName(), entities.iterator().next().getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.effect.clear.specific.success.multiple", effect.getDisplayName(), entities.size()), true);
        }

        return count;
    }
}