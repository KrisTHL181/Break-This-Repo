package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
   private static final CommandResponseTracker.MessagesWithArgs<LivingEntity, MobEffect, Integer> RESPONSE_GIVE = CommandResponseTracker.messages(
      ERROR_GIVE_FAILED,
      (entity, var1, effect, duration) -> Component.translatable(
         "commands.effect.give.success.single", effect.getDisplayName(), entity.getDisplayName(), duration / 20
      ),
      (entityCount, var1, effect, duration) -> Component.translatable(
         "commands.effect.give.success.multiple", effect.getDisplayName(), entityCount, duration / 20
      )
   );
   private static final CommandResponseTracker.Messages<Entity> RESPONSE_CLEAR_ALL = CommandResponseTracker.messages(
      ERROR_CLEAR_EVERYTHING_FAILED,
      (entity, var1) -> Component.translatable("commands.effect.clear.everything.success.single", entity.getDisplayName()),
      (entityCount, var1) -> Component.translatable("commands.effect.clear.everything.success.multiple", entityCount)
   );
   private static final CommandResponseTracker.MessagesWithArg<Entity, MobEffect> RESPONSE_CLEAR_SINGLE = CommandResponseTracker.messages(
      ERROR_CLEAR_SPECIFIC_FAILED,
      (entity, var1, effect) -> Component.translatable("commands.effect.clear.specific.success.single", effect.getDisplayName(), entity.getDisplayName()),
      (entityCount, var1, effect) -> Component.translatable("commands.effect.clear.specific.success.multiple", effect.getDisplayName(), entityCount)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("effect")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("clear")
                        .executes(
                           c -> clearEffects((CommandSourceStack)c.getSource(), ImmutableList.of(((CommandSourceStack)c.getSource()).getEntityOrException()))
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                              .executes(c -> clearEffects((CommandSourceStack)c.getSource(), EntityArgument.getEntities(c, "targets"))))
                           .then(
                              Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT))
                                 .executes(
                                    c -> clearEffect(
                                       (CommandSourceStack)c.getSource(), EntityArgument.getEntities(c, "targets"), ResourceArgument.getMobEffect(c, "effect")
                                    )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("give")
                  .then(
                     Commands.argument("targets", EntityArgument.entities())
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "effect", ResourceArgument.resource(context, Registries.MOB_EFFECT)
                                    )
                                    .executes(
                                       c -> giveEffect(
                                          (CommandSourceStack)c.getSource(),
                                          EntityArgument.getEntities(c, "targets"),
                                          ResourceArgument.getMobEffect(c, "effect"),
                                          null,
                                          0,
                                          true
                                       )
                                    ))
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                          .executes(
                                             c -> giveEffect(
                                                (CommandSourceStack)c.getSource(),
                                                EntityArgument.getEntities(c, "targets"),
                                                ResourceArgument.getMobEffect(c, "effect"),
                                                IntegerArgumentType.getInteger(c, "seconds"),
                                                0,
                                                true
                                             )
                                          ))
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                .executes(
                                                   c -> giveEffect(
                                                      (CommandSourceStack)c.getSource(),
                                                      EntityArgument.getEntities(c, "targets"),
                                                      ResourceArgument.getMobEffect(c, "effect"),
                                                      IntegerArgumentType.getInteger(c, "seconds"),
                                                      IntegerArgumentType.getInteger(c, "amplifier"),
                                                      true
                                                   )
                                                ))
                                             .then(
                                                Commands.argument("hideParticles", BoolArgumentType.bool())
                                                   .executes(
                                                      c -> giveEffect(
                                                         (CommandSourceStack)c.getSource(),
                                                         EntityArgument.getEntities(c, "targets"),
                                                         ResourceArgument.getMobEffect(c, "effect"),
                                                         IntegerArgumentType.getInteger(c, "seconds"),
                                                         IntegerArgumentType.getInteger(c, "amplifier"),
                                                         !BoolArgumentType.getBool(c, "hideParticles")
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("infinite")
                                       .executes(
                                          c -> giveEffect(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getEntities(c, "targets"),
                                             ResourceArgument.getMobEffect(c, "effect"),
                                             -1,
                                             0,
                                             true
                                          )
                                       ))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                             .executes(
                                                c -> giveEffect(
                                                   (CommandSourceStack)c.getSource(),
                                                   EntityArgument.getEntities(c, "targets"),
                                                   ResourceArgument.getMobEffect(c, "effect"),
                                                   -1,
                                                   IntegerArgumentType.getInteger(c, "amplifier"),
                                                   true
                                                )
                                             ))
                                          .then(
                                             Commands.argument("hideParticles", BoolArgumentType.bool())
                                                .executes(
                                                   c -> giveEffect(
                                                      (CommandSourceStack)c.getSource(),
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
      int duration = computeDurationInTicks(seconds, effect);
      CommandResponseTracker<LivingEntity> tracker = CommandResponseTracker.create();

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity livingEntity) {
            MobEffectInstance instance = new MobEffectInstance(effectHolder, duration, amplifier, false, particles);
            tracker.track(livingEntity, livingEntity.addEffect(instance, source.getEntity()));
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_GIVE, effect, duration);
   }

   private static int computeDurationInTicks(final @Nullable Integer seconds, final MobEffect effect) {
      if (seconds != null) {
         if (effect.isInstantaneous()) {
            return seconds;
         } else {
            return seconds == -1 ? -1 : seconds * 20;
         }
      } else {
         return effect.isInstantaneous() ? 1 : 600;
      }
   }

   private static int clearEffects(final CommandSourceStack source, final Collection<? extends Entity> entities) throws CommandSyntaxException {
      CommandResponseTracker<LivingEntity> tracker = CommandResponseTracker.create();

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity livingEntity) {
            tracker.track(livingEntity, livingEntity.removeAllEffects());
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_CLEAR_ALL);
   }

   private static int clearEffect(final CommandSourceStack source, final Collection<? extends Entity> entities, final Holder<MobEffect> effectHolder) throws CommandSyntaxException {
      MobEffect effect = effectHolder.value();
      CommandResponseTracker<LivingEntity> tracker = CommandResponseTracker.create();

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity livingEntity) {
            tracker.track(livingEntity, livingEntity.removeEffect(effectHolder));
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_CLEAR_SINGLE, effect);
   }
}
